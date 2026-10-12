package com.example.camel;

import com.example.camel.model.Pedido;
import com.example.camel.model.Produto;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class CamelDirectSedaApplication implements CommandLineRunner {

	@Autowired
	private ProducerTemplate producerTemplate;

	public static void main(String[] args) {
		SpringApplication.run(CamelDirectSedaApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println(">>> Simulando chegada de um pedido corporativo...");

		// Cria um pedido com 5 produtos
		Pedido pedido = new Pedido("PED-9988", Arrays.asList(
				new Produto("Notebook Gamer", 1),
				new Produto("Mouse Pad RGB", 2),
				new Produto("Teclado Mecânico", 1),
				new Produto("Monitor Ultrawide", 1),
				new Produto("Headset Wireless", 1)
		));

		// Envia o pedido para a rota do Camel
		producerTemplate.sendBody("direct:receberPedidoProcessarUsandoParallelProcessing", pedido);
		producerTemplate.sendBody("direct:receberPedidoProcessandoComSeda", pedido);
	}
}