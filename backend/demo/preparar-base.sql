\set ON_ERROR_STOP on

DO $$
BEGIN
    IF EXISTS (
        SELECT FROM pg_database
        WHERE datname = 'kaiju_inventory_demo' AND pg_get_userbyid(datdba) <> 'kaiju_demo'
    ) THEN
        RAISE EXCEPTION 'La base de demostración existe con otro propietario.';
    END IF;
    IF EXISTS (
        SELECT FROM pg_roles WHERE rolname = 'kaiju_demo'
          AND (NOT rolcanlogin OR rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
    ) OR EXISTS (
        SELECT FROM pg_auth_members WHERE member = (SELECT oid FROM pg_roles WHERE rolname = 'kaiju_demo')
    ) THEN
        RAISE EXCEPTION 'El rol de demostración existente tiene una configuración distinta de la prevista.';
    END IF;
END $$;

SELECT EXISTS (SELECT FROM pg_roles WHERE rolname = 'kaiju_demo') AS rol_existe,
       EXISTS (SELECT FROM pg_database WHERE datname = 'kaiju_inventory_demo') AS base_existe \gset

\if :rol_existe
    \echo Se conserva la contraseña de kaiju_demo.
\else
    BEGIN;
    CREATE ROLE kaiju_demo LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
    \echo Elige una contraseña exclusiva para kaiju_demo y confírmala.
    \password kaiju_demo
    COMMIT;
\endif

\if :base_existe
    \echo Se conserva la base de demostración existente.
\else
    CREATE DATABASE kaiju_inventory_demo OWNER kaiju_demo TEMPLATE template0;
\endif

REVOKE CONNECT ON DATABASE kaiju_inventory_demo FROM PUBLIC;
