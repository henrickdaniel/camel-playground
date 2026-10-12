package com.example.camel.route;

import com.example.camel.entity.Pedido;
import com.example.camel.processor.PedidoProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PedidoRoute extends RouteBuilder {

    private final PedidoProcessor pedidoProcessor;

    public PedidoRoute(PedidoProcessor pedidoProcessor) {
        this.pedidoProcessor = pedidoProcessor;
    }

    @Override
    public void configure() {

        // Tratamento simples de erro: tenta 3 vezes com intervalo de 2s
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(2000));

        // Rota principal
        from("jms:queue:pedidos.entrada")
                .routeId("rota-processa-pedido")
                .log("Mensagem recebida da fila: ${body}")
                .process(pedidoProcessor)
                .log("Objeto Pedido transformado: ${body.cliente} - R$ ${body.valor}")
                .to("jpa:" + Pedido.class.getName())
                .log("Pedido salvo com sucesso no banco de dados!");
    }
}