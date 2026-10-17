package com.example.camel.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentConsumerRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Formato JSON para marshaling/unmarshaling das respostas da API
        JacksonDataFormat jsonMapFormat = new JacksonDataFormat(Map.class);

        // 1. Consome da fila de pagamentos pendentes do ActiveMQ
        from("activemq:queue:pagamentos.pendentes")
                .routeId("payment-queue-consumer-route")
                .log("[CONSUMER] Mensagem recebida da fila ActiveMQ. Processando pagamento...")

                // Armazena o payload original do pedido em uma propriedade para uso posterior
                .setProperty("pedidoPayload", body())

                // ----------------------------------------------------
                // ETAPA A: Obter o Token JWT na API de Pagamento
                // ----------------------------------------------------
                .log("[AUTH] Solicitando token OAuth2 na Payment API...")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("client-id", constant("cliente-parceiro-123"))
                .setHeader("api-key", constant("secret-key-xyz"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(constant("")) // Corpo vazio, credenciais voam nos headers

                // Chamada HTTP para o endpoint de token (porta 8081)
                .to("http://localhost:8081/oauth/token?bridgeEndpoint=true")

                // Converte a resposta JSON em um Map para extrair o token
                .unmarshal(jsonMapFormat)
                .process(exchange -> {
                    Map<String, Object> responseBody = exchange.getIn().getBody(Map.class);
                    String accessToken = (String) responseBody.get("access_token");

                    if (accessToken == null) {
                        throw new RuntimeException("Falha ao obter access_token da API de Pagamento.");
                    }

                    // Salva o token em uma propriedade do Exchange
                    exchange.setProperty("bearerToken", accessToken);
                })
                .log("[AUTH] Token JWT obtido com sucesso!")

                // ----------------------------------------------------
                // ETAPA B: Enviar o Pagamento para o Endpoint Protegido
                // ----------------------------------------------------
                .log("[PAYMENT] Enviando requisição de pagamento para a API externa...")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .log("[PAYMENT] Usando Bearer Token: ${exchangeProperty.bearerToken}")
                .setHeader("Authorization", simple("Bearer ${exchangeProperty.bearerToken}"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))

                // Restaura o payload original do pedido que estava guardado na propriedade
                .setBody(exchange -> exchange.getProperty("pedidoPayload"))

                // Chamada HTTP para o endpoint protegido de pagamentos
                .to("http://localhost:8081/api/v1/payments?bridgeEndpoint=true")

                // Loga a resposta final de sucesso da API de Pagamento
                .log("[PAYMENT] Pagamento processado com sucesso pela API externa! Resposta: ${body}")

                // Aqui, futuramente, podemos disparar uma etapa para atualizar o status do pedido local
                // ou aguardar o Webhook chegar.
                .end();
    }
}