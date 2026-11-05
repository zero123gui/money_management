package br.com.financeiro.backend.service;

import br.com.financeiro.backend.dto.CadastroRequest;
import br.com.financeiro.backend.dto.LoginRequest;
import br.com.financeiro.backend.dto.TokenResponse;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.exception.ApiException;
import br.com.financeiro.backend.exception.TooManyLoginAttemptsException;
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

    private final AuthorizationService authorization;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginAttemptLimiter loginAttempts;
    private final String decoyHash;

    /**
     * Cria o service.
     *
     * @param authorization   diz quem pode criar conta
     * @param usuarios        acesso a usuários
     * @param passwordEncoder codificador de senhas
     * @param tokenService    emissor de tokens
     * @param loginAttempts   limite de tentativas de login
     */
    public AuthService(AuthorizationService authorization, UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder, TokenService tokenService, LoginAttemptLimiter loginAttempts) {
        this.authorization = authorization;
        this.usuarios = usuarios;
        this.loginAttempts = loginAttempts;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.decoyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Cadastra um usuário.
     *
     * @param request dados do cadastro
     * @return o usuário criado
     * @throws ApiException com 403 se o e-mail não puder criar conta, ou com 409 se já estiver cadastrado
     */
    public Usuario register(CadastroRequest request) {
        // Antes de procurar o e-mail: o 409 revelaria a quem está fora da lista quem já tem conta.
        authorization.assertCanRegister(request.email());
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
     * @throws TooManyLoginAttemptsException com 429 se o e-mail estiver bloqueado
     */
    public TokenResponse login(LoginRequest request) {
        // Antes de conferir a senha: uma senha certa também é recusada, senão o
        // atacante continuaria descobrindo se acertou mesmo com o bloqueio.
        loginAttempts.remainingBlock(request.email()).ifPresent(remaining -> {
            throw new TooManyLoginAttemptsException(remaining);
        });
        Optional<Usuario> usuario = usuarios.findByEmail(request.email());
        // Confere um hash mesmo sem usuário: o BCrypt é lento de propósito, e
        // responder mais rápido revelaria quais e-mails estão cadastrados.
        String hash = usuario.map(Usuario::getPasswordHash).orElse(decoyHash);
        boolean passwordMatches = passwordEncoder.matches(request.senha(), hash);
        if (usuario.isEmpty() || !passwordMatches) {
            loginAttempts.recordFailure(request.email());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha inválidos.");
        }
        loginAttempts.recordSuccess(request.email());
        return tokenService.issue(usuario.get());
    }

    private static ApiException emailAlreadyRegistered() {
        return new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado", "Já existe uma conta com este e-mail.");
    }
}
