"""Dados de exemplo para o Compose local, cadastrados pelas APIs dos serviços."""

import json
import os
from urllib.error import HTTPError
from urllib.request import Request, urlopen


RESTAURANTES_URL = os.environ["RESTAURANTES_URL"].rstrip("/")
CARDAPIO_URL = os.environ["CARDAPIO_URL"].rstrip("/")

LOCAL_DATABASES = {
    "RESTAURANTES_DB_URL": "jdbc:postgresql://db-restaurantes:5432/restaurantes",
    "CARDAPIO_DB_URL": "jdbc:postgresql://db-cardapio:5432/cardapio",
}

RESTAURANTE = {
    "restauranteNome": "Pizzaria Exemplo",
    "restauranteDesc": "Restaurante de demonstração do catálogo",
    "cidade": "Recife",
    "estado": "PE",
    "taxaEntrega": 500,
    "tempoEstimadoMin": 30,
    "tempoEstimadoMax": 45,
    "ownerUserId": 1,
    "statusRestaurante": "ABERTO",
    "categoriaRestaurante": "PIZZARIA",
}

PRODUTOS = [
    {
        "nome": "Pizza Margherita",
        "descricao": "Molho de tomate, muçarela e manjericão",
        "preco": 2590,
        "disponivel": True,
        "categoria": "PIZZA",
    },
    {
        "nome": "Refrigerante Lata",
        "descricao": "Lata de 350 ml",
        "preco": 600,
        "disponivel": True,
        "categoria": "BEBIDA",
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
        raise RuntimeError(f"{method} {path}: HTTP {error.code}: {detail}") from error


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

    restaurantes = api_json(RESTAURANTES_URL, "/api/restaurantes")
    restaurante = next(
        (item for item in restaurantes if item["restauranteNome"] == RESTAURANTE["restauranteNome"]),
        None,
    )
    if restaurante is None:
        restaurante = api_json(RESTAURANTES_URL, "/api/restaurantes", "POST", RESTAURANTE)
        print(f"Restaurante criado: {restaurante['restauranteNome']}")
    else:
        print(f"Restaurante já existe: {restaurante['restauranteNome']}")

    restaurante_id = restaurante["restauranteId"]
    path = f"/api/produtos/{restaurante_id}"
    nomes_existentes = {produto["nome"] for produto in api_json(CARDAPIO_URL, path)}
    for produto in PRODUTOS:
        if produto["nome"] in nomes_existentes:
            print(f"Produto já existe: {produto['nome']}")
            continue
        api_json(CARDAPIO_URL, path, "POST", produto)
        print(f"Produto criado: {produto['nome']}")

    print("Seed concluída.")


if __name__ == "__main__":
    main()
