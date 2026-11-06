package br.com.financeiro.backend.controller;

import br.com.financeiro.backend.dto.CadastroRequest;
import br.com.financeiro.backend.dto.LoginRequest;
import br.com.financeiro.backend.dto.TokenResponse;
import br.com.financeiro.backend.dto.UsuarioResponse;
import br.com.financeiro.backend.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas de autenticação ({@code docs/API.md}, seção 2.1). Cadastro e login são
 * públicos; sair exige o token que será encerrado.
 */
@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    /**
     * Cria o controller.
     *
     * @param authService regras de cadastro e login
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Cadastra um usuário.
     *
     * @param request dados do cadastro
     * @return 201 com o usuário criado, sem a senha
     */
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody CadastroRequest request) {
        UsuarioResponse created = UsuarioResponse.from(authService.register(request));
        return ResponseEntity.created(URI.create("/api/v1/usuarios/me")).body(created);
    }

    /**
     * Entra com e-mail e senha.
     *
     * @param request credenciais
     * @return 200 com o token
     */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Sai do aplicativo, encerrando o token usado no pedido. Única rota de
     * {@code /auth} que exige token.
     */
    @PostMapping("/sair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearer")
    public void logout() {
        authService.logout();
    }
}
