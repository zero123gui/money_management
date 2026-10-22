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

## Modelo de acesso

- Cada lançamento pertence a um usuário (`user_id`). Por padrão, só o dono pode ler.
- O isolamento é feito no backend. Toda consulta filtra pelo usuário autenticado.
- O cliente não decide o que exibir. A API devolve apenas o que o usuário pode ver.
- A regra de autorização fica em um service centralizado, para que mudanças futuras fiquem localizadas.

## Grupo familiar (futuro)

- O grupo vê apenas **resumos por categoria**, nunca lançamentos individuais.
- A configuração é uma única chave por usuário: `compartilha_resumo` (sim/não). Quando ativa, todas as categorias são compartilhadas.
- Os resumos são calculados sob demanda, sem cópia de dados. Ao desativar o compartilhamento, os resumos deixam de aparecer imediatamente.
- Tabelas previstas: `grupo_familiar` e `grupo_membro`.
- A regra futura de acesso: o dono lê seus dados, ou o solicitante pertence ao mesmo grupo e o usuário dono tem `compartilha_resumo = true` (apenas para resumos).

## Modelo de dados inicial

- `usuario`: id, dados de login, `compartilha_resumo` (booleano).
- `lancamento`: id, `user_id`, valor, data, categoria, descrição. Sempre privado.

## Convenções do projeto

Decisões tomadas em 2026-10-22 (sessão de setup). Estado: **proposta inicial, ajustável**.

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

### Documentação viva

- `docs/DECISOES.md` (este arquivo): decisões e motivos.
- `docs/PROGRESSO.md`: o que foi feito e o que fazer na próxima sessão. Atualizado ao fim de cada sessão.
- `AGENTS.md`: guia para agentes de IA que trabalharem no projeto.

## Próximos passos

1. Backend: modelos de usuário e lançamento, autenticação JWT e service de autorização.
2. Backend: endpoints de resumo por categoria, com a regra de compartilhamento.
3. Desktop: cliente JavaFX consumindo a API.

O detalhamento e o status ficam em `docs/PROGRESSO.md`.
