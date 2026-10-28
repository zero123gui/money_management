# API REST: Controle Financeiro Familiar

> **Estado: em revisão** (criado em 2026-10-28). Idioma português para URLs e campos JSON **aprovado** em 2026-10-28.
> Este é o contrato entre o `backend` e o `desktop`. Mudanças nele afetam os dois lados.
> Requisitos citados (RF/RNF) estão em `REQUISITOS.md`; decisões em `DECISOES.md`.

## 1. Convenções

### 1.1 URLs

- Prefixo com versão: **`/api/v1`**. Uma mudança incompatível cria `/api/v2`, e o desktop antigo continua funcionando.
- Recursos são **substantivos no plural**, em português, sem acento, em minúsculas e kebab-case: `/lancamentos`, `/categorias`.
- A ação é dada pelo **método HTTP**, não pela URL:

| Método   | Uso                          | Exemplo                       |
|----------|------------------------------|-------------------------------|
| `GET`    | consultar (não altera nada)  | `GET /lancamentos/42`         |
| `POST`   | criar                        | `POST /lancamentos`           |
| `PUT`    | alterar o recurso inteiro    | `PUT /lancamentos/42`         |
| `DELETE` | excluir                      | `DELETE /lancamentos/42`      |

- **Exceção**: ações que não são CRUD usam um sub-recurso com verbo, sempre com `POST` (ex.: `POST /lancamentos/42/restaurar`).
- Filtros, ordenação e paginação vão na **query string**: `GET /lancamentos?inicio=2026-10-01&tipo=DESPESA`.

### 1.2 Formato dos dados (JSON)

- Corpo de requisição e de resposta em **JSON**, com `Content-Type: application/json`.
- Nomes de campos em **camelCase**, em português, sem acento: `categoriaId`, `excluidoEm`.
- **Datas**: ISO 8601, `"2026-10-28"`. **Data e hora**: ISO 8601 em UTC, `"2026-10-28T14:30:00Z"`. O formato brasileiro `dd/MM/aaaa` (RNF-07) é responsabilidade da tela do desktop, não da API.
- **Valores monetários**: número JSON com até 2 casas decimais, sempre positivo (`150.75`). O desktop deve ler esses valores como `BigDecimal`, nunca como `double` (RNF-06).
- **Enums** em MAIÚSCULAS: `"RECEITA"`, `"DESPESA"`.
- Campos sem valor vêm como `null`, não são omitidos.

### 1.3 Autenticação

- Rotas públicas: apenas `POST /auth/cadastro` e `POST /auth/login`.
- Todas as outras exigem o cabeçalho `Authorization: Bearer <token>`.
- O token expira em **1 hora** (RNF-02). Quando ele expira, a API responde `401` e o desktop leva o usuário de volta para a tela de login.
- **Logout (RF-03)** não tem endpoint: o JWT não fica guardado no servidor, então o desktop apenas descarta o token.
- O usuário nunca é informado no corpo ou na URL: a API sempre usa o usuário do token (RNF-04). Por isso as rotas do próprio usuário usam `/usuarios/me`.

### 1.4 Privacidade nas respostas

- Pedir um recurso de outro usuário responde **`404 Not Found`**, igual a um recurso que não existe. Assim a API não confirma que aquele id existe.
- Categorias padrão (sem dono) são visíveis a todos, mas não podem ser alteradas: tentar alterar responde `403 Forbidden`.

### 1.5 Códigos de status

| Código | Quando                                                                      |
|--------|-----------------------------------------------------------------------------|
| `200`  | Consulta ou alteração bem-sucedida, com corpo na resposta.                  |
| `201`  | Recurso criado. Cabeçalho `Location` aponta para ele.                       |
| `204`  | Sucesso sem corpo (ex.: exclusão, troca de senha).                          |
| `400`  | Dados inválidos (campo faltando, valor negativo, categoria de outro tipo…). |
| `401`  | Sem token, token inválido ou expirado, ou login com credenciais erradas.    |
| `403`  | Autenticado, mas a ação não é permitida (ex.: alterar categoria padrão).    |
| `404`  | Recurso não existe ou pertence a outro usuário.                             |
| `409`  | Conflito com o estado atual (ex.: e-mail já cadastrado).                    |
| `500`  | Erro inesperado no servidor. Nunca expõe detalhes internos.                 |

### 1.6 Formato de erro

Todos os erros seguem a **RFC 9457 (Problem Details)**, que o Spring suporta com a classe `ProblemDetail`. `Content-Type: application/problem+json`.

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/v1/lancamentos",
  "erros": [
    { "campo": "valor", "mensagem": "deve ser maior que zero" }
  ]
}
```

O campo `erros` só aparece em erros de validação (`400`). Mensagens em português, prontas para exibir ao usuário.

### 1.7 Paginação

Listas que podem crescer muito (lançamentos) são paginadas.

- Parâmetros: `pagina` (começa em 0, padrão 0) e `tamanho` (padrão 50, máximo 200).
- No Spring, isso é configurado com `spring.data.web.pageable.page-parameter=pagina` e `size-parameter=tamanho`.
- Resposta:

```json
{
  "conteudo": [ ... ],
  "pagina": 0,
  "tamanho": 50,
  "totalElementos": 132,
  "totalPaginas": 3
}
```

### 1.8 Documentação automática

A API publica a especificação **OpenAPI** com a biblioteca `springdoc-openapi`. No ambiente local, a interface Swagger fica em `/swagger-ui.html`, onde dá para testar os endpoints pelo navegador. Ela fica **desativada em produção**.

## 2. Endpoints do MVP

Todas as rotas abaixo têm o prefixo `/api/v1`. 🔓 = pública; 🔒 = exige token.

### Resumo

| Método   | Rota                               | Requisito     | Acesso |
|----------|------------------------------------|---------------|--------|
| `POST`   | `/auth/cadastro`                   | RF-01         | 🔓     |
| `POST`   | `/auth/login`                      | RF-02         | 🔓     |
| `GET`    | `/usuarios/me`                     | —             | 🔒     |
| `PUT`    | `/usuarios/me/senha`               | RF-04         | 🔒     |
| `DELETE` | `/usuarios/me`                     | RF-05         | 🔒     |
| `GET`    | `/categorias`                      | RF-14, RF-15  | 🔒     |
| `POST`   | `/categorias`                      | RF-15         | 🔒     |
| `PUT`    | `/categorias/{id}`                 | RF-15         | 🔒     |
| `GET`    | `/lancamentos`                     | RF-10         | 🔒     |
| `GET`    | `/lancamentos/{id}`                | RF-10         | 🔒     |
| `POST`   | `/lancamentos`                     | RF-06, RF-07  | 🔒     |
| `PUT`    | `/lancamentos/{id}`                | RF-08         | 🔒     |
| `DELETE` | `/lancamentos/{id}`                | RF-09         | 🔒     |
| `GET`    | `/lancamentos/excluidos`           | RF-24         | 🔒     |
| `POST`   | `/lancamentos/{id}/restaurar`      | RF-24         | 🔒     |
| `GET`    | `/relatorios/saldo`                | RF-16         | 🔒     |
| `GET`    | `/relatorios/categorias`           | RF-17         | 🔒     |

### 2.1 Autenticação

#### `POST /auth/cadastro` 🔓

Cria um usuário. Requisição:

```json
{ "nome": "Ana", "email": "ana@exemplo.com", "senha": "uma-senha-forte" }
```

- Validações: todos obrigatórios; `email` em formato válido; `senha` com no mínimo 8 caracteres.
- `201` com o usuário criado (mesmo formato de `GET /usuarios/me`), sem a senha.
- `409` se o e-mail já estiver cadastrado.

#### `POST /auth/login` 🔓

```json
{ "email": "ana@exemplo.com", "senha": "uma-senha-forte" }
```

`200`:

```json
{ "token": "eyJhbGciOi...", "tipo": "Bearer", "expiraEm": "2026-10-28T15:30:00Z" }
```

- `401` com a mesma mensagem genérica ("E-mail ou senha inválidos"), seja o e-mail inexistente ou a senha errada. Assim a API não revela quais e-mails estão cadastrados.

### 2.2 Usuário

#### `GET /usuarios/me` 🔒

`200`:

```json
{ "id": 1, "nome": "Ana", "email": "ana@exemplo.com", "criadoEm": "2026-10-28T14:00:00Z" }
```

#### `PUT /usuarios/me/senha` 🔒

```json
{ "senhaAtual": "uma-senha-forte", "novaSenha": "outra-senha-forte" }
```

- `204` em caso de sucesso.
- `400` se a `senhaAtual` estiver errada ou a `novaSenha` for inválida.

#### `DELETE /usuarios/me` 🔒 (Desejável)

Exclui a conta e todos os dados do usuário (lançamentos e categorias próprias). É uma exclusão **definitiva**, diferente da exclusão de lançamentos. `204`.

### 2.3 Categorias

Formato de uma categoria:

```json
{ "id": 7, "nome": "Mercado", "tipo": "DESPESA", "padrao": false, "ativa": true }
```

`padrao: true` indica uma categoria do sistema (sem dono).

#### `GET /categorias` 🔒

Lista as categorias padrão e as do usuário. Não é paginada.

- Filtros: `tipo` (`RECEITA`/`DESPESA`), `incluirInativas` (padrão `false`).
- `200` com uma lista, ordenada por nome.

#### `POST /categorias` 🔒

```json
{ "nome": "Mercado", "tipo": "DESPESA" }
```

- `201`.
- `409` se o usuário já tiver uma categoria com o mesmo nome e tipo (sem diferenciar maiúsculas de minúsculas).

#### `PUT /categorias/{id}` 🔒

Renomeia, desativa ou reativa uma categoria própria.

```json
{ "nome": "Supermercado", "ativa": false }
```

- O `tipo` não pode mudar depois de criado. Mudar o tipo deixaria lançamentos antigos incoerentes.
- `200` com a categoria atualizada.
- `403` se for uma categoria padrão; `404` se for de outro usuário.
- Não existe `DELETE`: categorias são desativadas, nunca apagadas, para preservar o histórico.

### 2.4 Lançamentos

Formato de um lançamento:

```json
{
  "id": 42,
  "tipo": "DESPESA",
  "valor": 150.75,
  "data": "2026-10-28",
  "descricao": "Compra do mês",
  "categoria": { "id": 7, "nome": "Mercado" },
  "criadoEm": "2026-10-28T14:30:00Z",
  "atualizadoEm": "2026-10-28T14:30:00Z"
}
```

#### `GET /lancamentos` 🔒

Lista os lançamentos ativos (não excluídos) do usuário, paginados, do mais recente para o mais antigo.

- Filtros, todos opcionais: `inicio` e `fim` (datas, inclusivas), `tipo`, `categoriaId`.
- `200` no formato de paginação (seção 1.7).

#### `GET /lancamentos/{id}` 🔒

`200` com o lançamento. `404` se não existir, se for de outro usuário ou se estiver excluído.

#### `POST /lancamentos` 🔒

```json
{
  "tipo": "DESPESA",
  "valor": 150.75,
  "data": "2026-10-28",
  "descricao": "Compra do mês",
  "categoriaId": 7
}
```

- Validações:
  - `tipo`, `valor`, `data` e `categoriaId` obrigatórios; `descricao` opcional (até 255 caracteres);
  - `valor` maior que zero, com no máximo 2 casas decimais;
  - a categoria deve ser padrão ou do próprio usuário, estar ativa e ter o mesmo `tipo` do lançamento.
- `201` com o lançamento e o cabeçalho `Location: /api/v1/lancamentos/42`.

#### `PUT /lancamentos/{id}` 🔒

Mesmo corpo e validações do `POST`. `200` com o lançamento atualizado.

#### `DELETE /lancamentos/{id}` 🔒

**Exclusão lógica** (RF-09): preenche `excluidoEm`, sem apagar e sem alterar o valor. O lançamento deixa de aparecer nas listas e de contar no saldo e nos resumos. `204`.

#### `GET /lancamentos/excluidos` 🔒

Lista os lançamentos excluídos do usuário, paginados, do excluído mais recentemente para o mais antigo. Cada item inclui o campo `excluidoEm`.

#### `POST /lancamentos/{id}/restaurar` 🔒

Volta um lançamento excluído a ser ativo (`excluidoEm = null`).

- `200` com o lançamento restaurado.
- `404` se não existir, se for de outro usuário ou se não estiver excluído.
- Se a categoria do lançamento estiver desativada, ele é restaurado mesmo assim e mantém a categoria.

### 2.5 Relatórios

Ambos consideram apenas lançamentos ativos do usuário. `inicio` e `fim` são obrigatórios.

#### `GET /relatorios/saldo?inicio=2026-10-01&fim=2026-10-31` 🔒

Balanço geral do período (RF-16). `200`:

```json
{
  "inicio": "2026-10-01",
  "fim": "2026-10-31",
  "totalReceitas": 5000.00,
  "totalDespesas": 3200.50,
  "saldo": 1799.50
}
```

`saldo` pode ser negativo.

#### `GET /relatorios/categorias?inicio=2026-10-01&fim=2026-10-31&tipo=DESPESA` 🔒

Total por categoria no período (RF-17). `tipo` é opcional. `200`, ordenado do maior total para o menor:

```json
[
  { "categoriaId": 3, "nome": "Moradia", "tipo": "DESPESA", "total": 1500.00, "quantidade": 1 },
  { "categoriaId": 7, "nome": "Mercado", "tipo": "DESPESA", "total": 1200.00, "quantidade": 8 }
]
```

## 3. Fora do MVP

- Endpoints do grupo familiar (RF-20 a RF-23): serão desenhados quando o grupo entrar no escopo. Eles devolverão apenas resumos por categoria, nunca lançamentos (RNF-05).
- Forma de pagamento, recorrência, parcelas, exportação CSV e limites por categoria.
- Renovação automática do token (*refresh token*): por enquanto o usuário entra de novo a cada hora.
