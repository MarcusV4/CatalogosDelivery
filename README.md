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
| Restaurantes | `restaurantes-service` | `8004` | `db-restaurantes` (Postgres, host `5433`) | Cardápio (só na exclusão) |
| Cardápio | `cardapio-service` | `8005` | `db-cardapio` (Postgres, host `5434`) | Restaurantes |
| Busca | `busca-service` | `8006` | — (sem banco) | Restaurantes e Cardápio |

O módulo `catalogo-common` é uma biblioteca compartilhada (não é um serviço). Ela contém o formato de erro padrão e o cliente HTTP entre serviços, que tem timeout, gera log de cada chamada e converte falhas em erros amigáveis.

> As portas seguem a sugestão `8000 + 3 × (equipe − 1) + n` (Equipe 2 → 8004–8006) e ainda dependem do alinhamento da turma (#20). Todas podem ser trocadas pelo `.env`.

## Como os serviços conversam

- **Cardápio → Restaurantes:** ao cadastrar um produto, o Cardápio valida o restaurante em `GET /api/restaurantes/{id}`. O cardápio do restaurante é criado automaticamente no primeiro produto.
- **Restaurantes → Cardápio:** ao excluir um restaurante, o Restaurantes primeiro pede ao Cardápio para excluir o cardápio (`DELETE /api/cardapios/restaurante/{id}`). Se o Cardápio estiver fora do ar, a exclusão é recusada com 503 e **nada é apagado**, mantendo os dois serviços consistentes. No Cardápio a exclusão é lógica (`excluido_em`): os itens do restaurante excluído deixam de aparecer nas listagens e na busca, e a validação para o Checkout passa a responder `existe: false` para eles.
- **Busca → Restaurantes e Cardápio:** a busca lê `GET /api/restaurantes` e `GET /api/produtos` e aplica os filtros.
- Toda chamada aparece no log do terminal, por exemplo:
  ```
  [Cardápio] -> GET http://restaurantes:8004/api/restaurantes/1 (consultando o serviço de Restaurantes)
  [Cardápio] <- 200 de Restaurantes em 12 ms
  ```

## Resiliência

Se um serviço cair, quem depende dele **continua no ar** e responde com o formato de erro padrão:

| Situação | Resposta |
|---|---|
| Recurso não existe no outro serviço | `404 {"erro": true, "mensagem": "Restaurante 99 não encontrado."}` |
| Outro serviço fora do ar, em timeout (3 s) ou com erro 5xx | `503 {"erro": true, "mensagem": "O serviço de Restaurantes está indisponível no momento. ..."}` |
| Resposta inesperada do outro serviço | `502` |

A busca de itens continua funcionando sem o serviço de Restaurantes: os itens voltam apenas sem `restauranteNome`/`restauranteStatus`. Com o serviço no ar, itens de restaurantes que não existem mais são descartados.

## Executando

Pré-requisito: apenas Docker. Não é preciso instalar Java, Maven nem Postgres.

```bash
docker compose up -d --build      # sobe os 3 serviços e os 2 bancos
docker compose logs -f cardapio busca   # acompanha as integrações
docker compose down               # para tudo (adicione -v para apagar os dados)
```

Nenhum `.env` é necessário. Para trocar portas ou credenciais, copie `.env.example` para `.env`.

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

## Endpoints

| Serviço | Método e rota | Descrição |
|---|---|---|
| Restaurantes | `POST/GET /api/restaurantes`, `GET/DELETE /api/restaurantes/{id}` | CRUD de restaurantes |
| Cardápio | `GET /api/produtos` | Todos os itens (usado pela Busca) |
| Cardápio | `DELETE /api/cardapios/restaurante/{restauranteId}` | Exclui o cardápio de um restaurante (usado pelo Restaurantes) |
| Cardápio | `GET/POST /api/produtos/{restauranteId}` | Itens do cardápio de um restaurante |
| Cardápio | `POST /api/produtos/validacao` | Valida existência, disponibilidade, preço e restaurante dos itens para Checkout |
| Busca | `GET /api/busca/restaurantes?nome=&categoria=&status=` | Busca de restaurantes |
| Busca | `GET /api/busca/itens?nome=&categoria=&restauranteId=&disponivel=` | Busca de pratos ("Buscar Prato") |

A busca ignora maiúsculas/minúsculas e acentos (`acai` encontra `Açaí`).

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
| Dados para atualização do restaurante   | Proprietário/Frontend | Verificar `JWT.sub == ownerUserId` após a integração da #31                  | Restaurante atualizado                    | Cliente/Frontend |
| Dados de horário e status               | Proprietário/Frontend | Gerenciar o funcionamento do restaurante                                     | Horário/status atualizado                 | Cliente/Frontend |
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
