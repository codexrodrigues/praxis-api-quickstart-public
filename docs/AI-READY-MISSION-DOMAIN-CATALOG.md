# Exemplo AI-ready: dominio de missoes

Este documento mostra como o catalogo semantico deve ser usado por uma LLM, por um analista de negocio e por um desenvolvedor para entender um dominio em runtime, sem ler codigo-fonte.

O exemplo canonico e `operations.missoes`, porque ele nao e um cadastro isolado: ele se conecta a equipes, bases, participantes, eventos, incidentes, sinais de socorro e acordos regulatorios.

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
- Base: local ou estrutura operacional associada a execucao.
- Equipe: grupo responsavel pela execucao.
- Participante: pessoa ou agente associado a uma missao com papel especifico.
- Evento de missao: marco cronologico que registra progresso, pausa, retomada ou mudanca relevante.
- Incidente: ocorrencia associada a risco, impacto ou desvio operacional.
- Acordo regulatorio: obrigacao ou restricao externa que afeta a missao.

## Relacionamentos esperados

Uma LLM nao deve tratar `operations.missoes` como uma tabela plana. Ela deve procurar relacoes no catalogo antes de sugerir UI, regra, validacao ou automacao.

Relacionamentos representativos:

- Missao usa ou referencia uma base operacional.
- Missao possui participantes.
- Missao possui eventos.
- Missao pode estar associada a incidentes.
- Missao pode depender de acordos regulatorios.
- Missao pode consumir contexto de equipes e membros.

Essas relacoes aparecem como `edges` no catalogo e sao a base para prompts como:

> Explique quais recursos preciso consultar antes de criar uma tela de detalhe de missao.

Resposta esperada da LLM:

- Consultar o proprio recurso de missoes.
- Recuperar participantes e eventos relacionados.
- Verificar base, equipe e contexto regulatorio quando existirem edges ou bindings publicados.
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
