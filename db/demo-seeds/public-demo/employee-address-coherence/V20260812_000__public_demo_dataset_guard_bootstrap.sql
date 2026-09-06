-- Bootstrap the public-demo fingerprint only for databases that predate the
-- sentinel table but still match the immutable public fixture.
do $$
begin
    if to_regclass('public.praxis_demo_dataset_guard') is null and not (
        (select count(*) from public.funcionarios where id between 1 and 50) = 50
        and (select count(*) from public.departamentos where id between 1 and 27) = 27
        and (select count(*) from public.enderecos where id between 1 and 50) = 50
        and exists (
            select 1 from public.funcionarios
            where id = 1
              and nome_completo = 'Tony Stark'
              and cpf = '90000000175'
              and email = 'tony.stark@stark.demo.praxisui.dev'
        )
        and exists (
            select 1 from public.funcionarios
            where id = 2
              and nome_completo = 'Pepper Potts'
              and cpf = '90000000256'
              and email = 'pepper.potts@stark.demo.praxisui.dev'
        )
        and exists (
            select 1 from public.funcionarios
            where id = 3
              and nome_completo = 'Bruce Wayne'
              and cpf = '90000000337'
              and email = 'bruce.wayne@wayne.demo.praxisui.dev'
        )
        and exists (
            select 1 from public.funcionarios
            where id = 50
              and nome_completo = 'Maria Hill'
              and cpf = '90000005053'
              and email = 'maria.hill@shield.demo.praxisui.dev'
        )
        and exists (
            select 1 from public.enderecos
            where id = 2
              and funcionario_id = 2
              and (
                  (
                      logradouro = 'Alameda das Mansões'
                      and numero = '1'
                      and complemento = 'Mansão Wayne'
                      and bairro = 'Nobre'
                      and cidade = 'Gotham'
                      and estado = 'GT'
                      and cep = '54321-000'
                  )
                  or (
                      logradouro = 'Avenida Park'
                      and numero = '10880'
                      and complemento = 'Apartamento 42'
                      and bairro = 'Midtown'
                      and cidade = 'Nova York'
                      and estado = 'NY'
                      and cep = '10017-000'
                  )
              )
        )
    ) then
        raise exception 'PRAXIS_DEMO_GUARD: database does not match the immutable public-demo fixture; no sentinel was created.';
    end if;
end
$$;

create table if not exists public.praxis_demo_dataset_guard (
    dataset_key text primary key,
    dataset_fingerprint text not null
);

insert into public.praxis_demo_dataset_guard (dataset_key, dataset_fingerprint)
values ('praxis-public-demo', 'praxis-public-demo-2026-07-15')
on conflict (dataset_key) do nothing;

do $$
begin
    if not exists (
        select 1
        from public.praxis_demo_dataset_guard
        where dataset_key = 'praxis-public-demo'
          and dataset_fingerprint = 'praxis-public-demo-2026-07-15'
    ) then
        raise exception 'PRAXIS_DEMO_GUARD: an incompatible public-demo fingerprint already exists; transaction rolled back.';
    end if;
end
$$;
