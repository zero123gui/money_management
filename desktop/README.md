# desktop

Cliente desktop do Controle Financeiro Familiar.

- **Stack**: Java 21, JavaFX 21 (LTS), Maven.
- **Sistemas**: Windows no MVP; macOS no futuro. Exige internet.
- **Responsabilidade**: interface com o usuário. Consome a API do `backend` e exibe apenas o que ela devolve; não acessa o banco diretamente.

**Status:** login, cadastro e logout prontos. As demais telas estão listadas em [`../docs/PROGRESSO.md`](../docs/PROGRESSO.md).

## Como rodar

Com a API no ar (veja [`../backend/README.md`](../backend/README.md)), dentro de `desktop/`:

```bash
mvn javafx:run
```

O endereço da API vem da variável de ambiente `API_BASE_URL`, com `http://localhost:8080` como padrão. Fora da própria máquina, o app só aceita HTTPS.

## Testes

```bash
mvn verify
```

Não precisam da API nem do Docker: o cliente HTTP é testado contra um servidor local falso, e `ViewsLoadTest` carrega cada tela, o que exige um ambiente com tela. As telas em si são conferidas à mão.

## Organização

Pacote base `br.com.financeiro.desktop`:

- `api`: chamadas HTTP, DTOs e erros da API;
- `session`: token do usuário logado, só na memória;
- `controller`: um controller por tela, a troca de telas (`Navigator`) e as chamadas em segundo plano (`ApiTasks`).

Os arquivos FXML ficam em `src/main/resources/br/com/financeiro/desktop/view`.

Decisões em [`../docs/DECISOES.md`](../docs/DECISOES.md) e requisitos em [`../docs/REQUISITOS.md`](../docs/REQUISITOS.md).
