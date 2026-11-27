-- O dono do restaurante passa a ser a conta da Identidade: o sub do access token,
-- um UUID (conta_id). Antes era um número informado no corpo da requisição.
--
-- Bancos novos: a tabela ainda não existe quando o Flyway roda (o Hibernate a cria
-- depois, já com varchar), então não há nada a fazer.
-- Bancos existentes: converte a coluna numérica para texto. Os donos antigos (1, 2...)
-- viram '1', '2'... e não correspondem a nenhuma conta real; para gerenciar esses
-- restaurantes, atualize owner_user_id com o UUID da conta LOJA dona.
DO $$
DECLARE
    tipo_atual text;
BEGIN
    IF to_regclass('public.restaurante') IS NULL THEN
        RETURN;
    END IF;

    SELECT data_type
    INTO tipo_atual
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'restaurante'
      AND column_name = 'owner_user_id';

    IF tipo_atual IN ('smallint', 'integer', 'bigint') THEN
        ALTER TABLE public.restaurante
            ALTER COLUMN owner_user_id TYPE varchar(36) USING owner_user_id::text;
    END IF;
END $$;
