-- Beta contract hardening: a local ledger entry is not evidence of an external effect.
alter table public.extraordinary_benefit_request
    drop constraint if exists ck_extraordinary_benefit_request_effect;

update public.extraordinary_benefit_request
set effect_status = 'LOCAL_RECORDED'
where effect_status = 'EXECUTED';

alter table public.extraordinary_benefit_request
    add constraint ck_extraordinary_benefit_request_effect
        check (effect_status in ('PLANNED', 'LOCAL_RECORDED'));
