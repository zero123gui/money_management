package br.com.financeiro.backend.controller;

import org.junit.jupiter.api.Test;

/**
 * Garante que, sem pedido explícito, o Swagger fica fora do ar, como deve ser
 * em produção ({@code docs/API.md}, seção 1.8).
 */
class SwaggerDisabledTest extends ApiTestSupport {

    @Test
    void documentationRoutesAreNotPublicByDefault() {
        client.get().uri("/v3/api-docs").exchange().expectStatus().isUnauthorized();
        client.get().uri("/swagger-ui.html").exchange().expectStatus().isUnauthorized();
        client.get().uri("/swagger-ui/index.html").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void documentationDoesNotExistEvenWithToken() {
        String token = login(register());

        client.get().uri("/v3/api-docs")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }
}
