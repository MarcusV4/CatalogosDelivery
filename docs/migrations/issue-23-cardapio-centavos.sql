-- Execute no banco cardapio antes de iniciar a versão que usa centavos.
-- Converte o preço antigo em reais (numeric) para centavos (bigint).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'produto'
          AND column_name = 'preco' AND data_type <> 'bigint'
    ) THEN
        ALTER TABLE public.produto
            ALTER COLUMN preco TYPE bigint
            USING round(preco * 100)::bigint;
    END IF;
END $$;
