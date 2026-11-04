package br.com.financeiro.backend.service;

import br.com.financeiro.backend.dto.ResumoCategoriaResponse;
import br.com.financeiro.backend.dto.SaldoResponse;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.repository.LancamentoRepository;
import br.com.financeiro.backend.repository.TypeTotal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Relatórios do usuário autenticado (RF-16, RF-17). Os valores são calculados
 * na hora, só com lançamentos ativos dele.
 */
@Service
public class RelatorioService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final AuthorizationService authorization;
    private final LancamentoRepository lancamentos;

    /**
     * Cria o service.
     *
     * @param authorization identifica o usuário autenticado
     * @param lancamentos   acesso a lançamentos
     */
    public RelatorioService(AuthorizationService authorization, LancamentoRepository lancamentos) {
        this.authorization = authorization;
        this.lancamentos = lancamentos;
    }

    /**
     * Calcula o balanço de um período.
     *
     * @param start data inicial, inclusiva
     * @param end   data final, inclusiva
     * @return as receitas, as despesas e o saldo do período
     */
    public SaldoResponse balance(LocalDate start, LocalDate end) {
        List<TypeTotal> totals = lancamentos.sumActiveByType(authorization.currentUser().getId(), start, end);
        BigDecimal income = totalOf(totals, Tipo.RECEITA);
        BigDecimal expenses = totalOf(totals, Tipo.DESPESA);
        return new SaldoResponse(start, end, income, expenses, income.subtract(expenses));
    }

    /**
     * Calcula o total por categoria de um período.
     *
     * @param start data inicial, inclusiva
     * @param end   data final, inclusiva
     * @param type  tipo dos lançamentos, ou {@code null} para todos
     * @return os totais, do maior para o menor
     */
    public List<ResumoCategoriaResponse> byCategory(LocalDate start, LocalDate end, Tipo type) {
        return lancamentos.sumActiveByCategory(authorization.currentUser().getId(), start, end, type);
    }

    private static BigDecimal totalOf(List<TypeTotal> totals, Tipo type) {
        return totals.stream()
                .filter(total -> total.type() == type)
                .map(TypeTotal::total)
                .findFirst()
                .orElse(ZERO);
    }
}
