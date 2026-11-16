DO $$
DECLARE
    categoria_data_type text;
BEGIN
    IF to_regclass('public.produto') IS NOT NULL THEN
        SELECT data_type
        INTO categoria_data_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'produto'
          AND column_name = 'categoria';

        IF categoria_data_type IN ('smallint', 'integer', 'bigint') THEN
            IF EXISTS (
                SELECT 1
                FROM public.produto
                WHERE categoria IS NOT NULL
                  AND categoria NOT IN (0, 1, 2, 3, 4, 5, 6, 7, 8)
            ) THEN
                RAISE EXCEPTION 'Produto possui ordinal de categoria sem mapeamento.';
            END IF;

            ALTER TABLE public.produto
                ALTER COLUMN categoria TYPE varchar(255)
                USING CASE categoria
                    WHEN 0 THEN 'LANCHE'
                    WHEN 1 THEN 'PRATO_PRINCIPAL'
                    WHEN 2 THEN 'PIZZA'
                    WHEN 3 THEN 'ENTRADA'
                    WHEN 4 THEN 'SOBREMESA'
                    WHEN 5 THEN 'BEBIDA'
                    WHEN 6 THEN 'VEGETARIANO'
                    WHEN 7 THEN 'SALGADO'
                    WHEN 8 THEN 'ORIENTAL'
                    ELSE NULL
                END;
        ELSIF categoria_data_type IS NOT NULL
                AND categoria_data_type NOT IN ('character varying', 'text') THEN
            RAISE EXCEPTION 'Tipo inesperado para produto.categoria: %', categoria_data_type;
        END IF;
    END IF;
END $$;
