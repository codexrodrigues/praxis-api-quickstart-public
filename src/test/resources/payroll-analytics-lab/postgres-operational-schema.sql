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

create table public.cargos (
    id integer primary key,
    nome varchar(120) not null
);

create table public.funcionarios (
    id integer primary key,
    nome_completo varchar(255),
    departamento_id integer references public.departamentos(id),
    cargo_id integer references public.cargos(id)
);

create table public.folhas_pagamento (
    id integer primary key,
    funcionario_id integer not null references public.funcionarios(id),
    ano integer not null,
    mes integer not null,
    data_pagamento date,
    salario_bruto numeric(18,2) not null,
    total_descontos numeric(18,2) not null,
    salario_liquido numeric(18,2) not null
);

create table public.identidades_secretas (
    id integer primary key,
    funcionario_id integer not null references public.funcionarios(id),
    codinome text,
    universo text,
    exposicao_publica boolean
);

create table public.bases (
    id integer primary key,
    nome text,
    tipo text,
    sigilo text
);

create table public.equipes (
    id integer primary key,
    nome text,
    base_principal_id integer references public.bases(id)
);

create table public.equipe_membros (
    id integer primary key,
    funcionario_id integer not null references public.funcionarios(id),
    equipe_id integer not null references public.equipes(id),
    papel text,
    data_entrada date,
    data_saida date
);

create table public.eventos_folha (
    id integer primary key,
    folha_pagamento_id integer not null references public.folhas_pagamento(id),
    descricao text not null,
    tipo text not null,
    valor numeric(18,2) not null
);
