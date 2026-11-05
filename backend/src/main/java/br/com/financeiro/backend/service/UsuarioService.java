package br.com.financeiro.backend.service;

import br.com.financeiro.backend.dto.AlterarSenhaRequest;
import br.com.financeiro.backend.dto.ExcluirContaRequest;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.exception.InvalidFieldException;
import br.com.financeiro.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ações do usuário autenticado sobre a própria conta.
 */
@Service
public class UsuarioService {

    private final AuthorizationService authorization;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria o service.
     *
     * @param authorization   identifica o usuário autenticado
     * @param usuarios        acesso a usuários
     * @param passwordEncoder codificador de senhas
     */
    public UsuarioService(AuthorizationService authorization, UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder) {
        this.authorization = authorization;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Troca a senha do usuário autenticado (RF-04).
     *
     * @param request senha atual e nova senha
     * @throws InvalidFieldException se a senha atual estiver errada
     */
    @Transactional
    public void changePassword(AlterarSenhaRequest request) {
        Usuario usuario = authorization.currentUser();
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getPasswordHash())) {
            throw new InvalidFieldException("senhaAtual", "está incorreta");
        }
        usuario.changePassword(passwordEncoder.encode(request.novaSenha()));
    }

    /**
     * Exclui definitivamente a conta do usuário autenticado (RF-05). O banco
     * apaga em cascata as categorias próprias e os lançamentos. A senha é exigida
     * porque a ação não tem volta: um token roubado não basta para apagar tudo.
     *
     * @param request senha atual
     * @throws InvalidFieldException se a senha estiver errada
     */
    @Transactional
    public void deleteAccount(ExcluirContaRequest request) {
        Usuario usuario = authorization.currentUser();
        if (!passwordEncoder.matches(request.senha(), usuario.getPasswordHash())) {
            throw new InvalidFieldException("senha", "está incorreta");
        }
        usuarios.deleteById(usuario.getId());
    }
}
