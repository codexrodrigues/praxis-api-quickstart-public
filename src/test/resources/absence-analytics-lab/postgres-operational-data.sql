insert into public.departamentos values (1, 'OPS', 'Operacoes'), (2, 'HR', 'Recursos Humanos');
insert into public.funcionarios values (100), (101), (102);

insert into public.funcionario_lotacoes_departamento (funcionario_id, departamento_id, effective_from, effective_to)
values
    (100, 1, date '2026-07-01', null),
    (101, 1, date '2026-07-01', date '2026-07-11'),
    (101, 2, date '2026-07-11', null);

insert into public.ferias_afastamentos values
    (1, date '2026-07-01', date '2026-07-10', 100),
    (2, date '2026-07-05', date '2026-07-20', 100),
    (3, date '2026-07-09', date '2026-07-12', 101),
    (4, date '2026-07-01', date '2026-07-03', 102);
