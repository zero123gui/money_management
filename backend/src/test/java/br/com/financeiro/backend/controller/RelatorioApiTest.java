package br.com.financeiro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Confere o cálculo do saldo e do resumo por categoria (RNF-14) e que eles só
 * consideram os lançamentos ativos do próprio usuário.
 */
class RelatorioApiTest extends ApiTestSupport {

    private static final String OUTUBRO = "?inicio=2026-10-01&fim=2026-10-31";
    private static final String SALDO = "/api/v1/relatorios/saldo";
    private static final String CATEGORIAS = "/api/v1/relatorios/categorias";

    @Test
    void balanceIsIncomeMinusExpenses() {
        String token = newToken();
        addEntry(token, "RECEITA", "5000.00", "2026-10-05", "Receitas");
        addEntry(token, "DESPESA", "3000.25", "2026-10-10", "Moradia");
        addEntry(token, "DESPESA", "200.25", "2026-10-11", "Lazer");

        String body = report(token, SALDO + OUTUBRO);

        assertThat(body).contains("\"inicio\":\"2026-10-01\"", "\"fim\":\"2026-10-31\"",
                "\"totalReceitas\":5000.00", "\"totalDespesas\":3200.50", "\"saldo\":1799.50");
    }

    @Test
    void centsAddUpExactly() {
        String token = newToken();
        // Com double, 0.10 + 0.20 daria 0.30000000000000004 (RNF-06).
        addEntry(token, "RECEITA", "0.10", "2026-10-01", "Receitas");
        addEntry(token, "RECEITA", "0.20", "2026-10-02", "Receitas");

        assertThat(report(token, SALDO + OUTUBRO)).contains("\"totalReceitas\":0.30", "\"saldo\":0.30");
    }

    @Test
    void balanceCanBeNegative() {
        String token = newToken();
        addEntry(token, "DESPESA", "10.50", "2026-10-01", "Lazer");

        assertThat(report(token, SALDO + OUTUBRO))
                .contains("\"totalReceitas\":0.00", "\"totalDespesas\":10.50", "\"saldo\":-10.50");
    }

    @Test
    void periodWithoutEntriesGivesZeroAndEmptyList() {
        String token = newToken();

        assertThat(report(token, SALDO + OUTUBRO))
                .contains("\"totalReceitas\":0.00", "\"totalDespesas\":0.00", "\"saldo\":0.00");
        assertThat(report(token, CATEGORIAS + OUTUBRO)).isEqualTo("[]");
    }

    @Test
    void totalsCanExceedTheLimitOfASingleEntry() {
        String token = newToken();
        addEntry(token, "RECEITA", "9999999999999.99", "2026-10-01", "Receitas");
        addEntry(token, "RECEITA", "9999999999999.99", "2026-10-02", "Receitas");

        assertThat(report(token, SALDO + OUTUBRO)).contains("\"totalReceitas\":19999999999999.98");
    }

    @Test
    void periodIncludesBothEndsAndNothingElse() {
        String token = newToken();
        addEntry(token, "DESPESA", "1", "2026-09-30", "Lazer");
        addEntry(token, "DESPESA", "2", "2026-10-01", "Lazer");
        addEntry(token, "DESPESA", "4", "2026-10-31", "Lazer");
        addEntry(token, "DESPESA", "8", "2026-11-01", "Lazer");

        assertThat(report(token, SALDO + OUTUBRO)).contains("\"totalDespesas\":6.00");
        assertThat(report(token, CATEGORIAS + OUTUBRO)).contains("\"total\":6.00", "\"quantidade\":2");
    }

    @Test
    void startAfterEndFindsNothing() {
        String token = newToken();
        addEntry(token, "DESPESA", "1", "2026-10-15", "Lazer");

        assertThat(report(token, SALDO + "?inicio=2026-10-31&fim=2026-10-01")).contains("\"saldo\":0.00");
        assertThat(report(token, CATEGORIAS + "?inicio=2026-10-31&fim=2026-10-01")).isEqualTo("[]");
    }

    @Test
    void deletedEntriesDoNotCountUntilRestored() {
        String token = newToken();
        addEntry(token, "DESPESA", "10", "2026-10-01", "Lazer");
        int id = addEntry(token, "DESPESA", "5", "2026-10-02", "Lazer");

        request(token, client.delete().uri("/api/v1/lancamentos/" + id)).expectStatus().isNoContent();

        assertThat(report(token, SALDO + OUTUBRO)).contains("\"totalDespesas\":10.00");
        assertThat(report(token, CATEGORIAS + OUTUBRO)).contains("\"total\":10.00", "\"quantidade\":1");

        request(token, client.post().uri("/api/v1/lancamentos/" + id + "/restaurar")).expectStatus().isOk();

        assertThat(report(token, SALDO + OUTUBRO)).contains("\"totalDespesas\":15.00");
        assertThat(report(token, CATEGORIAS + OUTUBRO)).contains("\"total\":15.00", "\"quantidade\":2");
    }

    @Test
    void entriesOfOtherUsersAreNeverCounted() {
        String ana = newToken();
        addEntry(ana, "RECEITA", "100", "2026-10-01", "Receitas");
        addEntry(ana, "DESPESA", "40", "2026-10-02", "Lazer");
        String bia = newToken();
        addEntry(bia, "DESPESA", "7", "2026-10-03", "Lazer");

        assertThat(report(bia, SALDO + OUTUBRO)).contains("\"totalReceitas\":0.00", "\"totalDespesas\":7.00");
        assertThat(report(bia, CATEGORIAS + OUTUBRO)).contains("\"total\":7.00", "\"quantidade\":1");
    }

    @Test
    void summaryIsSortedFromLargestToSmallestTotal() {
        String token = newToken();
        addEntry(token, "DESPESA", "100", "2026-10-01", "Lazer");
        addEntry(token, "DESPESA", "150", "2026-10-02", "Lazer");
        addEntry(token, "DESPESA", "1500", "2026-10-03", "Moradia");
        addEntry(token, "RECEITA", "300", "2026-10-04", "Receitas");

        String body = report(token, CATEGORIAS + OUTUBRO);

        assertThat(JsonPath.<List<String>>read(body, "$[*].nome")).containsExactly("Moradia", "Receitas", "Lazer");
        assertThat(JsonPath.<String>read(body, "$[0].tipo")).isEqualTo("DESPESA");
        assertThat(JsonPath.<Integer>read(body, "$[2].quantidade")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(body, "$[0].categoriaId")).isEqualTo(categoryId(token, "Moradia"));
        assertThat(body).contains("\"total\":250.00");
    }

    @Test
    void summaryFiltersByType() {
        String token = newToken();
        addEntry(token, "DESPESA", "100", "2026-10-01", "Lazer");
        addEntry(token, "RECEITA", "300", "2026-10-04", "Receitas");

        String body = report(token, CATEGORIAS + OUTUBRO + "&tipo=DESPESA");

        assertThat(JsonPath.<List<String>>read(body, "$[*].nome")).containsExactly("Lazer");
    }

    @Test
    void summaryKeepsCategoriesDeactivatedLater() {
        String token = newToken();
        int category = JsonPath.read(request(token, client.post().uri("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("nome", "Mercado", "tipo", "DESPESA")))
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody(), "$.id");
        addEntry(token, "DESPESA", "80", "2026-10-01", category);
        request(token, client.put().uri("/api/v1/categorias/" + category)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("nome", "Mercado", "ativa", false)))
                .expectStatus().isOk();

        assertThat(report(token, CATEGORIAS + OUTUBRO)).contains("\"nome\":\"Mercado\"", "\"total\":80.00");
    }

    @Test
    void summaryNeverExposesIndividualEntries() {
        String token = newToken();
        addEntry(token, "DESPESA", "10", "2026-10-01", "Lazer");

        assertThat(report(token, CATEGORIAS + OUTUBRO)).doesNotContain("descricao", "Compra", "\"id\"", "\"data\"");
    }

    @Test
    void missingPeriodIsRejectedNamingTheParameter() {
        String token = newToken();

        for (String path : List.of(SALDO, CATEGORIAS)) {
            request(token, client.get().uri(path + "?fim=2026-10-31"))
                    .expectStatus().isBadRequest()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Dados inválidos")
                    .jsonPath("$.erros[0].campo").isEqualTo("inicio")
                    .jsonPath("$.erros[0].mensagem").isEqualTo("é obrigatório");
            request(token, client.get().uri(path + "?inicio=2026-10-01"))
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.erros[0].campo").isEqualTo("fim");
        }
    }

    @Test
    void invalidParametersGetBadRequest() {
        String token = newToken();

        request(token, client.get().uri(SALDO + "?inicio=01/10/2026&fim=2026-10-31")).expectStatus().isBadRequest();
        request(token, client.get().uri(CATEGORIAS + OUTUBRO + "&tipo=NAO_EXISTE")).expectStatus().isBadRequest();
    }

    @Test
    void reportsRequireToken() {
        client.get().uri(SALDO + OUTUBRO).exchange().expectStatus().isUnauthorized();
        client.get().uri(CATEGORIAS + OUTUBRO).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void tokenOfDeletedAccountCannotReadReports() {
        String token = newToken();
        request(token, client.delete().uri("/api/v1/usuarios/me")).expectStatus().isNoContent();

        request(token, client.get().uri(SALDO + OUTUBRO)).expectStatus().isUnauthorized();
        request(token, client.get().uri(CATEGORIAS + OUTUBRO)).expectStatus().isUnauthorized();
    }

    private String newToken() {
        return login(register());
    }

    private String report(String token, String uri) {
        return request(token, client.get().uri(uri)).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }

    private int addEntry(String token, String tipo, String valor, String data, String categoria) {
        return addEntry(token, tipo, valor, data, categoryId(token, categoria));
    }

    private int addEntry(String token, String tipo, String valor, String data, int categoriaId) {
        Map<String, Object> body = Map.of("tipo", tipo, "valor", new BigDecimal(valor), "data", data,
                "descricao", "Compra", "categoriaId", categoriaId);
        String response = request(token, client.post().uri("/api/v1/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body))
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(response, "$.id");
    }

    private int categoryId(String token, String nome) {
        List<Integer> ids = JsonPath.read(report(token, "/api/v1/categorias"), "$[?(@.nome == '" + nome + "')].id");
        return ids.getFirst();
    }

    private RestTestClient.ResponseSpec request(String token, RestTestClient.RequestHeadersSpec<?> spec) {
        return spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }
}
