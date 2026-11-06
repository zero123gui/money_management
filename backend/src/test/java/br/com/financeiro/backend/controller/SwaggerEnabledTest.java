package br.com.financeiro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Confere a especificação publicada quando o Swagger está ligado: ela precisa
 * bater com o contrato de {@code docs/API.md}.
 */
@TestPropertySource(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class SwaggerEnabledTest extends ApiTestSupport {

    private String spec() {
        return client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }

    private static List<String> queryParameters(String spec, String path) {
        return JsonPath.read(spec, "$.paths['" + path + "'].get.parameters[?(@.in == 'query')].name");
    }

    @Test
    void swaggerPageOpensWithoutToken() {
        client.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
    }

    @Test
    void specListsExactlyTheEndpointsOfTheContract() {
        Map<String, Map<String, Object>> paths = JsonPath.read(spec(), "$.paths");

        assertThat(paths).containsOnlyKeys(
                "/api/v1/auth/cadastro", "/api/v1/auth/login", "/api/v1/auth/sair",
                "/api/v1/usuarios/me", "/api/v1/usuarios/me/senha",
                "/api/v1/categorias", "/api/v1/categorias/{id}",
                "/api/v1/lancamentos", "/api/v1/lancamentos/{id}", "/api/v1/lancamentos/excluidos",
                "/api/v1/lancamentos/{id}/restaurar",
                "/api/v1/relatorios/saldo", "/api/v1/relatorios/categorias");
        assertThat(paths.get("/api/v1/usuarios/me")).containsOnlyKeys("get", "delete");
        assertThat(paths.get("/api/v1/categorias")).containsOnlyKeys("get", "post");
        assertThat(paths.get("/api/v1/categorias/{id}")).containsOnlyKeys("put");
        assertThat(paths.get("/api/v1/lancamentos")).containsOnlyKeys("get", "post");
        assertThat(paths.get("/api/v1/lancamentos/{id}")).containsOnlyKeys("get", "put", "delete");
        assertThat(paths.values().stream().mapToInt(Map::size).sum()).isEqualTo(18);
    }

    @Test
    void paginationUsesTheNamesOfTheContract() {
        String spec = spec();

        assertThat(queryParameters(spec, "/api/v1/lancamentos"))
                .containsExactlyInAnyOrder("inicio", "fim", "tipo", "categoriaId", "pagina", "tamanho");
        assertThat(queryParameters(spec, "/api/v1/lancamentos/excluidos"))
                .containsExactlyInAnyOrder("pagina", "tamanho");
    }

    @Test
    void onlyAuthRoutesAreMarkedAsPublic() {
        String spec = spec();

        assertThat(JsonPath.<List<?>>read(spec, "$.paths['/api/v1/auth/login'].post.security")).isEmpty();
        assertThat(JsonPath.<List<?>>read(spec, "$.paths['/api/v1/auth/cadastro'].post.security")).isEmpty();
        assertThat(JsonPath.<List<?>>read(spec, "$.paths['/api/v1/auth/sair'].post.security[*].bearer")).hasSize(1);
        assertThat(JsonPath.<List<?>>read(spec, "$.security[*].bearer")).hasSize(1);
        assertThat(JsonPath.<Map<String, Object>>read(spec, "$.paths['/api/v1/usuarios/me'].get"))
                .doesNotContainKey("security");
    }

    @Test
    void specDoesNotExposeInternalFields() {
        assertThat(spec()).doesNotContain("senhaHash", "passwordHash", "senha_hash");
    }

    @Test
    void otherRoutesStillRequireToken() {
        client.get().uri("/api/v1/lancamentos").exchange().expectStatus().isUnauthorized();
    }
}
