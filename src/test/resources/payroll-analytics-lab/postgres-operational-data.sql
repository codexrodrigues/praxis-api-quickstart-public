insert into public.departamentos (id, codigo, nome) values
    (1, 'D01', 'Operacoes Legadas'),
    (2, 'D02', 'Operacoes Atuais'),
    (3, 'D03', 'Projeto Encerrado'),
    (4, 'D04', 'Nova Unidade');

insert into public.cargos (id, nome) values (1, 'Analista');

insert into public.funcionarios (id, nome_completo, departamento_id, cargo_id) values
    (1, 'Pessoa Transferida', 2, 1),
    (2, 'Pessoa Estavel', 1, 1),
    (3, 'Pessoa Periodo Anterior', 3, 1),
    (4, 'Pessoa Nova Unidade', 4, 1),
    (5, 'Pessoa Sem Lotacao Historica', 1, 1);

insert into public.funcionario_lotacoes_departamento
    (funcionario_id, departamento_id, effective_from, effective_to) values
    (1, 1, date '2020-01-01', date '2026-07-01'),
    (1, 2, date '2026-07-01', null),
    (2, 1, date '2020-01-01', null),
    (3, 3, date '2020-01-01', null),
    (4, 4, date '2026-07-01', null);

insert into public.folhas_pagamento
    (id, funcionario_id, ano, mes, data_pagamento, salario_bruto, total_descontos, salario_liquido) values
    (101, 1, 2026, 6, date '2026-06-30', 1000.00, 100.00, 900.00),
    (102, 2, 2026, 6, date '2026-06-30', 2000.00, 200.00, 1800.00),
    (103, 3, 2026, 6, date '2026-06-30', 3000.00, 300.00, 2700.00),
    (201, 1, 2026, 7, date '2026-07-31', 1100.00, 110.00, 990.00),
    (202, 2, 2026, 7, date '2026-07-31', 2100.00, 210.00, 1890.00),
    (204, 4, 2026, 7, date '2026-07-31', 4000.00, 400.00, 3600.00),
    (205, 5, 2026, 7, date '2026-07-31', 5000.00, 500.00, 4500.00);
