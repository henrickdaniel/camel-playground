package com.example.camel.processor;

import com.example.camel.entity.Pedido;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PedidoProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // Obtém o corpo da mensagem vindo da fila (ex: "Cliente A;250.00")
        String body = exchange.getIn().getBody(String.class);

        String[] dados = body.split(";");
        String cliente = dados[0];
        BigDecimal valor = new BigDecimal(dados[1]);

        // Cria e insere o objeto Pedido no corpo da troca (Exchange)
        Pedido pedido = new Pedido(cliente, valor, "PROCESSADO");
        exchange.getIn().setBody(pedido);
    }
}