-- Public-demo-only correction for the employee dossier presentation.
-- This file is deliberately outside db/operational-migrations because it changes
-- fictional demonstration data, not the operational schema.
do $$
declare
    canonical_present boolean;
    duplicate_present boolean;
begin
    select exists (
        select 1
        from public.funcionarios
        where id = 1
          and nome_completo = 'Tony Stark'
          and cpf = '90000000175'
          and email = 'tony.stark@stark.demo.praxisui.dev'
    ) into canonical_present;

    if not canonical_present then
        raise exception 'PRAXIS_DEMO_GUARD: expected the recognized public-demo employee; no data was changed.';
    end if;

    select exists (
        select 1
        from public.dependentes
        where id = 1
          and nome_completo = 'Morgan Stark'
          and parentesco = 'Filha'
          and data_nascimento = date '2015-04-01'
          and funcionario_id = 1
    ) into canonical_present;

    select exists (
        select 1
        from public.dependentes
        where id = 68
          and nome_completo = 'Morgan Stark'
          and parentesco = 'FILHA'
          and data_nascimento = date '2018-05-04'
          and funcionario_id = 1
    ) into duplicate_present;

    if not canonical_present then
        raise exception 'PRAXIS_DEMO_GUARD: canonical dependent 1 diverged from the recognized fixture; no data was changed.';
    end if;

    if exists (select 1 from public.dependentes where id = 68) and not duplicate_present then
        raise exception 'PRAXIS_DEMO_GUARD: dependent 68 is not the recognized duplicate; no data was changed.';
    end if;
end
$$;

delete from public.dependentes
where id = 68
  and nome_completo = 'Morgan Stark'
  and parentesco = 'FILHA'
  and data_nascimento = date '2018-05-04'
  and funcionario_id = 1;

do $$
begin
    if not exists (
        select 1
        from public.dependentes
        where id = 1
          and nome_completo = 'Morgan Stark'
          and parentesco = 'Filha'
          and data_nascimento = date '2015-04-01'
          and funcionario_id = 1
    ) or exists (
        select 1
        from public.dependentes
        where id = 68
    ) then
        raise exception 'PRAXIS_DEMO_GUARD: dependent state diverged from the expected fixture; transaction rolled back.';
    end if;
end
$$;
