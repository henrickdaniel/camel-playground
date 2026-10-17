package com.example.camel.routes;

import com.example.camel.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentOutboxRoute extends RouteBuilder {

    private final IdempotentRepository idempotentRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void configure() throws Exception {

        // 1. Poller que lê eventos pendentes da tabela Outbox
        from("jpa:com.example.camel.entity.OutboxEvent" +
                "?namedQuery=findPendingEvents" +
                "&consumeDelete=false" +
                "&delay=5000")
                .routeId("payment-outbox-poller-route")
                .log("[OUTBOX] Evento pendente encontrado ID: ${body.id}, AggregateId: ${body.aggregateId}")

                // 2. Filtro de idempotência utilizando o aggregateId
                .idempotentConsumer(simple("${body.aggregateId}"), idempotentRepository)
                .skipDuplicate(true)
                .log("[IDEMPOTENCIA] AggregateId ${body.aggregateId} é novo. Processando...")

                // 3. Encaminha para o dispatcher que envia para a fila de pagamento do ActiveMQ
                .to("direct:despacharParaFilaPagamento")

                // 4. Atualiza o status do Outbox para SENT e altera o status do Pedido para PENDENTE_PAGAMENTO
                .process(exchange -> {
                    OutboxEvent event = exchange.getIn().getBody(OutboxEvent.class);
                    // A entidade é gerenciada pela transação do consumidor JPA, que grava o status no commit.
                    // Um save() aqui abriria outra conexão e travaria na mesma linha (deadlock).
                    event.setStatus("SENT");

                    // Aqui podemos também atualizar o status do pedido associado no banco de dados se necessário,
                    // ou deixar que o componente que consome a fila faça isso.
                })
                .log("[OUTBOX] Evento ID ${body.id} processado e marcado como SENT!")
                .end();

        // 5. Rota responsável por despachar a mensagem para a fila do ActiveMQ
        from("direct:despacharParaFilaPagamento")
                .routeId("payment-broker-dispatcher-route")
                .log("    [BROKER] Enviando evento de pagamento para a fila ActiveMQ... Payload: ${body.payload}")
                .setProperty("outboxEvent", body())
                .process(exchange -> {
                    OutboxEvent event = exchange.getIn().getBody(OutboxEvent.class);
                    JsonNode pedido = objectMapper.readTree(event.getPayload());

                    Map<String, Object> pagamento = new LinkedHashMap<>();
                    pagamento.put("pedidoId", event.getAggregateId());
                    pagamento.put("valor", pedido.path("valor").asDouble());
                    pagamento.put("descricao", pedido.path("descricao").asString());

                    exchange.getIn().setBody(objectMapper.writeValueAsString(pagamento));
                })
                // Exemplo de envio para o ActiveMQ (ajuste o nome da fila conforme seu componente jms/activemq)
                .to("activemq:queue:pagamentos.pendentes")
                .setBody(exchangeProperty("outboxEvent"));
    }
}