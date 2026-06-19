# Exemplo AI-ready: dominio de missoes

Este documento mostra como o catalogo semantico deve ser usado por uma LLM, por um analista de negocio e por um desenvolvedor para entender um dominio em runtime, sem ler codigo-fonte.

O exemplo canonico e `operations.missoes`, porque ele nao e um cadastro isolado: ele se conecta diretamente a ameacas e janelas operacionais, e tambem e contextualizado por participantes, eventos, incidentes, licencas, bases, equipes e acordos regulatorios por meio de recursos relacionados e surfaces publicadas.

## O que a LLM recebe

A LLM deve receber o catalogo publicado por:

```bash
GET /schemas/domain?resourceKey=operations.missoes
```

Depois de ingerido no config-store, ela pode recuperar contexto por:

```bash
GET /api/praxis/config/domain-catalog/context?serviceKey=praxis-service&resourceKey=operations.missoes&q=status&type=governance
```

O payload AI-ready precisa conter:

- `contexts`: o bounded context e o ownership semantico do recurso.
- `nodes`: entidades, campos, operacoes e conceitos nomeados.
- `edges`: relacoes entre missoes e outros recursos do dominio.
- `bindings`: ligacoes entre semantica, endpoints HTTP, schemas e superficies.
- `evidence`: evidencias que explicam de onde a interpretacao veio.
- `governance`: classificacao, categoria de dados, compliance tags e politica de uso por IA.

## Vocabulário de negócio

`operations.missoes` representa o planejamento e acompanhamento de uma missao operacional.

Conceitos principais:

- Missao: unidade de trabalho operacional planejada, executada e acompanhada.
- Status da missao: estado de ciclo de vida usado para governar acoes possiveis.
- Prioridade: urgencia operacional para ordenacao, escalonamento e dashboards.
- Ameaca: risco principal associado a missao por lookup de entidade governada.
- Base: estrutura operacional consultada por recursos relacionados, como acessos, equipes e licencas; nao e campo direto do cadastro de missao no contrato atual.
- Equipe: grupo responsavel pela execucao, normalmente materializado por participantes, membros ou licencas relacionadas; nao e campo direto do cadastro de missao no contrato atual.
- Participante: pessoa ou agente associado a uma missao com papel especifico.
- Evento de missao: marco cronologico que registra progresso, pausa, retomada ou mudanca relevante.
- Incidente: ocorrencia associada a risco, impacto ou desvio operacional.
- Acordo regulatorio: obrigacao ou restricao externa que afeta a missao.

## Relacionamentos esperados

Uma LLM nao deve tratar `operations.missoes` como uma tabela plana. Ela deve procurar relacoes no catalogo antes de sugerir UI, regra, validacao ou automacao.

Relacionamentos representativos no contrato atual:

- Missao referencia uma ameaca por `ameacaId`, usando a option source governada `threat` do recurso `risk-intelligence.ameacas`.
- Missao possui participantes em `operations.missao-participantes`.
- Missao possui eventos em `operations.missao-eventos`.
- Missao pode estar associada a incidentes por recursos de incidentes e indicadores de risco.
- Bases, equipes, licencas e acordos regulatorios sao contexto operacional relacionado; a LLM deve consulta-los quando houver `edges`, surfaces, actions ou bindings publicados, mas nao deve inventar campos diretos em `operations.missoes` sem contrato.

Essas relacoes aparecem como `edges` no catalogo e sao a base para prompts como:

> Explique quais recursos preciso consultar antes de criar uma tela de detalhe de missao.

Resposta esperada da LLM:

- Consultar o proprio recurso de missoes.
- Recuperar participantes e eventos relacionados.
- Verificar ameaca, participantes e eventos primeiro; depois consultar base, equipe, licenca e contexto regulatorio somente quando existirem edges, surfaces ou bindings publicados para a tarefa.
- Respeitar governanca antes de exibir campos sensiveis ou gerar regras.

## Governança e uso por IA

Campos com governanca nao sao apenas metadados visuais. Eles dizem o que a IA pode ver, explicar, mascarar ou usar para sugerir regras.

Exemplo de leitura esperada:

- `classification`: sensibilidade do dado.
- `dataCategory`: categoria como operacional, pessoal, financeira ou regulatoria.
- `complianceTags`: LGPD, GDPR, politica interna ou compliance setorial.
- `aiUsage.visibility`: se a IA pode ver o valor integral, resumido ou mascarado.
- `aiUsage.trainingUse`: se o conteudo pode ser usado para treino.
- `aiUsage.ruleAuthoring`: se uma regra sugerida exige revisao humana.

Uma LLM pode sugerir uma regra de UI ou dominio, mas nao deve assumir aprovacao automatica quando `ruleAuthoring=review_required`.

## Prompt realista para analista

```text
Crie uma melhoria para a tela de missoes.
Quero destacar missoes atrasadas, mostrar participantes e eventos recentes,
e bloquear sugestoes automaticas que usem dados sensiveis sem revisao.
Use somente o catalogo semantico disponivel em runtime.
```

Com o catalogo AI-ready, a LLM deve responder em etapas:

1. Identificar `operations.missoes` como recurso principal.
2. Ler status, datas e prioridade como candidatos para destaque visual.
3. Descobrir participantes e eventos por `edges`.
4. Consultar governanca antes de usar dados pessoais ou sensiveis.
5. Propor mudancas como draft, explicando evidencias e pontos que exigem aprovacao.

## Prompt realista para desenvolvedor

```text
Adicione um campo de categorizacao operacional em missoes e prepare a UI
para que a LLM explique esse campo ao analista.
Nao leia o codigo-fonte; use o contrato publicado em runtime.
```

A resposta correta deve pedir ou produzir:

- novo campo no recurso operacional;
- descricao semantica no catalogo;
- categoria de dados e governanca;
- evidencia da origem da nova semantica;
- atualizacao de bindings para schema/UI;
- teste garantindo que o recurso continua AI-ready.

## Limite importante

Este catalogo nao executa regra de negocio. Ele prepara o terreno para que regras compartilhaveis sejam criadas depois com contexto, evidencia, ownership, aprovacao e seguranca.

Regra executavel, politica OPA/Rego, validacao transacional e autorizacao continuam sendo camadas separadas.
