package br.com.financeiro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financeiro.backend.entity.Categoria;
import br.com.financeiro.backend.entity.Lancamento;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.repository.CategoriaRepository;
import br.com.financeiro.backend.repository.LancamentoRepository;
import br.com.financeiro.backend.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Exercita a troca de senha e a exclusão de conta.
 */
class UsuarioApiTest extends ApiTestSupport {

    private static final String NOVA_SENHA = "outra-senha-forte";

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CategoriaRepository categorias;

    @Autowired
    private LancamentoRepository lancamentos;

    @Test
    void changedPasswordReplacesTheOldOne() {
        String email = register();
        String hashBefore = usuarios.findByEmail(email).orElseThrow().getPasswordHash();

        putPassword(login(email), SENHA, NOVA_SENHA).expectStatus().isNoContent().expectBody().isEmpty();

        postLogin(email, SENHA).expectStatus().isUnauthorized();
        postLogin(email, NOVA_SENHA).expectStatus().isOk();
        assertThat(usuarios.findByEmail(email).orElseThrow().getPasswordHash())
                .isNotEqualTo(hashBefore)
                .startsWith("$2")
                .doesNotContain(NOVA_SENHA);
    }

    @Test
    void changingPasswordEndsEveryTokenOfTheUser() {
        String email = register();
        String notebook = login(email);
        String desktop = login(email);

        putPassword(notebook, SENHA, NOVA_SENHA).expectStatus().isNoContent();

        getMe(notebook).expectStatus().isUnauthorized();
        getMe(desktop).expectStatus().isUnauthorized();
        String body = postLogin(email, NOVA_SENHA).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        getMe(JsonPath.read(body, "$.token")).expectStatus().isOk();
    }

    @Test
    void failedPasswordChangeKeepsTheToken() {
        String token = login(register());

        putPassword(token, "senha-errada", NOVA_SENHA).expectStatus().isBadRequest();

        getMe(token).expectStatus().isOk();
    }

    @Test
    void wrongCurrentPasswordIsRejectedAndChangesNothing() {
        String email = register();

        putPassword(login(email), "senha-errada", NOVA_SENHA)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Dados inválidos")
                .jsonPath("$.erros[0].campo").isEqualTo("senhaAtual")
                .jsonPath("$.erros[0].mensagem").isEqualTo("está incorreta");

        postLogin(email, SENHA).expectStatus().isOk();
        postLogin(email, NOVA_SENHA).expectStatus().isUnauthorized();
    }

    @Test
    void invalidNewPasswordIsRejectedAndChangesNothing() {
        String email = register();

        putPassword(login(email), SENHA, "curta")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.erros[0].campo").isEqualTo("novaSenha");

        postLogin(email, SENHA).expectStatus().isOk();
    }

    @Test
    void changingPasswordDoesNotAffectOtherUsers() {
        String ana = register();
        String bia = register();

        putPassword(login(ana), SENHA, NOVA_SENHA).expectStatus().isNoContent();

        postLogin(bia, SENHA).expectStatus().isOk();
    }

    @Test
    void accountRoutesRequireToken() {
        client.put().uri("/api/v1/usuarios/me/senha")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senhaAtual", SENHA, "novaSenha", NOVA_SENHA))
                .exchange()
                .expectStatus().isUnauthorized();

        client.delete().uri("/api/v1/usuarios/me").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deletingAccountRemovesOwnDataAndKeepsEveryoneElses() {
        String anaEmail = register();
        String biaEmail = register();
        Owned ana = createData(anaEmail);
        Owned bia = createData(biaEmail);

        deleteAccount(login(anaEmail), SENHA)
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        assertThat(usuarios.findById(ana.userId())).isEmpty();
        assertThat(lancamentos.findByIdAndOwnerIdAndDeletedAtIsNull(ana.entryId(), ana.userId())).isEmpty();
        assertThat(categorias.findVisibleById(ana.categoryId(), ana.userId())).isEmpty();
        assertThat(usuarios.findById(bia.userId())).isPresent();
        assertThat(lancamentos.findByIdAndOwnerIdAndDeletedAtIsNull(bia.entryId(), bia.userId())).isPresent();
        assertThat(categorias.findVisibleById(bia.categoryId(), bia.userId())).isPresent();
        assertThat(categorias.findAllVisible(bia.userId())).filteredOn(Categoria::isStandard).hasSize(8);
    }

    @Test
    void deletedAccountCanNoLongerActOrLogIn() {
        String email = register();
        String token = login(email);

        deleteAccount(token, SENHA).expectStatus().isNoContent();

        deleteAccount(token, SENHA).expectStatus().isUnauthorized();
        putPassword(token, SENHA, NOVA_SENHA).expectStatus().isUnauthorized();
        postLogin(email, SENHA).expectStatus().isUnauthorized();
    }

    @Test
    void deletedEmailCanRegisterAgain() {
        String email = register();

        deleteAccount(login(email), SENHA).expectStatus().isNoContent();

        postRegistration("Ana", email, SENHA).expectStatus().isCreated();
    }

    @Test
    void wrongPasswordDoesNotDeleteTheAccount() {
        String email = register();
        String token = login(email);

        deleteAccount(token, "senha-errada")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.erros[0].campo").isEqualTo("senha")
                .jsonPath("$.erros[0].mensagem").isEqualTo("está incorreta");

        assertThat(usuarios.findByEmail(email)).isPresent();
        postLogin(email, SENHA).expectStatus().isOk();
    }

    @Test
    void deletingAccountWithoutPasswordIsRejected() {
        String email = register();
        String token = login(email);

        client.delete().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest();
        deleteAccount(token, " ")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.erros[0].campo").isEqualTo("senha");

        assertThat(usuarios.findByEmail(email)).isPresent();
    }

    private RestTestClient.ResponseSpec putPassword(String token, String senhaAtual, String novaSenha) {
        return client.put().uri("/api/v1/usuarios/me/senha")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senhaAtual", senhaAtual, "novaSenha", novaSenha))
                .exchange();
    }

    private Owned createData(String email) {
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        Categoria categoria = categorias.save(new Categoria("Mercado", Tipo.DESPESA, usuario));
        Lancamento lancamento = lancamentos.save(new Lancamento(usuario, Tipo.DESPESA, new BigDecimal("10.00"),
                LocalDate.of(2026, 10, 1), categoria, null));
        return new Owned(usuario.getId(), categoria.getId(), lancamento.getId());
    }

    private record Owned(Long userId, Long categoryId, Long entryId) {
    }
}
