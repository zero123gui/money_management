package br.com.financeiro.backend.service;

import br.com.financeiro.backend.dto.LancamentoRequest;
import br.com.financeiro.backend.entity.Categoria;
import br.com.financeiro.backend.entity.Lancamento;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.exception.ApiException;
import br.com.financeiro.backend.exception.InvalidFieldException;
import br.com.financeiro.backend.repository.CategoriaRepository;
import br.com.financeiro.backend.repository.LancamentoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta e manutenção de lançamentos (RF-06 a RF-10, RF-24). O dono sempre
 * vem do token, e toda consulta filtra por ele (RNF-04).
 */
@Service
public class LancamentoService {

    private final AuthorizationService authorization;
    private final LancamentoRepository lancamentos;
    private final CategoriaRepository categorias;

    /**
     * Cria o service.
     *
     * @param authorization identifica o usuário autenticado
     * @param lancamentos   acesso a lançamentos
     * @param categorias    acesso a categorias
     */
    public LancamentoService(AuthorizationService authorization, LancamentoRepository lancamentos,
            CategoriaRepository categorias) {
        this.authorization = authorization;
        this.lancamentos = lancamentos;
        this.categorias = categorias;
    }

    /**
     * Lista os lançamentos ativos do usuário autenticado.
     *
     * @param start      data inicial, inclusiva, ou {@code null}
     * @param end        data final, inclusiva, ou {@code null}
     * @param type       tipo do lançamento, ou {@code null}
     * @param categoryId identificador da categoria, ou {@code null}
     * @param pageable   página pedida
     * @return a página de lançamentos ativos
     */
    public Page<Lancamento> listActive(LocalDate start, LocalDate end, Tipo type, Long categoryId, Pageable pageable) {
        return lancamentos.findActive(authorization.currentUser().getId(), start, end, type, categoryId,
                withoutSort(pageable));
    }

    /**
     * Lista os lançamentos excluídos do usuário autenticado.
     *
     * @param pageable página pedida
     * @return a página de lançamentos excluídos
     */
    public Page<Lancamento> listDeleted(Pageable pageable) {
        return lancamentos.findByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(
                authorization.currentUser().getId(), withoutSort(pageable));
    }

    /**
     * Busca um lançamento ativo do usuário autenticado.
     *
     * @param id identificador do lançamento
     * @return o lançamento
     * @throws ApiException com 404 se não existir, for de outro usuário ou estiver excluído
     */
    public Lancamento get(Long id) {
        return findActive(id);
    }

    /**
     * Cria um lançamento do usuário autenticado.
     *
     * @param request dados do lançamento
     * @return o lançamento criado
     * @throws InvalidFieldException se a categoria não puder ser usada
     */
    public Lancamento create(LancamentoRequest request) {
        Usuario owner = authorization.currentUser();
        Categoria category = usableCategory(request, owner.getId());
        return lancamentos.save(new Lancamento(owner, request.tipo(), normalizedAmount(request), request.data(),
                category, normalizedDescription(request)));
    }

    /**
     * Substitui os dados de um lançamento ativo do usuário autenticado.
     *
     * @param id      identificador do lançamento
     * @param request novos dados
     * @return o lançamento alterado
     * @throws ApiException          com 404 se não existir, for de outro usuário ou estiver excluído
     * @throws InvalidFieldException se a categoria não puder ser usada
     */
    @Transactional
    public Lancamento update(Long id, LancamentoRequest request) {
        Lancamento lancamento = findActive(id);
        Categoria category = usableCategory(request, lancamento.getOwner().getId());
        lancamento.update(request.tipo(), normalizedAmount(request), request.data(), category,
                normalizedDescription(request));
        return lancamento;
    }

    /**
     * Exclui logicamente um lançamento ativo do usuário autenticado (RF-09).
     *
     * @param id identificador do lançamento
     * @throws ApiException com 404 se não existir, for de outro usuário ou já estiver excluído
     */
    @Transactional
    public void softDelete(Long id) {
        findActive(id).softDelete();
    }

    /**
     * Volta um lançamento excluído do usuário autenticado a ser ativo (RF-24).
     * Se a categoria tiver sido desativada, ele a mantém mesmo assim.
     *
     * @param id identificador do lançamento
     * @return o lançamento restaurado
     * @throws ApiException com 404 se não existir, for de outro usuário ou não estiver excluído
     */
    @Transactional
    public Lancamento restore(Long id) {
        Lancamento lancamento = lancamentos
                .findByIdAndOwnerIdAndDeletedAtIsNotNull(id, authorization.currentUser().getId())
                .orElseThrow(ApiException::notFound);
        lancamento.restore();
        return lancamento;
    }

    private Lancamento findActive(Long id) {
        return lancamentos.findByIdAndOwnerIdAndDeletedAtIsNull(id, authorization.currentUser().getId())
                .orElseThrow(ApiException::notFound);
    }

    private Categoria usableCategory(LancamentoRequest request, Long ownerId) {
        // Categoria de outro usuário recebe a mesma mensagem de uma inexistente, para não revelar que ela existe.
        Categoria category = categorias.findVisibleById(request.categoriaId(), ownerId)
                .orElseThrow(() -> new InvalidFieldException("categoriaId", "não existe"));
        if (!category.isActive()) {
            throw new InvalidFieldException("categoriaId", "está desativada");
        }
        if (category.getType() != request.tipo()) {
            throw new InvalidFieldException("categoriaId", "é de outro tipo");
        }
        return category;
    }

    // Duas casas sempre, para a resposta do POST mostrar 150.70 igual à de um GET posterior.
    private static BigDecimal normalizedAmount(LancamentoRequest request) {
        return request.valor().setScale(2);
    }

    private static String normalizedDescription(LancamentoRequest request) {
        if (request.descricao() == null || request.descricao().isBlank()) {
            return null;
        }
        return request.descricao().strip();
    }

    private static Pageable withoutSort(Pageable pageable) {
        // A ordem é a do contrato: um parâmetro "sort" na URL não pode mudá-la nem
        // ordenar por campos internos, o que permitiria deduzir o valor deles.
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
