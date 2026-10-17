package com.example.camel.payment.controller;

import com.example.camel.payment.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class PaymentApiController {

    private final JwtTokenProvider jwtTokenProvider;

    public PaymentApiController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/oauth/token")
    public ResponseEntity<?> gerarToken(
            @RequestHeader(value = "client-id", required = false) String clientId,
            @RequestHeader(value = "api-key", required = false) String apiKey,
            @RequestBody(required = false) Map<String, String> body) {

        String cId = clientId != null ? clientId : (body != null ? body.get("client_id") : null);
        String aKey = apiKey != null ? apiKey : (body != null ? body.get("api_key") : null);

        if ("cliente-parceiro-123".equals(cId) && "secret-key-xyz".equals(aKey)) {
            // Gera um JWT real assinado
            String jwtToken = jwtTokenProvider.gerarToken(cId);
            return ResponseEntity.ok(Map.of(
                    "access_token", jwtToken,
                    "token_type", "Bearer",
                    "expires_in", 3600
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("erro", "Credenciais inválidas"));
    }

    @PostMapping("/api/v1/payments")
    public ResponseEntity<?> processarPagamento(@RequestBody Map<String, Object> paymentPayload) {
        String paymentId = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "paymentId", paymentId,
                "status", "CONCLUIDO",
                "mensagem", "Pagamento processado com sucesso utilizando JWT autêntico",
                "detalhes", paymentPayload
        ));
    }
}