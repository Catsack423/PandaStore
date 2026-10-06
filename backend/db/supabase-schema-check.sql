-- Run AFTER supabase-schema.sql, as postgres. Every probe rolls back.
-- Exercises privileges, RLS, backend CRUD, enum checks and FK enforcement.
begin;
set local statement_timeout = '60s';

do $check$
declare
    app_tables text[] := array[
        'addresses', 'auth_sessions', 'cart_items', 'carts', 'categories',
        'customers', 'notifications', 'order_groups', 'order_items', 'orders',
        'payments', 'product_categories', 'product_images', 'products',
        'review_replies', 'reviews', 'seller_applications', 'seller_bank_accounts',
        'sellers', 'shipments', 'users'
    ];
    table_name text;
    first_column text;
    client_role text;
    operation text;
    denied integer := 0;
begin
    if (select count(*) from pg_catalog.pg_tables
        where schemaname = 'public' and tablename = any(app_tables) and rowsecurity) <> 21 then
        raise exception 'All 21 app tables must exist with RLS enabled';
    end if;
    if not exists (select 1 from pg_catalog.pg_roles
        where rolname = 'pandastore_backend' and not rolsuper and not rolbypassrls
            and not rolcreatedb and not rolcreaterole and not rolreplication) then
        raise exception 'Backend role has missing or excessive privileges';
    end if;
    foreach table_name in array app_tables loop
        foreach client_role in array array['anon', 'authenticated', 'service_role'] loop
            if has_table_privilege(client_role, format('public.%I', table_name),
                'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER') then
                raise exception 'Client role % has a direct table grant on %', client_role, table_name;
            end if;
        end loop;
        if has_table_privilege('pandastore_backend', format('public.%I', table_name),
            'TRUNCATE,REFERENCES,TRIGGER') then
            raise exception 'Backend has unnecessary table privileges on %', table_name;
        end if;
    end loop;
    if exists (
        select 1 from pg_catalog.pg_constraint c
        join pg_catalog.pg_class t on t.oid = c.conrelid
        join pg_catalog.pg_namespace n on n.oid = t.relnamespace
        where c.contype = 'f' and n.nspname = 'public' and t.relname = any(app_tables)
            and not exists (select 1 from pg_catalog.pg_index i
                where i.indrelid = c.conrelid and i.indisvalid and i.indisready
                    and i.indpred is null and i.indkey[0] = c.conkey[1])
    ) then
        raise exception 'An app foreign key has no supporting index';
    end if;

    -- Real permission denials for all four operations on every app table.
    foreach client_role in array array['anon', 'authenticated', 'service_role'] loop
        execute format('set local role %I', client_role);
        foreach table_name in array app_tables loop
            select a.attname into strict first_column from pg_catalog.pg_attribute a
                where a.attrelid = format('public.%I', table_name)::regclass
                    and a.attnum > 0 and not a.attisdropped order by a.attnum limit 1;
            foreach operation in array array[
                format('select * from public.%I limit 1', table_name),
                format('insert into public.%I default values', table_name),
                format('update public.%I set %I = %I where false', table_name, first_column, first_column),
                format('delete from public.%I where false', table_name)
            ] loop
                begin
                    execute operation;
                    raise exception 'Unexpected client access: %', operation;
                exception when insufficient_privilege then
                    denied := denied + 1;
                end;
            end loop;
        end loop;
        reset role;
    end loop;
    if denied <> 252 then
        raise exception 'Expected 252 CRUD denials, got %', denied;
    end if;
    raise notice 'PASS: 21 tables with RLS, indexed FKs, limited grants and 252 client CRUD denials';
end;
$check$;

set local role pandastore_backend;
do $check$
declare
    probe_category_id bigint := -9223372036854775806;
    actual_name text;
begin
    insert into public.categories (category_id, category_name) values (probe_category_id, 'schema_validation_probe');
    update public.categories set description = 'updated' where categories.category_id = probe_category_id;
    select description into strict actual_name from public.categories where categories.category_id = probe_category_id;
    if actual_name <> 'updated' then raise exception 'Backend read/update failed'; end if;
    delete from public.categories where categories.category_id = probe_category_id;
    if exists (select 1 from public.categories where categories.category_id = probe_category_id) then
        raise exception 'Backend delete failed';
    end if;
    begin
        insert into public.users (username, email, password_hash, role, status, created_at)
            values ('schema_constraint_probe', 'schema_constraint_probe@example.invalid',
                'not-a-login-credential', 'UNKNOWN_ROLE', 'ACTIVE', current_timestamp);
        raise exception 'Invalid enum value was accepted';
    exception when check_violation then null;
    end;
    begin
        insert into public.auth_sessions (token_hash, credential_hash, user_id, expires_at)
            values ('schema_fk_probe', 'not-a-login-credential', -9223372036854775808, current_timestamp);
        raise exception 'Invalid FK was accepted';
    exception when foreign_key_violation then null;
    end;
    raise notice 'PASS: backend CRUD, enum constraint and FK constraint';
end;
$check$;
reset role;

-- RLS must still deny browser roles even if someone mistakenly grants CRUD.
grant select, insert, update, delete on public.categories to anon, authenticated;
do $check$
declare
    probe_id bigint := -9223372036854775808;
    client_role text;
    row_count integer;
begin
    set local role pandastore_backend;
    insert into public.categories (category_id, category_name) values (probe_id, 'schema_rls_probe');
    reset role;
    foreach client_role in array array['anon', 'authenticated'] loop
        execute format('set local role %I', client_role);
        select count(*) into row_count from public.categories where category_id = probe_id;
        if row_count <> 0 then raise exception 'RLS exposed the backend row to %', client_role; end if;
        update public.categories set category_name = 'unauthorized' where category_id = probe_id;
        get diagnostics row_count = row_count;
        if row_count <> 0 then raise exception 'RLS allowed a client update'; end if;
        delete from public.categories where category_id = probe_id;
        get diagnostics row_count = row_count;
        if row_count <> 0 then raise exception 'RLS allowed a client delete'; end if;
        begin
            insert into public.categories (category_id, category_name) values (probe_id + 1, 'unauthorized');
            raise exception 'RLS allowed a client insert';
        exception when insufficient_privilege then null;
        end;
        reset role;
    end loop;
    set local role pandastore_backend;
    if not exists (select 1 from public.categories where category_id = probe_id and category_name = 'schema_rls_probe') then
        raise exception 'Denied client writes changed the backend row';
    end if;
    reset role;
    raise notice 'PASS: RLS rejects client CRUD even with accidental table grants; original row preserved';
end;
$check$;

rollback;
