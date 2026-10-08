# E-commerce Identity Service

Serviço responsável pelo cadastro e consulta de usuários, autenticação e emissão de tokens JWT. Os demais serviços validam tokens recebidos usando a chave pública configurada.

## Tecnologias

- Java 25, Maven e Spring Boot 4.1.1.
- Spring MVC, Spring Security, OAuth2 Resource Server e JWT (JJWT).
- Spring Data JPA, PostgreSQL e Flyway.
- Spring Boot Actuator e Micrometer Prometheus Registry.
- springdoc-openapi / Swagger UI.
- Testes com JUnit Jupiter, Mockito e Testcontainers para PostgreSQL.

## Swagger

[📚 Acessar Swagger](https://wlailson.github.io/e-commerce-identity-service/)

## Executar localmente

Pré-requisitos: JDK 25, Maven Wrapper e uma instância PostgreSQL acessível. O perfil padrão de desenvolvimento está em `src/main/resources/application-dev.yaml`; ajuste a configuração local do banco e as chaves JWT antes de iniciar.

```bash
./mvnw spring-boot:run
```

Para configurar outro perfil ou porta, use as variáveis Spring correspondentes, por exemplo `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. Em ambientes que usam o perfil de produção, forneça também `JWT_PRIVATE_KEY` e `JWT_PUBLIC_KEY`.

Gere um par RSA próprio para cada ambiente. Não reutilize nem publique chaves de desenvolvimento. Nunca configure uma chave privada real em código ou documentação.

## Testes

```bash
./mvnw test
```

Os testes que usam Testcontainers precisam de Docker disponível.

## API e observabilidade

Os endpoints de usuário ficam sob `/users`, incluindo autenticação em `/users/login`. Para ver operações e modelos, com a aplicação em execução consulte:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Métricas Prometheus: `http://localhost:8080/actuator/prometheus`
- Saúde: `http://localhost:8080/actuator/health`

A porta pode ser alterada com `SERVER_PORT`. O endpoint Prometheus expõe métricas da aplicação; a coleta e os dashboards precisam ser configurados fora deste serviço.

## Projeto

O [README central do BFF](https://github.com/wlailson/e-commerce-BFF-service#readme) descreve a arquitetura e as integrações entre os seis serviços.
