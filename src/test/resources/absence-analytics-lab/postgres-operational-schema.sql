do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'praxis_service_user') then
        create role praxis_service_user;
    end if;
end
$$;

create table public.departamentos (
    id integer primary key,
    codigo varchar(40) not null,
    nome varchar(120) not null
);

create table public.funcionarios (
    id integer primary key,
    nome_completo varchar(255),
    cpf varchar(32),
    email varchar(255)
);

create table public.ferias_afastamentos (
    id integer primary key,
    data_inicio date not null,
    data_fim date not null,
    funcionario_id integer not null references public.funcionarios(id)
);
