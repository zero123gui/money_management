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

- **Backend**: Spring Boot, API REST, Spring Security com autenticação JWT, PostgreSQL (SQLite para começar).
- **Desktop**: JavaFX, consumindo a API.

## Versões e build

Decidido em 2026-10-28.

- **Java 21 (LTS)** no backend e no desktop. Motivo: padrão escolhido pelo usuário; LTS com suporte longo.
- **Spring Boot 4.x**: a linha estável atual. A versão exata de patch é fixada no `pom.xml` quando o projeto for criado e registrada aqui. Motivo: a linha 3.x já saiu do suporte gratuito; o 4.x é compatível com Java 21.
- **Maven** como ferramenta de build. Motivo:
  - configuração declarativa em XML (`pom.xml`), sem precisar aprender uma linguagem de script (o Gradle usa Groovy ou Kotlin);
  - é o padrão mais comum em tutoriais de Spring Boot e JavaFX, e tem plugin oficial para JavaFX (`javafx-maven-plugin`);
  - suporta projeto multimódulo, que é o formato do monorepo (ver abaixo).
  O Gradle compila mais rápido em projetos grandes, mas essa vantagem não pesa num projeto deste tamanho.
- **JavaFX**: versão a definir quando o projeto desktop for criado.

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

## Modelo de acesso

- Cada lançamento pertence a um usuário (`user_id`). Por padrão, só o dono pode ler.
- O isolamento é feito no backend. Toda consulta filtra pelo usuário autenticado.
- O cliente não decide o que exibir. A API devolve apenas o que o usuário pode ver.
- A regra de autorização fica em um service centralizado, para que mudanças futuras fiquem localizadas.

## Hospedagem

Decidido em 2026-10-28.

- A API e o banco de dados rodarão **na nuvem**, para atender todos os dispositivos da família.
- O desktop **exige internet**. Motivo: depende de autenticação e dos dados de cada usuário no servidor. Uso offline está fora do escopo.
- Com a API exposta na internet, **HTTPS é obrigatório**.
- **Em aberto:**
  - escolha do provedor de nuvem;
  - se o banco começa direto em PostgreSQL. O SQLite é um arquivo no disco do servidor, e em muitos serviços de nuvem esse disco é apagado a cada novo deploy. Um PostgreSQL gerenciado evita a perda de dados e já traz backup (RNF-11).

## Grupo familiar (futuro)

- O grupo vê apenas **resumos por categoria**, nunca lançamentos individuais.
- A configuração é uma única chave por usuário: `compartilha_resumo` (sim/não). Quando ativa, todas as categorias são compartilhadas.
- Os resumos são calculados sob demanda, sem cópia de dados. Ao desativar o compartilhamento, os resumos deixam de aparecer imediatamente.
- Tabelas previstas: `grupo_familiar` e `grupo_membro`.
- A regra futura de acesso: o dono lê seus dados, ou o solicitante pertence ao mesmo grupo e o usuário dono tem `compartilha_resumo = true` (apenas para resumos).

## Modelo de dados inicial

- `usuario`: id, dados de login, `compartilha_resumo` (booleano).
- `lancamento`: id, `user_id`, valor, data, categoria, descrição. Sempre privado.

Já decidido (2026-10-28): `lancamento` terá um campo `tipo` (`RECEITA` ou `DESPESA`). Motivo: o app precisa registrar receitas para calcular o balanço geral.

Este modelo é preliminar. Ele será revisado a partir de `docs/REQUISITOS.md`, e a versão final será registrada aqui.

## Convenções do projeto

Propostas em 2026-10-22 (sessão de setup). **Aprovadas pelo usuário em 2026-10-28.**

### Idioma

- Documentação, mensagens de commit e comentários: **português**.
- Identificadores de código (classes, métodos, variáveis): **inglês**, seguindo o padrão Java.
  Exceção: termos de domínio sem boa tradução podem manter o nome em português (ex.: `lancamento`), desde que consistentes.

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
- Hoje não há nenhuma variável de ambiente, então esses arquivos modelo só serão criados quando a primeira configuração existir (decidido em 2026-10-28).

### Documentação viva

- `docs/DECISOES.md` (este arquivo): decisões e motivos.
- `docs/PROGRESSO.md`: o que foi feito e o que fazer na próxima sessão. Atualizado ao fim de cada sessão.
- `docs/REQUISITOS.md`: requisitos funcionais e não funcionais, e os dados derivados deles.
- `AGENTS.md`: guia para agentes de IA que trabalharem no projeto.

## Próximos passos

0. Validar `docs/REQUISITOS.md` e fechar o modelo de dados.
1. Backend: modelos de usuário e lançamento, autenticação JWT e service de autorização.
2. Backend: endpoints de resumo por categoria, com a regra de compartilhamento.
3. Desktop: cliente JavaFX consumindo a API.

O detalhamento e o status ficam em `docs/PROGRESSO.md`.
