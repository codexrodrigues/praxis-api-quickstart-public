Decisões de padronização — HR DTOs (Praxis API Quickstart)

- Telefone (FuncionarioDTO.telefone)
  - Validação: `^\+?\d{8,15}$` (E.164 opcional com `+`).
  - Controle: INPUT (padrão). Caso o starter passe a expor um `PHONE_INPUT`, migrar.

- URL (FuncionarioDTO.fotoPerfilUrl)
  - Tipo UI: FieldDataType.URL
  - Tamanho máx.: 300
  - Alternativa: adicionar Pattern de URL se necessário no futuro.

- Campos de texto curtos
  - Título (MissaoDTO.titulo): max 200, INPUT, required.
  - Local (MissaoDTO.local): max 200, INPUT.
  - Planeta (AmeacaDTO.planeta): max 120, INPUT.
  - Nome (AmeacaDTO.nome): max 200, INPUT, required.

- Texto longo
  - Objetivo (MissaoDTO.objetivo): TEXTAREA, max 4000.
  - Descrições e observações: TEXTAREA, max 2000–4000 conforme semântica.

- Datas e instantes
  - LocalDate: DATE_PICKER
  - OffsetDateTime: DATE_TIME_PICKER

- Números e moeda
  - Inteiros/decimais: FieldDataType.NUMBER
  - Monetários: FieldControlType.CURRENCY_INPUT

- Enums e relacionamentos
  - Enums: SELECT
  - Relacionamentos: SELECT com `valueField=id`, `displayField=label`,
    endpoint `ApiPaths.HumanResources.<RECURSO> + "/options/filter"`.

- Texto curto (padrão)
  - Limite padrão: 200 caracteres
  - controlType: INPUT
  - Exemplos: nome, local, jurisdicao, codinome, universo, nome de equipe/veículo

- URLs
  - Limite padrão: 500 caracteres
  - type: FieldDataType.URL

- FilterDTOs (strings)
  - Aplicar controlType=INPUT + maxLength
  - Padrões espelhados dos DTOs: nome 200, sigla 12, código 20, cpf 20, planeta 120, objetivo/descrição até 4000 quando aplicável.

- Faixa monetária em filtros (padrão enterprise)
  - Contrato canônico aceito no backend por campo:
    - `{ "valor": { "minPrice": 6500, "maxPrice": 15000, "currency": "BRL" } }`
  - Compatibilidade de legado ativa (normalização centralizada no starter, sem parser ad-hoc no app):
    - `valorBetween: [min, max]`
    - `valorMin` / `valorMax` top-level
  - Semântica de filtro:
    - parcial permitido (`>= min` ou `<= max`)
    - quando `min > max`, backend normaliza (swap) antes de montar predicados.
