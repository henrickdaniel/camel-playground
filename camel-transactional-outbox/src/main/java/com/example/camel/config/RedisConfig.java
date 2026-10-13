package com.example.camel.config;

import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public IdempotentRepository idempotentRepository() {
        // Cria um repositório idempotente em memória com capacidade para 1000 elementos
        MemoryIdempotentRepository memoryIdempotentRepository = new MemoryIdempotentRepository();
        memoryIdempotentRepository.setCacheSize(1000);
        return memoryIdempotentRepository;
    }
}