package br.com.financeiro.backend.service;

import br.com.financeiro.backend.dto.CadastroRequest;
import br.com.financeiro.backend.dto.LoginRequest;
import br.com.financeiro.backend.dto.TokenResponse;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.exception.ApiException;
import br.com.financeiro.backend.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Cadastro e login de usuários.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String decoyHash;

    /**
     * Cria o service.
     *
     * @param usuarios        acesso a usuários
     * @param passwordEncoder codificador de senhas
     * @param tokenService    emissor de tokens
     */
    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.decoyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Cadastra um usuário.
     *
     * @param request dados do cadastro
     * @return o usuário criado
     * @throws ApiException com 409 se o e-mail já estiver cadastrado
     */
    public Usuario register(CadastroRequest request) {
        if (usuarios.findByEmail(request.email()).isPresent()) {
            throw emailAlreadyRegistered();
        }
        try {
            return usuarios.save(new Usuario(request.nome(), request.email(), passwordEncoder.encode(request.senha())));
        } catch (DataIntegrityViolationException concurrentRegistration) {
            // Dois cadastros simultâneos passam pela consulta acima; o índice único barra o segundo.
            throw emailAlreadyRegistered();
        }
    }

    /**
     * Confere as credenciais e emite um token.
     *
     * @param request e-mail e senha
     * @return o token do usuário
     * @throws ApiException com 401 se o e-mail não existir ou a senha estiver errada
     */
    public TokenResponse login(LoginRequest request) {
        Optional<Usuario> usuario = usuarios.findByEmail(request.email());
        // Confere um hash mesmo sem usuário: o BCrypt é lento de propósito, e
        // responder mais rápido revelaria quais e-mails estão cadastrados.
        String hash = usuario.map(Usuario::getPasswordHash).orElse(decoyHash);
        boolean passwordMatches = passwordEncoder.matches(request.senha(), hash);
        if (usuario.isEmpty() || !passwordMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha inválidos.");
        }
        return tokenService.issue(usuario.get());
    }

    private static ApiException emailAlreadyRegistered() {
        return new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado", "Já existe uma conta com este e-mail.");
    }
}
