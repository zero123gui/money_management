package br.com.financeiro.backend.service;

import br.com.financeiro.backend.dto.AlterarCategoriaRequest;
import br.com.financeiro.backend.dto.CriarCategoriaRequest;
import br.com.financeiro.backend.entity.Categoria;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.exception.ApiException;
import br.com.financeiro.backend.repository.CategoriaRepository;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta e manutenção de categorias (RF-14, RF-15).
 */
@Service
public class CategoriaService {

    private final AuthorizationService authorization;
    private final CategoriaRepository categorias;

    /**
     * Cria o service.
     *
     * @param authorization regras de acesso e usuário autenticado
     * @param categorias    acesso a categorias
     */
    public CategoriaService(AuthorizationService authorization, CategoriaRepository categorias) {
        this.authorization = authorization;
        this.categorias = categorias;
    }

    /**
     * Lista as categorias padrão e as do usuário autenticado, ordenadas por nome.
     * A ordem é feita aqui porque a do banco depende da configuração dele: em
     * algumas, "Água" viria depois de "Zoológico".
     *
     * @param type            filtra por este tipo; {@code null} traz todos
     * @param includeInactive se as categorias desativadas entram na lista
     * @return as categorias que o usuário pode ver
     */
    public List<Categoria> list(Tipo type, boolean includeInactive) {
        Comparator<Object> byPortugueseName = Collator.getInstance(Locale.of("pt", "BR"));
        return categorias.findAllVisible(authorization.currentUser().getId()).stream()
                .filter(categoria -> type == null || categoria.getType() == type)
                .filter(categoria -> includeInactive || categoria.isActive())
                .sorted(Comparator.comparing(Categoria::getName, byPortugueseName))
                .toList();
    }

    /**
     * Cria uma categoria própria do usuário autenticado.
     *
     * @param request nome e tipo
     * @return a categoria criada
     * @throws ApiException com 409 se o usuário já tiver uma categoria com esse nome e tipo
     */
    public Categoria create(CriarCategoriaRequest request) {
        Usuario owner = authorization.currentUser();
        String name = request.nome().strip();
        if (categorias.existsOwned(owner.getId(), name, request.tipo())) {
            throw duplicateName();
        }
        try {
            return categorias.save(new Categoria(name, request.tipo(), owner));
        } catch (DataIntegrityViolationException concurrentCreation) {
            // Dois pedidos simultâneos passam pela consulta acima; o índice único barra o segundo.
            throw duplicateName();
        }
    }

    /**
     * Renomeia, desativa ou reativa uma categoria própria.
     *
     * @param id      identificador da categoria
     * @param request novo nome e situação
     * @return a categoria alterada
     * @throws ApiException com 404 se a categoria não existir ou for de outro usuário,
     *                      com 403 se for padrão, ou com 409 se o nome já estiver em uso
     */
    @Transactional
    public Categoria update(Long id, AlterarCategoriaRequest request) {
        Long ownerId = authorization.currentUser().getId();
        Categoria categoria = categorias.findVisibleById(id, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Não encontrado",
                        "O recurso pedido não existe."));
        authorization.assertCanEdit(categoria);

        String name = request.nome().strip();
        if (categorias.existsOwnedExcept(ownerId, name, categoria.getType(), id)) {
            throw duplicateName();
        }
        categoria.update(name, request.ativa());
        try {
            return categorias.saveAndFlush(categoria);
        } catch (DataIntegrityViolationException concurrentUpdate) {
            throw duplicateName();
        }
    }

    private static ApiException duplicateName() {
        return new ApiException(HttpStatus.CONFLICT, "Categoria já existe",
                "Você já tem uma categoria com este nome e tipo.");
    }
}
