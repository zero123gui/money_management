package br.com.financeiro.backend.controller;

import br.com.financeiro.backend.dto.UsuarioResponse;
import br.com.financeiro.backend.service.AuthorizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas do próprio usuário ({@code docs/API.md}, seção 2.2).
 */
@RestController
@RequestMapping("/api/v1/usuarios/me")
public class UsuarioController {

    private final AuthorizationService authorization;

    /**
     * Cria o controller.
     *
     * @param authorization identifica o usuário autenticado
     */
    public UsuarioController(AuthorizationService authorization) {
        this.authorization = authorization;
    }

    /**
     * Consulta os dados do usuário autenticado.
     *
     * @return os dados do usuário, sem a senha
     */
    @GetMapping
    public UsuarioResponse me() {
        return UsuarioResponse.from(authorization.currentUser());
    }
}
