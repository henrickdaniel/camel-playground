package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ArtemisRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Consome da fila do ActiveMQ Artemis de forma assíncrona
        from("jms:queue:pedidos.fila")
                .routeId("artemis-consumer-route")
                .log(" [ARTEMIS CONSUMER] Mensagem capturada da fila: ${body}")
                .process(exchange -> {
                    // Simula uma regra de negócio ou processamento
                    Thread.sleep(500);
                })
                .log(" [ARTEMIS CONSUMER] Mensagem processada com sucesso!");
    }
}