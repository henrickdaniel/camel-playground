package com.exemplo.camel.camelsample.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TesteController {

    private final ProducerTemplate producerTemplate;

    public TesteController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @PostMapping("/enviar-pedido")
    public ResponseEntity<String> enviarPedido(@RequestBody String payload) {
        // Envia o payload recebido diretamente para a fila JMS do ActiveMQ
        producerTemplate.sendBody("jms:queue:pedidos.entrada", payload);
        return ResponseEntity.ok("Mensagem enviada para a fila pedidos.entrada!");
    }
}