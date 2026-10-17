package com.example.camel.routes;

import com.example.camel.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentWebhookRoute extends RouteBuilder {

    private final PedidoRepository pedidoRepository;
    private final StringRedisTemplate redisTemplate; // Redis template do Spring Boot

    @Override
    public void configure() throws Exception {

        JacksonDataFormat jsonMapFormat = new JacksonDataFormat(Map.class);

        from("direct:payment-webhook")
                .routeId("payment-webhook-receiver-route")
                .log("[WEBHOOK] Notificação de pagamento recebida da API externa!")

                // 1. Converte o payload do webhook para Map
                .unmarshal(jsonMapFormat)

                // 2. Extrai os identificadores principais
                .process(exchange -> {
                    Map<String, Object> body = exchange.getIn().getBody(Map.class);

                    String paymentId = (String) body.get("paymentId"); // ID único da transação de pagamento
                    String statusPagamento = (String) body.get("status");

                    Map<String, Object> detalhes = (Map<String, Object>) body.get("detalhes");
                    String pedidoId = (detalhes != null) ? String.valueOf(detalhes.get("pedidoId")) : null;

                    if (paymentId == null || pedidoId == null) {
                        throw new RuntimeException("Payload do Webhook inválido: paymentId ou pedidoId ausentes.");
                    }

                    exchange.setProperty("paymentId", paymentId);
                    exchange.setProperty("pedidoId", pedidoId);
                    exchange.setProperty("statusPagamento", statusPagamento);
                })

                // 3. BLINDAGEM DE IDEMPOTÊNCIA COM REDIS (SETNX com TTL de 24 horas)
                .process(exchange -> {
                    String paymentId = exchange.getProperty("paymentId", String.class);
                    String redisKey = "webhook:processed:payment:" + paymentId;

                    // Tenta inserir a chave no Redis. O setIfAbsent (SETNX) retorna true se a chave NÃO existia.
                    // Se retornar false, significa que este webhook já foi processado anteriormente!
                    Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSANDO", Duration.ofHours(24));

                    if (Boolean.FALSE.equals(isNew)) {
                        log.warn("[IDEMPOTENCIA-REDIS] Webhook duplicado detectado para o paymentId: {}. Ignorando processamento.", paymentId);
                        exchange.setProperty("webhookDuplicado", true);
                    } else {
                        exchange.setProperty("webhookDuplicado", false);
                    }
                })

                // 4. Se for duplicado, pula direto para a resposta HTTP 200 OK (sem reprocessar o banco)
                .choice()
                .when(exchangeProperty("webhookDuplicado").isEqualTo(true))
                .log("[WEBHOOK] Retornando 200 OK para webhook duplicado sem alterar o estado.")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant(Map.of("mensagem", "Webhook já processado anteriormente (Idempotente)")))
                .stop() // Interrompe a execução da rota aqui

                .otherwise()
                // 5. Se for inédito, prossegue com a regra de negócio e atualização transacional no banco
                .log("[WEBHOOK] Webhook inédito. Atualizando status do Pedido ID: ${exchangeProperty.pedidoId}...")
                .process(exchange -> {
                    String pedidoIdStr = exchange.getProperty("pedidoId", String.class);
                    String statusPagamento = exchange.getProperty("statusPagamento", String.class);
                    Long pedidoId = Long.valueOf(pedidoIdStr);

                    pedidoRepository.findById(pedidoId).ifPresent(pedido -> {
                        if ("CONCLUIDO".equalsIgnoreCase(statusPagamento)) {
                            pedido.setStatus("PAGO");
                        } else {
                            pedido.setStatus("FALHA_PAGAMENTO");
                        }
                        pedidoRepository.save(pedido);
                    });
                })

                // 6. Responde com sucesso para o gateway de pagamento externo
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant(Map.of("mensagem", "Webhook processado com sucesso")))
                .log("[WEBHOOK] Status atualizado no banco com sucesso e HTTP 200 retornado.")
                .end();
    }
}