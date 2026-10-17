package com.example.camel;

import com.example.camel.dto.CriarPedidoRequest;
import com.example.camel.entity.OutboxEvent;
import com.example.camel.exception.IdempotencyPayloadMismatchException;
import com.example.camel.service.PedidoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PedidoIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private PedidoService pedidoService;

    @Test
    void deveGarantirIdempotenciaOpenFinanceComSucessoEPayloadMismatch() {
        String idempotencyKey = "chave-idempotencia-abc-123";

        CriarPedidoRequest requestOriginal = CriarPedidoRequest.builder()
                .clienteId("Henrick Soares")
                .descricao("Compra de Equipamento")
                .valor(500.00)
                .build();

        OutboxEvent evento1 = pedidoService.processarCriacaoPedido(requestOriginal, idempotencyKey);
        Assertions.assertNotNull(evento1);
        Assertions.assertEquals(idempotencyKey, evento1.getAggregateId());

        OutboxEvent evento2 = pedidoService.processarCriacaoPedido(requestOriginal, idempotencyKey);
        Assertions.assertEquals(evento1.getId(), evento2.getId());

        CriarPedidoRequest requestAlterado = CriarPedidoRequest.builder()
                .clienteId("Henrick Soares")
                .descricao("Compra Alterada")
                .valor(999.99)
                .build();

        Assertions.assertThrows(IdempotencyPayloadMismatchException.class, () -> {
            pedidoService.processarCriacaoPedido(requestAlterado, idempotencyKey);
        });
    }
}