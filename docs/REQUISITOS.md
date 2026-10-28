# Requisitos: Controle Financeiro Familiar

> **Estado: rascunho para validação** (criado em 2026-10-28).
> Este documento é a base para decidir quais dados a aplicação guarda.
> Quando um requisito for aprovado, alterado ou descartado, atualize a coluna **Status**.
> O modelo de dados final vai para `DECISOES.md`.

## Legendas

**Prioridade**

| Valor      | Significado                                    |
|------------|------------------------------------------------|
| Essencial  | Sem isso a primeira versão (v1) não funciona.  |
| Importante | Deve entrar na v1, mas pode ser simplificado.  |
| Desejável  | Entra se houver tempo; senão, fica para depois.|
| Futuro     | Planejado, fora da v1.                         |

**Origem**: `DECISOES` = já consta em `DECISOES.md`; `Sugestão` = proposto nesta sessão, precisa de validação.

**Status**: `Proposto`, `Aprovado`, `Alterado`, `Descartado`.

## Requisitos funcionais (RF)

### Usuários e acesso

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-01 | Cadastrar usuário com nome, e-mail e senha.                                 | Essencial  | Sugestão | Proposto |
| RF-02 | Entrar com e-mail e senha e receber um token JWT.                           | Essencial  | DECISOES | Proposto |
| RF-03 | Sair (logout) no desktop, descartando o token.                              | Essencial  | Sugestão | Proposto |
| RF-04 | Alterar a própria senha.                                                    | Importante | Sugestão | Proposto |
| RF-05 | Excluir a própria conta e todos os seus dados.                              | Desejável  | Sugestão | Proposto |

### Lançamentos

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-06 | Registrar lançamento com valor, data, categoria e descrição.                | Essencial  | DECISOES | Proposto |
| RF-07 | Classificar o lançamento como **receita** ou **despesa**.                   | Essencial  | Sugestão | Aprovado |
| RF-08 | Editar um lançamento próprio.                                               | Essencial  | Sugestão | Proposto |
| RF-09 | Excluir um lançamento próprio.                                              | Essencial  | Sugestão | Proposto |
| RF-10 | Listar os próprios lançamentos, filtrando por período, categoria e tipo.    | Essencial  | Sugestão | Proposto |
| RF-11 | Informar a forma de pagamento ou conta (dinheiro, Pix, débito, crédito…).   | Desejável  | Sugestão | Proposto |
| RF-12 | Lançamento recorrente (ex.: aluguel todo mês).                              | Futuro     | Sugestão | Proposto |
| RF-13 | Lançamento parcelado (ex.: compra em 10x).                                  | Futuro     | Sugestão | Proposto |

### Categorias

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-14 | Oferecer categorias padrão (Alimentação, Moradia, Transporte, Salário…).    | Importante | Sugestão | Proposto |
| RF-15 | Criar, renomear e desativar categorias próprias.                            | Importante | Sugestão | Proposto |

### Consultas e relatórios

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-16 | Mostrar o saldo de um período (receitas − despesas), o balanço geral.       | Essencial  | Sugestão | Aprovado |
| RF-17 | Mostrar o resumo por categoria de um período (total por categoria).         | Essencial  | DECISOES | Proposto |
| RF-18 | Definir um limite de gasto mensal por categoria e acompanhar o consumo.     | Futuro     | Sugestão | Proposto |
| RF-19 | Exportar os próprios lançamentos em CSV.                                    | Desejável  | Sugestão | Proposto |

### Grupo familiar (futuro)

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-20 | Ativar ou desativar o compartilhamento de resumo (`compartilha_resumo`).    | Futuro     | DECISOES | Proposto |
| RF-21 | Criar um grupo familiar e adicionar membros.                                | Futuro     | DECISOES | Proposto |
| RF-22 | Sair do grupo ou remover um membro.                                         | Futuro     | Sugestão | Proposto |
| RF-23 | Ver o resumo por categoria dos membros que compartilham (nunca lançamentos).| Futuro     | DECISOES | Proposto |

## Requisitos não funcionais (RNF)

| ID     | Categoria         | Requisito                                                                                                   | Prioridade | Origem   | Status   |
|--------|-------------------|-------------------------------------------------------------------------------------------------------------|------------|----------|----------|
| RNF-01 | Segurança         | Senhas guardadas apenas como hash (BCrypt), nunca em texto puro.                                            | Essencial  | Sugestão | Proposto |
| RNF-02 | Segurança         | Autenticação por JWT com prazo de expiração; a chave de assinatura vem de variável de ambiente.             | Essencial  | DECISOES | Proposto |
| RNF-03 | Segurança         | Comunicação por HTTPS sempre que a API estiver fora da máquina local (obrigatório, pois a API ficará na nuvem). | Essencial  | Sugestão | Aprovado |
| RNF-04 | Privacidade       | Toda consulta filtra pelo usuário autenticado, no backend, por meio de um service centralizado.             | Essencial  | DECISOES | Proposto |
| RNF-05 | Privacidade       | O grupo familiar só acessa resumos calculados sob demanda; desativar o compartilhamento tem efeito imediato. | Futuro     | DECISOES | Proposto |
| RNF-06 | Exatidão          | Valores monetários usam `BigDecimal` no Java e `NUMERIC(15,2)` no banco; nunca `float` ou `double`.        | Essencial  | Sugestão | Proposto |
| RNF-07 | Localização       | Interface em português; moeda Real (R$); datas `dd/MM/aaaa`; vírgula como separador decimal.                | Essencial  | Sugestão | Proposto |
| RNF-08 | Multi-dispositivo | Os dados ficam no servidor; o desktop exige internet e não guarda dados financeiros localmente. Uso offline está fora do escopo. | Essencial  | Sugestão | Aprovado |
| RNF-09 | Persistência      | Trocar SQLite por PostgreSQL sem reescrever código: JPA e migrações versionadas (Flyway).                   | Importante | Sugestão | Proposto |
| RNF-10 | Auditoria         | Todo registro guarda data de criação e de última alteração.                                                 | Importante | Sugestão | Proposto |
| RNF-11 | Confiabilidade    | Backup periódico do banco de dados.                                                                         | Importante | Sugestão | Proposto |
| RNF-12 | Portabilidade     | O desktop roda no Windows (outros sistemas a validar).                                                      | Essencial  | Sugestão | Proposto |
| RNF-13 | Desempenho        | Listagem e resumo de um ano de lançamentos respondem em menos de 2 segundos.                                | Desejável  | Sugestão | Proposto |
| RNF-14 | Testabilidade     | Testes automatizados cobrem as regras de autorização e o cálculo de saldo e resumos.                        | Essencial  | Sugestão | Proposto |
| RNF-15 | Manutenibilidade  | O código segue as convenções de `DECISOES.md` (Javadoc, commits, branches).                                 | Essencial  | DECISOES | Proposto |

## Dados derivados (rascunho)

Campos que os requisitos acima exigem. Os itens entre colchetes dependem das perguntas em aberto.

### `usuario`

| Campo                | Tipo sugerido  | Motivo           |
|----------------------|----------------|------------------|
| `id`                 | inteiro (PK)   | identificação    |
| `nome`               | texto          | RF-01            |
| `email`              | texto, único   | RF-01, RF-02     |
| `senha_hash`         | texto          | RF-01, RNF-01    |
| `compartilha_resumo` | booleano       | RF-20 (futuro)   |
| `criado_em`          | data e hora    | RNF-10           |
| `atualizado_em`      | data e hora    | RNF-10           |

### `lancamento`

| Campo               | Tipo sugerido        | Motivo                 |
|---------------------|----------------------|------------------------|
| `id`                | inteiro (PK)         | identificação          |
| `usuario_id`        | FK → `usuario`       | RNF-04 (isolamento)    |
| `tipo`              | `RECEITA`/`DESPESA`  | RF-07, RF-16           |
| `valor`             | `NUMERIC(15,2)` > 0  | RF-06, RNF-06          |
| `data`              | data                 | RF-06, RF-10           |
| `categoria_id`      | FK → `categoria`     | RF-06, RF-14, RF-17    |
| `descricao`         | texto                | RF-06                  |
| [`forma_pagamento`] | enum ou FK           | RF-11 (pergunta 4)     |
| `criado_em`         | data e hora          | RNF-10                 |
| `atualizado_em`     | data e hora          | RNF-10                 |

### `categoria` (nova tabela, depende da pergunta 2)

| Campo        | Tipo sugerido        | Motivo                                       |
|--------------|----------------------|----------------------------------------------|
| `id`         | inteiro (PK)         | identificação                                |
| `nome`       | texto                | RF-14, RF-15                                 |
| `tipo`       | `RECEITA`/`DESPESA`  | evita usar "Salário" numa despesa            |
| `usuario_id` | FK, **nulo = padrão**| RF-14 (padrão do sistema), RF-15 (própria)   |
| `ativa`      | booleano             | RF-15 (desativar sem perder o histórico)     |

### Futuro: `grupo_familiar` e `grupo_membro`

Já previstas em `DECISOES.md` (RF-21 a RF-23). Os campos serão detalhados quando o grupo familiar entrar no escopo.

## Perguntas em aberto

As respostas definem o modelo de dados final. Perguntas respondidas ficam riscadas, com a resposta e a data.

1. ~~**Receita e despesa (RF-07):** o app registra as duas, ou só despesas?~~
   **Resposta (2026-10-28):** registra as duas, para gerar o balanço geral (RF-16).
2. **Categoria (RF-14, RF-15):** texto livre no lançamento (como está hoje em `DECISOES.md`) ou tabela própria? Com texto livre, "Mercado" e "mercado" viram duas categorias no resumo. A tabela resolve isso e muda o modelo atual, por isso precisa da sua aprovação.
3. **Sinal do valor:** sugiro valor sempre positivo e o `tipo` dizendo se entra ou sai. De acordo?
4. **Forma de pagamento (RF-11):** precisa? Se sim, cartão de crédito com fatura (fechamento e vencimento) é bem mais complexo e ficaria para depois.
5. **Recorrência e parcelas (RF-12, RF-13):** concordam em deixar fora da v1?
6. **Exclusão (RF-09):** apagar de fato, ou marcar como excluído e permitir desfazer?
7. **Login (RF-02):** e-mail ou nome de usuário?
8. **Sistemas operacionais (RNF-12):** só Windows ou também macOS e Linux?
9. ~~**Uso sem internet (RNF-08):** o desktop precisa funcionar sem conexão com o servidor?~~
   **Resposta (2026-10-28):** não. O desktop exige internet, porque depende de autenticação e dos dados de cada usuário no servidor.
10. ~~**Hospedagem:** onde a API vai rodar para atender vários dispositivos?~~
    **Resposta (2026-10-28):** API e banco na nuvem. O provedor ainda será escolhido (ver `DECISOES.md`, "Hospedagem").
11. **Banco na nuvem:** com a API na nuvem, começar direto com PostgreSQL em vez de SQLite? Ver `DECISOES.md`, "Hospedagem".
