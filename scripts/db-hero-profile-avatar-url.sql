-- Publishes the employee profile photo in the 360 profile read projection.
--
-- Why this exists:
-- /api/human-resources/funcionarios/{id}/hero-profile is rendered by Praxis UI
-- from the backend-published projection. The dynamic form cannot display the
-- profile photo unless the projection itself exposes an avatar field.

create or replace view public.vw_perfil_heroi as
select
    f.id as funcionario_id,
    f.nome_completo,
    i.codinome,
    i.universo,
    i.exposicao_publica,
    c.nome as cargo,
    d.nome as departamento,
    r.score_publico,
    r.score_governamental,
    ((r.score_publico + r.score_governamental)::numeric / 2::numeric) as score_medio,
    coalesce(string_agg(h.nome, ', '::text order by h.nome), '-'::text) as habilidades,
    e.nome as equipe_principal,
    b.nome as base_principal,
    f.foto_perfil_url as avatar_url
from public.funcionarios f
left join public.cargos c on c.id = f.cargo_id
left join public.departamentos d on d.id = f.departamento_id
left join public.identidades_secretas i on i.funcionario_id = f.id
left join public.funcionario_habilidades fh on fh.funcionario_id = f.id
left join public.habilidades h on h.id = fh.habilidade_id
left join public.equipe_membros em on em.funcionario_id = f.id
left join public.equipes e on e.id = em.equipe_id
left join public.bases b on e.base_principal_id = b.id
left join public.reputacoes r on r.funcionario_id = f.id
group by
    f.id,
    f.foto_perfil_url,
    f.nome_completo,
    i.codinome,
    i.universo,
    i.exposicao_publica,
    c.nome,
    d.nome,
    r.score_publico,
    r.score_governamental,
    e.nome,
    b.nome;

comment on view public.vw_perfil_heroi is 'Perfil completo do heroi com foto, cargo, equipe, base e habilidades.';
