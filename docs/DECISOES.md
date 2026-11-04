# Controle Financeiro Familiar: Decisões de Arquitetura

> Este arquivo é o registro oficial das decisões do projeto.
> Toda decisão nova entra aqui, com data e motivo. Não apague decisões antigas:
> se uma decisão mudar, marque a antiga como **Substituída** e aponte para a nova.

## Contexto

Aplicação desktop de controle financeiro para a família, usada em vários dispositivos. Cada usuário tem login próprio e dados privados. No futuro, haverá um grupo familiar que compartilha resumos.

## Perfil do desenvolvedor

Experiência com Java. Pouca familiaridade com Python, Electron e Tauri.

## Ferramentas

- **Claude Code**: desenvolvimento do projeto dentro do repositório.
- **Cowork**: apoio opcional, para documentos e especificações.
- **Repositório**: monorepo no GitHub, com as pastas `backend/` e `desktop/`.

## Stack

- **Backend**: Spring Boot, API REST, Spring Security com autenticação JWT, **PostgreSQL desde o início**.
  ~~SQLite para começar~~: **Substituída** em 2026-10-28. Motivo: com a API e o banco na nuvem, o arquivo do SQLite pode ser apagado a cada deploy (ver "Hospedagem").
- **Desktop**: JavaFX, consumindo a API. **Windows** no MVP; **macOS** no futuro (o JavaFX roda nos dois).

## Versões e build

Decidido em 2026-10-28.

- **Java 21 (LTS)** no backend e no desktop. Motivo: padrão escolhido pelo usuário; LTS com suporte longo.
- **Spring Boot 4.x**: a linha estável atual. A versão exata de patch é fixada no `pom.xml` quando o projeto for criado e registrada aqui. Motivo: a linha 3.x já saiu do suporte gratuito; o 4.x é compatível com Java 21.
  - Fixado em **4.1.2** em 2026-10-29, a versão estável mais recente no Maven Central na criação do projeto.
- **Maven** como ferramenta de build. Motivo:
  - configuração declarativa em XML (`pom.xml`), sem precisar aprender uma linguagem de script (o Gradle usa Groovy ou Kotlin);
  - é o padrão mais comum em tutoriais de Spring Boot e JavaFX, e tem plugin oficial para JavaFX (`javafx-maven-plugin`);
  - suporta projeto multimódulo, que é o formato do monorepo (ver abaixo).
  O Gradle compila mais rápido em projetos grandes, mas essa vantagem não pesa num projeto deste tamanho.
- **Flyway** para migrações do banco, decidido em 2026-10-28. Cada mudança de estrutura (criar tabela, adicionar coluna) vira um arquivo SQL numerado em `backend/src/main/resources/db/migration` (ex.: `V1__criar_tabelas_iniciais.sql`). O Flyway aplica os arquivos pendentes ao iniciar a API. Motivo: garante que o banco local e o da nuvem tenham a mesma estrutura, e o histórico do banco fica versionado no Git. Regras:
  - uma migração já aplicada **nunca é editada**; para corrigir, cria-se uma nova;
  - o Hibernate não cria nem altera tabelas (`spring.jpa.hibernate.ddl-auto=validate`); só o Flyway faz isso;
  - as categorias padrão também são inseridas por migração.
- **JavaFX 21 (LTS)**, decidido em 2026-10-28. A versão exata de patch é fixada no `pom.xml` quando o projeto for criado. Motivo: o usuário pediu a versão mais estável. A 21 é LTS, e as versões mais novas do JavaFX exigem um JDK mais novo que o Java 21 do projeto.

## Estrutura do repositório

Decidido em 2026-10-28.

```
financeiro/
├── backend/   API REST (Spring Boot)
├── desktop/   cliente JavaFX
└── docs/      decisões, requisitos e progresso
```

- No futuro, a raiz terá um `pom.xml` agregador (Maven multimódulo) com `backend` e `desktop` como módulos. Assim, um único `mvn install` na raiz compila e testa tudo, e cada parte continua independente.
- **Por que não um projeto único?** MVC é um padrão de organização *dentro* de uma aplicação, não uma forma de juntar as duas. O backend já segue camadas (controller, service, repository) e o desktop segue MVC com FXML. Juntar tudo num só programa faria o desktop acessar o banco direto: a senha do banco ficaria em cada computador e o isolamento de dados por usuário deixaria de ser garantido pelo servidor, contrariando o "Modelo de acesso".
- Um módulo compartilhado (`common/`, por exemplo para DTOs) **não** será criado por enquanto. Motivo: o desktop conversa com a API por JSON; compartilhar classes acopla as duas versões. Reavaliar se houver duplicação real.

## Backend: projeto e banco

Decidido em 2026-10-29, na criação do projeto.

- **Coordenadas Maven**: `groupId` `br.com.financeiro`; o POM agregador da raiz é `financeiro` e o backend é `financeiro-backend`. Pacote base: `br.com.financeiro.backend`.
- O **POM da raiz só agrega** os módulos; não é pai deles. Motivo: o backend precisa herdar do `spring-boot-starter-parent`, e o desktop terá outras dependências. O `desktop` entra na lista de módulos quando tiver o seu `pom.xml`.
- **Configuração local em `backend/.env`** (fora do Git), lido pelo próprio Spring com `spring.config.import=optional:file:.env[.properties]`. Motivo: dispensa plugin ou biblioteca extra e funciona igual no terminal e na IDE. Variáveis de ambiente reais têm prioridade sobre o arquivo, então em produção basta defini-las no provedor. O modelo versionado é `backend/.env.example`.
- Variáveis: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION` (padrão `PT1H`).
- **Banco local**: banco `financeiro`, usuário `financeiro_app`, dono do banco. Motivo: a API não usa o superusuário `postgres`.
- **Detalhes da migração `V1`** que vão além do modelo de dados:
  - chaves `BIGINT` com `GENERATED BY DEFAULT AS IDENTITY`;
  - datas e horas em `TIMESTAMPTZ`, gravadas em UTC (a API devolve UTC, `docs/API.md` 1.2);
  - e-mail único sem diferenciar maiúsculas (índice único em `lower(email)`);
  - categoria única por dono, nome (sem diferenciar maiúsculas) e tipo;
  - o tipo do lançamento é igual ao da categoria, garantido por chave estrangeira composta `(categoria_id, tipo)`;
  - `valor > 0` garantido por `CHECK`;
  - excluir o usuário apaga em cascata suas categorias e lançamentos (RF-05);
  - índices parciais para lançamentos ativos e excluídos.

## Ambiente de desenvolvimento

Decidido em 2026-10-28.

- **Desenvolver, buildar e testar tudo localmente antes de ir para a nuvem.** Motivo: erros são mais fáceis de achar e corrigir na própria máquina, sem custo e sem expor uma API incompleta na internet. A nuvem entra quando o MVP funcionar localmente.
- **PostgreSQL instalado direto no Windows** do desenvolvedor, que já está instalado. Motivo: já está disponível.
  ~~O Docker seria mais uma ferramenta para aprender sem ganho real para um único desenvolvedor. Reavaliar se surgir a necessidade de recriar o banco do zero com frequência ou de rodar o mesmo ambiente em outra máquina.~~ **Substituída** em 2026-11-03 pelo item abaixo.
- **Testes de integração com Testcontainers e Docker**, decidido em 2026-11-03. Cada execução sobe um PostgreSQL 16 descartável (`postgres:16-alpine`), aplica as migrações do Flyway e descarta o contêiner no fim. O Docker Desktop precisa estar aberto para rodar `mvn verify`.
- O banco local usa **dados fictícios**. Dados financeiros reais da família só existem no banco da nuvem e nunca são copiados para a máquina de desenvolvimento.
- A **estrutura** do banco (tabelas, colunas, restrições) é igual no local e na nuvem. Os **dados** são diferentes.

## Modelo de acesso

- Cada lançamento pertence a um usuário (`user_id`). Por padrão, só o dono pode ler.
- O isolamento é feito no backend. Toda consulta filtra pelo usuário autenticado.
- O cliente não decide o que exibir. A API devolve apenas o que o usuário pode ver.
- A regra de autorização fica em um service centralizado, para que mudanças futuras fiquem localizadas.
- **JWT com o suporte do próprio Spring Security** (`spring-boot-starter-security-oauth2-resource-server`), decidido em 2026-11-03, em vez de uma biblioteca à parte como a JJWT. Motivo: o Spring já emite e valida o token, sem filtro escrito à mão para conferir assinatura e validade. O nome "OAuth2" vem do padrão de token usado; a API não usa login externo.
  - Assinatura HS256 com a chave de `JWT_SECRET`, em Base64 e com pelo menos 32 bytes. Motivo: emissor e validador são o mesmo servidor, então uma chave simétrica basta. A API não sobe com uma chave inválida.
  - O token só carrega o id do usuário, pois o conteúdo de um JWT pode ser lido por quem o tiver.
  - Sem tolerância de relógio na validade, pois o mesmo servidor emite e valida.
- **Senha com no máximo 72 bytes**, decidido em 2026-11-03. Motivo: é o limite do BCrypt. O limite é em bytes, não em caracteres, porque letras acentuadas ocupam mais de um byte. O login não aplica esse limite: uma senha maior só resulta em credenciais inválidas.
- **Repositories só expõem consultas filtradas por dono**, decidido em 2026-11-03: estendem `Repository`, não `JpaRepository`. Motivo: `JpaRepository` traz `findAll` e `findById` sem filtro, e um uso distraído vazaria dados de outro usuário. Sem esses métodos, o erro nem compila. `LancamentoRepository` também não tem método de exclusão física, pois a exclusão de lançamento é lógica.
- O **token JWT expira em 1 hora** (decidido em 2026-10-28). Depois disso, o usuário entra de novo. O valor fica em configuração, não fixo no código, para poder mudar sem recompilar.

## API

Proposta e **aprovada** em 2026-10-28. Contrato completo em `docs/API.md`. O usuário delegou ao agente os detalhes de paginação.

- API REST com prefixo de versão `/api/v1`, recursos no plural e ação pelo método HTTP.
- **URLs e campos JSON em português** (sem acento, camelCase), seguindo os nomes do domínio e do banco (`lancamentos`, `categoriaId`). Motivo: o contrato fica com os mesmos termos dos requisitos e do modelo de dados. **Aprovado** em 2026-10-28 como exceção à regra "identificadores em inglês" (ver "Idioma").
- Erros no formato RFC 9457 (Problem Details), com mensagens em português.
- Recurso de outro usuário responde `404`, para não revelar que ele existe.
- Documentação OpenAPI com `springdoc-openapi`, com o Swagger disponível só no ambiente local.
  - **Desligado por padrão**, decidido em 2026-11-04: só liga com `SWAGGER_ENABLED=true`, definido no `.env` local. Motivo: se a variável faltar em produção, a documentação continua fora do ar. Um perfil do Spring teria o risco inverso, de publicá-la caso o perfil de produção não fosse ativado.
  - Versão **3.2.0**, a linha do Spring Boot 4, fixada no `pom.xml` em 2026-11-04.

## Hospedagem

Decidido em 2026-10-28.

- A API e o banco de dados rodarão **na nuvem**, para atender todos os dispositivos da família.
- O desktop **exige internet**. Motivo: depende de autenticação e dos dados de cada usuário no servidor. Uso offline está fora do escopo.
- Com a API exposta na internet, **HTTPS é obrigatório**.
- O banco é **PostgreSQL gerenciado** pelo provedor. Motivo: o SQLite é um arquivo no disco do servidor, e em muitos serviços de nuvem esse disco é apagado a cada novo deploy. O serviço gerenciado evita a perda de dados e já traz backup (RNF-11).
- **Em aberto:** escolha do provedor de nuvem. Ela fica para depois do MVP rodar localmente (ver "Ambiente de desenvolvimento"), e preços e planos gratuitos devem ser conferidos no momento da escolha.

## Grupo familiar (futuro)

- O grupo vê apenas **resumos por categoria**, nunca lançamentos individuais.
- A configuração é uma única chave por usuário: `compartilha_resumo` (sim/não). Quando ativa, todas as categorias são compartilhadas.
- Os resumos são calculados sob demanda, sem cópia de dados. Ao desativar o compartilhamento, os resumos deixam de aparecer imediatamente.
- Tabelas previstas: `grupo_familiar` e `grupo_membro`.
- A regra futura de acesso: o dono lê seus dados, ou o solicitante pertence ao mesmo grupo e o usuário dono tem `compartilha_resumo = true` (apenas para resumos).

## Modelo de dados inicial

**Substituída** em 2026-10-28 pela seção "Modelo de dados (MVP)" abaixo.

- `usuario`: id, dados de login, `compartilha_resumo` (booleano).
- `lancamento`: id, `user_id`, valor, data, categoria, descrição. Sempre privado.

## Modelo de dados (MVP)

Decidido em 2026-10-28, a partir de `docs/REQUISITOS.md` (lá está de qual requisito vem cada campo).

**`usuario`**: `id`, `nome`, `email` (único; é o login), `senha_hash`, `compartilha_resumo`, `criado_em`, `atualizado_em`.

**`categoria`**: `id`, `nome`, `tipo` (`RECEITA`/`DESPESA`), `usuario_id` (nulo = categoria padrão do sistema), `ativa`.

**`lancamento`**: `id`, `usuario_id`, `tipo` (`RECEITA`/`DESPESA`), `valor` (`NUMERIC(15,2)`), `data`, `categoria_id`, `descricao`, `excluido_em`, `criado_em`, `atualizado_em`. Sempre privado.

Regras e motivos:

- **Login por e-mail**, não por nome de usuário. Motivo: escolha do usuário; o e-mail já é único por natureza.
- **Senha só como hash BCrypt** em `senha_hash`. Guardar o hash na tabela de usuários é o padrão do Spring Security: o hash não pode ser revertido para a senha, e o login compara o hash da senha digitada com o salvo.
- **Categoria é uma tabela**, não texto livre. Motivo: evita categorias duplicadas por erro de digitação ("Mercado" × "mercado"), permite renomear sem mexer no histórico, oferecer categorias padrão e deixar o resumo por categoria confiável.
- **Categoria nunca é apagada se tiver lançamentos**; ela é desativada (`ativa = false`) e some das opções de novos lançamentos.
- **Valor sempre positivo.** O `tipo` diz o sentido: receita é entrada, despesa é saída. Saldo = soma das receitas − soma das despesas.
- **Datas de auditoria (RNF-10)**, decidido em 2026-10-29: `criado_em` e `atualizado_em` existem em `lancamento` e `usuario`, mas **não em `categoria`**. Motivo: o usuário decidiu que a auditoria importa nos lançamentos; a categoria só tem nome, tipo e situação, e o histórico dela não é consultado.
- **Exclusão lógica de lançamento:** excluir preenche `excluido_em` e não altera o valor. O lançamento some das listas, do saldo e dos resumos, e restaurar volta `excluido_em` para nulo. Toda consulta de lançamentos filtra `excluido_em IS NULL`, além do usuário autenticado.
- **Fora do MVP:** forma de pagamento (RF-11), recorrência (RF-12), parcelas (RF-13) e exportação CSV (RF-19).

### Categorias padrão

Decidido em 2026-10-28 (RF-14). Criadas por migração do Flyway, com `usuario_id` nulo; valem para todos os usuários e não podem ser alteradas por eles.

| Nome         | Tipo      |
|--------------|-----------|
| Receitas     | `RECEITA` |
| Investimentos| `DESPESA` |
| Moradia      | `DESPESA` |
| Saúde        | `DESPESA` |
| Transporte   | `DESPESA` |
| Alimentação  | `DESPESA` |
| Pessoal      | `DESPESA` |
| Lazer        | `DESPESA` |

- Os nomes são dados exibidos ao usuário, por isso mantêm acento.
- Cada usuário pode criar categorias próprias além dessas (RF-15), por exemplo "Salário" ou "Freelance" como receita.
- **"Investimentos" é `DESPESA`** (decidido em 2026-10-28): registra o dinheiro aplicado, que sai do saldo. Os rendimentos e resgates entram como receita na categoria "Receitas". Motivo: é como a família já controla hoje.

## Convenções do projeto

Propostas em 2026-10-22 (sessão de setup). **Aprovadas pelo usuário em 2026-10-28.**

### Idioma

- Documentação, mensagens de commit e comentários: **português**.
- Identificadores de código (classes, métodos, variáveis): **inglês**, seguindo o padrão Java.
  Exceção: termos de domínio sem boa tradução podem manter o nome em português (ex.: `lancamento`), desde que consistentes.
  - **Entidades JPA**, decidido em 2026-11-03: as classes usam os termos do domínio (`Usuario`, `Categoria`, `Lancamento`, `Tipo`) e os atributos ficam em inglês, ligados às colunas por `@Column` (ex.: `amount` em `valor`). Motivo: aplica a regra acima, pois "valor" e "descricao" têm boa tradução; só os nomes de domínio são exceção.
- Banco de dados (tabelas e colunas) e contrato da API (URLs e campos JSON): **português**, sem acento. Os DTOs Java que representam o JSON usam os mesmos nomes dos campos (ex.: `categoriaId`). Decidido em 2026-10-28.

### Commits: Conventional Commits

Formato: `tipo(escopo): descrição curta no imperativo`

- Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`.
- Escopos: `backend`, `desktop`, `docs`, `repo`.
- Descrição com até ~72 caracteres, sem ponto final.
- Corpo opcional explica o *porquê*. Mudança incompatível: rodapé `BREAKING CHANGE:`.
- Exemplo: `feat(backend): adicionar autenticação JWT`.
- Agentes de IA não fazem commit: deixam os arquivos em staging e entregam a mensagem sugerida para o usuário revisar (decidido em 2026-10-28).

### Branches

Formato: `tipo/descricao-curta-em-kebab-case`

- Tipos: `feature/`, `fix/`, `docs/`, `refactor/`, `chore/`.
- `main` é sempre estável; nada é commitado direto nela. Mudanças entram por Pull Request.
- Exemplos: `feature/backend-auth-jwt`, `docs/atualiza-decisoes`.

### Comentários de código

- Java: **Javadoc** em toda classe e método público (o que faz, `@param`, `@return`, `@throws`).
- Comentários inline explicam o *porquê*, não o *quê*.
- Sem código comentado commitado; o Git guarda o histórico.
- `TODO(autor): descrição` somente com referência a item do `docs/PROGRESSO.md`.

### Segredos

- Chaves de API, tokens, senhas, chave de assinatura do JWT e bancos locais **nunca** entram no Git. O `.gitignore` bloqueia os padrões comuns.
- Configuração sensível vem de variáveis de ambiente ou de arquivos locais ignorados.
- O repositório traz apenas arquivos modelo (`.env.example`, `application-example.properties`) sem valores reais.
- ~~Hoje não há nenhuma variável de ambiente, então esses arquivos modelo só serão criados quando a primeira configuração existir (decidido em 2026-10-28).~~ **Substituída** em 2026-10-29: as primeiras variáveis surgiram com o backend (ver "Backend: projeto e banco").
- A senha do PostgreSQL e a chave do JWT também seguem essa regra: em produção, ficam nas variáveis de ambiente do provedor de nuvem.

### Documentação viva

- `docs/DECISOES.md` (este arquivo): decisões e motivos.
- `docs/PROGRESSO.md`: o que foi feito e o que fazer na próxima sessão. Atualizado ao fim de cada sessão.
- `docs/REQUISITOS.md`: requisitos funcionais e não funcionais, e os dados derivados deles.
- `docs/API.md`: contrato da API REST (convenções e endpoints).
- `AGENTS.md`: guia para agentes de IA que trabalharem no projeto.

## Próximos passos

1. Backend: modelos de usuário, categoria e lançamento, autenticação JWT e service de autorização.
2. Backend: endpoints de resumo por categoria, com a regra de compartilhamento.
3. Desktop: cliente JavaFX consumindo a API.

O detalhamento e o status ficam em `docs/PROGRESSO.md`.
