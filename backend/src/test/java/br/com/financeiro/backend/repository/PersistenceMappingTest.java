package br.com.financeiro.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financeiro.backend.entity.Categoria;
import br.com.financeiro.backend.entity.Lancamento;
import br.com.financeiro.backend.entity.Tipo;
import br.com.financeiro.backend.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Confere as entidades e os repositories contra o esquema criado pelo Flyway,
 * em um PostgreSQL descartável. Subir o contexto já prova o mapeamento, pois
 * {@code ddl-auto=validate} falha se ele divergir das tabelas.
 */
// O datasource vem do contêiner; a propriedade só satisfaz o ${DB_PASSWORD} do
// application.properties quando não há .env, como no CI.
@DataJpaTest(properties = "DB_PASSWORD=nao-usada")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PersistenceMappingTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 50);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CategoriaRepository categorias;

    @Autowired
    private LancamentoRepository lancamentos;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void readsBackAllFieldsOfAnEntry() {
        Usuario ana = newUser("ana@exemplo.com");
        Categoria mercado = categorias.save(new Categoria("Mercado", Tipo.DESPESA, ana));
        Lancamento saved = lancamentos.save(new Lancamento(ana, Tipo.DESPESA, new BigDecimal("150.75"),
                LocalDate.of(2026, 10, 28), mercado, "Compra do mês"));
        flushAndClear();

        Lancamento found = lancamentos.findByIdAndOwnerIdAndDeletedAtIsNull(saved.getId(), ana.getId()).orElseThrow();

        assertThat(found.getType()).isEqualTo(Tipo.DESPESA);
        assertThat(found.getAmount()).isEqualByComparingTo("150.75");
        assertThat(found.getDate()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(found.getCategory().getName()).isEqualTo("Mercado");
        assertThat(found.getDescription()).isEqualTo("Compra do mês");
        assertThat(found.getDeletedAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void entryOfAnotherUserIsNotFound() {
        Usuario ana = newUser("ana@exemplo.com");
        Usuario bia = newUser("bia@exemplo.com");
        Lancamento entry = newEntry(ana, Tipo.DESPESA, standardCategory(ana, "Lazer"));
        flushAndClear();

        assertThat(lancamentos.findByIdAndOwnerIdAndDeletedAtIsNull(entry.getId(), bia.getId())).isEmpty();
        assertThat(lancamentos.findActive(bia.getId(), null, null, null, null, FIRST_PAGE)).isEmpty();
    }

    @Test
    void softDeletedEntryLeavesActiveListsUntilRestored() {
        Usuario ana = newUser("ana@exemplo.com");
        Lancamento entry = newEntry(ana, Tipo.DESPESA, standardCategory(ana, "Lazer"));

        entry.softDelete();
        flushAndClear();

        assertThat(lancamentos.findActive(ana.getId(), null, null, null, null, FIRST_PAGE)).isEmpty();
        assertThat(lancamentos.findByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(ana.getId(), FIRST_PAGE))
                .hasSize(1);

        lancamentos.findByIdAndOwnerIdAndDeletedAtIsNotNull(entry.getId(), ana.getId()).orElseThrow().restore();
        flushAndClear();

        assertThat(lancamentos.findByIdAndOwnerIdAndDeletedAtIsNull(entry.getId(), ana.getId())).isPresent();
    }

    @Test
    void databaseRejectsEntryWithCategoryOfAnotherType() {
        Usuario ana = newUser("ana@exemplo.com");
        Categoria lazer = standardCategory(ana, "Lazer");

        assertThatThrownBy(() -> newEntry(ana, Tipo.RECEITA, lazer))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_lancamento_categoria");
    }

    @Test
    void userSeesStandardAndOwnCategoriesOnly() {
        Usuario ana = newUser("ana@exemplo.com");
        Usuario bia = newUser("bia@exemplo.com");
        Categoria mercado = categorias.save(new Categoria("Mercado", Tipo.DESPESA, ana));
        flushAndClear();

        assertThat(categorias.findAllVisible(ana.getId())).hasSize(9);
        assertThat(categorias.findAllVisible(bia.getId())).hasSize(8).allMatch(Categoria::isStandard);
        assertThat(categorias.findVisibleById(mercado.getId(), ana.getId())).isPresent();
        assertThat(categorias.findVisibleById(mercado.getId(), bia.getId())).isEmpty();
    }

    @Test
    void ownedCategoryNameCheckIgnoresCase() {
        Usuario ana = newUser("ana@exemplo.com");
        categorias.save(new Categoria("Mercado", Tipo.DESPESA, ana));
        flushAndClear();

        assertThat(categorias.existsOwned(ana.getId(), "MERCADO", Tipo.DESPESA)).isTrue();
        assertThat(categorias.existsOwned(ana.getId(), "MERCADO", Tipo.RECEITA)).isFalse();
    }

    @Test
    void emailLookupIgnoresCase() {
        Usuario ana = newUser("Ana@Exemplo.com");
        flushAndClear();

        assertThat(usuarios.findByEmail("ana@exemplo.com").map(Usuario::getId)).contains(ana.getId());
    }

    @Test
    void deletingUserCascadesToCategoriesAndEntries() {
        Usuario ana = newUser("ana@exemplo.com");
        Categoria mercado = categorias.save(new Categoria("Mercado", Tipo.DESPESA, ana));
        Lancamento entry = newEntry(ana, Tipo.DESPESA, mercado);
        flushAndClear();

        // Direto no banco, pois a cascata é do esquema (ON DELETE CASCADE), não do JPA.
        entityManager.getEntityManager().createNativeQuery("delete from usuario where id = :id")
                .setParameter("id", ana.getId())
                .executeUpdate();
        flushAndClear();

        assertThat(entityManager.find(Lancamento.class, entry.getId())).isNull();
        assertThat(entityManager.find(Categoria.class, mercado.getId())).isNull();
    }

    private Usuario newUser(String email) {
        return usuarios.save(new Usuario("Nome", email, "hash-de-teste"));
    }

    private Lancamento newEntry(Usuario owner, Tipo type, Categoria category) {
        return lancamentos.save(new Lancamento(owner, type, new BigDecimal("10.00"),
                LocalDate.of(2026, 10, 1), category, null));
    }

    private Categoria standardCategory(Usuario viewer, String name) {
        return categorias.findAllVisible(viewer.getId()).stream()
                .filter(categoria -> categoria.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
