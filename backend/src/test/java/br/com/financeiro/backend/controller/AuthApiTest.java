package br.com.financeiro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financeiro.backend.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Exercita o cadastro, o login e a validação do token.
 */
class AuthApiTest extends ApiTestSupport {

    private static final int LOGIN_BODY_LIMIT = 4 * 1024;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void registeredUserLogsInAndReadsOwnData() {
        String email = uniqueEmail();

        postRegistration("Ana", email, SENHA)
                .expectStatus().isCreated()
                .expectHeader().valueEquals(HttpHeaders.LOCATION, "/api/v1/usuarios/me")
                .expectBody()
                .jsonPath("$.nome").isEqualTo("Ana")
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.criadoEm").value(String.class, value -> assertThat(value).matches(".*:\\d{2}Z"))
                .jsonPath("$.senha").doesNotExist()
                .jsonPath("$.senhaHash").doesNotExist();
        Long id = usuarios.findByEmail(email).orElseThrow().getId();

        client.get().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(email))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.intValue())
                .jsonPath("$.email").isEqualTo(email);
    }

    @Test
    void passwordIsStoredOnlyAsBcryptHash() {
        String email = register();

        String hash = usuarios.findByEmail(email).orElseThrow().getPasswordHash();

        assertThat(hash).startsWith("$2").doesNotContain(SENHA);
    }

    @Test
    void loginReturnsBearerTokenExpiringInOneHour() {
        String email = register();

        String body = postLogin(email, SENHA).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(JsonPath.<String>read(body, "$.tipo")).isEqualTo("Bearer");
        assertThat(JsonPath.<String>read(body, "$.expiraEm")).matches(".*:\\d{2}Z");
        Instant expiresAt = Instant.parse(JsonPath.read(body, "$.expiraEm"));
        assertThat(Duration.between(Instant.now(), expiresAt)).isBetween(Duration.ofMinutes(59), Duration.ofHours(1));
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() {
        String email = register();

        postRegistration("Outra", email.toUpperCase(), SENHA)
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.title").isEqualTo("E-mail já cadastrado");
    }

    @Test
    void loginIgnoresEmailCase() {
        String email = register();

        postLogin(email.toUpperCase(), SENHA).expectStatus().isOk();
    }

    @Test
    void invalidRegistrationListsEachFieldInPortuguese() {
        postRegistration("", "nao-e-email", "curta")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Dados inválidos")
                .jsonPath("$.instance").isEqualTo("/api/v1/auth/cadastro")
                .jsonPath("$.erros.length()").isEqualTo(3)
                .jsonPath("$.erros[0].campo").isEqualTo("email")
                .jsonPath("$.erros[0].mensagem").isEqualTo("deve ser um e-mail válido")
                .jsonPath("$.erros[1].campo").isEqualTo("nome")
                .jsonPath("$.erros[2].campo").isEqualTo("senha");
    }

    @Test
    void passwordOverBcryptLimitIsRejectedInBytes() {
        // 40 caracteres, mas 80 bytes em UTF-8: passaria em um limite por caracteres.
        String password = "é".repeat(40);
        assertThat(password.getBytes(StandardCharsets.UTF_8)).hasSize(80);

        postRegistration("Ana", uniqueEmail(), password)
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.erros[0].campo").isEqualTo("senha");
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() {
        String email = register();

        String wrongPassword = postLogin(email, "senha-errada").expectStatus().isUnauthorized()
                .expectBody(String.class).returnResult().getResponseBody();
        String unknownEmail = postLogin(uniqueEmail(), SENHA).expectStatus().isUnauthorized()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(unknownEmail).isEqualTo(wrongPassword).contains("E-mail ou senha inválidos.");
    }

    private void failLogin(String email, int times) {
        for (int i = 0; i < times; i++) {
            postLogin(email, "senha-errada").expectStatus().isUnauthorized();
        }
    }

    @Test
    void fiveWrongPasswordsBlockTheEmailEvenWithTheRightPassword() {
        String email = register();
        failLogin(email, 5);

        postLogin(email, SENHA)
                .expectStatus().isEqualTo(429)
                .expectHeader().value(HttpHeaders.RETRY_AFTER,
                        seconds -> assertThat(Long.parseLong(seconds)).isBetween(1L, 900L))
                .expectBody()
                .jsonPath("$.status").isEqualTo(429)
                .jsonPath("$.detail").isEqualTo("Muitas tentativas de login. Tente novamente em 15 minutos.");
    }

    @Test
    void blockedAnswerDoesNotRevealWhetherTheEmailExists() {
        String registered = register();
        String unknown = uniqueEmail();
        failLogin(registered, 5);
        failLogin(unknown, 5);

        String registeredAnswer = postLogin(registered, SENHA).expectStatus().isEqualTo(429)
                .expectBody(String.class).returnResult().getResponseBody();
        String unknownAnswer = postLogin(unknown, SENHA).expectStatus().isEqualTo(429)
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(unknownAnswer).isEqualTo(registeredAnswer.replace(registered, unknown));
    }

    @Test
    void capitalLettersDoNotEscapeTheBlock() {
        String email = register();
        failLogin(email, 5);

        postLogin(email.toUpperCase(), SENHA).expectStatus().isEqualTo(429);
    }

    @Test
    void rightPasswordClearsTheFailureCount() {
        String email = register();
        failLogin(email, 4);
        postLogin(email, SENHA).expectStatus().isOk();

        failLogin(email, 4);

        postLogin(email, SENHA).expectStatus().isOk();
    }

    @Test
    void blockingOneEmailDoesNotAffectOthersOrValidTokens() {
        String blocked = register();
        String token = login(blocked);
        String other = register();
        failLogin(blocked, 5);

        postLogin(other, SENHA).expectStatus().isOk();
        client.get().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void loginWithVeryLongPasswordIsJustWrong() {
        String email = register();

        postLogin(email, "x".repeat(500)).expectStatus().isUnauthorized();
    }

    @Test
    void loginBodyAtTheLimitIsAccepted() {
        postRawLogin(loginBodyOfSize(LOGIN_BODY_LIMIT)).expectStatus().isUnauthorized();
    }

    @Test
    void loginBodyOverTheLimitIsRejected() {
        postRawLogin(loginBodyOfSize(LOGIN_BODY_LIMIT + 1))
                .expectStatus().isEqualTo(413)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(413)
                .jsonPath("$.detail").isEqualTo("O corpo do pedido de login é grande demais.");
    }

    @Test
    void chunkedLoginBodyOverTheLimitIsRejected() throws Exception {
        assertThat(postChunkedLogin(loginBodyOfSize(LOGIN_BODY_LIMIT + 1))).isEqualTo(413);
    }

    @Test
    void chunkedLoginWithinTheLimitStillWorks() throws Exception {
        String email = register();
        String body = "{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}";

        assertThat(postChunkedLogin(body)).isEqualTo(200);
    }

    private RestTestClient.ResponseSpec postRawLogin(String body) {
        return client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    /** Envia sem declarar o tamanho, como faz um cliente que tenta escapar do limite. */
    private int postChunkedLogin(String body) throws Exception {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(content)))
                .build();
        try (HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }

    private static String loginBodyOfSize(int size) {
        String email = uniqueEmail();
        String template = "{\"email\":\"" + email + "\",\"senha\":\"\"}";
        String password = "x".repeat(size - template.length());
        return "{\"email\":\"" + email + "\",\"senha\":\"" + password + "\"}";
    }

    @Test
    void malformedJsonGetsBadRequest() {
        client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{isto nao e json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Dados inválidos");
    }

    @Test
    void protectedRouteRejectsMissingToken() {
        client.get().uri("/api/v1/usuarios/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Não autenticado")
                .jsonPath("$.instance").isEqualTo("/api/v1/usuarios/me");
    }

    @Test
    void protectedRouteRejectsGarbageToken() {
        assertUnauthorized("isto-nao-e-um-token");
    }

    @Test
    void protectedRouteRejectsExpiredToken() {
        String email = register();
        Long id = usuarios.findByEmail(email).orElseThrow().getId();
        Instant past = Instant.now().minus(Duration.ofHours(2));

        assertUnauthorized(sign(jwtEncoder, id, past, past.plus(Duration.ofHours(1))));
    }

    @Test
    void protectedRouteRejectsTokenSignedWithAnotherKey() {
        String email = register();
        Long id = usuarios.findByEmail(email).orElseThrow().getId();
        JwtEncoder forger = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(randomKey(), "HmacSHA256")));

        assertUnauthorized(sign(forger, id, Instant.now(), Instant.now().plus(Duration.ofHours(1))));
    }

    @Test
    void tokenOfDeletedAccountStopsWorking() {
        String email = register();
        String token = login(email);
        usuarios.deleteById(usuarios.findByEmail(email).orElseThrow().getId());

        assertUnauthorized(token);
    }

    @Test
    void logoutEndsOnlyTheTokenUsed() {
        String email = register();
        String notebook = login(email);
        String desktop = login(email);

        logout(notebook).expectStatus().isNoContent().expectBody().isEmpty();

        assertUnauthorized(notebook);
        getMe(desktop).expectStatus().isOk();
        postLogin(email, SENHA).expectStatus().isOk();
    }

    @Test
    void logoutRequiresToken() {
        client.post().uri("/api/v1/auth/sair").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void loggedOutTokenCannotLogOutAgain() {
        String token = login(register());
        logout(token).expectStatus().isNoContent();

        logout(token).expectStatus().isUnauthorized();
    }

    @Test
    void validlySignedTokenWithoutIdOrVersionIsRejected() {
        // Assinado com a chave certa e dentro da validade, mas sem as claims que o encerram.
        String email = register();
        Long id = usuarios.findByEmail(email).orElseThrow().getId();

        assertUnauthorized(sign(jwtEncoder, id, Instant.now(), Instant.now().plus(Duration.ofHours(1))));
    }

    @Test
    void unknownRouteDoesNotRevealItselfWithoutToken() {
        client.get().uri("/api/v1/nao-existe").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void unknownRouteGetsPortugueseNotFoundWithToken() {
        client.get().uri("/api/v1/nao-existe")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(register()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Não encontrado");
    }

    private void assertUnauthorized(String token) {
        client.get().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.title").isEqualTo("Não autenticado");
    }

    private static String sign(JwtEncoder encoder, Long userId, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
