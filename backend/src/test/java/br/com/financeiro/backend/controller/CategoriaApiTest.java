package br.com.financeiro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Exercita a listagem, a criação e a alteração de categorias, com atenção à
 * separação entre usuários.
 */
class CategoriaApiTest extends ApiTestSupport {

    @Test
    void newUserSeesTheEightStandardCategories() {
        get(newToken(), "/api/v1/categorias")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(8)
                .jsonPath("$[0].nome").isEqualTo("Alimentação")
                .jsonPath("$[0].tipo").isEqualTo("DESPESA")
                .jsonPath("$[0].padrao").isEqualTo(true)
                .jsonPath("$[0].ativa").isEqualTo(true);
    }

    @Test
    void listIsSortedByNameIgnoringCaseAndAccents() {
        String token = newToken();
        create(token, "água", "DESPESA");
        create(token, "zoológico", "DESPESA");
        create(token, "Banco", "DESPESA");

        assertThat(names(token, "/api/v1/categorias"))
                .containsSubsequence("água", "Alimentação", "Banco", "Moradia", "zoológico");
    }

    @Test
    void userDoesNotSeeCategoriesOfOtherUsers() {
        String ana = newToken();
        String bia = newToken();
        create(ana, "Mercado", "DESPESA");

        assertThat(names(ana, "/api/v1/categorias")).contains("Mercado");
        assertThat(names(bia, "/api/v1/categorias")).doesNotContain("Mercado");
    }

    @Test
    void listFiltersByType() {
        String token = newToken();
        create(token, "Salário", "RECEITA");

        assertThat(names(token, "/api/v1/categorias?tipo=RECEITA")).containsExactly("Receitas", "Salário");
    }

    @Test
    void invalidTypeFilterGetsBadRequest() {
        get(newToken(), "/api/v1/categorias?tipo=NAO_EXISTE").expectStatus().isBadRequest();
    }

    @Test
    void inactiveCategoriesAreHiddenUnlessAsked() {
        String token = newToken();
        int id = create(token, "Mercado", "DESPESA");
        put(token, id, Map.of("nome", "Mercado", "ativa", false)).expectStatus().isOk();

        assertThat(names(token, "/api/v1/categorias")).doesNotContain("Mercado");
        assertThat(names(token, "/api/v1/categorias?incluirInativas=true")).contains("Mercado");
    }

    @Test
    void createReturnsTheCategoryAndItsLocation() {
        String token = newToken();

        String body = post(token, Map.of("nome", "  Mercado  ", "tipo", "DESPESA"))
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(JsonPath.<String>read(body, "$.nome")).isEqualTo("Mercado");
        assertThat(JsonPath.<String>read(body, "$.tipo")).isEqualTo("DESPESA");
        assertThat(JsonPath.<Boolean>read(body, "$.padrao")).isFalse();
        assertThat(JsonPath.<Boolean>read(body, "$.ativa")).isTrue();
    }

    @Test
    void createSetsLocationHeader() {
        String token = newToken();

        RestTestClient.ResponseSpec response = post(token, Map.of("nome", "Mercado", "tipo", "DESPESA"))
                .expectStatus().isCreated();

        response.expectHeader().value(HttpHeaders.LOCATION,
                location -> assertThat(location).matches("/api/v1/categorias/\\d+"));
    }

    @Test
    void duplicateNameIsRejectedIgnoringCaseAndSpaces() {
        String token = newToken();
        create(token, "Mercado", "DESPESA");

        post(token, Map.of("nome", " MERCADO ", "tipo", "DESPESA"))
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.title").isEqualTo("Categoria já existe");
    }

    @Test
    void inactiveCategoryStillHoldsItsName() {
        String token = newToken();
        int id = create(token, "Mercado", "DESPESA");
        put(token, id, Map.of("nome", "Mercado", "ativa", false)).expectStatus().isOk();

        post(token, Map.of("nome", "Mercado", "tipo", "DESPESA")).expectStatus().isEqualTo(409);
    }

    @Test
    void sameNameIsAllowedForAnotherTypeOrAnotherUser() {
        String ana = newToken();
        create(ana, "Extra", "DESPESA");

        post(ana, Map.of("nome", "Extra", "tipo", "RECEITA")).expectStatus().isCreated();
        post(newToken(), Map.of("nome", "Extra", "tipo", "DESPESA")).expectStatus().isCreated();
    }

    @Test
    void invalidCreationListsTheFields() {
        post(newToken(), Map.of("nome", " "))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.erros.length()").isEqualTo(2)
                .jsonPath("$.erros[0].campo").isEqualTo("nome")
                .jsonPath("$.erros[1].campo").isEqualTo("tipo");
    }

    @Test
    void tooLongNameIsRejected() {
        post(newToken(), Map.of("nome", "x".repeat(51), "tipo", "DESPESA"))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.erros[0].campo").isEqualTo("nome");
    }

    @Test
    void unknownTypeInBodyGetsBadRequest() {
        post(newToken(), Map.of("nome", "Mercado", "tipo", "NAO_EXISTE"))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Dados inválidos");
    }

    @Test
    void ownerRenamesDeactivatesAndReactivates() {
        String token = newToken();
        int id = create(token, "Mercado", "DESPESA");

        put(token, id, Map.of("nome", "Supermercado", "ativa", false))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.nome").isEqualTo("Supermercado")
                .jsonPath("$.ativa").isEqualTo(false);

        put(token, id, Map.of("nome", "Supermercado", "ativa", true))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.ativa").isEqualTo(true);
    }

    @Test
    void typeCannotChange() {
        String token = newToken();
        int id = create(token, "Mercado", "DESPESA");

        put(token, id, Map.of("nome", "Mercado", "ativa", true, "tipo", "RECEITA"))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.tipo").isEqualTo("DESPESA");

        assertThat(names(token, "/api/v1/categorias?tipo=DESPESA")).contains("Mercado");
        assertThat(names(token, "/api/v1/categorias?tipo=RECEITA")).doesNotContain("Mercado");
    }

    @Test
    void standardCategoryCannotBeChanged() {
        String token = newToken();
        int lazer = idOf(token, "Lazer");

        put(token, lazer, Map.of("nome", "Diversão", "ativa", false))
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.title").isEqualTo("Ação não permitida");

        assertThat(names(token, "/api/v1/categorias")).contains("Lazer").doesNotContain("Diversão");
    }

    @Test
    void categoryOfAnotherUserLooksLikeItDoesNotExist() {
        String ana = newToken();
        int id = create(ana, "Mercado", "DESPESA");

        String otherUsers = put(newToken(), id, Map.of("nome", "Roubada", "ativa", false))
                .expectStatus().isNotFound()
                .expectBody(String.class).returnResult().getResponseBody();
        String missing = put(ana, Integer.MAX_VALUE, Map.of("nome", "Roubada", "ativa", false))
                .expectStatus().isNotFound()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(JsonPath.<String>read(otherUsers, "$.detail")).isEqualTo(JsonPath.<String>read(missing, "$.detail"));
        assertThat(names(ana, "/api/v1/categorias")).contains("Mercado").doesNotContain("Roubada");
    }

    @Test
    void renamingOntoAnotherCategoryIsRejected() {
        String token = newToken();
        create(token, "Mercado", "DESPESA");
        int feira = create(token, "Feira", "DESPESA");

        put(token, feira, Map.of("nome", "mercado", "ativa", true))
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.title").isEqualTo("Categoria já existe");

        assertThat(names(token, "/api/v1/categorias")).contains("Feira");
    }

    @Test
    void changingOnlyTheCaseOfOwnNameIsAllowed() {
        String token = newToken();
        int id = create(token, "mercado", "DESPESA");

        put(token, id, Map.of("nome", "Mercado", "ativa", true))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.nome").isEqualTo("Mercado");
    }

    @Test
    void invalidUpdateListsTheFields() {
        String token = newToken();
        int id = create(token, "Mercado", "DESPESA");

        put(token, id, Map.of("nome", ""))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.erros[0].campo").isEqualTo("ativa")
                .jsonPath("$.erros[1].campo").isEqualTo("nome");
    }

    @Test
    void categoryRoutesRequireToken() {
        client.get().uri("/api/v1/categorias").exchange().expectStatus().isUnauthorized();
        client.post().uri("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", "Mercado", "tipo", "DESPESA"))
                .exchange()
                .expectStatus().isUnauthorized();
        client.put().uri("/api/v1/categorias/1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", "Mercado", "ativa", true))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void tokenOfDeletedAccountCannotUseCategories() {
        String token = newToken();
        client.delete().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        get(token, "/api/v1/categorias").expectStatus().isUnauthorized();
        post(token, Map.of("nome", "Mercado", "tipo", "DESPESA")).expectStatus().isUnauthorized();
    }

    private String newToken() {
        return login(register());
    }

    private RestTestClient.ResponseSpec get(String token, String uri) {
        return client.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();
    }

    private RestTestClient.ResponseSpec post(String token, Map<String, ?> body) {
        return client.post().uri("/api/v1/categorias")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    private RestTestClient.ResponseSpec put(String token, int id, Map<String, ?> body) {
        return client.put().uri("/api/v1/categorias/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    private int create(String token, String nome, String tipo) {
        String body = post(token, Map.of("nome", nome, "tipo", tipo))
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$.id");
    }

    private List<String> names(String token, String uri) {
        String body = get(token, uri).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$[*].nome");
    }

    private int idOf(String token, String nome) {
        String body = get(token, "/api/v1/categorias").expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        List<Integer> ids = JsonPath.read(body, "$[?(@.nome == '" + nome + "')].id");
        return ids.getFirst();
    }
}
