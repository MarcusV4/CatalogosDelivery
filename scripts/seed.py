"""Dados de exemplo para o Compose local, cadastrados pelas APIs dos serviços.

Pode ser executada várias vezes: restaurantes, produtos e adicionais que já
existem (mesmo nome dentro do respectivo restaurante ou produto) são mantidos,
e só o que falta é cadastrado.
"""

import base64
import json
import os
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


RESTAURANTES_URL = os.environ["RESTAURANTES_URL"].rstrip("/")
CARDAPIO_URL = os.environ["CARDAPIO_URL"].rstrip("/")

LOCAL_DATABASES = {
    "RESTAURANTES_DB_URL": "jdbc:postgresql://db-restaurantes:5432/restaurantes",
    "CARDAPIO_DB_URL": "jdbc:postgresql://db-cardapio:5432/cardapio",
}

# Nomes dos campos da resposta de GET /api/restaurantes. Se a #20 renomear
# os campos (ex.: "nome" em vez de "restauranteNome"), ajuste só aqui.
CAMPO_ID = "restauranteId"
CAMPO_NOME = "restauranteNome"

# As rotas de escrita exigem "Authorization: Bearer <JWT>" e o dono do
# restaurante é o "sub" do token. No Compose os serviços sobem no perfil dev,
# em que o mock da Identidade aceita um JWT sem assinatura. Com a Identidade
# real (Equipe 1), informe um token válido de lojista em SEED_TOKEN.
OWNER_USER_ID = 1


def token_dev(sub, role):
    def b64(dados):
        texto = json.dumps(dados, separators=(",", ":")).encode("utf-8")
        return base64.urlsafe_b64encode(texto).rstrip(b"=").decode("ascii")

    return f"{b64({'alg': 'none', 'typ': 'JWT'})}.{b64({'sub': str(sub), 'role': role})}."


TOKEN = os.getenv("SEED_TOKEN") or token_dev(OWNER_USER_ID, "LOJISTA")

# Valores monetários em centavos (2590 = R$ 25,90).
RESTAURANTES = [
    {
        "dados": {
            "restauranteNome": "Pizzaria Exemplo",
            "restauranteDesc": "Pizzas tradicionais no forno a lenha",
            "logradouro": "Avenida Boa Viagem",
            "numero": "1000",
            "bairro": "Boa Viagem",
            "cidade": "Recife",
            "estado": "PE",
            "cep": "51011-000",
            "restauranteTelefone": "8133330001",
            "taxaEntrega": 500,
            "tempoEstimadoMin": 30,
            "tempoEstimadoMax": 45,
            "categoriaRestaurante": "PIZZARIA",
        },
        "status": "ABERTO",
        "produtos": [
            {
                "nome": "Pizza Margherita",
                "descricao": "Molho de tomate, muçarela e manjericão",
                "preco": 2590,
                "disponivel": True,
                "categoria": "PIZZA",
                "adicionais": [
                    {"nome": "Queijo extra", "preco": 350, "disponivel": True},
                    {"nome": "Borda recheada", "preco": 600, "disponivel": True},
                ],
            },
            {
                "nome": "Pizza Calabresa",
                "descricao": "Calabresa fatiada, cebola e muçarela",
                "preco": 2890,
                "disponivel": True,
                "categoria": "PIZZA",
                "adicionais": [
                    {"nome": "Queijo extra", "preco": 350, "disponivel": True},
                    {"nome": "Borda recheada", "preco": 600, "disponivel": True},
                ],
            },
            {
                "nome": "Pizza Quatro Queijos",
                "descricao": "Muçarela, provolone, parmesão e gorgonzola",
                "preco": 3290,
                "disponivel": False,
                "categoria": "PIZZA",
            },
            {
                "nome": "Refrigerante Lata",
                "descricao": "Lata de 350 ml",
                "preco": 600,
                "disponivel": True,
                "categoria": "BEBIDA",
            },
        ],
    },
    {
        "dados": {
            "restauranteNome": "Açaí Exemplo",
            "restauranteDesc": "Açaí na tigela com complementos",
            "logradouro": "Rua do Amparo",
            "numero": "50",
            "bairro": "Carmo",
            "cidade": "Olinda",
            "estado": "PE",
            "cep": "53020-190",
            "restauranteTelefone": "8133330002",
            "taxaEntrega": 300,
            "tempoEstimadoMin": 20,
            "tempoEstimadoMax": 30,
            "categoriaRestaurante": "ACAI",
        },
        "status": "FECHADO",
        "produtos": [
            {
                "nome": "Açaí 500 ml",
                "descricao": "Açaí com banana e granola",
                "preco": 1800,
                "disponivel": True,
                "categoria": "SOBREMESA",
                "adicionais": [
                    {"nome": "Leite em pó", "preco": 250, "disponivel": True},
                    {"nome": "Granola extra", "preco": 150, "disponivel": False},
                ],
            },
            {
                "nome": "Água Mineral",
                "descricao": "Garrafa de 500 ml",
                "preco": 350,
                "disponivel": True,
                "categoria": "BEBIDA",
            },
        ],
    },
]


def api_json(base_url, path, method="GET", data=None):
    body = json.dumps(data).encode("utf-8") if data is not None else None
    headers = {"Content-Type": "application/json"} if body else {}
    if method != "GET":
        headers["Authorization"] = f"Bearer {TOKEN}"
    request = Request(base_url + path, data=body, headers=headers, method=method)
    try:
        with urlopen(request, timeout=10) as response:
            return json.load(response)
    except HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {base_url}{path}: HTTP {error.code}: {detail}") from error
    except URLError as error:
        raise RuntimeError(f"{method} {base_url}{path}: {error.reason}") from error


def obter_ou_criar_restaurante(dados, status, existentes):
    nome = dados[CAMPO_NOME]
    restaurante = next((item for item in existentes if item.get(CAMPO_NOME) == nome), None)
    if restaurante is not None:
        print(f"Restaurante já existe: {nome}")
        return restaurante
    restaurante = api_json(RESTAURANTES_URL, "/api/restaurantes", "POST", dados)
    print(f"Restaurante criado: {nome}")
    # O cadastro não recebe status (todo restaurante nasce ABERTO); outro status vai pelo PATCH.
    if restaurante.get("statusRestaurante") != status:
        restaurante = api_json(
            RESTAURANTES_URL,
            f"/api/restaurantes/{restaurante[CAMPO_ID]}/status",
            "PATCH",
            {"statusRestaurante": status},
        )
        print(f"  Status alterado para {status}")
    return restaurante


def cadastrar_adicionais_faltantes(produto_id, adicionais):
    if not adicionais:
        return

    path = f"/api/produtos/{produto_id}/adicionais"
    nomes_existentes = {item["nome"] for item in api_json(CARDAPIO_URL, path)}
    for adicional in adicionais:
        if adicional["nome"] in nomes_existentes:
            print(f"    Adicional já existe: {adicional['nome']}")
            continue
        api_json(CARDAPIO_URL, path, "POST", adicional)
        nomes_existentes.add(adicional["nome"])
        print(f"    Adicional criado: {adicional['nome']}")


def cadastrar_produtos_faltantes(restaurante_id, produtos):
    path = f"/api/restaurantes/{restaurante_id}/cardapio"
    existentes = {produto["nome"]: produto for produto in api_json(CARDAPIO_URL, path)}
    for exemplo in produtos:
        produto = existentes.get(exemplo["nome"])
        if produto is None:
            dados_produto = {chave: valor for chave, valor in exemplo.items() if chave != "adicionais"}
            produto = api_json(CARDAPIO_URL, path, "POST", dados_produto)
            existentes[exemplo["nome"]] = produto
            print(f"  Produto criado: {exemplo['nome']}")
        else:
            print(f"  Produto já existe: {exemplo['nome']}")

        cadastrar_adicionais_faltantes(produto["id"], exemplo.get("adicionais", []))


def main():
    if os.getenv("SEED_ENABLED", "true").lower() != "true":
        print("Seed desativada (SEED_ENABLED=false).")
        return

    external_database = any(
        os.getenv(key) != url for key, url in LOCAL_DATABASES.items()
    )
    if external_database and os.getenv("SEED_ALLOW_EXTERNAL", "false").lower() != "true":
        print("Seed ignorada: banco externo configurado. Use SEED_ALLOW_EXTERNAL=true para autorizar.")
        return

    existentes = api_json(RESTAURANTES_URL, "/api/restaurantes")
    for exemplo in RESTAURANTES:
        restaurante = obter_ou_criar_restaurante(exemplo["dados"], exemplo["status"], existentes)
        cadastrar_produtos_faltantes(restaurante[CAMPO_ID], exemplo["produtos"])

    print("Seed concluída.")


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as error:
        sys.exit(f"Seed falhou: {error}")
