package com.example.camel.controller;

import com.example.camel.dto.CriarPedidoRequest;
import com.example.camel.entity.OutboxEvent;
import com.example.camel.exception.IdempotencyPayloadMismatchException;
import com.example.camel.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<?> criarPedido(
            @RequestHeader("x-idempotency-key") String idempotencyKey,
            @Valid @RequestBody CriarPedidoRequest request) {

        try {
            OutboxEvent evento = pedidoService.processarCriacaoPedido(request, idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(evento);
        } catch (IdempotencyPayloadMismatchException e) {
            // Regra do Open Finance: Retornar 422 Unprocessable Entity para payload divergente com a mesma chave
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErroResponse("IDEMPOTENCY_PAYLOAD_MISMATCH", e.getMessage()));
        }
    }

    // DTO auxiliar para erros de API
    public record ErroResponse(String codigo, String mensagem) {}
}