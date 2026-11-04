package br.com.financeiro.backend.controller;

import com.jayway.jsonpath.JsonPath;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de ponta a ponta da API: HTTP real, filtros de segurança e
 * PostgreSQL descartável.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
abstract class ApiTestSupport {

    static final String SENHA = "uma-senha-forte";

    private static final String JWT_SECRET = Base64.getEncoder().encodeToString(randomKey());

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    // Iniciado aqui, sem @Container: o Spring reaproveita o mesmo contexto entre
    // as classes de teste, e o contêiner precisa continuar de pé entre elas.
    static {
        POSTGRES.start();
    }

    /** A chave é sorteada a cada execução; o datasource vem do contêiner. */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> JWT_SECRET);
        registry.add("JWT_EXPIRATION", () -> "PT1H");
        registry.add("DB_PASSWORD", () -> "nao-usada");
        // Fixado para o .env de quem roda os testes não mudar o resultado.
        registry.add("SWAGGER_ENABLED", () -> "false");
        registry.add("LOGIN_MAX_ATTEMPTS", () -> "5");
        registry.add("LOGIN_BLOCK_DURATION", () -> "PT15M");
    }

    @Autowired
    RestTestClient client;

    String register() {
        String email = uniqueEmail();
        postRegistration("Ana", email, SENHA).expectStatus().isCreated();
        return email;
    }

    String login(String email) {
        String body = postLogin(email, SENHA).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$.token");
    }

    RestTestClient.ResponseSpec postRegistration(String nome, String email, String senha) {
        return client.post().uri("/api/v1/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", nome, "email", email, "senha", senha))
                .exchange();
    }

    RestTestClient.ResponseSpec postLogin(String email, String senha) {
        return client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "senha", senha))
                .exchange();
    }

    static byte[] randomKey() {
        byte[] key = new byte[48];
        new SecureRandom().nextBytes(key);
        return key;
    }

    static String uniqueEmail() {
        return UUID.randomUUID() + "@exemplo.com";
    }
}
