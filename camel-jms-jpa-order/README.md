# 🐪 Camel Playground

Repositório de laboratório e estudos práticos com Apache Camel, Spring Boot e integrações.

## 🚀 Roadmap de Exemplos

### Fase 1: Fundamentos
- [ ] **`camel-direct-seda`**: Comunicação síncrona (`direct`) e assíncrona (`seda`) entre rotas na mesma JVM.
- [ ] **`camel-file-poller`**: Leitura e movimentação de arquivos locais (File Component).
- [ ] **`camel-bean-processor`**: Manipulação de mensagens usando POJOs (`.bean()`) vs `Processor`.

### Fase 2: Transformação e Mensageria
- [ ] **`camel-json-transform`**: Conversão de objetos Java para JSON usando Jackson DataFormat.
- [ ] **`camel-activemq-artemis`**: Envio e consumo de filas JMS (mensageria assíncrona).
- [ ] **`camel-azure-servicebus`**: Integração com filas e tópicos na nuvem (Azure Service Bus).

### Fase 3: Bancos de Dados e APIs
- [ ] **`camel-jpa-crud`**: Inserção e leitura de dados utilizando JPA e banco relacional.
- [ ] **`camel-rest-dsl`**: Exposição de endpoints RESTful usando o REST DSL nativo do Camel.
- [ ] **`camel-http-client`**: Consumo de APIs externas utilizando o componente HTTP.

### Fase 4: Resiliência e Padrões Avançados
- [ ] **`camel-circuit-breaker`**: Tratamento de falhas e resiliência com Resilience4j.
- [ ] **`camel-error-handling`**: Configuração de `onException`, Dead Letter Channel e reprocessamento.
- [ ] **`camel-apache-camel-routes`**: Rotas complexas usando Apache Camel.