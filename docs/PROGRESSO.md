# Progresso do Projeto

> Atualize este arquivo **ao fim de cada sessão**. Decisões e motivos ficam em `DECISOES.md`.

**Última atualização:** 2026-11-06

## Já feito

- [x] Primeiro commit do repositório.
- [x] `docs/DECISOES.md` criado com contexto, stack, modelo de acesso e modelo de dados.
- [x] Sessão de setup (2026-10-22):
  - [x] `.gitignore` criado (segredos, bancos locais, build, IDEs, SO).
  - [x] `AGENTS.md` criado (guia para agentes de IA).
  - [x] `docs/PROGRESSO.md` criado (este arquivo).
  - [x] Convenções de commits, branches, comentários e segredos registradas em `DECISOES.md`.
  - [x] Escapes quebrados do Markdown corrigidos em `DECISOES.md`.
- [x] Sessão de planejamento (2026-10-28):
  - [x] Convenções aprovadas pelo usuário.
  - [x] Regra "agentes não fazem commit, só staging" registrada em `AGENTS.md` e `DECISOES.md`.
  - [x] Decididos Java 21, Spring Boot 4.x e Maven (motivos em `DECISOES.md`).
  - [x] Estrutura do monorepo criada: `backend/` e `desktop/`, cada uma com `README.md`.
  - [x] `README.md` da raiz reescrito com a descrição e o status do projeto.
  - [x] `docs/REQUISITOS.md` criado com requisitos funcionais e não funcionais.
  - [x] Todas as 11 perguntas em aberto respondidas.
  - [x] Decidido PostgreSQL desde o início (substitui "SQLite para começar") e API e banco na nuvem.
  - [x] Modelo de dados do MVP fechado em `DECISOES.md`: `usuario`, `categoria` (nova tabela), `lancamento` com `tipo` e exclusão lógica.
  - [x] Branch `docs/setup-e-requisitos` publicada no GitHub e PR aberto pelo usuário.
  - [x] Decidido: desenvolver e testar localmente antes da nuvem, PostgreSQL instalado no Windows, JWT de 1 hora e JavaFX 21 (LTS).
  - [x] `docs/API.md` criado com as convenções REST e os 17 endpoints do MVP.
  - [x] Aprovado: URLs e campos JSON da API em português.
  - [x] Todos os requisitos de `REQUISITOS.md` aprovados pelo usuário.
  - [x] `docs/API.md` aprovado (paginação delegada ao agente).
  - [x] Flyway escolhido para as migrações do banco.
  - [x] Categorias padrão definidas; "Investimentos" é despesa e os rendimentos entram em "Receitas".
  - **Fase de planejamento concluída.** Próxima fase: código do backend.
- [x] Sessão de estrutura do backend (2026-10-29):
  - [x] PR de documentação (#1) mergeado na `main`.
  - [x] Branch `feature/backend-estrutura-inicial` criada a partir da `main`.
  - [x] `pom.xml` agregador na raiz e projeto Spring Boot 4.1.2 em `backend/` (pacote `br.com.financeiro.backend`).
  - [x] Migração `V1__criar_tabelas_iniciais.sql`: tabelas `usuario`, `categoria`, `lancamento` e as 8 categorias padrão.
  - [x] `backend/.env.example` versionado e `backend/.env` local (fora do Git) lido pelo Spring.
  - [x] `mvn verify` passa. A API sobe e tenta conectar no PostgreSQL local.
  - [x] Banco `financeiro` e usuário `financeiro_app` criados pelo usuário; `DB_PASSWORD` preenchido no `.env`.
  - [x] API executada: o Flyway aplicou a `V1` e as tabelas foram conferidas pelo usuário.
  - [x] RNF-10 alterado: datas de criação e alteração só em `lancamento` e `usuario`; `categoria` não precisa.
- [x] Sessão de entidades JPA (2026-11-03):
  - [x] PR da estrutura inicial (#2) mergeado na `main`.
  - [x] Decidido usar Testcontainers com Docker nos testes de integração.
  - [x] Entidades `Usuario`, `Categoria` e `Lancamento`, e o enum `Tipo`, validadas pelo `ddl-auto=validate`.
  - [x] Repositories só com consultas filtradas por dono, sem `findAll` nem exclusão física de lançamento.
  - [x] `PersistenceMappingTest` roda contra um PostgreSQL 16 descartável, com as migrações do Flyway.
  - [x] Branches locais `claude/financial-app-setup-a0e84b` e `claude/pendencias-status-dia-c95625` removidas.
- [x] Sessão de autenticação (2026-11-03):
  - [x] PR das entidades (#3) mergeado na `main`.
  - [x] Spring Security com JWT: `POST /auth/cadastro`, `POST /auth/login` e `GET /usuarios/me`; as demais rotas exigem token.
  - [x] `AuthorizationService`: identifica o usuário do token e concentra a regra de alteração de categoria.
  - [x] Erros no formato RFC 9457, em português, inclusive os dos filtros de segurança.
  - [x] Limite de 72 bytes na senha registrado em `docs/API.md` e `DECISOES.md`.
  - [x] `AuthApiTest` (HTTP real e PostgreSQL descartável) e `AuthorizationServiceTest`.
- [x] Sessão de senha e conta (2026-11-03):
  - [x] PR da autenticação (#4) mergeado na `main`.
  - [x] `PUT /usuarios/me/senha` (RF-04) e `DELETE /usuarios/me` (RF-05).
  - [x] `UsuarioApiTest` e base `ApiTestSupport`, que reaproveita o mesmo contêiner entre as classes de teste.
- [x] Sessão de categorias (2026-11-03):
  - [x] PR de senha e conta (#5) mergeado na `main`.
  - [x] `GET`, `POST` e `PUT /categorias` (RF-14, RF-15), usando `AuthorizationService.assertCanEdit`.
  - [x] `CategoriaApiTest`, que também cobre a resposta `403` por HTTP.
- [x] Sessão de lançamentos (2026-11-04):
  - [x] PR de categorias (#6) mergeado na `main`.
  - [x] Rotas de `/lancamentos` (RF-06 a RF-10, RF-24): filtros, paginação, exclusão lógica, lista de excluídos e restauração.
  - [x] `LancamentoApiTest`, com a separação entre usuários em todas as rotas.
- [x] Sessão de relatórios (2026-11-04):
  - [x] `GET /relatorios/saldo` (RF-16) e `GET /relatorios/categorias` (RF-17).
  - [x] `RelatorioApiTest`, que confere os cálculos (RNF-14).
  - **Todos os 17 endpoints do MVP estão implementados no backend.**
- [x] Sessão de OpenAPI (2026-11-04):
  - [x] PRs de lançamentos (#7) e relatórios (#8) mergeados na `main`.
  - [x] `springdoc-openapi` com Swagger desligado por padrão; liga com `SWAGGER_ENABLED=true`.
  - [x] `SwaggerEnabledTest` confere a especificação contra as 17 rotas do contrato, e `SwaggerDisabledTest` confere o bloqueio padrão.
- [x] Sessão da base do desktop (2026-11-04):
  - [x] PR de OpenAPI (#9) mergeado na `main`; Swagger conferido pelo usuário no navegador.
  - [x] Proposta do desktop aprovada (decisões em `DECISOES.md`, "Desktop: projeto e sessão").
  - [x] Módulo `desktop` com JavaFX 21: login, cadastro e logout (RF-01 a RF-03), com o token só na memória.
  - [x] `401` em rota autenticada volta para o login com aviso.
  - [x] `ApiClientTest`, `ApiConfigTest` e `ViewsLoadTest`.
  - [x] `mvn verify` na raiz passa: 117 testes no backend e 33 no desktop.
  - [x] Conferido à mão pelo usuário: login com Enter e Tab, e a mensagem de API fora do ar.
  - [x] Branches locais já mergeadas removidas.
- [x] Sessão da listagem do desktop (2026-11-04):
  - [x] PR da base do desktop (#10) mergeado na `main`; branches antigas do backend removidas do GitHub.
  - [x] Menu lateral e tela de lançamentos com filtros por período, tipo e categoria, e paginação (RF-10).
  - [x] Formatos brasileiros de moeda e data (RNF-07).
  - [x] `FormatsTest` e novos casos em `ApiClientTest` e `ViewsLoadTest`.
- [x] Sessão do formulário de lançamentos (2026-11-04):
  - [x] PR da listagem (#11) mergeado na `main`.
  - [x] Criar, editar e excluir lançamentos pelo desktop (RF-06 a RF-09), com confirmação na exclusão.
  - [x] Sessão expirada fecha o formulário aberto e volta para o login.
  - [x] `LancamentoFilterTest` e novos casos em `ApiClientTest`, `FormatsTest` e `ViewsLoadTest`.
- [x] Sessão do resumo (2026-11-04):
  - [x] PR do formulário (#12) mergeado na `main`.
  - [x] Tela de Resumo com saldo e total por categoria do período (RF-16, RF-17), aberta depois do login.
  - [x] Novos casos em `ApiClientTest`, `FormatsTest` e `ViewsLoadTest`.
  - **Todos os requisitos funcionais essenciais do MVP estão disponíveis no desktop.**
- [x] Sessão de categorias do desktop (2026-11-04):
  - [x] PR do resumo (#13) mergeado na `main`.
  - [x] Tela de Categorias: lista padrão e próprias; cria, renomeia, desativa e reativa as próprias (RF-14, RF-15).
  - [x] Novos casos em `ApiClientTest` e `ViewsLoadTest`.
- [x] Sessão de excluídos do desktop (2026-11-04):
  - [x] PR de categorias (#14) mergeado na `main`.
  - [x] Tela de Excluídos: lista paginada e restauração (RF-24), com a hora da exclusão no fuso do computador.
  - [x] Novos casos em `ApiClientTest`, `FormatsTest` e `ViewsLoadTest`.
- [x] Sessão da conta do desktop (2026-11-04):
  - [x] Merge dos excluídos na `main`.
  - [x] Tela de Conta: dados do usuário, troca de senha (RF-04) e exclusão da conta com confirmação pelo e-mail (RF-05).
  - [x] Novos casos em `ApiClientTest` e `ViewsLoadTest`.
  - **Todas as telas do MVP estão prontas no desktop, rodando contra a API local.**
- [x] Sessão do limite de login (2026-11-04):
  - [x] PR da conta (#16) mergeado na `main`.
  - [x] Backend: 5 falhas seguidas bloqueiam o e-mail por 15 minutos, com `429` e `Retry-After`.
  - [x] `LoginAttemptLimiterTest` e novos casos em `AuthApiTest` e no `ApiClientTest` do desktop.
- [x] Sessão da revisão de segurança (2026-11-04 e 2026-11-05):
  - [x] PR do limite de login (#17) mergeado na `main`.
  - [x] Revisão feita por leitura do código e sondagens locais. Não foi um teste de invasão, e nenhum scanner de vulnerabilidades foi rodado.
  - [x] Cadastro restrito à lista de e-mails, ligado por padrão; `SIGNUP_RESTRICTED=false` libera em testes e homologação.
  - [x] `DELETE /usuarios/me` exige a senha, e a janela de exclusão do desktop passou a pedi-la.
  - [x] Corpo do pedido de login limitado a 4 KB, com `413`.
  - [x] `SWAGGER_ENABLED` removido do `.env.example`.
  - [x] Regra de senha mantida em 8 caracteres, sem exigências extras.
  - [x] `SignupRestrictionTest` e novos casos em `AuthApiTest`, `UsuarioApiTest`, `AuthorizationServiceTest` e no `ApiClientTest` do desktop.
  - [x] Branches locais já mergeadas removidas.

- [x] Sessão da escolha da hospedagem (2026-11-06):
  - [x] PR da revisão de segurança (#18) mergeado na `main`.
  - [x] Comparados provedores e formas de empacotar o desktop, com preços conferidos no dia.
  - [x] Decidido: banco no Supabase grátis, API no Render grátis, os dois no leste dos EUA.
  - [x] Decidido: backup semanal manual, sem criptografia, no desktop de casa, separado do notebook de desenvolvimento.
  - [x] O usuário vai tornar o repositório público no GitHub. Aceitou que o e-mail pessoal apareça como autor dos commits.
  - [x] Trocar a senha encerra todos os tokens do usuário, e sair encerra o token daquele aparelho: `POST /auth/sair` e migração `V2` (`versao_token` e `token_revogado`).
  - [x] Desktop: Sair e fechar a janela encerram o token na API; trocar a senha volta para o login.
  - [x] Desktop: espera de até 3 minutos pela API, com aviso no login depois de 5 segundos.
  - [x] Testes de integração passaram para o PostgreSQL 17, a versão do Supabase.
  - [x] Novos casos em `AuthApiTest`, `UsuarioApiTest`, `SwaggerEnabledTest` e no `ApiClientTest` do desktop.
  - [x] Itens menores da revisão descartados pelo usuário.
  - [x] Licença MIT adicionada (`LICENSE`).

## Próxima sessão

Publicar a API e o banco, seguindo `DECISOES.md`, "Provedores de nuvem":

- Criar o projeto no Supabase (`us-east-1`) e o serviço no Render (Virginia), e preparar a imagem Docker da API.
- `DB_URL` pelo *session pooler* do Supabase, com `sslmode=require`.
- Definir `SIGNUP_RESTRICTED=true`, `SIGNUP_ALLOWED_EMAILS` (a lista real está no `backend/.env` local) e um `JWT_SECRET` novo no Render.
- Medir quanto a API leva para acordar no Render grátis e conferir se os 3 minutos de espera do desktop bastam.
- Conferir o HTTPS e o HSTS, e que o Render repassa o corpo do `DELETE /usuarios/me`.
- Montar o backup semanal no desktop de casa: instalar o `pg_dump` 17 e agendar o script.
- Limite geral de pedidos e limite por IP: ver o que o Render oferece e reavaliar.

## Backlog (depois)

- [ ] Desktop: gerar um instalador para o Windows, para a família usar sem Maven nem JDK. Recomendação, ainda não decidida: `jpackage` gerado pelo GitHub Actions e publicado nas versões do GitHub; atualização automática com o jDeploy depois.
- [ ] CI (build e testes), com verificação de vulnerabilidades nas dependências, e proteção da branch `main`. Conferir o `ViewsLoadTest` em ambiente sem tela: ele deveria ser ignorado, mas isso ainda não foi testado.
- [ ] Tela de administração para o usuário incluir e-mails na lista de cadastro. Exige guardar a lista no banco e criar um perfil de administrador; nenhum dos dois existe hoje.
- [ ] Pós-MVP: forma de pagamento, recorrência, parcelas, exportação CSV, limite por categoria.
- [ ] Pós-MVP: versão para macOS.
- [ ] Futuro: grupo familiar (`grupo_familiar`, `grupo_membro`, `compartilha_resumo`).

## Pendências e dúvidas em aberto

- O PostgreSQL local do Windows é o 16, e o Supabase e os testes usam o 17. Avaliar instalar o 17 localmente.
- Testar à mão no desktop:
  - a expiração real do token, depois de 1 hora de sessão (o retorno ao login só foi testado com um token inválido);
  - a janela de exclusão da conta, com a senha certa e com a errada;
  - Sair, fechar a janela e trocar a senha: depois de cada um, o token antigo deve receber `401`;
  - o aviso de espera no login, com a API demorando para responder.
