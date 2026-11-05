package br.com.financeiro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Exercita as rotas de lançamentos, com atenção à separação entre usuários
 * (RNF-04) e à exclusão lógica.
 */
class LancamentoApiTest extends ApiTestSupport {

    private static final String LANCAMENTOS = "/api/v1/lancamentos";

    @Test
    void createReturnsTheEntryAndItsLocation() {
        String token = newToken();
        int lazer = standardCategory(token, "Lazer");

        post(token, entry("DESPESA", "150.7", "2026-10-28", lazer))
                .expectStatus().isCreated()
                .expectHeader().value(HttpHeaders.LOCATION, location -> assertThat(location).matches(LANCAMENTOS + "/\\d+"))
                .expectBody()
                .jsonPath("$.tipo").isEqualTo("DESPESA")
                .jsonPath("$.valor").isEqualTo(150.70)
                .jsonPath("$.data").isEqualTo("2026-10-28")
                .jsonPath("$.descricao").isEqualTo("Compra")
                .jsonPath("$.categoria.id").isEqualTo(lazer)
                .jsonPath("$.categoria.nome").isEqualTo("Lazer")
                .jsonPath("$.criadoEm").value(String.class, value -> assertThat(value).matches(".*:\\d{2}Z"))
                .jsonPath("$.atualizadoEm").value(String.class, value -> assertThat(value).matches(".*:\\d{2}Z"))
                .jsonPath("$.excluidoEm").doesNotExist();
    }

    @Test
    void amountKeepsExactCentsThroughTheDatabase() {
        String token = newToken();
        int id = create(token, "1234567890123.45");

        String body = get(token, LANCAMENTOS + "/" + id).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(body).contains("\"valor\":1234567890123.45");
    }

    @Test
    void missingDescriptionComesBackAsNull() {
        String token = newToken();
        Map<String, Object> body = entry("DESPESA", "10", "2026-10-01", standardCategory(token, "Lazer"));
        body.put("descricao", "   ");

        String response = post(token, body).expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(response).contains("\"descricao\":null");
    }

    @Test
    void invalidEntryListsEachField() {
        post(newToken(), Map.of("descricao", "x".repeat(256)))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.erros[*].campo").value(List.class, fields -> assertThat(fields)
                        .containsExactly("categoriaId", "data", "descricao", "tipo", "valor"));
    }

    @Test
    void invalidAmountsAreRejected() {
        String token = newToken();
        int lazer = standardCategory(token, "Lazer");

        for (String amount : List.of("0", "-1", "10.123", "12345678901234")) {
            post(token, entry("DESPESA", amount, "2026-10-01", lazer))
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.erros[0].campo").isEqualTo("valor");
        }
    }

    @Test
    void invalidDateOrTypeGetsBadRequest() {
        String token = newToken();
        int lazer = standardCategory(token, "Lazer");

        post(token, entry("DESPESA", "10", "28/10/2026", lazer)).expectStatus().isBadRequest();
        post(token, entry("NAO_EXISTE", "10", "2026-10-01", lazer)).expectStatus().isBadRequest();
    }

    @Test
    void categoryOfAnotherTypeIsRejected() {
        String token = newToken();

        assertCategoryRejected(token, entry("RECEITA", "10", "2026-10-01", standardCategory(token, "Lazer")),
                "é de outro tipo");
    }

    @Test
    void inactiveCategoryIsRejected() {
        String token = newToken();

        assertCategoryRejected(token, entry("DESPESA", "10", "2026-10-01", inactiveCategory(token)), "está desativada");
    }

    @Test
    void categoryOfAnotherUserLooksLikeItDoesNotExist() {
        String ana = newToken();
        int anaCategory = ownCategory(ana, "Mercado");
        String bia = newToken();

        assertCategoryRejected(bia, entry("DESPESA", "10", "2026-10-01", anaCategory), "não existe");
        assertCategoryRejected(bia, entry("DESPESA", "10", "2026-10-01", Integer.MAX_VALUE), "não existe");
    }

    @Test
    void ownCategoryCanBeUsed() {
        String token = newToken();

        post(token, entry("DESPESA", "10", "2026-10-01", ownCategory(token, "Mercado")))
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.categoria.nome").isEqualTo("Mercado");
    }

    @Test
    void entryOfAnotherUserLooksLikeItDoesNotExist() {
        String ana = newToken();
        int id = create(ana, "10");
        String bia = newToken();
        int lazer = standardCategory(bia, "Lazer");

        get(bia, LANCAMENTOS + "/" + id).expectStatus().isNotFound();
        put(bia, id, entry("DESPESA", "999", "2026-10-01", lazer)).expectStatus().isNotFound();
        delete(bia, id).expectStatus().isNotFound();
        restore(bia, id).expectStatus().isNotFound();

        get(ana, LANCAMENTOS + "/" + id).expectStatus().isOk()
                .expectBody().jsonPath("$.valor").isEqualTo(10.00);
    }

    @Test
    void deletedEntryOfAnotherUserCannotBeRestored() {
        String ana = newToken();
        int id = create(ana, "10");
        delete(ana, id).expectStatus().isNoContent();

        restore(newToken(), id).expectStatus().isNotFound();

        get(ana, LANCAMENTOS + "/" + id).expectStatus().isNotFound();
    }

    @Test
    void listsOnlyShowOwnEntries() {
        String ana = newToken();
        create(ana, "10");
        delete(ana, create(ana, "20")).expectStatus().isNoContent();
        String bia = newToken();

        assertThat(amounts(bia, LANCAMENTOS)).isEmpty();
        assertThat(amounts(bia, LANCAMENTOS + "/excluidos")).isEmpty();
        assertThat(amounts(ana, LANCAMENTOS)).hasSize(1);
    }

    @Test
    void filteringByCategoryOfAnotherUserFindsNothing() {
        String ana = newToken();
        int category = ownCategory(ana, "Mercado");
        post(ana, entry("DESPESA", "10", "2026-10-01", category)).expectStatus().isCreated();

        assertThat(amounts(newToken(), LANCAMENTOS + "?categoriaId=" + category)).isEmpty();
    }

    @Test
    void listIsSortedFromMostRecentToOldest() {
        String token = newToken();
        int lazer = standardCategory(token, "Lazer");
        post(token, entry("DESPESA", "1", "2026-10-01", lazer)).expectStatus().isCreated();
        post(token, entry("DESPESA", "2", "2026-10-03", lazer)).expectStatus().isCreated();
        post(token, entry("DESPESA", "3", "2026-10-02", lazer)).expectStatus().isCreated();
        post(token, entry("DESPESA", "4", "2026-10-03", lazer)).expectStatus().isCreated();

        assertThat(amounts(token, LANCAMENTOS)).containsExactly(4.0, 2.0, 3.0, 1.0);
    }

    @Test
    void sortParameterCannotChangeTheOrder() {
        String token = newToken();
        int lazer = standardCategory(token, "Lazer");
        post(token, entry("DESPESA", "1", "2026-10-01", lazer)).expectStatus().isCreated();
        post(token, entry("DESPESA", "2", "2026-10-03", lazer)).expectStatus().isCreated();

        assertThat(amounts(token, LANCAMENTOS + "?sort=amount,asc")).containsExactly(2.0, 1.0);
        assertThat(amounts(token, LANCAMENTOS + "?sort=owner.passwordHash")).containsExactly(2.0, 1.0);
    }

    @Test
    void filtersWorkAloneAndTogether() {
        String token = newToken();
        int lazer = standardCategory(token, "Lazer");
        int moradia = standardCategory(token, "Moradia");
        int receitas = standardCategory(token, "Receitas");
        post(token, entry("DESPESA", "1", "2026-09-30", lazer)).expectStatus().isCreated();
        post(token, entry("DESPESA", "2", "2026-10-01", moradia)).expectStatus().isCreated();
        post(token, entry("RECEITA", "3", "2026-10-15", receitas)).expectStatus().isCreated();
        post(token, entry("DESPESA", "4", "2026-10-31", lazer)).expectStatus().isCreated();
        post(token, entry("DESPESA", "5", "2026-11-01", moradia)).expectStatus().isCreated();

        assertThat(amounts(token, LANCAMENTOS + "?inicio=2026-10-01")).containsExactly(5.0, 4.0, 3.0, 2.0);
        assertThat(amounts(token, LANCAMENTOS + "?fim=2026-10-31")).containsExactly(4.0, 3.0, 2.0, 1.0);
        assertThat(amounts(token, LANCAMENTOS + "?inicio=2026-10-01&fim=2026-10-31")).containsExactly(4.0, 3.0, 2.0);
        assertThat(amounts(token, LANCAMENTOS + "?tipo=RECEITA")).containsExactly(3.0);
        assertThat(amounts(token, LANCAMENTOS + "?categoriaId=" + lazer)).containsExactly(4.0, 1.0);
        assertThat(amounts(token, LANCAMENTOS + "?inicio=2026-10-01&fim=2026-10-31&tipo=DESPESA&categoriaId=" + moradia))
                .containsExactly(2.0);
        assertThat(amounts(token, LANCAMENTOS + "?inicio=2026-11-01&fim=2026-10-01")).isEmpty();
    }

    @Test
    void invalidFiltersGetBadRequest() {
        String token = newToken();

        get(token, LANCAMENTOS + "?inicio=01/10/2026").expectStatus().isBadRequest();
        get(token, LANCAMENTOS + "?tipo=NAO_EXISTE").expectStatus().isBadRequest();
        get(token, LANCAMENTOS + "?categoriaId=abc").expectStatus().isBadRequest();
    }

    @Test
    void pagesFollowTheContract() {
        String token = newToken();
        int lazer = standardCategory(token, "Lazer");
        for (int day = 1; day <= 3; day++) {
            post(token, entry("DESPESA", String.valueOf(day), "2026-10-0" + day, lazer)).expectStatus().isCreated();
        }

        get(token, LANCAMENTOS + "?pagina=1&tamanho=2")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.conteudo.length()").isEqualTo(1)
                .jsonPath("$.conteudo[0].valor").isEqualTo(1.00)
                .jsonPath("$.pagina").isEqualTo(1)
                .jsonPath("$.tamanho").isEqualTo(2)
                .jsonPath("$.totalElementos").isEqualTo(3)
                .jsonPath("$.totalPaginas").isEqualTo(2);
    }

    @Test
    void pageSizeDefaultsToFiftyAndStopsAtTwoHundred() {
        String token = newToken();

        get(token, LANCAMENTOS).expectBody().jsonPath("$.tamanho").isEqualTo(50);
        get(token, LANCAMENTOS + "?tamanho=500").expectBody().jsonPath("$.tamanho").isEqualTo(200);
    }

    @Test
    void updateReplacesTheEntry() {
        String token = newToken();
        int id = create(token, "10");
        Map<String, Object> changed = entry("RECEITA", "99.9", "2026-12-25", standardCategory(token, "Receitas"));
        changed.remove("descricao");

        put(token, id, changed)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.tipo").isEqualTo("RECEITA")
                .jsonPath("$.valor").isEqualTo(99.90)
                .jsonPath("$.data").isEqualTo("2026-12-25")
                .jsonPath("$.descricao").isEmpty()
                .jsonPath("$.categoria.nome").isEqualTo("Receitas");

        get(token, LANCAMENTOS + "/" + id).expectBody().jsonPath("$.valor").isEqualTo(99.90);
    }

    @Test
    void updateAppliesTheSameRulesAsCreate() {
        String token = newToken();
        int id = create(token, "10");

        put(token, id, entry("DESPESA", "0", "2026-10-01", standardCategory(token, "Lazer")))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.erros[0].campo").isEqualTo("valor");
        put(token, id, entry("RECEITA", "10", "2026-10-01", standardCategory(token, "Lazer")))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.erros[0].campo").isEqualTo("categoriaId");
        put(token, id, entry("DESPESA", "10", "2026-10-01", inactiveCategory(token)))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.erros[0].mensagem").isEqualTo("está desativada");

        get(token, LANCAMENTOS + "/" + id).expectBody().jsonPath("$.valor").isEqualTo(10.00);
    }

    @Test
    void deletedEntryLeavesActiveRoutesAndKeepsItsAmount() {
        String token = newToken();
        int id = create(token, "42.50");

        delete(token, id).expectStatus().isNoContent().expectBody().isEmpty();

        get(token, LANCAMENTOS + "/" + id).expectStatus().isNotFound();
        put(token, id, entry("DESPESA", "1", "2026-10-01", standardCategory(token, "Lazer"))).expectStatus().isNotFound();
        delete(token, id).expectStatus().isNotFound();
        assertThat(amounts(token, LANCAMENTOS)).isEmpty();
        get(token, LANCAMENTOS + "/excluidos")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElementos").isEqualTo(1)
                .jsonPath("$.conteudo[0].id").isEqualTo(id)
                .jsonPath("$.conteudo[0].valor").isEqualTo(42.50)
                .jsonPath("$.conteudo[0].excluidoEm").value(String.class, value -> assertThat(value).matches(".*:\\d{2}Z"));
    }

    @Test
    void deletedListStartsWithTheMostRecentlyDeleted() {
        String token = newToken();
        int first = create(token, "1");
        int second = create(token, "2");
        delete(token, second).expectStatus().isNoContent();
        delete(token, first).expectStatus().isNoContent();

        assertThat(amounts(token, LANCAMENTOS + "/excluidos")).containsExactly(1.0, 2.0);
    }

    @Test
    void restoredEntryIsActiveAgain() {
        String token = newToken();
        int id = create(token, "42.50");
        delete(token, id).expectStatus().isNoContent();

        restore(token, id)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.valor").isEqualTo(42.50)
                .jsonPath("$.categoria.nome").isEqualTo("Lazer");

        get(token, LANCAMENTOS + "/" + id).expectStatus().isOk();
        assertThat(amounts(token, LANCAMENTOS + "/excluidos")).isEmpty();
    }

    @Test
    void activeEntryCannotBeRestored() {
        String token = newToken();

        restore(token, create(token, "10")).expectStatus().isNotFound();
    }

    @Test
    void entryIsRestoredEvenIfItsCategoryWasDeactivated() {
        String token = newToken();
        int category = ownCategory(token, "Desativada");
        int id = JsonPath.read(post(token, entry("DESPESA", "10", "2026-10-01", category))
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody(), "$.id");
        delete(token, id).expectStatus().isNoContent();
        deactivate(token, category);

        restore(token, id)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.categoria.id").isEqualTo(category);
    }

    @Test
    void updateChangesOnlyTheLastModifiedDate() throws InterruptedException {
        String token = newToken();
        int id = create(token, "10");
        String before = get(token, LANCAMENTOS + "/" + id).expectBody(String.class).returnResult().getResponseBody();
        // As datas da API têm precisão de segundos.
        Thread.sleep(1100);

        String after = put(token, id, entry("DESPESA", "20", "2026-10-01", standardCategory(token, "Lazer")))
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(JsonPath.<String>read(after, "$.criadoEm")).isEqualTo(JsonPath.<String>read(before, "$.criadoEm"));
        assertThat(JsonPath.<String>read(after, "$.atualizadoEm"))
                .isGreaterThan(JsonPath.<String>read(before, "$.atualizadoEm"));
    }

    @Test
    void routesRequireToken() {
        client.get().uri(LANCAMENTOS).exchange().expectStatus().isUnauthorized();
        client.get().uri(LANCAMENTOS + "/1").exchange().expectStatus().isUnauthorized();
        client.get().uri(LANCAMENTOS + "/excluidos").exchange().expectStatus().isUnauthorized();
        client.delete().uri(LANCAMENTOS + "/1").exchange().expectStatus().isUnauthorized();
        client.post().uri(LANCAMENTOS + "/1/restaurar").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void tokenOfDeletedAccountCannotUseEntries() {
        String token = newToken();
        int lazer = standardCategory(token, "Lazer");
        deleteAccount(token, SENHA).expectStatus().isNoContent();

        get(token, LANCAMENTOS).expectStatus().isUnauthorized();
        get(token, LANCAMENTOS + "/excluidos").expectStatus().isUnauthorized();
        post(token, entry("DESPESA", "10", "2026-10-01", lazer)).expectStatus().isUnauthorized();
    }

    private void assertCategoryRejected(String token, Map<String, Object> body, String message) {
        post(token, body)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.erros[0].campo").isEqualTo("categoriaId")
                .jsonPath("$.erros[0].mensagem").isEqualTo(message);
    }

    private static Map<String, Object> entry(String tipo, String valor, String data, int categoriaId) {
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", tipo);
        // Como número no JSON: é assim que o desktop envia (docs/API.md, seção 1.2).
        body.put("valor", new BigDecimal(valor));
        body.put("data", data);
        body.put("descricao", "Compra");
        body.put("categoriaId", categoriaId);
        return body;
    }

    private String newToken() {
        return login(register());
    }

    private int create(String token, String valor) {
        String body = post(token, entry("DESPESA", valor, "2026-10-01", standardCategory(token, "Lazer")))
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$.id");
    }

    private int standardCategory(String token, String nome) {
        String body = get(token, "/api/v1/categorias").expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        List<Integer> ids = JsonPath.read(body, "$[?(@.nome == '" + nome + "')].id");
        return ids.getFirst();
    }

    private int ownCategory(String token, String nome) {
        String body = client.post().uri("/api/v1/categorias")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", nome, "tipo", "DESPESA"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$.id");
    }

    private int inactiveCategory(String token) {
        int id = ownCategory(token, "Desativada");
        deactivate(token, id);
        return id;
    }

    private void deactivate(String token, int categoryId) {
        client.put().uri("/api/v1/categorias/" + categoryId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", "Desativada", "ativa", false))
                .exchange()
                .expectStatus().isOk();
    }

    private List<Double> amounts(String token, String uri) {
        String body = get(token, uri).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$.conteudo[*].valor");
    }

    private RestTestClient.ResponseSpec get(String token, String uri) {
        return client.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();
    }

    private RestTestClient.ResponseSpec post(String token, Map<String, ?> body) {
        return client.post().uri(LANCAMENTOS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    private RestTestClient.ResponseSpec put(String token, int id, Map<String, ?> body) {
        return client.put().uri(LANCAMENTOS + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    private RestTestClient.ResponseSpec delete(String token, int id) {
        return client.delete().uri(LANCAMENTOS + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();
    }

    private RestTestClient.ResponseSpec restore(String token, int id) {
        return client.post().uri(LANCAMENTOS + "/" + id + "/restaurar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();
    }
}
