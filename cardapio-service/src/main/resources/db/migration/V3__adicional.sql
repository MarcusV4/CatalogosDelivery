-- Flyway roda antes do Hibernate: em banco novo, produto ainda não existe.
-- O Hibernate cria produto e completa a FK; em banco existente, a FK é
-- criada aqui. A exclusão em cascata também é garantida pelo mapeamento JPA.
CREATE SEQUENCE IF NOT EXISTS adicional_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS adicional (
    id BIGINT PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    preco BIGINT NOT NULL,
    disponivel BOOLEAN NOT NULL,
    CONSTRAINT adicional_preco_nao_negativo CHECK (preco >= 0)
);

DO $$
BEGIN
    IF to_regclass('public.produto') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.adicional'::regclass
             AND contype = 'f'
             AND conname = 'fk_adicional_produto'
       ) THEN
        ALTER TABLE public.adicional
            ADD CONSTRAINT fk_adicional_produto
            FOREIGN KEY (produto_id) REFERENCES public.produto(id) ON DELETE CASCADE;
    END IF;
END $$;
