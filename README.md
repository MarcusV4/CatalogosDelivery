## Equipe

- Pedro Vinicius
- Marcus Vinicius
- Lucas Cassiano
- João Faria

Os serviços possuem três responsabilidades principais:

* **Restaurantes:** gerenciamento dos dados, status e horários de funcionamento dos restaurantes.
* **Cardápio:** gerenciamento dos itens, preços, categorias e disponibilidade.
* **Busca:** pesquisa e filtragem de restaurantes e itens do cardápio.

---

# Arquitetura

Cada responsabilidade é um **microsserviço independente** (Spring Boot), com porta própria e comunicação **somente via HTTP**. Não há JOIN entre bancos: cada serviço com dados tem o **seu próprio banco** (*database per service*).

| Serviço | Módulo | Porta | Banco | Consome via HTTP |
|---|---|---|---|---|
| Restaurantes | `restaurantes-service` | `8004` | `db-restaurantes` (Postgres, host `5433`) | — |
| Cardápio | `cardapio-service` | `8005` | `db-cardapio` (Postgres, host `5434`) | Restaurantes |
| Busca | `busca-service` | `8006` | — (sem banco) | Restaurantes e Cardápio |

O módulo `catalogo-common` é uma biblioteca compartilhada (não é um serviço). Ela contém o `GlobalExceptionHandler`, que faz todo erro dos 3 serviços sair no formato padrão, e o cliente HTTP entre serviços, que tem timeout, gera log de cada chamada e converte falhas em erros amigáveis.

> As portas seguem a sugestão `8000 + 3 × (equipe − 1) + n` (Equipe 2 → 8004–8006) e ainda dependem do alinhamento da turma (#20). Todas podem ser trocadas pelo `.env`.

## Como os serviços conversam

- **Cardápio → Restaurantes:** ao cadastrar um produto, o Cardápio valida o restaurante em `GET /api/restaurantes/{id}`. O cardápio do restaurante é criado automaticamente no primeiro produto.
- **Restaurantes:** `DELETE /api/restaurantes/{id}` é uma exclusão lógica, altera o status para `INDISPONIVEL` e não remove o cardápio do outro serviço.
- **Busca → Restaurantes e Cardápio:** a busca lê `GET /api/restaurantes` e `GET /api/produtos` e aplica os filtros.
- Toda chamada aparece no log do terminal, por exemplo:
  ```
  [Cardápio] -> GET http://restaurantes:8004/api/restaurantes/1 (consultando o serviço de Restaurantes)
  [Cardápio] <- 200 de Restaurantes em 12 ms
  ```

## Datas e horários

As APIs usam ISO 8601. Horários de funcionamento são enviados como `HH:mm:ss`
(por exemplo, `18:00:00`) e o dia da semana usa os valores do enum `DiaSemana`
(`DOMINGO` a `SABADO`). Instantes, como `excluidoEm`, usam UTC com `Z`
(por exemplo, `2026-08-24T14:30:00Z`). Não envie datas em formato regional.

Os horários de funcionamento representam a hora local de `America/Recife`.
Esse é o fuso definido para o futuro cálculo de "aberto agora"; instantes
gravados e serializados usam UTC.

## Resiliência

Se um serviço cair, quem depende dele **continua no ar** e responde com o formato de erro padrão:

| Situação | Resposta |
|---|---|
| Recurso não existe no outro serviço | `404 {"erro": true, "mensagem": "Restaurante 99 não encontrado."}` |
| Requisição inválida (ex.: cadastro sem `ownerUserId`) | `400 {"erro": true, "mensagem": "Informe ownerUserId ..."}` |
| Outro serviço fora do ar, em timeout (3 s) ou com erro 5xx | `503 {"erro": true, "mensagem": "O serviço de Restaurantes está indisponível no momento. ..."}` |
| Resposta inesperada do outro serviço | `502` |

A busca de itens continua funcionando sem o serviço de Restaurantes: os itens voltam apenas sem `restauranteNome`/`restauranteStatus`/`restauranteAbertoAgora`. Com o serviço no ar, itens de restaurantes que não existem mais são descartados. A exceção são os filtros que dependem do restaurante (`categoriaRestaurante`, `abertoAgora` e `entregaGratis` em `/api/busca/itens`): sem o serviço não há como aplicá-los, e a busca responde `503`.

## Executando

Pré-requisito: apenas Docker. Não é preciso instalar Java, Maven nem Postgres.

```bash
docker compose up -d --build      # sobe os 3 serviços, os 2 bancos, a seed e o front (http://localhost:3000)
docker compose logs -f cardapio busca   # acompanha as integrações
docker compose down               # para tudo (adicione -v para apagar os dados)
```

Nenhum `.env` é necessário. Para trocar portas ou credenciais, copie `.env.example` para `.env`.

### Dados de exemplo

No Compose local, o contêiner `seed` espera Restaurantes e Cardápio ficarem
saudáveis e cadastra, pelas APIs, dois restaurantes pensados para exercitar
os filtros da Busca:

| Restaurante | Cidade | Status | Categoria | Produtos |
|---|---|---|---|---|
| Pizzaria Exemplo | Recife | `ABERTO` | `PIZZARIA` | Pizza Margherita (R$ 25,90), Pizza Calabresa (R$ 28,90), Pizza Quatro Queijos (R$ 32,90, **indisponível**), Refrigerante Lata (R$ 6,00) |
| Açaí Exemplo | Olinda | `FECHADO` | `ACAI` | Açaí 500 ml (R$ 18,00), Água Mineral (R$ 3,50) |

As pizzas Margherita e Calabresa recebem **Queijo extra** (R$ 3,50) e
**Borda recheada** (R$ 6,00). O Açaí recebe **Leite em pó** (R$ 2,50)
e **Granola extra** (R$ 1,50, indisponível).

A seed termina após o cadastro e não duplica registros: restaurantes, produtos
e adicionais que já existem (mesmo nome no respectivo contexto) são mantidos;
só o que falta é cadastrado. Os dados ficam nos volumes dos bancos. Os dados
e o dono de exemplo (`ownerUserId` 1) estão em `scripts/seed.py`.

```bash
docker compose logs seed                  # confere o resultado da seed
curl http://localhost:8004/api/restaurantes
curl http://localhost:8005/api/produtos
curl "http://localhost:8006/api/busca/restaurantes?status=ABERTO"
curl "http://localhost:8006/api/busca/itens?nome=acai"          # ignora acentos
curl "http://localhost:8006/api/busca/itens?disponivel=false"
curl "http://localhost:8006/api/busca/itens?nome=pizza&precoMin=2000&precoMax=3000&sort=preco,desc"
curl "http://localhost:8006/api/busca/restaurantes?abertoAgora=true&page=0&size=10"
curl "http://localhost:8006/api/busca/restaurantes?entregaGratis=true"
curl "http://localhost:8006/api/busca/itens?nome=calabresa&abertoAgora=true&categoriaRestaurante=PIZZARIA"
```

Para desativar a seed, defina `SEED_ENABLED=false` no `.env`. Se algum serviço
apontar para um banco externo, ela é ignorada; `SEED_ALLOW_EXTERNAL=true` no
`.env` permite executar a seed nesse banco explicitamente. Para adicionar os
novos exemplos a volumes já existentes, execute `docker compose run --rm seed`.

### Swagger e especificação OpenAPI

Com `docker compose up -d --build`, cada serviço disponibiliza sua própria documentação:

| Serviço | Swagger UI | OpenAPI (JSON) |
|---|---|---|
| Restaurantes | <http://localhost:8004/swagger-ui.html> | <http://localhost:8004/v3/api-docs> |
| Cardápio | <http://localhost:8005/swagger-ui.html> | <http://localhost:8005/v3/api-docs> |
| Busca | <http://localhost:8006/swagger-ui.html> | <http://localhost:8006/v3/api-docs> |

No Swagger UI, abra uma rota e use **Try it out** para enviar uma requisição.
Para verificar a especificação diretamente, execute, por exemplo,
`curl http://localhost:8004/v3/api-docs`. Se alterar as portas no `.env`, use
as portas configuradas. O healthcheck de cada serviço está em
`http://localhost:{porta}/actuator/health`.

### Rodando um serviço pela IDE

Suba só os bancos com `docker compose up -d db-restaurantes db-cardapio` e rode a classe `*Application` do módulo desejado. Os valores padrão já apontam para `localhost`.

### Build e testes

```bash
./mvnw verify                          # todos os módulos
./mvnw -pl cardapio-service -am package  # um serviço (e o que ele usa)
```

### Relatórios de cobertura

O comando `./mvnw verify` também gera um relatório HTML do JaCoCo em cada módulo.
Abra o `index.html` do módulo que deseja consultar:

- `catalogo-common/target/site/jacoco/index.html`
- `restaurantes-service/target/site/jacoco/index.html`
- `cardapio-service/target/site/jacoco/index.html`
- `busca-service/target/site/jacoco/index.html`

O `verify` falha se algum módulo, ou qualquer classe de service, ficar abaixo de
75% de cobertura de linhas.

## Frontend

O front em `frontend/` é **complementar**: a avaliação continua sendo feita pelas
APIs (Postman/Swagger). Ele tem a base (rotas, tema, client HTTP e tratamento de
erro), a Home com a busca de restaurantes e pratos e a página de cada restaurante com o cardápio,
somente leitura. O login é apenas visual e depende da integração com a Identidade (#31).

**Stack:** Vite + React + TypeScript (estrito), React Router, TanStack Query para
dados e cache, `fetch` nativo, CSS puro com variáveis (cores, fontes e espaçamentos
do protótipo), Vitest, ESLint e Prettier. Requer Node 22.12 ou mais novo.

```
frontend/src/
├── assets/       # imagens, fontes e arquivos estáticos
├── components/   # componentes compartilhados (Layout, Navbar, Botao, EstadoMensagem, Carregando...)
├── context/      # estado global: notificações (toasts) e provedor do TanStack Query
├── hooks/        # hooks reutilizáveis (useNotificacao, useTituloPagina, useDebounce) e de consulta (useRestaurante, useBuscaRestaurantes...)
├── pages/        # uma pasta por rota: Home (/), Restaurante (/restaurante/:id), Login (/login) e NaoEncontrada (404)
├── services/     # http.ts (wrapper do fetch), consultas.ts (QueryClient) e um arquivo por API
├── styles/       # tema.css (variáveis) e global.css
└── utils/        # preço (centavos e digitado em reais), datas, rótulos, cardápio, horários, filtros da busca e paginação
```

### Variáveis de ambiente

| Variável | Padrão local | Uso |
|---|---|---|
| `VITE_API_RESTAURANTES_URL` | `http://localhost:8004` | Serviço de Restaurantes |
| `VITE_API_CARDAPIO_URL` | `http://localhost:8005` | Serviço de Cardápio |
| `VITE_API_BUSCA_URL` | `http://localhost:8006` | Serviço de Busca |
| `VITE_API_IDENTIDADE_URL` | vazio | Reservada para o login (#31); ainda não é usada |

> As `VITE_*` são **gravadas no JavaScript durante o build** e quem chama as APIs
> é o **navegador**. Por isso elas apontam para `localhost` com as portas
> publicadas, e não para os nomes internos do Docker (`restaurantes:8004`). Mudou
> uma URL? Gere o build de novo (`docker compose up -d --build frontend`). No
> Compose, os valores vêm das portas do `.env` (`RESTAURANTES_PORT` etc.).

### Rodando localmente

```bash
docker compose up -d restaurantes cardapio busca   # back-end (ou tudo, com a seed)
cd frontend
cp .env.example .env
npm install
npm run dev            # http://localhost:5173
```

| Comando (em `frontend/`) | O que faz |
|---|---|
| `npm run test` | Testes unitários (Vitest) dos utilitários, dos serviços, do client HTTP e do QueryClient |
| `npm run lint` | ESLint |
| `npm run format` / `npm run format:check` | Prettier |
| `npm run build` | Checagem de tipos e build de produção em `dist/` |

### Com Docker Compose

`docker compose up -d --build` também gera a imagem do front (Node para o build e
nginx para servir os arquivos, com fallback para `index.html` nas rotas do React).
O contêiner `frontend` só sobe depois que Restaurantes, Cardápio e Busca ficam
saudáveis e responde em <http://localhost:3000>. A porta pode ser trocada com
`FRONTEND_PORT` no `.env`.

### Páginas

| Rota | Página | Dados |
|---|---|---|
| `/` | Home: busca de restaurantes e de pratos (somente leitura) | `GET /api/busca/restaurantes` e `GET /api/busca/itens` |
| `/restaurante/:id` | Restaurante e cardápio (somente leitura) | `GET /api/restaurantes/{id}`, `GET /api/restaurantes/{id}/horarios` e `GET /api/restaurantes/{id}/cardapio` |
| `/login` | Login, apenas visual (#31) | — |
| qualquer outra | Página não encontrada | — |

**Tela inicial (busca).** Usa só o serviço de Busca, numa consulta por vez: a aba
**Restaurantes** chama `/api/busca/restaurantes` e a aba **Pratos**, `/api/busca/itens`.
Cada cartão (restaurante ou prato) abre a página do restaurante.

| Na tela | Na URL do front | `/api/busca/restaurantes` | `/api/busca/itens` |
|---|---|---|---|
| Campo de busca (nome) | `nome` | `nome` | `nome` |
| Chip de categoria do restaurante | `categoria` | `categoria` | `categoriaRestaurante` |
| "Abertos agora" | `abertoAgora=true` | `abertoAgora=true` | `abertoAgora=true` |
| Preço "De" / "Até" (em reais), só na aba Pratos | `precoMin` / `precoMax` (centavos) | — | `precoMin` / `precoMax` |
| Aba Pratos | `tipo=pratos` | — | — |
| Página | `pagina` (começa em 1) | `page = pagina − 1`, `size=12` | igual |

- **Filtros na URL:** o link pode ser compartilhado e o "voltar" do navegador funciona.
  Valores inválidos na URL são ignorados, para não gerar `400`. "Abertos agora" desligado
  omite o parâmetro (a API trataria `false` como "só os fechados").
- **Busca por nome:** aplicada 400 ms depois da última tecla (ou na hora, com Enter), sem
  criar uma entrada no histórico por tecla. Ignora acentos e maiúsculas, como a API.
- **Preço:** digitado em reais (`25`, `25,90` ou `25.90`) e convertido para centavos inteiros
  sem ponto flutuante. Limites incluídos. Valor inválido ou "De" maior que "Até" mostra a
  mensagem no campo e não chama a API. Ao voltar para a aba Restaurantes, a faixa é
  limpa; o termo, a categoria e "Abertos agora" continuam.
- **Paginação:** 12 por página, com "Anterior" e "Próxima". A página anterior continua na
  tela (esmaecida) enquanto a nova carrega, e o foco vai para o título dos resultados.
  Uma página além da última (por exemplo, num link antigo) leva para a última.
- **Estados:** carregando; lista vazia com "Limpar filtros"; Busca fora do ar, ou `503`
  por Restaurantes/Cardápio fora do ar, mostram "A busca está indisponível no momento."
  com "Tentar de novo", sem detalhes técnicos. Se a Busca devolver pratos sem
  `restauranteNome` (Restaurantes fora do ar e nenhum filtro de restaurante), os cartões
  aparecem sem o restaurante e com um aviso. Pratos indisponíveis aparecem esmaecidos.
- **Diferenças para o protótipo:** o texto não encontra restaurantes pelos pratos nem pela
  categoria (a API procura no nome e na descrição); não há ordenação por nota nem
  "abertos primeiro" (ordem da API: nome); restaurantes sem cardápio também aparecem.

**Página do restaurante.** As três consultas correm em paralelo; só a do restaurante
bloqueia a tela.

- **Cabeçalho:** nome, categoria, tempo estimado, taxa de entrega e o selo
  "Aberto agora" / "Fechado agora" / "Indisponível", que vem de `abertoAgora` (calculado
  pelo back). Fora do horário ou com status `INDISPONIVEL` aparece um aviso, mas o
  cardápio continua visível (o restaurante desativado segue acessível pelo id).
- **Informações:** descrição, endereço, telefone e horários. O resumo mostra o dia de
  hoje em `America/Recife` ("Hoje, 18:00 às 23:00" ou "Fechado hoje") e abre a semana
  de domingo a sábado; dias sem horário aparecem como "Fechado".
- **Cardápio:** itens agrupados por categoria numa ordem fixa de exibição (entradas,
  salgados, lanches, pratos principais, pizzas, orientais, vegetarianos, sobremesas e
  bebidas, em `utils/cardapio.ts`); categorias desconhecidas vão para o fim, em ordem
  alfabética. Dentro da categoria, a ordem é a de cadastro (`id`). Preços via
  `formatarPreco`. Itens indisponíveis aparecem esmaecidos, com `aria-disabled` e o
  texto "Indisponível no momento". Sem imagem (ou se ela falhar), aparece a inicial do nome.
  Adicionais e o botão de adicionar à sacola ficam para a issue da sacola.
- **Estados:** carregando; id inválido (ex.: `/restaurante/abc`, sem chamar o back) ou
  404 de Restaurantes mostram "Restaurante não encontrado"; falha de Restaurantes mostra
  erro com "Tentar de novo"; falha do Cardápio ou dos horários afeta só a respectiva
  parte. O Cardápio responde `[]` (nunca 404) para restaurante sem itens, e a tela mostra
  "Este restaurante ainda não cadastrou o cardápio."

A seed não cadastra horários; para ver a semana preenchida, use
`PUT /api/restaurantes/{id}/horarios` (veja o Swagger de Restaurantes).

### Erros e convenções

- Todo acesso às APIs passa por `services/http.ts`, que converte o erro padrão
  `{"erro": true, "mensagem": "..."}`, respostas 5xx, falhas de rede e timeout
  (10 s) em `ApiError` (`status` e `message` em português; status `0` sem resposta).
- O `QueryClient` mostra esses erros num toast automaticamente. Uma tela que trata
  o erro por conta própria passa `meta: { silenciarErro: true }` na consulta.
  Erros 4xx não são repetidos; falhas de rede e 5xx são tentadas mais duas vezes.
- Preços chegam em centavos: use `formatarPreco(2590)` → `R$ 25,90`. Datas e horas
  usam `formatarDataHora` / `formatarHorario` (fuso `America/Recife`).

### CORS

Os três serviços aceitam chamadas do navegador vindas das origens em
`catalogo.cors.origens-permitidas` (variável `CATALOGO_CORS_ORIGENS_PERMITIDAS`,
lista separada por vírgula). O padrão é `http://localhost:5173,http://localhost:3000`.
A configuração fica no `catalogo-common` e vale para as rotas `/api/**`, com os
métodos `GET`, `POST`, `PUT`, `PATCH` e `DELETE`, sem credenciais (cookies).

## Endpoints

### Valores monetários

Todos os valores monetários da API são inteiros em centavos: `2590` representa
R$ 25,90. Os campos `preco` (produto, busca e validação para Checkout) e
`taxaEntrega` (restaurante) mantêm os nomes atuais, mas agora recebem e retornam
inteiros não negativos. Requisições com preço decimal ou negativo são recusadas.
`taxaEntrega` representa somente uma taxa fixa informada pelo restaurante;
o cálculo de frete pertence ao serviço de Despacho. A permanência dessa taxa
no contrato do Catálogo ainda depende do alinhamento com a Equipe 4 e da issue #20.

**Bancos que já existiam antes dessa mudança:**

- **Cardápio:** nada a fazer. A migração Flyway
  `V2__preco_produto_em_centavos.sql` converte `produto.preco` de reais para
  centavos automaticamente ao subir o serviço.
- **Restaurantes:** o serviço ainda não usa Flyway, então converta
  `taxa_entrega` uma vez, **antes** de subir a nova versão (com as credenciais
  padrão do Compose; se mudou no `.env`, ajuste o usuário):

  ```bash
  docker compose stop restaurantes
  docker compose exec -T db-restaurantes psql -v ON_ERROR_STOP=1 -U catalogo -d restaurantes < docs/migrations/issue-23-restaurantes-centavos.sql
  docker compose up -d --build
  ```

Em bancos novos não é preciso fazer nada. Se você já subiu a versão em centavos
sobre um banco antigo sem converter, os valores ficaram misturados (reais e
centavos) e não há como separá-los: recrie os bancos locais com
`docker compose down -v`.

| Serviço | Método e rota | Descrição |
|---|---|---|
| Restaurantes | `POST/GET /api/restaurantes`, `GET/PUT/DELETE /api/restaurantes/{id}` | Cadastro, consulta, atualização e desativação lógica (`DELETE` retorna 204) |
| Restaurantes | `PATCH /api/restaurantes/{id}/status` | Altera o status do restaurante |
| Restaurantes | `GET/PUT /api/restaurantes/{id}/horarios` | Consulta ou substitui os horários da semana; dias omitidos ficam fechados |
| Cardápio | `GET /api/produtos` | Todos os itens (usado pela Busca) |
| Cardápio | `DELETE /api/cardapios/restaurante/{restauranteId}` | Exclui logicamente o cardápio de um restaurante (rota administrativa; o Restaurantes não a chama) |
| Cardápio | `GET/POST /api/restaurantes/{restauranteId}/cardapio` | Consulta ou cadastra itens do cardápio |
| Cardápio | `GET/PUT/PATCH/DELETE /api/produtos/{id}` | Consulta, altera ou exclui um produto |
| Cardápio | `PATCH /api/produtos/{id}/disponibilidade` | Altera a disponibilidade de um produto |
| Cardápio | `GET /api/categorias` | Enum de categorias de produtos |
| Cardápio | `GET/POST /api/produtos/{produtoId}/adicionais` | Lista ou cria extras de um produto |
| Cardápio | `PUT/PATCH/DELETE /api/produtos/{produtoId}/adicionais/{adicionalId}` | Altera ou exclui um extra |
| Cardápio | `PATCH /api/produtos/{produtoId}/adicionais/{adicionalId}/disponibilidade` | Altera a disponibilidade de um extra |
| Cardápio | `POST /api/produtos/{restauranteId}` | Rota antiga de cadastro, obsoleta; use `POST /api/restaurantes/{id}/cardapio` |
| Cardápio | `POST /api/produtos/validacao` | Valida existência, disponibilidade, preço e restaurante dos itens para Checkout |
| Busca | `GET /api/busca/restaurantes?nome=&categoria=&status=&abertoAgora=&entregaGratis=&page=&size=&sort=` | Busca de restaurantes |
| Busca | `GET /api/busca/itens?nome=&categoria=&precoMin=&precoMax=&disponivel=&restauranteId=&categoriaRestaurante=&abertoAgora=&entregaGratis=&page=&size=&sort=` | Busca de pratos ("Buscar Prato") |

As respostas de restaurantes incluem endereço, telefone, taxa em centavos, tempo estimado, `ownerUserId` e `abertoAgora`. Este último só é `true` quando o status é `ABERTO` e o horário local de `America/Recife` está dentro do intervalo cadastrado. Os intervalos não podem cruzar a meia-noite.

No cadastro (`POST`) e na atualização (`PUT`) de restaurante, só `complemento` e `urlImagem` são opcionais. `estado` é a sigla de 2 letras (gravada em maiúsculas), `cep` segue `00000-000` ou `00000000`, `restauranteTelefone` tem 10 ou 11 dígitos sem máscara (`81999990000`), `taxaEntrega` é `>= 0` e os tempos estimados são `> 0`. O cadastro não recebe `statusRestaurante`: todo restaurante nasce `ABERTO`, e outro status é definido pelo `PATCH .../status`. Em `PUT .../horarios`, cada item exige `diaSemana`, `horaAbertura` e `horaFechamento`. Campos inválidos retornam 400 com a lista `campo: mensagem` em `mensagem`.

Para atualização, status, horários e exclusão lógica, o contrato temporário recebe `ownerUserId` e compara com o proprietário cadastrado. Atualização, status e horários o recebem no JSON; `DELETE` recebe `?ownerUserId=...`. A identidade autenticada ainda depende da integração JWT da #31; esse valor informado pelo cliente não deve ser tratado como prova de identidade em produção.

Nas rotas por id (`/api/restaurantes/{id}`, `/{id}/status` e `/{id}/horarios`), um restaurante
inexistente responde `404 {"erro": true, "mensagem": "Restaurante {id} não encontrado."}` antes
de qualquer outra verificação: corpo inválido, `ownerUserId` ausente ou de outro proprietário
não viram `400` nem `403`. Um id não numérico (ex.: `/api/restaurantes/abc`) responde `400`.

A busca ignora maiúsculas/minúsculas e acentos (`acai` encontra `Açaí`).
O parâmetro `nome` procura no nome **e na descrição** (do prato ou do
restaurante). Todos os filtros são opcionais e combináveis; `precoMin`/`precoMax`
são em centavos e inclusivos. `entregaGratis=true` traz só restaurantes com
`taxaEntrega` igual a `0` (`false`, só os com taxa). Em `/api/busca/itens`,
`categoriaRestaurante`, `abertoAgora` e `entregaGratis` filtram pelo restaurante
do item, e `categoria` continua sendo a categoria do produto. Cada item volta com o restaurante (`restauranteId`,
`restauranteNome`, `restauranteStatus`, `restauranteAbertoAgora`), o preço e a
disponibilidade.

As duas rotas são paginadas e ordenadas:

- `page` (começa em 0, padrão `0`) e `size` (1 a 100, padrão `20`);
- `sort=campo` ou `sort=campo,asc|desc` (padrão `nome,asc`). Campos de
  restaurantes: `nome`, `categoria`, `taxaEntrega`, `tempoEstimado`; de itens:
  `nome`, `preco`, `categoria`. Valores nulos ficam sempre no fim.

A resposta é uma página:

```json
{"conteudo": [ ... ], "pagina": 0, "tamanho": 20, "totalElementos": 42, "totalPaginas": 3}
```

Paginação, faixa de preço ou ordenação inválidas retornam `400` sem consultar
os outros serviços.

`GET /api/produtos/{id}` agora consulta um **produto**. A antiga listagem
`GET /api/produtos/{restauranteId}` foi removida, pois teria o mesmo caminho;
use `GET /api/restaurantes/{restauranteId}/cardapio`. Busca continua usando
`GET /api/produtos`, e a seed usa a nova rota do cardápio.

O produto retornado inclui `restauranteId`, `urlImagem` e `adicionais`.
Cada adicional tem `id`, `nome`, `preco` em centavos e `disponivel`;
seu restaurante é determinado pelo produto, sem campo duplicado. Nomes iguais
são permitidos no mesmo produto. Ao excluir o produto, os adicionais são
excluídos. Cardápios logicamente excluídos não podem receber escritas e seus
produtos e adicionais não aparecem nas consultas (404 por ID). As categorias
são os valores fixos de `CategoriaProduto`; acrescentar uma categoria exige
alterar o enum e publicar uma nova versão.

Restaurante desativado (`INDISPONIVEL`, exclusão lógica do serviço de
Restaurantes) não recebe novos produtos: o cadastro responde `409`. Ele também
sai de `GET /api/restaurantes` e, por consequência, da Busca, junto com seus
itens. O cardápio é preservado, então o restaurante pode ser reativado com
`PATCH /api/restaurantes/{id}/status`.

**PENDENTE (#31): as escritas do Cardápio estão sem controle de propriedade.**
Não há autenticação neste repositório, então qualquer chamador pode criar,
alterar ou excluir produtos e adicionais. Isso afeta:

- `POST /api/restaurantes/{id}/cardapio` e o antigo `POST /api/produtos/{restauranteId}`;
- `PUT/PATCH/DELETE /api/produtos/{id}` e `PATCH /api/produtos/{id}/disponibilidade`;
- `POST /api/produtos/{produtoId}/adicionais`;
- `PUT/PATCH/DELETE /api/produtos/{produtoId}/adicionais/{adicionalId}`;
- `PATCH /api/produtos/{produtoId}/adicionais/{adicionalId}/disponibilidade`.
Todas essas operações chamam `AutorizacaoRestauranteService.validarPermissao`,
atualmente vazio. A #31 deverá implementar ali a checagem do usuário
autenticado contra o dono mantido em Restaurantes.

Exemplos para validar o fluxo local:

```bash
curl http://localhost:8005/api/categorias
curl http://localhost:8005/api/restaurantes/1/cardapio
curl -X POST http://localhost:8005/api/restaurantes/1/cardapio \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Pizza exemplo","descricao":"Massa fina","preco":2590,"disponivel":true,"categoria":"PIZZA"}'
curl http://localhost:8005/api/produtos/7
curl -X PUT http://localhost:8005/api/produtos/7 \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Pizza exemplo","descricao":"Massa fina","preco":2790,"disponivel":true,"categoria":"PIZZA"}'
curl -X PATCH http://localhost:8005/api/produtos/7/disponibilidade \
  -H 'Content-Type: application/json' -d '{"disponivel":false}'
curl -X POST http://localhost:8005/api/produtos/7/adicionais \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Queijo extra","preco":350,"disponivel":true}'
curl http://localhost:8005/api/produtos/7/adicionais
curl -X PATCH http://localhost:8005/api/produtos/7/adicionais/3/disponibilidade \
  -H 'Content-Type: application/json' -d '{"disponivel":false}'
curl -X DELETE http://localhost:8005/api/produtos/7/adicionais/3
```

Substitua os IDs `7` e `3` pelos IDs retornados no seu banco.

---

# Requisitos Funcionais

| **ID** | **Requisito**                              | **Descrição**                                                                                                                                                            |
| ------ | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| RF01   | Cadastro de restaurantes                   | O sistema deve permitir cadastrar restaurantes com suas informações básicas, como nome, endereço, telefone e descrição, associando o restaurante ao usuário responsável. |
| RF02   | Consulta de restaurantes                   | O sistema deve permitir consultar as informações de um restaurante.                                                                                                      |
| RF03   | Atualização de restaurantes                | O sistema deve permitir ao proprietário alterar as informações cadastradas de seu restaurante.                                                                           |
| RF04   | Gerenciamento do status do restaurante     | O sistema deve permitir alterar e consultar o status de funcionamento de um restaurante.                                                                                 |
| RF05   | Gerenciamento de horários                  | O sistema deve permitir cadastrar, consultar e atualizar os horários de funcionamento de um restaurante.                                                                 |
| RF06   | Cadastro de itens do cardápio              | O sistema deve permitir cadastrar itens vinculados a um restaurante, contendo informações como nome, descrição, preço e disponibilidade.                                 |
| RF07   | Consulta de itens do cardápio              | O sistema deve permitir consultar e listar os itens disponíveis no cardápio de um restaurante.                                                                           |
| RF08   | Atualização de itens do cardápio           | O sistema deve permitir ao proprietário alterar as informações de um item do cardápio de seu restaurante.                                                                |
| RF09   | Gerenciamento da disponibilidade dos itens | O sistema deve permitir alterar e consultar a disponibilidade de um item do cardápio.                                                                                    |
| RF10   | Gerenciamento de categorias                | O sistema deve permitir cadastrar, consultar e associar categorias aos itens do cardápio.                                                                                |
| RF11   | Busca de restaurantes                      | O sistema deve permitir realizar buscas por restaurantes utilizando critérios de pesquisa.                                                                               |
| RF12   | Busca de itens                             | O sistema deve permitir realizar buscas por itens do cardápio.                                                                                                           |
| RF13   | Filtragem do catálogo                      | O sistema deve permitir filtrar restaurantes e itens utilizando critérios como categoria, status, disponibilidade e faixa de preço.                                      |
| RF14   | Consulta de cardápio por restaurante       | O sistema deve permitir consultar o cardápio completo associado a determinado restaurante.                                                                               |
| RF15   | Disponibilização dos dados do catálogo     | O sistema deve disponibilizar os dados de restaurantes, cardápios e itens por meio de APIs para os demais serviços da plataforma.                                        |
| RF16   | Validação de propriedade                   | O sistema deve verificar se o usuário autenticado é proprietário do restaurante antes de permitir operações de gerenciamento sobre o restaurante ou seu cardápio.        |
| RF17   | Validação de itens para Checkout           | O sistema deve permitir que o Checkout consulte informações atualizadas de itens, incluindo preço e disponibilidade.                                                     |
| RF18   | Validação de restaurante para Checkout     | O sistema deve permitir que o Checkout consulte o status e os horários de funcionamento de um restaurante.                                                               |

---

# Requisitos Não Funcionais

| **ID** | **Requisito**         | **Descrição**                                                                                                                     |
| ------ | --------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| RNF01  | Desempenho            | O sistema deve responder às requisições da API em tempo adequado, considerando as operações normalmente realizadas no catálogo.   |
| RNF02  | Disponibilidade       | O serviço deve permanecer disponível durante o funcionamento da plataforma, salvo situações de manutenção ou falhas externas.     |
| RNF03  | Segurança             | O acesso às operações de gerenciamento deve ser protegido por mecanismos de autenticação e autorização.                           |
| RNF04  | Usabilidade           | As APIs devem possuir respostas claras e padronizadas, facilitando seu entendimento e utilização.                                 |
| RNF05  | Manutenibilidade      | O código deve ser organizado de forma que alterações e correções possam ser realizadas sem comprometer as demais funcionalidades. |
| RNF06  | Integridade dos dados | O sistema deve garantir que os dados armazenados sejam válidos e consistentes.                                                    |

---

# Requisitos de Integração

| **ID** | **Requisito**                   | **Descrição**                                                                                                                                                  |
| ------ | ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| RI01   | Padronização dos endpoints      | Os endpoints devem seguir um padrão de nomenclatura e estrutura definido pela equipe.                                                                          |
| RI02   | Padronização dos erros          | As APIs devem retornar erros utilizando o formato e os códigos HTTP definidos pela plataforma.                                                                 |
| RI03   | Comunicação via API             | O sistema deve disponibilizar APIs HTTP/REST para comunicação com os demais microsserviços.                                                                    |
| RI04   | Documentação da API             | Os endpoints devem ser documentados utilizando Swagger/OpenAPI.                                                                                                |
| RI05   | Tratamento de indisponibilidade | O serviço deve tratar falhas de comunicação com outros serviços e retornar respostas adequadas.                                                                |
| RI06   | Integração com Identidade       | O serviço deve utilizar as informações de identidade disponibilizadas pelo mecanismo de autenticação para identificar o usuário responsável pelas requisições. |
| RI07   | Integração com Checkout         | O serviço deve fornecer ao Checkout informações atualizadas sobre restaurantes, itens, preços e disponibilidade.                                               |

---

# Fronteiras do Microsserviço

O Catálogo é responsável exclusivamente pelas informações pertencentes ao domínio de restaurantes e cardápios.

Informações relacionadas à autenticação e às permissões gerais dos usuários pertencem ao serviço de **Identidade**, enquanto carrinho, subtotal, promoções e pedidos pertencem ao serviço de **Checkout**.

O Catálogo mantém apenas a referência necessária para relacionar um restaurante ao seu proprietário, utilizando o identificador do usuário fornecido pelo mecanismo de autenticação.

## Entradas e Saídas

| **Entra no Catálogo**                   | **Vem de**            | **Uso no Catálogo**                                                          | **Sai do Catálogo**                       | **Vai para**     |
| --------------------------------------- | --------------------- | ---------------------------------------------------------------------------- | ----------------------------------------- | ---------------- |
| JWT (`sub`, `role`)                     | Identidade            | Identificar o usuário e verificar suas permissões gerais                     | Autorização da operação ou erro de acesso | Cliente/Frontend |
| Dados para criação de restaurante       | Cliente/Frontend      | Receber `ownerUserId` no DTO nesta etapa; a #31 irá obtê-lo do JWT validado  | Restaurante criado                        | Cliente/Frontend |
| Dados para atualização, horário e status | Proprietário/Frontend | Comparar `ownerUserId` informado com o cadastrado; validar JWT na #31        | Restaurante/horário/status atualizado    | Cliente/Frontend |
| Dados de itens do cardápio              | Proprietário/Frontend | Criar ou atualizar itens pertencentes ao restaurante                         | Item criado/atualizado                    | Cliente/Frontend |
| Solicitação de listagem de restaurantes | Cliente/Frontend      | Buscar restaurantes disponíveis                                              | Lista de restaurantes                     | Cliente/Frontend |
| Termos e filtros de busca               | Cliente/Frontend      | Filtrar por nome, categoria, status, disponibilidade ou preço                | Resultados filtrados                      | Cliente/Frontend |
| Solicitação de cardápio                 | Cliente/Frontend      | Consultar o cardápio de determinado restaurante                              | Itens, preços e disponibilidade           | Cliente/Frontend |
| IDs dos itens selecionados              | Checkout              | Consultar e validar informações atuais dos itens                             | Itens, preços e disponibilidade           | Checkout         |
| ID do restaurante                       | Checkout              | Consultar status e horário de funcionamento                                  | Status e informações de funcionamento     | Checkout         |

---

# Propriedade dos Restaurantes

O serviço de Identidade é responsável por identificar o usuário, enquanto o Catálogo é responsável por determinar quais restaurantes pertencem a esse usuário.

O Catálogo armazena somente o identificador do proprietário em `ownerUserId`, sem copiar nome, e-mail ou CPF do serviço de Identidade. Nesta etapa, o DTO de criação recebe `ownerUserId`; a extração desse valor a partir do `sub` do JWT e a verificação de propriedade ficam para a integração de Identidade (#31), usando o contrato acordado na #20. Até essa integração, o valor do DTO ainda não prova a identidade do usuário nem deve ser usado para autorizar operações.

Exemplo de informações obtidas a partir do JWT:

```json
{
  "sub": 57,
  "role": "USER"
}
```

Exemplo simplificado de restaurante armazenado pelo Catálogo:

```json
{
  "id": 10,
  "name": "Pizzaria do João",
  "ownerUserId": 57,
  "status": "OPEN"
}
```

Quando a integração da #31 estiver concluída, o Catálogo comparará a identidade validada do usuário com o proprietário do restaurante:

```text
JWT.sub == Restaurant.ownerUserId
```

Caso os identificadores sejam iguais, o usuário pode realizar as operações permitidas sobre aquele restaurante. Caso contrário, a operação deve ser rejeitada.

Dessa forma, o serviço de Identidade não precisa conhecer os restaurantes existentes e o Catálogo não precisa armazenar os dados completos dos usuários.

---

# Integração com Checkout

O Catálogo funciona como a **fonte de verdade** para informações relacionadas aos restaurantes e seus cardápios.

Quando um cliente seleciona itens para realizar uma compra, o Checkout pode validar os itens selecionados no Cardápio:

```json
{
  "restauranteId": 10,
  "itens": [
    {
      "itemId": 101,
      "quantidade": 2
    },
    {
      "itemId": 102,
      "quantidade": 1
    }
  ]
}
```

`POST /api/produtos/validacao` retorna por item `existe`, `disponivel`, `preco` em centavos, `restauranteId` e `nome`. Um item inexistente retorna `existe: false`; itens pertencentes a outro restaurante são rejeitados. O formato final deve seguir o contrato combinado com Checkout na #20/#30.

O Catálogo é responsável por fornecer informações atualizadas sobre esses itens, como:

* existência do item;
* preço atual;
* disponibilidade;
* restaurante ao qual pertence;
* status e horário de funcionamento do restaurante, consultados pelo endpoint próprio de Restaurantes.

O cálculo do subtotal, aplicação de promoções e criação do pedido permanecem sob responsabilidade do **Microsserviço de Checkout**.

## Fluxo Simplificado

<p align="center">
  <img src="docs/fluxo-catalogo.png" alt="Fluxo de integração do Microsserviço de Catálogo" width="900">
</p>

---

# Fora do Escopo

Não são responsabilidades do Microsserviço de Catálogo:

* autenticação de usuários;
* armazenamento de credenciais;
* gerenciamento global de usuários e permissões;
* gerenciamento do carrinho;
* cálculo do subtotal do pedido;
* aplicação de cupons e promoções;
* criação e gerenciamento de pedidos;
* processamento de pagamentos;
* cálculo de frete;
* gerenciamento de entregadores;
* rastreamento de entregas;
* notificações;
* avaliações.

Essas funcionalidades pertencem aos demais microsserviços da plataforma.
