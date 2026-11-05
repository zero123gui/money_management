package br.com.financeiro.backend.controller;

import br.com.financeiro.backend.dto.AlterarSenhaRequest;
import br.com.financeiro.backend.dto.ExcluirContaRequest;
import br.com.financeiro.backend.dto.UsuarioResponse;
import br.com.financeiro.backend.service.AuthorizationService;
import br.com.financeiro.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas do próprio usuário ({@code docs/API.md}, seção 2.2).
 */
@RestController
@RequestMapping("/api/v1/usuarios/me")
public class UsuarioController {

    private final AuthorizationService authorization;
    private final UsuarioService usuarioService;

    /**
     * Cria o controller.
     *
     * @param authorization  identifica o usuário autenticado
     * @param usuarioService ações sobre a própria conta
     */
    public UsuarioController(AuthorizationService authorization, UsuarioService usuarioService) {
        this.authorization = authorization;
        this.usuarioService = usuarioService;
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

    /**
     * Troca a senha do usuário autenticado.
     *
     * @param request senha atual e nova senha
     */
    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody AlterarSenhaRequest request) {
        usuarioService.changePassword(request);
    }

    /**
     * Exclui definitivamente a conta do usuário autenticado e todos os seus dados.
     *
     * @param request senha atual, que confirma a exclusão
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@Valid @RequestBody ExcluirContaRequest request) {
        usuarioService.deleteAccount(request);
    }
}
