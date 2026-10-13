package com.example.camel;

import com.example.camel.dto.CriarPedidoRequest;
import com.example.camel.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class OutboxApplication implements CommandLineRunner {

	@Autowired
	private PedidoService pedidoService;

	public static void main(String[] args) {
		SpringApplication.run(OutboxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println(">>> Simulando criação de pedido com Transactional Outbox e Idempotência...");

		// Criando o DTO de request
		CriarPedidoRequest request = CriarPedidoRequest.builder()
				.clienteId("Henrick Soares")
				.descricao("Pedido de Teste Outbox")
				.valor(250.00)
				.build();

		// Gerando uma chave de idempotência única para a simulação inicial
		String idempotencyKey = UUID.randomUUID().toString();

		// Chamando o serviço com o novo contrato
		pedidoService.processarCriacaoPedido(request, idempotencyKey);
		pedidoService.processarCriacaoPedido(request, idempotencyKey);

		System.out.println(">>> Pedido processado e evento gravado no Outbox com sucesso!");
	}
}