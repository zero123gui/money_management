\# Controle Financeiro Familiar: Decisões de Arquitetura



\## Contexto

Aplicação desktop de controle financeiro para a família, usada em vários dispositivos. Cada usuário tem login próprio e dados privados. No futuro, haverá um grupo familiar que compartilha resumos.



\## Perfil do desenvolvedor

Experiência com Java. Pouca familiaridade com Python, Electron e Tauri.



\## Ferramentas

\- \*\*Claude Code\*\*: desenvolvimento do projeto dentro do repositório.

\- \*\*Cowork\*\*: apoio opcional, para documentos e especificações.

\- \*\*Repositório\*\*: monorepo no GitHub, com as pastas `backend/` e `desktop/`.



\## Stack

\- \*\*Backend\*\*: Spring Boot, API REST, Spring Security com autenticação JWT, PostgreSQL (SQLite para começar).

\- \*\*Desktop\*\*: JavaFX, consumindo a API.



\## Modelo de acesso

\- Cada lançamento pertence a um usuário (`user\_id`). Por padrão, só o dono pode ler.

\- O isolamento é feito no backend. Toda consulta filtra pelo usuário autenticado.

\- O cliente não decide o que exibir. A API devolve apenas o que o usuário pode ver.

\- A regra de autorização fica em um service centralizado, para que mudanças futuras fiquem localizadas.



\## Grupo familiar (futuro)

\- O grupo vê apenas \*\*resumos por categoria\*\*, nunca lançamentos individuais.

\- A configuração é uma única chave por usuário: `compartilha\_resumo` (sim/não). Quando ativa, todas as categorias são compartilhadas.

\- Os resumos são calculados sob demanda, sem cópia de dados. Ao desativar o compartilhamento, os resumos deixam de aparecer imediatamente.

\- Tabelas previstas: `grupo\_familiar` e `grupo\_membro`.

\- A regra futura de acesso: o dono lê seus dados, ou o solicitante pertence ao mesmo grupo e o usuário dono tem `compartilha\_resumo = true` (apenas para resumos).



\## Modelo de dados inicial

\- `usuario`: id, dados de login, `compartilha\_resumo` (booleano).

\- `lancamento`: id, `user\_id`, valor, data, categoria, descrição. Sempre privado.



\## Próximos passos

1\. Backend: modelos de usuário e lançamento, autenticação JWT e service de autorização.

2\. Backend: endpoints de resumo por categoria, com a regra de compartilhamento.

3\. Desktop: cliente JavaFX consumindo a API.

