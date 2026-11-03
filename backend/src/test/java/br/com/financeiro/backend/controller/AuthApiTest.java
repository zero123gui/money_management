package br.com.financeiro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.financeiro.backend.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Exercita a autenticação de ponta a ponta: HTTP real, filtros de segurança e
 * PostgreSQL descartável.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Testcontainers
class AuthApiTest {

    private static final String SENHA = "uma-senha-forte";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    /** A chave é sorteada a cada execução; o datasource vem do contêiner. */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> Base64.getEncoder().encodeToString(randomKey()));
        registry.add("JWT_EXPIRATION", () -> "PT1H");
        registry.add("DB_PASSWORD", () -> "nao-usada");
    }

    @Autowired
    private RestTestClient client;

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

    @Test
    void loginWithVeryLongPasswordIsJustWrong() {
        String email = register();

        postLogin(email, "x".repeat(500)).expectStatus().isUnauthorized();
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

    private String register() {
        String email = uniqueEmail();
        postRegistration("Ana", email, SENHA).expectStatus().isCreated();
        return email;
    }

    private String login(String email) {
        String body = postLogin(email, SENHA).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$.token");
    }

    private RestTestClient.ResponseSpec postRegistration(String nome, String email, String senha) {
        return client.post().uri("/api/v1/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", nome, "email", email, "senha", senha))
                .exchange();
    }

    private RestTestClient.ResponseSpec postLogin(String email, String senha) {
        return client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "senha", senha))
                .exchange();
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

    private static byte[] randomKey() {
        byte[] key = new byte[48];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@exemplo.com";
    }
}
