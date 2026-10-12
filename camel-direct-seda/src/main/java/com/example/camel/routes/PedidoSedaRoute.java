package com.example.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PedidoSedaRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // 1. Rota principal: Recebe o pedido e quebra em itens individuais
        from("direct:receberPedidoProcessandoComSeda")
                .log(">>> [PEDIDO RECEBIDO] ${body.pedidoId}")
                .split(simple("${body.produtos}"))
                .log("     [ENVIANDO PARA FILA SEDA] Produto: ${body.nome}")
                .to("seda:filaProdutos?concurrentConsumers=2"); // Joga para a fila SEDA assíncrona

        // 2. Rota Assíncrona: Consome da fila SEDA em background e em paralelo
        from("seda:filaProdutos?concurrentConsumers=2")
                .log("          [WORKER SEDA] Processando item: ${body.nome} (Thread: ${threadName})")
                .process(exchange -> {
                    // Simula uma operação pesada (ex: chamada a serviço externo ou salvamento)
                    Thread.sleep(2000);
                })
                .log("          [WORKER SEDA] Concluído com sucesso: ${body.nome}");
    }
}