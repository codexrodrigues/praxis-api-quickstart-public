Visão Geral de Segurança (roteamento e liberação)

Flags principais (env → application.properties)
- app.security.csrf.disable (APP_SECURITY_CSRF_DISABLE)
  - true em dev: desativa CSRF para facilitar chamadas sem X-XSRF-TOKEN.
  - false em prod: mantém proteção.
- app.security.read-open (APP_SECURITY_READ_OPEN)
  - true: libera GET/HEAD para /api/** e expõe endpoints de consulta úteis inclusive via POST (`filter`, `filter/cursor`, `locate`, `filtered`, `export`, `options` e `stats`).
  - false: mantém proteção e usa a whitelist abaixo.
- app.security.read-open.whitelist (APP_SECURITY_READ_OPEN_WHITELIST)
  - CSV de padrões para GET/HEAD públicos em prod.
  - Padrão do projeto: /api/human-resources/**, /api/*/*/schemas/**, /api/*/*/schema/**, /api/*/*/filters/**, /api/*/*/options/**, /api/*/*/filtered, /schemas/**
- app.security.write-disabled (APP_SECURITY_WRITE_DISABLED)
  - true: nega POST/PUT/PATCH/DELETE em /api/** (exceto permissões explícitas de filtros/options/filtered/export acima).
  - Útil para demos on-line.
- app.security.schemas-aggregator.enabled (APP_SECURITY_SCHEMAS_AGGREGATOR_ENABLED)
  - true: torna públicos `GET /schemas/**` e `POST /schemas/filtered`.
- app.security.demo-allow-bulk-actions (APP_SECURITY_DEMO_ALLOW_BULK_ACTIONS)
  - true: libera POST em `/api/*/*/actions/**` (ex.: `/api/human-resources/eventos-folha/actions/bulk-approve`) mesmo quando `write-disabled=true` — útil para demonstração do fluxo de ações em lote.
- app.cors.allowed-origins (CORS_ALLOWED_ORIGINS)
  - CSV de origens da UI. Use origens explícitas para chamadas com cookies/credenciais; `*` permanece permitido apenas sem credenciais.
  - O host expõe por CORS os headers canônicos de contrato `ETag`, `X-Schema-Hash` e `X-Data-Version`, permitindo que UIs leiam cache/revalidação, hash de schema e versão lógica de dados sem duplicar a semântica dos starters.

Workflow actions tipadas
- O quickstart publica workflow actions tipadas no endpoint piloto `POST /api/human-resources/eventos-folha/actions/bulk-approve`.
- Nesta migração piloto o workflow permanece sem enforcement RBAC dedicado; o catálogo publica apenas hints de estado (`allowedStates`) e a política de autorização corporativa continua fora do escopo do host de referência.
- O login demo (`POST /auth/login` com o usuário admin) emite um cookie `SESSION` autenticado suficiente para exercitar o fluxo do piloto e o restante da proteção HTTP/CSRF.

Leitura arquitetural correta:
- `workflow action` significa comando de negócio explícito, não CRUD disfarçado.
- a segurança HTTP protege o endpoint real; o catálogo semântico apenas publica que a action existe e em quais estados ela faz sentido.
- `capabilities` continuam sendo snapshot agregado para a UI; não substituem o enforcement do endpoint.
- `_links` e catálogos publicados são affordances HTTP reais da plataforma, não documentação decorativa.

Fluxo de liberação por perfil
- Público (sempre):
  - /, /index.html, /favicon.ico, /assets/**
  - /swagger-ui.html, /swagger-ui/**, /v3/api-docs, /v3/api-docs/**, /v3/api-docs.yaml
  - /actuator/health, /actuator/health/**
  - /auth/login, /auth/logout, /auth/session
- Dev (read-open=true):
  - GET/HEAD: /api/**
  - GET extra: /api/*/*/schemas/**, /api/*/*/schema/**, /api/*/*/filters/**, /api/*/*/options/**, /api/*/*/filtered (+ /schemas/filtered se habilitado)
  - POST extra: /api/*/*/filters/**, /api/*/*/filter, /api/*/*/filter/cursor, /api/*/*/locate, /api/*/*/filtered, /api/*/*/export, /api/*/*/options/**, /api/*/*/stats/** (+ /schemas/filtered se habilitado)
- Prod (read-open=false):
  - GET/HEAD: somente os padrões de `app.security.read-open.whitelist`.
  - Recomendada a whitelist padrão do projeto para facilitar integração com UIs.

Armadilhas de Path Patterns (Spring 6)
- Não use '/**/' no meio do path. Em PathPattern, '**' só pode aparecer sem dados após ele (tipicamente no final do padrão). Exemplos inválidos: `/api/**/filters/**`, `/api/**/schemas/**`.
- Em vez disso, use o nível esperado: `/api/*/*/filters/**`, `/api/*/*/schemas/**` (cobre `/api/{grupo}/{recurso}/...`).
- Para variáveis multi-match `{*var}`, não coloque dados depois do '}'.

Receitas
- Dev (liberar UI sem login inicialmente):
  - APP_SECURITY_READ_OPEN=true
  - APP_SECURITY_CSRF_DISABLE=true
  - APP_SECURITY_WRITE_DISABLED=false
  - APP_SECURITY_SCHEMAS_AGGREGATOR_ENABLED=true
  - CORS_ALLOWED_ORIGINS=http://localhost:4200,http://127.0.0.1:4200
  - Headers legíveis no browser: `ETag`, `X-Schema-Hash`, `X-Data-Version`.
- Prod (seguro por padrão, leitura pública limitada):
  - APP_SECURITY_READ_OPEN=false
  - APP_SECURITY_READ_OPEN_WHITELIST=/api/human-resources/**,/api/*/*/schemas/**,/api/*/*/schema/**,/api/*/*/filters/**,/api/*/*/options/**,/api/*/*/filtered,/schemas/**
  - APP_SECURITY_WRITE_DISABLED=true (opcional para demo read-only)

Dica para UI (Angular/Vite) em dev
- Proxie também '/schemas' para o backend além de '/api', ex. `proxy.conf.json`:
```
{
  "/api": { "target": "http://localhost:8088", "secure": false, "changeOrigin": true },
  "/schemas": { "target": "http://localhost:8088", "secure": false, "changeOrigin": true }
}
```
