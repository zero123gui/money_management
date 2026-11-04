package br.com.financeiro.desktop.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import br.com.financeiro.desktop.api.dto.CadastroRequest;
import br.com.financeiro.desktop.api.dto.CategoriaResponse;
import br.com.financeiro.desktop.api.dto.LancamentoRequest;
import br.com.financeiro.desktop.api.dto.LancamentoResponse;
import br.com.financeiro.desktop.api.dto.PageResponse;
import br.com.financeiro.desktop.api.dto.Tipo;
import br.com.financeiro.desktop.api.dto.LoginRequest;
import br.com.financeiro.desktop.api.dto.TokenResponse;
import br.com.financeiro.desktop.api.dto.UsuarioResponse;
import br.com.financeiro.desktop.session.Session;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testa o cliente contra um servidor HTTP local, que devolve as respostas do
 * contrato de {@code docs/API.md}.
 */
class ApiClientTest {

    private static final String USER_JSON = """
            {"id":1,"nome":"Ana","email":"ana@exemplo.com","criadoEm":"2026-10-28T14:00:00Z"}""";

    private final Session session = new Session();
    private final AtomicReference<String> receivedMethod = new AtomicReference<>();
    private final AtomicReference<String> receivedQuery = new AtomicReference<>();
    private final AtomicReference<String> receivedPath = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    private HttpServer server;
    private ApiClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        client = new ApiClient(ApiConfig.of("http://127.0.0.1:" + server.getAddress().getPort()), session);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void respondWith(String path, int status, String contentType, String body) {
        server.createContext("/api/v1" + path, exchange -> {
            requestCount.incrementAndGet();
            receivedMethod.set(exchange.getRequestMethod());
            receivedQuery.set(exchange.getRequestURI().getQuery());
            receivedPath.set(exchange.getRequestURI().getPath());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            receivedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    private void respondWithProblem(String path, int status, String detail) {
        respondWith(path, status, "application/problem+json", """
                {"type":"about:blank","title":"Erro","status":%d,"detail":"%s","instance":"/x"}"""
                .formatted(status, detail));
    }

    @Test
    void loginSendsCredentialsAndReadsToken() {
        respondWith("/auth/login", 200, "application/json", """
                {"token":"abc","tipo":"Bearer","expiraEm":"2026-10-28T15:30:00Z"}""");

        TokenResponse token = client.login(new LoginRequest("ana@exemplo.com", "senha-forte"));

        assertThat(token.token()).isEqualTo("abc");
        assertThat(token.expiraEm()).isEqualTo(Instant.parse("2026-10-28T15:30:00Z"));
        assertThat(receivedMethod.get()).isEqualTo("POST");
        assertThat(receivedBody.get()).isEqualTo("""
                {"email":"ana@exemplo.com","senha":"senha-forte"}""");
    }

    @Test
    void loginDoesNotOpenTheSessionByItself() {
        respondWith("/auth/login", 200, "application/json", """
                {"token":"abc","tipo":"Bearer","expiraEm":"2026-10-28T15:30:00Z"}""");

        client.login(new LoginRequest("ana@exemplo.com", "senha-forte"));

        assertThat(session.isActive()).isFalse();
    }

    @Test
    void publicRoutesDoNotSendToken() {
        session.open("token-antigo");
        respondWith("/auth/login", 200, "application/json", """
                {"token":"abc","tipo":"Bearer","expiraEm":"2026-10-28T15:30:00Z"}""");

        client.login(new LoginRequest("ana@exemplo.com", "senha-forte"));

        assertThat(receivedAuthorization.get()).isNull();
    }

    @Test
    void wrongCredentialsAreAFormErrorNotAnExpiredSession() {
        respondWithProblem("/auth/login", 401, "E-mail ou senha inválidos.");

        ApiException failure = catchThrowableOfType(ApiException.class,
                () -> client.login(new LoginRequest("ana@exemplo.com", "errada")));

        assertThat(failure).isNotInstanceOf(SessionExpiredException.class);
        assertThat(failure.getStatus()).isEqualTo(401);
        assertThat(failure.getMessage()).isEqualTo("E-mail ou senha inválidos.");
    }

    @Test
    void registerReadsCreatedUser() {
        respondWith("/auth/cadastro", 201, "application/json", USER_JSON);

        UsuarioResponse user = client.register(new CadastroRequest("Ana", "ana@exemplo.com", "senha-forte"));

        assertThat(user.nome()).isEqualTo("Ana");
        assertThat(user.criadoEm()).isEqualTo(Instant.parse("2026-10-28T14:00:00Z"));
    }

    @Test
    void validationErrorsListEachFieldWithItsLabel() {
        respondWith("/auth/cadastro", 400, "application/problem+json", """
                {"title":"Dados inválidos","status":400,"detail":"Um ou mais campos estão inválidos.",
                 "erros":[{"campo":"email","mensagem":"deve ser um e-mail válido"},
                          {"campo":"nome","mensagem":"é obrigatório"}]}""");

        ApiException failure = catchThrowableOfType(ApiException.class,
                () -> client.register(new CadastroRequest("", "x", "senha-forte")));

        assertThat(failure.getStatus()).isEqualTo(400);
        assertThat(failure.describe(Map.of("email", "E-mail")))
                .isEqualTo("E-mail: deve ser um e-mail válido\nnome: é obrigatório");
    }

    @Test
    void duplicatedEmailKeepsTheApiMessage() {
        respondWithProblem("/auth/cadastro", 409, "E-mail já cadastrado.");

        ApiException failure = catchThrowableOfType(ApiException.class,
                () -> client.register(new CadastroRequest("Ana", "ana@exemplo.com", "senha-forte")));

        assertThat(failure.getStatus()).isEqualTo(409);
        assertThat(failure.describe(Map.of())).isEqualTo("E-mail já cadastrado.");
    }

    @Test
    void authenticatedRoutesSendTheBearerToken() {
        session.open("meu-token");
        respondWith("/usuarios/me", 200, "application/json", USER_JSON);

        UsuarioResponse user = client.currentUser();

        assertThat(user.email()).isEqualTo("ana@exemplo.com");
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer meu-token");
    }

    @Test
    void rejectedTokenMeansExpiredSession() {
        session.open("token-vencido");
        respondWithProblem("/usuarios/me", 401, "Token inválido ou expirado.");

        assertThatThrownBy(client::currentUser).isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void withoutTokenNothingIsSent() {
        respondWith("/usuarios/me", 200, "application/json", USER_JSON);

        assertThatThrownBy(client::currentUser).isInstanceOf(SessionExpiredException.class);
        assertThat(requestCount).hasValue(0);
    }

    private static final String ENTRIES_PAGE_JSON = """
            {"conteudo":[{"id":42,"tipo":"DESPESA","valor":9999999999999.99,"data":"2026-10-28",
              "descricao":null,"categoria":{"id":7,"nome":"Moradia"},
              "criadoEm":"2026-10-28T14:30:00Z","atualizadoEm":"2026-10-28T14:30:00Z"}],
             "pagina":1,"tamanho":50,"totalElementos":51,"totalPaginas":2}""";

    @Test
    void listsEntriesKeepingTheExactAmount() {
        session.open("meu-token");
        respondWith("/lancamentos", 200, "application/json", ENTRIES_PAGE_JSON);

        PageResponse<LancamentoResponse> page =
                client.listLancamentos(new LancamentoFilter(null, null, null, null), 1, 50);

        LancamentoResponse entry = page.conteudo().getFirst();
        assertThat(entry.valor()).isEqualByComparingTo(new BigDecimal("9999999999999.99"));
        assertThat(entry.tipo()).isEqualTo(Tipo.DESPESA);
        assertThat(entry.data()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(entry.descricao()).isNull();
        assertThat(entry.categoria().nome()).isEqualTo("Moradia");
        assertThat(page.pagina()).isEqualTo(1);
        assertThat(page.totalElementos()).isEqualTo(51);
        assertThat(page.totalPaginas()).isEqualTo(2);
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer meu-token");
    }

    @Test
    void sendsOnlyTheFiltersThatWereChosen() {
        session.open("meu-token");
        respondWith("/lancamentos", 200, "application/json", ENTRIES_PAGE_JSON);

        client.listLancamentos(new LancamentoFilter(null, null, null, null), 0, 50);
        assertThat(receivedQuery.get()).isEqualTo("pagina=0&tamanho=50");

        client.listLancamentos(new LancamentoFilter(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), Tipo.RECEITA, 7L), 2, 20);
        assertThat(receivedQuery.get())
                .isEqualTo("inicio=2026-10-01&fim=2026-10-31&tipo=RECEITA&categoriaId=7&pagina=2&tamanho=20");
    }

    @Test
    void listingEntriesWithARejectedTokenMeansExpiredSession() {
        session.open("token-vencido");
        respondWithProblem("/lancamentos", 401, "Token inválido ou expirado.");

        assertThatThrownBy(() -> client.listLancamentos(new LancamentoFilter(null, null, null, null), 0, 50))
                .isInstanceOf(SessionExpiredException.class);
    }

    private static final String ENTRY_JSON = """
            {"id":42,"tipo":"DESPESA","valor":150.75,"data":"2026-10-28","descricao":"Compra",
             "categoria":{"id":7,"nome":"Moradia"},
             "criadoEm":"2026-10-28T14:30:00Z","atualizadoEm":"2026-10-28T14:30:00Z"}""";
    private static final LancamentoRequest ENTRY_REQUEST = new LancamentoRequest(
            Tipo.DESPESA, new BigDecimal("1000.00"), LocalDate.of(2026, 10, 28), "Compra", 7L);
    private static final String ENTRY_REQUEST_JSON = """
            {"tipo":"DESPESA","valor":1000.00,"data":"2026-10-28","descricao":"Compra","categoriaId":7}""";

    @Test
    void createsEntryInTheContractFormat() {
        session.open("meu-token");
        respondWith("/lancamentos", 201, "application/json", ENTRY_JSON);

        LancamentoResponse created = client.createLancamento(ENTRY_REQUEST);

        assertThat(receivedMethod.get()).isEqualTo("POST");
        assertThat(receivedPath.get()).isEqualTo("/api/v1/lancamentos");
        assertThat(receivedBody.get()).isEqualTo(ENTRY_REQUEST_JSON);
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer meu-token");
        assertThat(created.id()).isEqualTo(42L);
    }

    @Test
    void missingRequiredFieldsAreSentAsNullForTheApiToReport() {
        session.open("meu-token");
        respondWith("/lancamentos", 201, "application/json", ENTRY_JSON);

        client.createLancamento(new LancamentoRequest(Tipo.DESPESA, null, null, null, null));

        assertThat(receivedBody.get()).isEqualTo("""
                {"tipo":"DESPESA","valor":null,"data":null,"descricao":null,"categoriaId":null}""");
    }

    @Test
    void updatesEntryByItsId() {
        session.open("meu-token");
        respondWith("/lancamentos/42", 200, "application/json", ENTRY_JSON);

        client.updateLancamento(42L, ENTRY_REQUEST);

        assertThat(receivedMethod.get()).isEqualTo("PUT");
        assertThat(receivedPath.get()).isEqualTo("/api/v1/lancamentos/42");
        assertThat(receivedBody.get()).isEqualTo(ENTRY_REQUEST_JSON);
    }

    @Test
    void deletesEntryByItsIdAcceptingAnEmptyResponse() {
        session.open("meu-token");
        respondWith("/lancamentos/42", 204, "application/json", "");

        client.deleteLancamento(42L);

        assertThat(receivedMethod.get()).isEqualTo("DELETE");
        assertThat(receivedPath.get()).isEqualTo("/api/v1/lancamentos/42");
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer meu-token");
    }

    @Test
    void deletingAnEntryThatIsGoneReportsTheApiMessage() {
        session.open("meu-token");
        respondWithProblem("/lancamentos/42", 404, "Lançamento não encontrado.");

        assertThatThrownBy(() -> client.deleteLancamento(42L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Lançamento não encontrado.");
    }

    @Test
    void writingEntriesWithARejectedTokenMeansExpiredSession() {
        session.open("token-vencido");
        respondWithProblem("/lancamentos", 401, "Token inválido ou expirado.");

        assertThatThrownBy(() -> client.createLancamento(ENTRY_REQUEST)).isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void listsCategoriesIncludingInactiveOnes() {
        session.open("meu-token");
        respondWith("/categorias", 200, "application/json", """
                [{"id":3,"nome":"Moradia","tipo":"DESPESA","padrao":true,"ativa":true},
                 {"id":9,"nome":"Antiga","tipo":"RECEITA","padrao":false,"ativa":false}]""");

        List<CategoriaResponse> categories = client.listCategorias();

        assertThat(receivedQuery.get()).isEqualTo("incluirInativas=true");
        assertThat(categories).containsExactly(
                new CategoriaResponse(3L, "Moradia", Tipo.DESPESA, true, true),
                new CategoriaResponse(9L, "Antiga", Tipo.RECEITA, false, false));
    }

    @Test
    void ignoresFieldsTheClientDoesNotKnow() {
        session.open("meu-token");
        respondWith("/usuarios/me", 200, "application/json", """
                {"id":1,"nome":"Ana","email":"ana@exemplo.com","criadoEm":"2026-10-28T14:00:00Z","novo":true}""");

        assertThat(client.currentUser().nome()).isEqualTo("Ana");
    }

    @Test
    void errorThatIsNotJsonBecomesAGenericMessage() {
        respondWith("/auth/login", 502, "text/html", "<html>Bad Gateway</html>");

        ApiException failure = catchThrowableOfType(ApiException.class,
                () -> client.login(new LoginRequest("ana@exemplo.com", "senha-forte")));

        assertThat(failure.getStatus()).isEqualTo(502);
        assertThat(failure.getMessage()).isEqualTo("A API respondeu de forma inesperada. Tente novamente.");
    }

    @Test
    void successThatIsNotJsonBecomesAGenericMessage() {
        respondWith("/auth/login", 200, "text/html", "<html>portal de login do wi-fi</html>");

        assertThatThrownBy(() -> client.login(new LoginRequest("ana@exemplo.com", "senha-forte")))
                .isInstanceOf(ApiException.class)
                .hasMessage("A API respondeu de forma inesperada. Tente novamente.");
    }

    @Test
    void unreachableApiIsReportedAsNoResponse() {
        server.stop(0);

        ApiException failure = catchThrowableOfType(ApiException.class,
                () -> client.login(new LoginRequest("ana@exemplo.com", "senha-forte")));

        assertThat(failure.getStatus()).isEqualTo(ApiException.NO_RESPONSE);
        assertThat(failure.getMessage()).startsWith("Não foi possível conectar à API");
    }
}
