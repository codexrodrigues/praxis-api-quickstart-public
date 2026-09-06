-- G2 HR analytics: department attribution is temporal, never inferred from the employee's current department.
-- Validity is half-open: [effective_from, effective_to). A null effective_to means open-ended.

create extension if not exists btree_gist;

create table if not exists public.funcionario_lotacoes_departamento (
    id bigserial primary key,
    funcionario_id integer not null,
    departamento_id integer not null,
    effective_from date not null,
    effective_to date,
    created_at timestamptz not null default current_timestamp,
    constraint fk_funcionario_lotacao_funcionario
        foreign key (funcionario_id) references public.funcionarios (id) on delete restrict,
    constraint fk_funcionario_lotacao_departamento
        foreign key (departamento_id) references public.departamentos (id) on delete restrict,
    constraint ck_funcionario_lotacao_interval
        check (effective_to is null or effective_to > effective_from),
    constraint ex_funcionario_lotacao_no_overlap
        exclude using gist (
            funcionario_id with =,
            daterange(effective_from, coalesce(effective_to, 'infinity'::date), '[)') with &&
        )
);

create index if not exists idx_funcionario_lotacao_effective_lookup
    on public.funcionario_lotacoes_departamento (funcionario_id, effective_from desc);
