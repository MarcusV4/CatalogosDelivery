-- Converte produto.preco de reais (numeric) para centavos (bigint): 25.90 -> 2590.
-- Em banco novo a tabela ainda não existe (o Hibernate cria depois, já como bigint),
-- e em banco já convertido a coluna já é bigint: nos dois casos nada é feito.
DO $$
DECLARE
    preco_data_type text;
BEGIN
    IF to_regclass('public.produto') IS NOT NULL THEN
        SELECT data_type
        INTO preco_data_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'produto'
          AND column_name = 'preco';

        IF preco_data_type IN ('numeric', 'real', 'double precision') THEN
            ALTER TABLE public.produto
                ALTER COLUMN preco TYPE bigint
                USING round(preco::numeric * 100)::bigint;
        ELSIF preco_data_type IS NOT NULL AND preco_data_type <> 'bigint' THEN
            RAISE EXCEPTION 'Tipo inesperado para produto.preco: %', preco_data_type;
        END IF;
    END IF;
END $$;
