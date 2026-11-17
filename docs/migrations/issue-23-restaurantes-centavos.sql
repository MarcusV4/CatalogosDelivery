-- Execute no banco restaurantes antes de iniciar a versão que usa centavos.
-- Converte a taxa antiga em reais (real) para centavos (bigint).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'restaurante'
          AND column_name = 'taxa_entrega' AND data_type <> 'bigint'
    ) THEN
        ALTER TABLE public.restaurante
            ALTER COLUMN taxa_entrega TYPE bigint
            USING round(taxa_entrega::numeric * 100)::bigint;
    END IF;
END $$;
