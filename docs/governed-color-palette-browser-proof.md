# Prova browser de paletas governadas

Esta prova conecta o Color Lab ao Quickstart real, com login do host, cookie JWT,
CORS, restrição de Origin, autorização por papel e persistência PostgreSQL. Não
intercepta respostas de paleta no navegador e não substitui o repositório de
aprovações. A única dependência simulada é o vector store de IA, fora deste fluxo.

## Execução local

Pré-requisitos: dependências locais instaladas e Config Starter deste corte instalado
no Maven local. As portas isoladas `8088` (Quickstart) e `4014` (Color Lab) devem
estar livres. O teste inicia e encerra o próprio host Angular; não reutiliza nem
interrompe o servidor de desenvolvimento do usuário. Usar o worktree correto em
`praxis.palette.ui-root`.

```sh
mvn -q -Dpraxis.palette.browser=true \
  -Dpraxis.palette.ui-root=/caminho/absoluto/do/worktree/ui \
  -Dtest=GovernedColorPaletteBrowserPostgresTest test
```

O teste cria PostgreSQL descartável, inicia Angular com proxy focal para o Quickstart
e gera credenciais efêmeras para as seis identidades do governance lab. O navegador
recebe a senha somente por ambiente de processo, nunca por arquivo versionado ou
resposta HTTP. O cookie exclusivo do teste não substitui a sessão normal do usuário.

## Jornada coberta

1. Negar leitura anônima e origem não autorizada.
2. Entrar como autor, validar preview e criar v1 em draft.
3. Negar publicação ao autor e bloquear publicação de draft pelo publicador.
4. Propor como autor, aprovar como aprovador e publicar como publicador.
5. Publicar e listar a família light/dark/high-contrast em ordem determinística, e carregar a
   publicação nos três controles do Color Lab por HTTP cross-origin.
6. Revalidar pelo ETag; conferir 304 na rede, mesmo quando Chromium entrega ao
   JavaScript uma resposta 200 recomposta com o corpo em cache.
7. Publicar v2, preservar corpo/ETag de v1 e carregar a nova versão na vitrine.
8. Selecionar uma categoria semântica, editar versão/ETag do picker, salvar,
   conferir a paleta histórica e reabrir o editor em viewport estreita.
9. Recarregar a página com a referência fixada e comprovar falha fechada sem sessão.
10. Retirar as decisões como aprovador, conferir as aprovações no banco e encerrar
    processos/banco descartáveis.

Logs: `target/palette-browser.log` e Surefire. Capturas: diretório
`test-results/palette-http` do worktree Angular.

## Limites importantes

- A rota sem query continua sendo demonstração local. A rota
  `/color-lab?paletteKey=<chave>&version=<versão>` consome o Quickstart; omitir
  `version` consulta a última publicação. Falhas não acionam a fixture local.
- Os editores da vitrine mantêm metadata em memória. Salvar metadata não publica
  uma decisão nem prova persistência remota da configuração do componente.
- A prova aplica migrations canônicas focais V5, V7, V9, V20, V22–V25, V36–V37,
  V41, V45–V49, V55, V58–V59 e V61–V62. As três tabelas de domínios externos são pré-requisitos
  mínimos. Não é upgrade completo do Config/pgvector nem certificação do banco
  operacional do host ou de um deployment publicado.
- CORS, Origin, CSRF e rate limiting permanecem configurados no host. Os endpoints
  Config seguem a exceção CSRF já existente; nenhuma exceção foi criada no teste.
- Governance Lab é autenticação real do host de referência, não integração com
  um IdP corporativo. Nenhuma chamada paga de IA é feita.
