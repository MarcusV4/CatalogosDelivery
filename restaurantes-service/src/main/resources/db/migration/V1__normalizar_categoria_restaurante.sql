DO $$
DECLARE
    categoria_data_type text;
    restricao record;
BEGIN
    IF to_regclass('public.restaurante') IS NOT NULL THEN
        SELECT data_type
        INTO categoria_data_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'restaurante'
          AND column_name = 'categoria_restaurante';

        IF categoria_data_type IN ('smallint', 'integer', 'bigint') THEN
            IF EXISTS (
                SELECT 1 FROM public.restaurante
                WHERE categoria_restaurante IS NOT NULL
                  AND categoria_restaurante NOT BETWEEN 0 AND 18
            ) THEN
                RAISE EXCEPTION 'Restaurante possui ordinal de categoria sem mapeamento.';
            END IF;

            FOR restricao IN
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
                WHERE c.conrelid = 'public.restaurante'::regclass
                  AND c.contype = 'c'
                  AND a.attname = 'categoria_restaurante'
            LOOP
                EXECUTE format('ALTER TABLE public.restaurante DROP CONSTRAINT %I', restricao.conname);
            END LOOP;

            ALTER TABLE public.restaurante
                ALTER COLUMN categoria_restaurante TYPE varchar(255)
                USING CASE categoria_restaurante
                    WHEN 0 THEN 'HAMBURGUERIA'
                    WHEN 1 THEN 'PIZZARIA'
                    WHEN 2 THEN 'COMIDA_BRASILEIRA'
                    WHEN 3 THEN 'COMIDA_REGIONAL'
                    WHEN 4 THEN 'COMIDA_JAPONESA'
                    WHEN 5 THEN 'COMIDA_CHINESA'
                    WHEN 6 THEN 'COMIDA_ITALIANA'
                    WHEN 7 THEN 'COMIDA_MEXICANA'
                    WHEN 8 THEN 'COMIDA_ARABE'
                    WHEN 9 THEN 'LANCHONETE'
                    WHEN 10 THEN 'MARMITARIA'
                    WHEN 11 THEN 'CHURRASCARIA'
                    WHEN 12 THEN 'COMIDA_SAUDAVEL'
                    WHEN 13 THEN 'COMIDA_VEGETARIANA_E_VEGANA'
                    WHEN 14 THEN 'PADARIA'
                    WHEN 15 THEN 'DOCERIA'
                    WHEN 16 THEN 'SORVETERIA'
                    WHEN 17 THEN 'ACAI'
                    WHEN 18 THEN 'OUTROS'
                END;
        ELSIF categoria_data_type IN ('character varying', 'text') THEN
            UPDATE public.restaurante
            SET categoria_restaurante = CASE categoria_restaurante
                WHEN 'COMIDA_ÁRABE' THEN 'COMIDA_ARABE'
                WHEN 'COMIDA_SAUDÁVEL' THEN 'COMIDA_SAUDAVEL'
                WHEN 'AÇAÍ' THEN 'ACAI'
                ELSE categoria_restaurante
            END;
        ELSIF categoria_data_type IS NOT NULL THEN
            RAISE EXCEPTION 'Tipo inesperado para restaurante.categoria_restaurante: %', categoria_data_type;
        END IF;
    END IF;
END $$;
