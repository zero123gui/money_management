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
  - Fixado em **21.0.13** em 2026-11-04, a versão 21 mais recente no Maven Central na criação do projeto.

## Estrutura do repositório

Decidido em 2026-10-28.

```
financeiro/
├── backend/   API REST (Spring Boot)
├── desktop/   cliente JavaFX
└── docs/      decisões, requisitos e progresso
```

- A raiz tem um `pom.xml` agregador (Maven multimódulo) com `backend` e `desktop` como módulos. Assim, um único `mvn verify` na raiz compila e testa tudo, e cada parte continua independente.
- **Por que não um projeto único?** MVC é um padrão de organização *dentro* de uma aplicação, não uma forma de juntar as duas. O backend já segue camadas (controller, service, repository) e o desktop segue MVC com FXML. Juntar tudo num só programa faria o desktop acessar o banco direto: a senha do banco ficaria em cada computador e o isolamento de dados por usuário deixaria de ser garantido pelo servidor, contrariando o "Modelo de acesso".
- Um módulo compartilhado (`common/`, por exemplo para DTOs) **não** será criado por enquanto. Motivo: o desktop conversa com a API por JSON; compartilhar classes acopla as duas versões. Reavaliar se houver duplicação real.

## Backend: projeto e banco

Decidido em 2026-10-29, na criação do projeto.

- **Coordenadas Maven**: `groupId` `br.com.financeiro`; o POM agregador da raiz é `financeiro` e o backend é `financeiro-backend`. Pacote base: `br.com.financeiro.backend`.
- O **POM da raiz só agrega** os módulos; não é pai deles. Motivo: o backend precisa herdar do `spring-boot-starter-parent`, e o desktop terá outras dependências. O `desktop` entrou na lista de módulos em 2026-11-04.
- **Configuração local em `backend/.env`** (fora do Git), lido pelo próprio Spring com `spring.config.import=optional:file:.env[.properties]`. Motivo: dispensa plugin ou biblioteca extra e funciona igual no terminal e na IDE. Variáveis de ambiente reais têm prioridade sobre o arquivo, então em produção basta defini-las no provedor. O modelo versionado é `backend/.env.example`.
- Variáveis: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION` (padrão `PT1H`), `SWAGGER_ENABLED` (só no ambiente local), `LOGIN_MAX_ATTEMPTS` (padrão `5`), `LOGIN_BLOCK_DURATION` (padrão `PT15M`), `SIGNUP_RESTRICTED` (padrão `true`), `SIGNUP_ALLOWED_EMAILS` (padrão vazio).
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

## Desktop: projeto e sessão

Decidido em 2026-11-04, com a proposta aprovada pelo usuário.

- **Coordenadas Maven**: `financeiro-desktop`, pacote base `br.com.financeiro.desktop`.
- **`HttpClient` do próprio Java** e **Jackson** para o JSON, sem Spring. Motivo: o cliente é pequeno, e o Spring só acrescentaria peso.
- **Tema padrão do JavaFX com um CSS próprio pequeno.** Motivo: não adiciona dependência, e o tema pode ser trocado depois.
- **Token só na memória.** O usuário entra ao abrir o aplicativo e de novo quando o token expira, depois de 1 hora. Motivo: nenhuma credencial fica no disco (ver RNF-08). "Lembrar de mim" só será reavaliado depois do MVP.
- **`401` em rota autenticada volta para o login**, com um aviso. O que estiver sendo digitado em um formulário aberto se perde. Motivo: simples, e aceito pelo usuário por enquanto; pode ser revisto se incomodar. No login, `401` é só credencial errada.
- **Endereço da API em `API_BASE_URL`**, com `http://localhost:8080` como padrão. Fora da própria máquina, o aplicativo recusa HTTP. Motivo: publicar na nuvem não exige mudar o código, e senhas e tokens não passam abertos pela rede (RNF-03).
- **Validação fica na API.** O cliente só compara os campos de senha e de confirmação no cadastro, pois a API recebe uma senha só.
- **Depois do cadastro, o aplicativo já faz o login.** Se esse login falhar, leva à tela de login em vez de mostrar erro, pois a conta já existe.
- **A lista de lançamentos abre no mês atual.** O botão Limpar tira todos os filtros, inclusive o período. Motivo: o mês corrente é o caso mais comum, e a API não define um período padrão.
- **Valor na cor do tipo, sem sinal de menos**: a API sempre devolve valor positivo, e o tipo também aparece em uma coluna própria.
- **Formulário de lançamento em janela própria**, sobre a principal, para criar e editar. Motivo: a lista e os filtros continuam como estavam ao voltar.
- **Valor digitado só no formato brasileiro**: `1.234,56`, `1234,5` ou `1234`. Valores como `10.50` e `1,234` são recusados em vez de adivinhados. Motivo: lidos do jeito errado, virariam outro valor sem ninguém perceber. Os demais limites do valor continuam na API.
- **Exclusão com confirmação**, mesmo sendo reversível. Motivo: o lançamento some da lista e do saldo com um clique, e a tela de excluídos fica em outro lugar.
- **Resumo é a primeira tela depois do login** e abre no mês atual. Os números vêm sempre da API, sem cálculo no cliente (RNF-14). Motivo: é a visão geral do mês, e o cálculo já está testado no backend.
- **Trocar de tela pelo menu recarrega a tela de destino**, e os filtros voltam ao padrão. Motivo: os números nunca ficam velhos depois de uma alteração em outra tela. Guardar os filtros pode ser avaliado se incomodar.
- **Desativar ou reativar categoria não pede confirmação**, ao contrário de excluir lançamento. Motivo: nada some da tela nem dos totais, e o mesmo botão desfaz a ação.
- **Nas categorias padrão, os botões de alteração ficam desligados**, só para evitar uma tentativa inútil. Quem proíbe continua sendo a API, com `403`.
- **Data e hora aparecem no fuso do computador**, convertidas na tela a partir do UTC da API, decidido em 2026-11-04. A API e o banco continuam em UTC. Motivo: cada computador mostra o próprio horário, sem configuração, e o servidor na nuvem pode estar em outro fuso.
- **Restaurar lançamento não pede confirmação.** Motivo: a ação não perde nada, e o lançamento pode ser excluído de novo.
- **Excluir a conta exige digitar o e-mail**, e o botão só é liberado com o texto certo. Motivo: é a única ação do aplicativo sem volta. A API não pede essa confirmação; ela só evita um engano na tela. A janela também pede a senha desde 2026-11-05, conferida pela API (ver "Exclusão da conta exige a senha").
- **Trocar a senha mantém a sessão aberta.** Motivo: o token continua válido até expirar, como já diz `docs/API.md`.
- **Resumo sem gráfico no MVP**: uma tabela já atende o RF-17.
- **Testes**: cliente HTTP contra um servidor local falso e carregamento de cada FXML. As telas são conferidas à mão. Motivo: testes automatizados de tela costumam ser frágeis, e as regras importantes já estão testadas no backend.

## Ambiente de desenvolvimento

Decidido em 2026-10-28.

- **Desenvolver, buildar e testar tudo localmente antes de ir para a nuvem.** Motivo: erros são mais fáceis de achar e corrigir na própria máquina, sem custo e sem expor uma API incompleta na internet. A nuvem entra quando o MVP funcionar localmente.
- **PostgreSQL instalado direto no Windows** do desenvolvedor, que já está instalado. Motivo: já está disponível.
  ~~O Docker seria mais uma ferramenta para aprender sem ganho real para um único desenvolvedor. Reavaliar se surgir a necessidade de recriar o banco do zero com frequência ou de rodar o mesmo ambiente em outra máquina.~~ **Substituída** em 2026-11-03 pelo item abaixo.
- **Testes de integração com Testcontainers e Docker**, decidido em 2026-11-03. Cada execução sobe um PostgreSQL 16 descartável (`postgres:16-alpine`), aplica as migrações do Flyway e descarta o contêiner no fim. O Docker Desktop precisa estar aberto para rodar `mvn verify`.
- O banco local usa **dados fictícios**. Dados financeiros reais da família só existem no banco da nuvem e nunca são copiados para a máquina de desenvolvimento.
- A **estrutura** do banco (tabelas, colunas, restrições) é igual no local e na nuvem. Os **dados** são diferentes.

## Modelo de acesso

- Cada lançamento pertence a um usuário (`usuario_id`). Por padrão, só o dono pode ler.
- O isolamento é feito no backend. Toda consulta filtra pelo usuário autenticado.
- O cliente não decide o que exibir. A API devolve apenas o que o usuário pode ver.
- A regra de autorização fica em um service centralizado, para que mudanças futuras fiquem localizadas.
- **JWT com o suporte do próprio Spring Security** (`spring-boot-starter-security-oauth2-resource-server`), decidido em 2026-11-03, em vez de uma biblioteca à parte como a JJWT. Motivo: o Spring já emite e valida o token, sem filtro escrito à mão para conferir assinatura e validade. O nome "OAuth2" vem do padrão de token usado; a API não usa login externo.
  - Assinatura HS256 com a chave de `JWT_SECRET`, em Base64 e com pelo menos 32 bytes. Motivo: emissor e validador são o mesmo servidor, então uma chave simétrica basta. A API não sobe com uma chave inválida.
  - O token só carrega o id do usuário, pois o conteúdo de um JWT pode ser lido por quem o tiver.
  - Sem tolerância de relógio na validade, pois o mesmo servidor emite e valida.
- **Senha com no máximo 72 bytes**, decidido em 2026-11-03. Motivo: é o limite do BCrypt. O limite é em bytes, não em caracteres, porque letras acentuadas ocupam mais de um byte. O login não aplica esse limite: uma senha maior só resulta em credenciais inválidas.
- **Repositories só expõem consultas filtradas por dono**, decidido em 2026-11-03: estendem `Repository`, não `JpaRepository`. Motivo: `JpaRepository` traz `findAll` e `findById` sem filtro, e um uso distraído vazaria dados de outro usuário. Sem esses métodos, o erro nem compila. `LancamentoRepository` também não tem método de exclusão física, pois a exclusão de lançamento é lógica.
- **Limite de tentativas de login por e-mail**, decidido em 2026-11-04: 5 falhas seguidas bloqueiam o e-mail por 15 minutos a partir da última, com `429`. Os valores vêm de `LOGIN_MAX_ATTEMPTS` e `LOGIN_BLOCK_DURATION`. Motivo: reduz a adivinhação de senha antes de a API ir para a internet.
  - E-mails que não existem contam igual, e a senha certa também é recusada durante o bloqueio. Motivo: a API não revela quais e-mails existem nem se a senha estava certa.
  - **Contagem na memória**, sem biblioteca nova. Motivo: a API terá uma única instância. A contagem recomeça quando ela reinicia, e isso precisa ser revisto se houver mais de uma instância.
  - **Sem limite por IP por enquanto.** Motivo: atrás do proxy do provedor de nuvem, todos os pedidos podem parecer vir do mesmo IP, e o bloqueio atingiria a família inteira. Reavaliar ao escolher o provedor.
  - **Risco aceito:** quem souber um e-mail pode bloquear o login dessa pessoa errando a senha de propósito. Quem já está logado não é afetado, e o bloqueio acaba sozinho.
- **Cadastro restrito a uma lista de e-mails**, decidido em 2026-11-05, depois da revisão de segurança. Com `SIGNUP_RESTRICTED=true`, o padrão, só os e-mails de `SIGNUP_ALLOWED_EMAILS` criam conta; os demais recebem `403`. Testes e homologação usam `SIGNUP_RESTRICTED=false`. Motivo: na internet, qualquer pessoa poderia criar conta e encher o banco; nos testes, a lista só atrapalharia.
  - **Ligada por padrão.** Motivo: se a variável faltar em produção, o cadastro continua fechado, como o Swagger. Com a lista vazia, ninguém se cadastra.
  - **A lista fica em variável de ambiente, não no Git.** Motivo: são e-mails reais da família. A lista real está no `backend/.env` local e, na publicação, vai para as variáveis do provedor.
  - A regra só vale para criar conta: quem já tem conta continua entrando, mesmo fora da lista.
  - Incluir alguém exige mudar a variável e reiniciar a API, até existir a tela de administração prevista no backlog.
- **Exclusão da conta exige a senha**, decidido em 2026-11-05: `DELETE /usuarios/me` recebe a `senha` no corpo. Motivo: a ação não tem volta, e um token roubado não deve bastar para apagar tudo. O corpo em `DELETE` foi mantido para não mudar a rota do contrato; a RFC 9110 não define o significado dele, então é preciso conferir que o proxy do provedor o repassa.
- **Limite de 4 KB no corpo do pedido de login**, decidido em 2026-11-05, com `413`. Motivo: a rota é pública, e a revisão confirmou que ela aceitava 5 MB. O filtro lê no máximo o limite, então também vale para corpo sem `Content-Length`. **Só no login**, por decisão do usuário em 2026-11-05: `POST /auth/cadastro`, também pública, e as rotas com token ficam sem esse limite.
- **Senha com no mínimo 8 caracteres, sem exigências extras**, confirmado pelo usuário em 2026-11-05. Motivo: o aplicativo não movimenta dinheiro, e o usuário não quer endurecer mais essa regra.
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

1. Escolher o provedor de nuvem e publicar a API e o banco.
2. Desktop: gerar um instalador para o Windows.
3. Tela de administração para incluir e-mails na lista de cadastro.
4. Depois do MVP: grupo familiar, com a regra de compartilhamento.

O detalhamento e o status ficam em `docs/PROGRESSO.md`.
