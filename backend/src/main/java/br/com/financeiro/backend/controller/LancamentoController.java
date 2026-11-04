package br.com.financeiro.backend.controller;

import br.com.financeiro.backend.dto.LancamentoExcluidoResponse;
import br.com.financeiro.backend.dto.LancamentoRequest;
import br.com.financeiro.backend.dto.LancamentoResponse;
import br.com.financeiro.backend.dto.PaginaResponse;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.service.LancamentoService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas de lançamentos ({@code docs/API.md}, seção 2.4).
 */
@RestController
@RequestMapping("/api/v1/lancamentos")
public class LancamentoController {

    private final LancamentoService lancamentoService;

    /**
     * Cria o controller.
     *
     * @param lancamentoService regras de lançamentos
     */
    public LancamentoController(LancamentoService lancamentoService) {
        this.lancamentoService = lancamentoService;
    }

    /**
     * Lista os lançamentos ativos do usuário.
     *
     * @param inicio      data inicial, inclusiva
     * @param fim         data final, inclusiva
     * @param tipo        tipo do lançamento
     * @param categoriaId identificador da categoria
     * @param pageable    parâmetros {@code pagina} e {@code tamanho}
     * @return a página de lançamentos, do mais recente para o mais antigo
     */
    @GetMapping
    @PaginationParameters
    public PaginaResponse<LancamentoResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Tipo tipo,
            @RequestParam(required = false) Long categoriaId,
            @Parameter(hidden = true) Pageable pageable) {
        return PaginaResponse.from(lancamentoService.listActive(inicio, fim, tipo, categoriaId, pageable),
                LancamentoResponse::from);
    }

    /**
     * Lista os lançamentos excluídos do usuário.
     *
     * @param pageable parâmetros {@code pagina} e {@code tamanho}
     * @return a página de lançamentos, do excluído mais recentemente para o mais antigo
     */
    @GetMapping("/excluidos")
    @PaginationParameters
    public PaginaResponse<LancamentoExcluidoResponse> listDeleted(@Parameter(hidden = true) Pageable pageable) {
        return PaginaResponse.from(lancamentoService.listDeleted(pageable), LancamentoExcluidoResponse::from);
    }

    /**
     * Consulta um lançamento ativo.
     *
     * @param id identificador do lançamento
     * @return o lançamento
     */
    @GetMapping("/{id}")
    public LancamentoResponse get(@PathVariable Long id) {
        return LancamentoResponse.from(lancamentoService.get(id));
    }

    /**
     * Cria um lançamento.
     *
     * @param request dados do lançamento
     * @return 201 com o lançamento criado
     */
    @PostMapping
    public ResponseEntity<LancamentoResponse> create(@Valid @RequestBody LancamentoRequest request) {
        LancamentoResponse created = LancamentoResponse.from(lancamentoService.create(request));
        return ResponseEntity.created(URI.create("/api/v1/lancamentos/" + created.id())).body(created);
    }

    /**
     * Substitui os dados de um lançamento ativo.
     *
     * @param id      identificador do lançamento
     * @param request novos dados
     * @return o lançamento alterado
     */
    @PutMapping("/{id}")
    public LancamentoResponse update(@PathVariable Long id, @Valid @RequestBody LancamentoRequest request) {
        return LancamentoResponse.from(lancamentoService.update(id, request));
    }

    /**
     * Exclui logicamente um lançamento.
     *
     * @param id identificador do lançamento
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        lancamentoService.softDelete(id);
    }

    /**
     * Volta um lançamento excluído a ser ativo.
     *
     * @param id identificador do lançamento
     * @return o lançamento restaurado
     */
    @PostMapping("/{id}/restaurar")
    public LancamentoResponse restore(@PathVariable Long id) {
        return LancamentoResponse.from(lancamentoService.restore(id));
    }
}
