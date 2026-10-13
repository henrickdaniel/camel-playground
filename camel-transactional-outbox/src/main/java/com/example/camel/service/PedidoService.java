package com.example.camel.service;

import com.example.camel.dto.CriarPedidoRequest;
import com.example.camel.entity.OutboxEvent;
import com.example.camel.exception.IdempotencyPayloadMismatchException;
import com.example.camel.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final OutboxEventRepository outboxEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:pedido:";

    @Transactional
    public OutboxEvent processarCriacaoPedido(CriarPedidoRequest request, String idempotencyKey) {
        // Regra: Validação da obrigatoriedade da chave de idempotência
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Header x-idempotency-key é obrigatório.");
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(request);
            String currentPayloadHash = hashPayload(payloadJson);
            String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;

            // Verifica se a chave de idempotência já foi registrada no Redis
            String savedPayloadHash = redisTemplate.opsForValue().get(redisKey);

            if (savedPayloadHash != null) {
                // Regra 2 Open Finance: Mesma chave com payload DIFERENTE -> Retorna 422 (UNPROCESSABLE_ENTITY)
                if (!savedPayloadHash.equals(currentPayloadHash)) {
                    throw new IdempotencyPayloadMismatchException(
                            "A chave de idempotência foi utilizada anteriormente com um payload diferente."
                    );
                }

                // Regra 1 Open Finance: Mesma chave com payload IDÊNTICO -> Retorna o recurso existente/já processado
                // Buscamos o evento pendente ou já processado associado a essa chave no Outbox
                return outboxEventRepository.findByAggregateId(idempotencyKey)
                        .orElseThrow(() -> new IllegalStateException("Registro de outbox associado à chave não encontrado."));
            }

            // Se a chave é nova, salvamos o hash do payload no Redis com expiração de 24 horas
            redisTemplate.opsForValue().set(redisKey, currentPayloadHash, Duration.ofHours(24));

            // Criação do evento transacional no Outbox utilizando a idempotencyKey como aggregateId
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("PEDIDO")
                    .aggregateId(idempotencyKey)
                    .eventType("PEDIDO_CRIADO")
                    .payload(payloadJson)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            return outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            if (e instanceof IdempotencyPayloadMismatchException || e instanceof IllegalArgumentException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Erro ao processar idempotência do pedido", e);
        }
    }

    private String hashPayload(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash do payload", e);
        }
    }
}