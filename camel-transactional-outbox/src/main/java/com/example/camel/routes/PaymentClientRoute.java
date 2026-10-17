package com.example.camel.outbox.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentClientRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Formato JSON para marshaling/unmarshaling
        JacksonDataFormat jsonMapFormat = new JacksonDataFormat(Map.class);

        // Rota acionada internamente para processar o pagamento de um pedido
        from("direct:enviar-pagamento-externo")
                .routeId("payment-client-integration-route")
                .log("Iniciando requisição de token OAuth2 na Payment API...")

                // 1. Configura headers e chama o endpoint /oauth/token
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("client-id", constant("cliente-parceiro-123"))
                .setHeader("api-key", constant("secret-key-xyz"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(constant(Map.of())) // Corpo vazio, pois credenciais vão no header

                // Dispara chamada HTTP para obter o token
                .to("http://localhost:8081/oauth/token?bridgeEndpoint=true")

                // Converte a resposta JSON do token para um Map
                .unmarshal(jsonMapFormat)
                .log("Token JWT obtido com sucesso. Extraindo access_token...")

                // Extrai o token gerado e armazena em uma Exchange Property para uso posterior
                .process(exchange -> {
                    Map<String, Object> responseBody = exchange.getIn().getBody(Map.class);
                    String accessToken = (String) responseBody.get("access_token");
                    exchange.setProperty("bearerToken", accessToken);
                })

                // Recupera o payload original do pedido que estava transitando na rota
                .log("Encaminhando pagamento para a API externa com o Bearer Token...")

                // 2. Configura a chamada para o endpoint protegido /api/v1/payments
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty.bearerToken}"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))

                // O corpo original do pedido (ex: dados do outbox) é mantido ou redefinido conforme necessário
                // .setBody(simple("${exchangeProperty.pedidoPayload}"))

                // Dispara chamada HTTP para o endpoint protegido
                .to("http://localhost:8081/api/v1/payments?bridgeEndpoint=true")

                .log("Resposta da API de Pagamento recebida com sucesso: ${body}");
    }
}