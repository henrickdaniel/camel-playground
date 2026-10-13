package com.example.camel.routes;

import com.example.camel.entity.OutboxEvent;
import com.example.camel.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRoute extends RouteBuilder {

    private final OutboxEventRepository outboxEventRepository;
    private final IdempotentRepository idempotentRepository;

    @Override
    public void configure() throws Exception {

        from("jpa:com.example.camel.entity.OutboxEvent" +
                "?namedQuery=findPendingEvents" +
                "&consumeDelete=false" +
                "&delay=5000")
                .routeId("outbox-poller-route")
                .log("[OUTBOX] Evento pendente encontrado ID: ${body.id}, AggregateId: ${body.aggregateId}")

                // Filtro de idempotência utilizando o aggregateId como chave
                .idempotentConsumer(simple("${body.aggregateId}"), idempotentRepository)
                .skipDuplicate(true)
                .log("[IDEMPOTENCIA] AggregateId ${body.aggregateId} é novo. Processando...")

                .to("direct:despacharMensagem")

                .process(exchange -> {
                    OutboxEvent event = exchange.getIn().getBody(OutboxEvent.class);
                    event.setStatus("SENT");
                    outboxEventRepository.save(event);
                })
                .log("[OUTBOX] Evento ID ${body.id} marcado como SENT com sucesso!")
                .end();

        from("direct:despacharMensagem")
                .routeId("message-dispatcher-route")
                .log("   [DISPATCHER] Enviando payload para o broker... ${body.payload}");
    }
}