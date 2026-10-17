package com.example.camel.controller;

import com.example.camel.BaseIntegrationTest;
import com.example.camel.dto.CriarPedidoRequest;
import com.example.camel.entity.OutboxEvent;
import com.example.camel.service.PedidoService;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidoControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PedidoService pedidoService;

    @Test
    void deveCriarPedidoComSucessoQuandoCabecalhoIdempotenciaPresente() throws Exception {
        CriarPedidoRequest request = CriarPedidoRequest.builder()
                .clienteId("Henrick Soares")
                .descricao("Teste de Pedido")
                .valor(150.00)
                .build();

        OutboxEvent dummyEvent = new OutboxEvent();
        dummyEvent.setId(1L);
        dummyEvent.setAggregateId("chave-123");

        when(pedidoService.processarCriacaoPedido(any(CriarPedidoRequest.class), eq("chave-123")))
                .thenReturn(dummyEvent);

        mockMvc.perform(post("/api/v1/pedidos")
                        .header("x-idempotency-key", "chave-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.aggregateId").value("chave-123"));
    }

    @Test
    void deveRetornar400QuandoCabecalhoIdempotenciaEstiverAusente() throws Exception {
        CriarPedidoRequest request = CriarPedidoRequest.builder()
                .clienteId("Henrick Soares")
                .descricao("Teste sem chave")
                .valor(50.00)
                .build();

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}