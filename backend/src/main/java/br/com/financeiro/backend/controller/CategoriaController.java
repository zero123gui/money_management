package br.com.financeiro.backend.controller;

import br.com.financeiro.backend.dto.AlterarCategoriaRequest;
import br.com.financeiro.backend.dto.CategoriaResponse;
import br.com.financeiro.backend.dto.CriarCategoriaRequest;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.service.CategoriaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas de categorias ({@code docs/API.md}, seção 2.3).
 */
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    /**
     * Cria o controller.
     *
     * @param categoriaService regras de categorias
     */
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /**
     * Lista as categorias padrão e as do usuário.
     *
     * @param tipo            filtro opcional por tipo
     * @param incluirInativas se as categorias desativadas entram na lista
     * @return as categorias, ordenadas por nome
     */
    @GetMapping
    public List<CategoriaResponse> list(
            @RequestParam(required = false) Tipo tipo,
            @RequestParam(defaultValue = "false") boolean incluirInativas) {
        return categoriaService.list(tipo, incluirInativas).stream().map(CategoriaResponse::from).toList();
    }

    /**
     * Cria uma categoria própria.
     *
     * @param request nome e tipo
     * @return 201 com a categoria criada
     */
    @PostMapping
    public ResponseEntity<CategoriaResponse> create(@Valid @RequestBody CriarCategoriaRequest request) {
        CategoriaResponse created = CategoriaResponse.from(categoriaService.create(request));
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + created.id())).body(created);
    }

    /**
     * Renomeia, desativa ou reativa uma categoria própria.
     *
     * @param id      identificador da categoria
     * @param request novo nome e situação
     * @return a categoria alterada
     */
    @PutMapping("/{id}")
    public CategoriaResponse update(@PathVariable Long id, @Valid @RequestBody AlterarCategoriaRequest request) {
        return CategoriaResponse.from(categoriaService.update(id, request));
    }
}
