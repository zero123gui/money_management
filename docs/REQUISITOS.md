# Requisitos: Controle Financeiro Familiar

> **Estado: em validação** (criado em 2026-10-28, última revisão em 2026-10-28).
> Este documento é a base para decidir quais dados a aplicação guarda.
> Quando um requisito for aprovado, alterado ou descartado, atualize a coluna **Status**.
> O modelo de dados final fica em `DECISOES.md`.

## Legendas

**Prioridade**

| Valor      | Significado                                       |
|------------|---------------------------------------------------|
| Essencial  | Sem isso o MVP (primeira versão) não funciona.    |
| Importante | Deve entrar no MVP, mas pode ser simplificado.    |
| Desejável  | Entra no MVP se houver tempo; senão, fica depois. |
| Futuro     | Planejado, fora do MVP.                           |

**Origem**: `DECISOES` = já constava em `DECISOES.md`; `Sugestão` = proposto durante o levantamento.

**Status**: `Proposto` (aguarda validação), `Aprovado`, `Alterado` (aprovado com mudança), `Descartado`.

## Requisitos funcionais (RF)

### Usuários e acesso

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-01 | Cadastrar usuário com nome, e-mail e senha.                                 | Essencial  | Sugestão | Proposto |
| RF-02 | Entrar com **e-mail** e senha e receber um token JWT.                       | Essencial  | DECISOES | Aprovado |
| RF-03 | Sair (logout) no desktop, descartando o token.                              | Essencial  | Sugestão | Proposto |
| RF-04 | Alterar a própria senha.                                                    | Importante | Sugestão | Proposto |
| RF-05 | Excluir a própria conta e todos os seus dados.                              | Desejável  | Sugestão | Proposto |

### Lançamentos

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-06 | Registrar lançamento com valor, data, categoria e descrição.                | Essencial  | DECISOES | Proposto |
| RF-07 | Classificar o lançamento como **receita** (entrada) ou **despesa** (saída). | Essencial  | Sugestão | Aprovado |
| RF-08 | Editar um lançamento próprio.                                               | Essencial  | Sugestão | Proposto |
| RF-09 | Excluir um lançamento próprio por **exclusão lógica**: ele é marcado como excluído, some das listas e deixa de contar no saldo e nos resumos, sem ser apagado do banco. | Essencial  | Sugestão | Alterado |
| RF-10 | Listar os próprios lançamentos, filtrando por período, categoria e tipo.    | Essencial  | Sugestão | Proposto |
| RF-11 | Informar a forma de pagamento ou conta (dinheiro, Pix, débito, crédito…).   | Futuro     | Sugestão | Aprovado |
| RF-12 | Lançamento recorrente (ex.: aluguel todo mês).                              | Futuro     | Sugestão | Aprovado |
| RF-13 | Lançamento parcelado (ex.: compra em 10x).                                  | Futuro     | Sugestão | Aprovado |
| RF-24 | Ver os lançamentos excluídos e restaurar um deles.                          | Importante | Sugestão | Aprovado |

### Categorias

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-14 | Oferecer categorias padrão (Alimentação, Moradia, Transporte, Salário…).    | Importante | Sugestão | Aprovado |
| RF-15 | Criar, renomear e desativar categorias próprias.                            | Importante | Sugestão | Aprovado |

### Consultas e relatórios

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-16 | Mostrar o saldo de um período (receitas − despesas), o balanço geral.       | Essencial  | Sugestão | Aprovado |
| RF-17 | Mostrar o resumo por categoria de um período (total por categoria).         | Essencial  | DECISOES | Proposto |
| RF-18 | Definir um limite de gasto mensal por categoria e acompanhar o consumo.     | Futuro     | Sugestão | Proposto |
| RF-19 | Exportar os próprios lançamentos em CSV.                                    | Futuro     | Sugestão | Alterado |

### Grupo familiar (futuro)

| ID    | Requisito                                                                   | Prioridade | Origem   | Status   |
|-------|-----------------------------------------------------------------------------|------------|----------|----------|
| RF-20 | Ativar ou desativar o compartilhamento de resumo (`compartilha_resumo`).    | Futuro     | DECISOES | Proposto |
| RF-21 | Criar um grupo familiar e adicionar membros.                                | Futuro     | DECISOES | Proposto |
| RF-22 | Sair do grupo ou remover um membro.                                         | Futuro     | Sugestão | Proposto |
| RF-23 | Ver o resumo por categoria dos membros que compartilham (nunca lançamentos).| Futuro     | DECISOES | Proposto |

## Requisitos não funcionais (RNF)

| ID     | Categoria         | Requisito                                                                                                   | Prioridade | Origem   | Status     |
|--------|-------------------|-------------------------------------------------------------------------------------------------------------|------------|----------|------------|
| RNF-01 | Segurança         | Senhas guardadas apenas como hash (BCrypt), nunca em texto puro.                                            | Essencial  | Sugestão | Aprovado   |
| RNF-02 | Segurança         | Autenticação por JWT com expiração de **1 hora** (configurável); a chave de assinatura vem de variável de ambiente. | Essencial  | DECISOES | Aprovado   |
| RNF-03 | Segurança         | Comunicação por HTTPS sempre que a API estiver fora da máquina local (obrigatório, pois a API ficará na nuvem). | Essencial  | Sugestão | Aprovado   |
| RNF-04 | Privacidade       | Toda consulta filtra pelo usuário autenticado, no backend, por meio de um service centralizado.             | Essencial  | DECISOES | Proposto   |
| RNF-05 | Privacidade       | O grupo familiar só acessa resumos calculados sob demanda; desativar o compartilhamento tem efeito imediato. | Futuro     | DECISOES | Proposto   |
| RNF-06 | Exatidão          | Valores monetários usam `BigDecimal` no Java e `NUMERIC(15,2)` no banco; nunca `float` ou `double`.        | Essencial  | Sugestão | Proposto   |
| RNF-07 | Localização       | Interface em português; moeda Real (R$); datas `dd/MM/aaaa`; vírgula como separador decimal.                | Essencial  | Sugestão | Proposto   |
| RNF-08 | Multi-dispositivo | Os dados ficam no servidor; o desktop exige internet e não guarda dados financeiros localmente. Uso offline está fora do escopo. | Essencial  | Sugestão | Aprovado   |
| RNF-09 | Persistência      | ~~Trocar SQLite por PostgreSQL sem reescrever código.~~ Sem sentido: o projeto começa direto em PostgreSQL. | -          | Sugestão | Descartado |
| RNF-10 | Auditoria         | Todo registro guarda data de criação e de última alteração.                                                 | Importante | Sugestão | Proposto   |
| RNF-11 | Confiabilidade    | Backup periódico do banco de dados (oferecido pelo PostgreSQL gerenciado na nuvem).                         | Importante | Sugestão | Proposto   |
| RNF-12 | Portabilidade     | O desktop roda no **Windows** no MVP; **macOS** no futuro.                                                  | Essencial  | Sugestão | Alterado   |
| RNF-13 | Desempenho        | Listagem e resumo de um ano de lançamentos respondem em menos de 2 segundos.                                | Desejável  | Sugestão | Proposto   |
| RNF-14 | Testabilidade     | Testes automatizados cobrem as regras de autorização e o cálculo de saldo e resumos.                        | Essencial  | Sugestão | Proposto   |
| RNF-15 | Manutenibilidade  | O código segue as convenções de `DECISOES.md` (Javadoc, commits, branches).                                 | Essencial  | DECISOES | Proposto   |

## Dados derivados

O modelo aprovado está em `DECISOES.md`, seção "Modelo de dados (MVP)". As tabelas abaixo mostram de qual requisito vem cada campo.

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

`senha_hash` guarda o **resultado do BCrypt**, não a senha. O hash não pode ser revertido para a senha original: no login, a API calcula o hash da senha digitada e compara os dois. Este é o padrão de mercado (é assim que o Spring Security funciona).

### `categoria`

| Campo        | Tipo sugerido        | Motivo                                       |
|--------------|----------------------|----------------------------------------------|
| `id`         | inteiro (PK)         | identificação                                |
| `nome`       | texto                | RF-14, RF-15                                 |
| `tipo`       | `RECEITA`/`DESPESA`  | evita usar "Salário" numa despesa            |
| `usuario_id` | FK, **nulo = padrão**| RF-14 (padrão do sistema), RF-15 (própria)   |
| `ativa`      | booleano             | RF-15 (desativar sem perder o histórico)     |

### `lancamento`

| Campo           | Tipo sugerido        | Motivo                                         |
|-----------------|----------------------|------------------------------------------------|
| `id`            | inteiro (PK)         | identificação                                  |
| `usuario_id`    | FK → `usuario`       | RNF-04 (isolamento)                            |
| `tipo`          | `RECEITA`/`DESPESA`  | RF-07, RF-16                                   |
| `valor`         | `NUMERIC(15,2)` > 0  | RF-06, RNF-06 (sempre positivo; o `tipo` diz se entra ou sai) |
| `data`          | data                 | RF-06, RF-10                                   |
| `categoria_id`  | FK → `categoria`     | RF-06, RF-14, RF-17                            |
| `descricao`     | texto                | RF-06                                          |
| `excluido_em`   | data e hora, nulo    | RF-09, RF-24 (nulo = ativo)                    |
| `criado_em`     | data e hora          | RNF-10                                         |
| `atualizado_em` | data e hora          | RNF-10                                         |

`forma_pagamento` (RF-11) fica para depois do MVP.

### Futuro: `grupo_familiar` e `grupo_membro`

Já previstas em `DECISOES.md` (RF-21 a RF-23). Os campos serão detalhados quando o grupo familiar entrar no escopo.

## Perguntas em aberto

Todas respondidas em 2026-10-28.

1. ~~**Receita e despesa (RF-07)**~~: registra as duas, para gerar o balanço geral (RF-16).
2. ~~**Categoria (RF-14, RF-15)**~~: vira tabela própria.
3. ~~**Sinal do valor**~~: valor sempre positivo; despesa é saída e receita é entrada.
4. ~~**Forma de pagamento (RF-11)**~~: é importante, mas fica para depois do MVP.
5. ~~**Recorrência e parcelas (RF-12, RF-13)**~~: fora do MVP.
6. ~~**Exclusão (RF-09)**~~: exclusão lógica e reversível (RF-24).
7. ~~**Login (RF-02)**~~: por e-mail.
8. ~~**Sistemas operacionais (RNF-12)**~~: Windows no MVP; macOS no futuro.
9. ~~**Uso sem internet (RNF-08)**~~: o desktop exige internet.
10. ~~**Hospedagem**~~: API e banco na nuvem; provedor a escolher.
11. ~~**Banco na nuvem**~~: PostgreSQL desde o início.
