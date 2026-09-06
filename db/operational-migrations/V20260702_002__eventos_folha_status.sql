alter table public.eventos_folha
    add column if not exists status varchar(20) not null default 'PENDENTE';

create index if not exists idx_eventos_folha_status
    on public.eventos_folha(status);
