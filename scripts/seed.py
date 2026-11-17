"""Dados de exemplo para o Compose local, cadastrados pelas APIs dos serviços.

Pode ser executada várias vezes: restaurantes e produtos que já existem
(mesmo nome) são mantidos, e só o que falta é cadastrado.
"""

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

# TODO(#31): quando as rotas de escrita exigirem JWT, enviar um token do mock
# da Identidade e remover ownerUserId do body (ele passará a vir do token).
OWNER_USER_ID = 1

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
            "restauranteTelefone": "(81) 3333-0001",
            "taxaEntrega": 500,
            "tempoEstimadoMin": 30,
            "tempoEstimadoMax": 45,
            "ownerUserId": OWNER_USER_ID,
            "statusRestaurante": "ABERTO",
            "categoriaRestaurante": "PIZZARIA",
        },
        "produtos": [
            {
                "nome": "Pizza Margherita",
                "descricao": "Molho de tomate, muçarela e manjericão",
                "preco": 2590,
                "disponivel": True,
                "categoria": "PIZZA",
            },
            {
                "nome": "Pizza Calabresa",
                "descricao": "Calabresa fatiada, cebola e muçarela",
                "preco": 2890,
                "disponivel": True,
                "categoria": "PIZZA",
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
            "restauranteTelefone": "(81) 3333-0002",
            "taxaEntrega": 300,
            "tempoEstimadoMin": 20,
            "tempoEstimadoMax": 30,
            "ownerUserId": OWNER_USER_ID,
            "statusRestaurante": "FECHADO",
            "categoriaRestaurante": "ACAI",
        },
        "produtos": [
            {
                "nome": "Açaí 500 ml",
                "descricao": "Açaí com banana e granola",
                "preco": 1800,
                "disponivel": True,
                "categoria": "SOBREMESA",
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
    request = Request(
        base_url + path,
        data=body,
        headers={"Content-Type": "application/json"} if body else {},
        method=method,
    )
    try:
        with urlopen(request, timeout=10) as response:
            return json.load(response)
    except HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {base_url}{path}: HTTP {error.code}: {detail}") from error
    except URLError as error:
        raise RuntimeError(f"{method} {base_url}{path}: {error.reason}") from error


def obter_ou_criar_restaurante(dados, existentes):
    nome = dados[CAMPO_NOME]
    restaurante = next((item for item in existentes if item.get(CAMPO_NOME) == nome), None)
    if restaurante is not None:
        print(f"Restaurante já existe: {nome}")
        return restaurante
    restaurante = api_json(RESTAURANTES_URL, "/api/restaurantes", "POST", dados)
    print(f"Restaurante criado: {nome}")
    return restaurante


def cadastrar_produtos_faltantes(restaurante_id, produtos):
    path = f"/api/produtos/{restaurante_id}"
    nomes_existentes = {produto["nome"] for produto in api_json(CARDAPIO_URL, path)}
    for produto in produtos:
        if produto["nome"] in nomes_existentes:
            print(f"  Produto já existe: {produto['nome']}")
            continue
        api_json(CARDAPIO_URL, path, "POST", produto)
        print(f"  Produto criado: {produto['nome']}")


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
        restaurante = obter_ou_criar_restaurante(exemplo["dados"], existentes)
        cadastrar_produtos_faltantes(restaurante[CAMPO_ID], exemplo["produtos"])

    print("Seed concluída.")


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as error:
        sys.exit(f"Seed falhou: {error}")
