package com.example.camel.controller;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final ProducerTemplate producerTemplate;

    @PostMapping
    public ResponseEntity<Map<?, ?>> receber(@RequestBody String payload) {
        Map<?, ?> resposta = producerTemplate.requestBody("direct:payment-webhook", payload, Map.class);
        return ResponseEntity.ok(resposta);
    }
}
