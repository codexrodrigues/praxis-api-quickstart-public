Leitura pública de endpoints (schemas/filters/options)

Propriedades
- app.security.read-open (boolean)
  - true: libera GET/HEAD para todos os paths em `/api/**` e também schemas/filters/options/filtered/export (GET/POST) — ideal para dev.
  - false: mantém proteção padrão e usa a lista branca abaixo, se configurada.

- app.security.read-open.whitelist (CSV de padrões de caminho)
  - Padrões liberados para GET/HEAD mesmo quando `read-open=false`.
  - Padrão do projeto (exemplo/demo com dados fictícios):
    - `/api/human-resources/**`
    - `/api/*/*/schemas/**`, `/api/*/*/schema/**`
    - `/api/*/*/filters/**`, `/api/*/*/options/**`, `/api/*/*/filtered`
    - `/schemas/filtered` (agregador de schemas filtrados, usado pela UI)

Ambientes
- Dev (application-dev.properties): `app.security.read-open=true`.
- Produção: por padrão `read-open=false`, mas `app.security.read-open.whitelist` abre leitura nos paths acima.

Observações
- Escrita (POST/PUT/PATCH/DELETE) continua protegida, exceto os endpoints de filtros/options/export (POST) explicitamente liberados.
- Ajuste CORS via `app.cors.allowed-origins` conforme necessidade.
 - Consulte também: `docs/security-overview.md` (resumo do fluxo e armadilhas de patterns).
