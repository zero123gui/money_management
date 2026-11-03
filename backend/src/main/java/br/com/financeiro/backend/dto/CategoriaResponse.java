package br.com.financeiro.backend.dto;

import br.com.financeiro.backend.entity.Categoria;
import br.com.financeiro.backend.entity.Tipo;

/**
 * Categoria devolvida pela API.
 *
 * @param id     identificador da categoria
 * @param nome   nome da categoria
 * @param tipo   tipo da categoria
 * @param padrao se é uma categoria do sistema, sem dono
 * @param ativa  se a categoria aparece nas opções de novos lançamentos
 */
public record CategoriaResponse(Long id, String nome, Tipo tipo, boolean padrao, boolean ativa) {

    /**
     * Monta a resposta a partir da entidade.
     *
     * @param categoria categoria a expor
     * @return os dados da categoria
     */
    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getName(), categoria.getType(),
                categoria.isStandard(), categoria.isActive());
    }
}
