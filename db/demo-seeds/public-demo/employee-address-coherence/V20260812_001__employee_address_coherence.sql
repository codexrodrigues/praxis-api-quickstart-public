-- Public-demo-only correction for the employee dossier presentation.
-- This file is deliberately outside db/operational-migrations because it changes
-- fictional demonstration data, not the operational schema.
do $$
begin
    if not (
        exists (
            select 1
            from public.praxis_demo_dataset_guard guard
            where guard.dataset_key = 'praxis-public-demo'
              and guard.dataset_fingerprint = 'praxis-public-demo-2026-07-15'
        )
        and exists (
            select 1
            from public.funcionarios
            where id = 2
              and nome_completo = 'Pepper Potts'
              and cpf = '90000000256'
              and email = 'pepper.potts@stark.demo.praxisui.dev'
        )
        and exists (
            select 1
            from public.enderecos
            where id = 2
              and funcionario_id = 2
        )
    ) then
        raise exception 'PRAXIS_DEMO_GUARD: expected the recognized public-demo employee and address; no data was changed.';
    end if;
end
$$;

update public.enderecos
set logradouro = 'Avenida Park',
    numero = '10880',
    complemento = 'Apartamento 42',
    bairro = 'Midtown',
    cidade = 'Nova York',
    estado = 'NY',
    cep = '10017-000'
where id = 2
  and funcionario_id = 2
  and logradouro = 'Alameda das Mansões'
  and numero = '1'
  and complemento = 'Mansão Wayne'
  and bairro = 'Nobre'
  and cidade = 'Gotham'
  and estado = 'GT'
  and cep = '54321-000';

do $$
begin
    if not exists (
        select 1
        from public.enderecos
        where id = 2
          and funcionario_id = 2
          and logradouro = 'Avenida Park'
          and numero = '10880'
          and complemento = 'Apartamento 42'
          and bairro = 'Midtown'
          and cidade = 'Nova York'
          and estado = 'NY'
          and cep = '10017-000'
    ) then
        raise exception 'PRAXIS_DEMO_GUARD: address state diverged from the recognized fixture; transaction rolled back.';
    end if;
end
$$;
