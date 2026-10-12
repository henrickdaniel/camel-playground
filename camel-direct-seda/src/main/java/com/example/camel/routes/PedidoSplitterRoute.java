package com.example.camel.routes;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PedidoSplitterRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Rota que recebe o pedido principal
        from("direct:receberPedidoProcessarUsandoParallelProcessing")
                .log(">>> [PEDIDO CHEGOU] ${body.pedidoId} contendo ${body.produtos.size()} produtos.")
                .to("direct:processarProdutosEmParalelo");

        // Rota que faz o split e processa os itens concorrentemente
        from("direct:processarProdutosEmParalelo")
                // O split pega a lista de produtos de dentro do Pedido
                .split(simple("${body.produtos}"))
                // parallelProcessing() faz cada produto rodar em uma thread diferente (paralelo)
                .parallelProcessing()
                .log("     [ITEM INICIADO] Processando produto: ${body.nome} (Qtd: ${body.quantidade})")
                .process(exchange -> {
                    // Simula uma consulta de estoque ou validação demorada (1,5 segundos)
                    log.info("         [ITEM EM PROCESSAMENTO] Consultando estoque para o produto: {}", exchange.getIn().getBody().toString());
                    Thread.sleep(1500);
                })
                .log("     [ITEM FINALIZADO] Produto processado com sucesso: ${body.nome}")
                .end() // Fecha o split
                .log("<<< [PEDIDO CONCLUÍDO] Todos os produtos do pedido foram validados e processados em paralelo!");
    }
}