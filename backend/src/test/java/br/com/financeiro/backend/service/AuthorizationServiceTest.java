package br.com.financeiro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.financeiro.backend.entity.Categoria;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.exception.ApiException;
import br.com.financeiro.backend.repository.UsuarioRepository;
import br.com.financeiro.backend.security.SignupProperties;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regras do service centralizado de autorização (RNF-14).
 */
class AuthorizationServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AuthorizationService authorization = new AuthorizationService(usuarios,
            new SignupProperties(true, Set.of("Na-Lista@exemplo.com")));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void restrictedSignupAcceptsOnlyTheList() {
        assertThatCode(() -> authorization.assertCanRegister("na-lista@EXEMPLO.com")).doesNotThrowAnyException();
        assertThatThrownBy(() -> authorization.assertCanRegister("fora@exemplo.com"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void restrictedSignupWithEmptyListAcceptsNobody() {
        AuthorizationService closed = new AuthorizationService(usuarios, new SignupProperties(true, Set.of()));

        assertThatThrownBy(() -> closed.assertCanRegister("qualquer@exemplo.com")).isInstanceOf(ApiException.class);
    }

    @Test
    void unrestrictedSignupAcceptsAnyEmail() {
        AuthorizationService open = new AuthorizationService(usuarios, new SignupProperties(false, Set.of()));

        assertThatCode(() -> open.assertCanRegister("qualquer@exemplo.com")).doesNotThrowAnyException();
    }

    @Test
    void currentUserIdComesFromToken() {
        authenticateAs(42L);

        assertThat(authorization.currentUserId()).isEqualTo(42L);
    }

    @Test
    void noTokenIsRejected() {
        assertThatThrownBy(authorization::currentUserId)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void tokenOfDeletedAccountIsRejected() {
        authenticateAs(42L);
        when(usuarios.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(authorization::currentUser)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void ownerCanEditOwnCategory() {
        authenticateAs(1L);

        assertThatCode(() -> authorization.assertCanEdit(categoryOwnedBy(1L))).doesNotThrowAnyException();
    }

    @Test
    void categoryOfAnotherUserLooksLikeItDoesNotExist() {
        authenticateAs(1L);

        assertThatThrownBy(() -> authorization.assertCanEdit(categoryOwnedBy(2L)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void standardCategoryCannotBeEdited() {
        authenticateAs(1L);
        Categoria standard = new Categoria("Lazer", Tipo.DESPESA, null);

        assertThatThrownBy(() -> authorization.assertCanEdit(standard))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static Categoria categoryOwnedBy(long ownerId) {
        Usuario owner = new Usuario("Nome", "dono@exemplo.com", "hash-de-teste");
        ReflectionTestUtils.setField(owner, "id", ownerId);
        return new Categoria("Mercado", Tipo.DESPESA, owner);
    }

    private static void authenticateAs(long userId) {
        Jwt jwt = Jwt.withTokenValue("token-de-teste")
                .header("alg", "HS256")
                .subject(String.valueOf(userId))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
