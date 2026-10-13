package com.example.camel;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ArtemisApplication implements CommandLineRunner {

	@Autowired
	private ProducerTemplate producerTemplate;

	public static void main(String[] args) {
		SpringApplication.run(ArtemisApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println(">>> Enviando mensagens para o ActiveMQ Artemis...");

		for (int i = 1; i <= 3; i++) {
			String payload = "Mensagem de pedido Artemis #" + i;
			// Envia diretamente para a fila JMS do Artemis
			producerTemplate.sendBody("jms:queue:pedidos.fila", payload);
			System.out.println("Enviado com sucesso -> " + payload);
			Thread.sleep(1000);
		}
	}
}