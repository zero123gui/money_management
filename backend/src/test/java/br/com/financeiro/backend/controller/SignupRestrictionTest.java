package br.com.financeiro.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Confere o cadastro com a restrição ligada, como em produção. As demais classes
 * de teste rodam sem ela, pois criam contas com e-mails sorteados.
 */
@TestPropertySource(properties = {
    "app.signup.restricted=true",
    "app.signup.allowed-emails=Na-Lista@exemplo.com, tambem-na-lista@exemplo.com"
})
class SignupRestrictionTest extends ApiTestSupport {

    @Test
    void emailOnTheListRegistersAndLogsIn() {
        postRegistration("Ana", "na-lista@exemplo.com", SENHA).expectStatus().isCreated();

        postLogin("na-lista@exemplo.com", SENHA).expectStatus().isOk();
    }

    @Test
    void listIgnoresCapitalLettersAndSpaces() {
        postRegistration("Bia", "TAMBEM-NA-LISTA@exemplo.com", SENHA).expectStatus().isCreated();
    }

    @Test
    void emailOutOfTheListIsRefused() {
        postRegistration("Caio", uniqueEmail(), SENHA)
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.title").isEqualTo("Cadastro não permitido")
                .jsonPath("$.detail").isEqualTo("Este e-mail não está autorizado a criar uma conta.");
    }

    @Test
    void invalidFieldsStillGetBadRequest() {
        postRegistration("", "nao-e-email", "curta").expectStatus().isBadRequest();
    }
}
