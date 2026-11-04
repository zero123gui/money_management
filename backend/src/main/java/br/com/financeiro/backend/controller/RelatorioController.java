package br.com.financeiro.backend.controller;

import br.com.financeiro.backend.dto.ResumoCategoriaResponse;
import br.com.financeiro.backend.dto.SaldoResponse;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.service.RelatorioService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas de relatórios ({@code docs/API.md}, seção 2.5).
 */
@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    /**
     * Cria o controller.
     *
     * @param relatorioService cálculo dos relatórios
     */
    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    /**
     * Consulta o balanço de um período.
     *
     * @param inicio data inicial, inclusiva
     * @param fim    data final, inclusiva
     * @return as receitas, as despesas e o saldo
     */
    @GetMapping("/saldo")
    public SaldoResponse balance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorioService.balance(inicio, fim);
    }

    /**
     * Consulta o total por categoria de um período.
     *
     * @param inicio data inicial, inclusiva
     * @param fim    data final, inclusiva
     * @param tipo   filtro opcional por tipo
     * @return os totais, do maior para o menor
     */
    @GetMapping("/categorias")
    public List<ResumoCategoriaResponse> byCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Tipo tipo) {
        return relatorioService.byCategory(inicio, fim, tipo);
    }
}
