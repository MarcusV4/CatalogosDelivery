-- Pedido mínimo do restaurante (#79), em centavos e sem a taxa de entrega.
-- No acordo com a Identidade, o Catálogo passou a ser a fonte oficial desse valor
-- (o Perfis deixou de aceitar valor_minimo_pedido). Nulo: sem pedido mínimo.
--
-- Bancos novos: a tabela ainda não existe quando o Flyway roda (o Hibernate a cria
-- depois, já com a coluna), então não há nada a fazer.
-- Bancos existentes: adiciona a coluna; os restaurantes atuais ficam sem mínimo.
DO $$
BEGIN
    IF to_regclass('public.restaurante') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE public.restaurante ADD COLUMN IF NOT EXISTS pedido_minimo bigint;
END $$;
