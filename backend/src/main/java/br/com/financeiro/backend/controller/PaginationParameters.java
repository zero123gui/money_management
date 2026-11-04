package br.com.financeiro.backend.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Descreve no Swagger os parâmetros de paginação do contrato
 * ({@code docs/API.md}, seção 1.7). Sem isto, ele mostraria os nomes padrão do
 * Spring e um parâmetro de ordenação que a API ignora.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Parameters({
    @Parameter(name = "pagina", in = ParameterIn.QUERY, description = "Número da página, a partir de 0",
            schema = @Schema(type = "integer", defaultValue = "0")),
    @Parameter(name = "tamanho", in = ParameterIn.QUERY, description = "Itens por página, no máximo 200",
            schema = @Schema(type = "integer", defaultValue = "50"))
})
public @interface PaginationParameters {
}
