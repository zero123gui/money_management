package br.com.financeiro.backend.service;

import br.com.financeiro.backend.entity.Categoria;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.exception.ApiException;
import br.com.financeiro.backend.repository.UsuarioRepository;
import br.com.financeiro.backend.security.SignupProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Ponto único das regras de autorização (RNF-04). O usuário sempre vem do
 * token, nunca do corpo ou da URL. Controllers e outros services usam este
 * service em vez de repetir as regras.
 */
@Service
public class AuthorizationService {

    private final UsuarioRepository usuarios;
    private final SignupProperties signup;

    /**
     * Cria o service.
     *
     * @param usuarios acesso a usuários
     * @param signup   quem pode criar conta
     */
    public AuthorizationService(UsuarioRepository usuarios, SignupProperties signup) {
        this.usuarios = usuarios;
        this.signup = signup;
    }

    /**
     * Garante que o e-mail pode criar conta.
     *
     * @param email e-mail informado no cadastro
     * @throws ApiException com 403 se o cadastro for restrito e o e-mail não estiver na lista
     */
    public void assertCanRegister(String email) {
        if (!signup.allows(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cadastro não permitido",
                    "Este e-mail não está autorizado a criar uma conta.");
        }
    }

    /**
     * Devolve o id do usuário autenticado, usado para filtrar as consultas.
     *
     * @return o id presente no token
     * @throws AuthenticationCredentialsNotFoundException se não houver token validado
     */
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return Long.valueOf(token.getName());
        }
        throw new AuthenticationCredentialsNotFoundException("Requisição sem token validado");
    }

    /**
     * Devolve o usuário autenticado.
     *
     * @return o usuário do token
     * @throws AuthenticationCredentialsNotFoundException se não houver token validado
     *         ou se a conta tiver sido excluída depois de o token ser emitido
     */
    public Usuario currentUser() {
        return usuarios.findById(currentUserId())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Conta do token não existe mais"));
    }

    /**
     * Garante que o usuário autenticado pode alterar a categoria.
     *
     * @param categoria categoria a alterar
     * @throws ApiException com 403 se for uma categoria padrão, ou com 404 se for
     *                      de outro usuário, para não revelar que ela existe
     */
    public void assertCanEdit(Categoria categoria) {
        if (categoria.isStandard()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ação não permitida",
                    "Categorias padrão não podem ser alteradas.");
        }
        if (!categoria.getOwner().getId().equals(currentUserId())) {
            throw ApiException.notFound();
        }
    }
}
