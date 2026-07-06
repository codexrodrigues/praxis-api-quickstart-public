package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.FuncionarioDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateFuncionarioDTO;
import com.example.praxis.apiquickstart.hr.dto.DependenteDTO;
import com.example.praxis.apiquickstart.hr.dto.EnderecoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFuncionarioDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFuncionarioProfileDTO;
import com.example.praxis.apiquickstart.hr.dto.VwAnalyticsFolhaPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.VwPerfilHeroiDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FuncionarioFilterDTO;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.hr.mapper.FuncionarioMapper;
import com.example.praxis.apiquickstart.hr.service.DependenteService;
import com.example.praxis.apiquickstart.hr.service.EnderecoService;
import com.example.praxis.apiquickstart.hr.service.FuncionarioService;
import com.example.praxis.apiquickstart.hr.service.VwAnalyticsFolhaPagamentoService;
import com.example.praxis.apiquickstart.hr.service.VwPerfilHeroiService;
import com.example.praxis.apiquickstart.operations.dto.MissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.service.MissaoParticipanteService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Links;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.praxisplatform.uischema.surface.RelatedResourceChildOperation;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import java.util.List;

/**
 * Recurso mutavel de funcionarios usado como referencia principal de CRUD no quickstart.
 *
 * <p>Este controller e um dos melhores exemplos do projeto para entender o baseline da plataforma
 * em um recurso de negocio comum: CRUD canonico com {@code @ApiResource}, schemas e options
 * derivados do starter, surface parcial orientada a caso de uso real e hypermedia de resposta.</p>
 *
 * <p>Por isso, ele deve permanecer especialmente legivel para leitores que estejam aprendendo como
 * modelar um recurso de RH metadata-driven dentro da Praxis.</p>
 */
@RestController
@ApiResource(
        value = ApiPaths.HumanResources.FUNCIONARIOS,
        resourceKey = "human-resources.funcionarios",
        title = "Funcionários",
        description = "Pessoas, perfis, vínculos, reputação e participação operacional que sustentam a execução do serviço.",
        icon = "users",
        visualTone = "human-resources"
)
@ApiGroup("human-resources")
public class FuncionarioController extends AbstractQuickstartCrudController<Funcionario, FuncionarioDTO, Integer, FuncionarioFilterDTO, CreateFuncionarioDTO, UpdateFuncionarioDTO> {

    private final FuncionarioService service;
    private final FuncionarioMapper mapper;
    private final VwPerfilHeroiService perfilHeroiService;
    private final VwAnalyticsFolhaPagamentoService analyticsFolhaPagamentoService;
    private final MissaoParticipanteService missaoParticipanteService;
    private final DependenteService dependenteService;
    private final EnderecoService enderecoService;

    @Autowired
    public FuncionarioController(
            FuncionarioService service,
            FuncionarioMapper mapper,
            VwPerfilHeroiService perfilHeroiService,
            VwAnalyticsFolhaPagamentoService analyticsFolhaPagamentoService,
            MissaoParticipanteService missaoParticipanteService,
            DependenteService dependenteService,
            EnderecoService enderecoService
    ) {
        this.service = service;
        this.mapper = mapper;
        this.perfilHeroiService = perfilHeroiService;
        this.analyticsFolhaPagamentoService = analyticsFolhaPagamentoService;
        this.missaoParticipanteService = missaoParticipanteService;
        this.dependenteService = dependenteService;
        this.enderecoService = enderecoService;
    }

    @Override
    protected FuncionarioService getService() {
        return service;
    }

    @Override
    protected FuncionarioDTO toDto(Funcionario entity) { return mapper.toDto(entity); }

    @Override
    protected Funcionario toEntity(FuncionarioDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Funcionario entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(FuncionarioDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar funcionários", description = "Lista colaboradores por identificação, cargo, departamento, situação ativa e contatos para consulta cadastral, composição organizacional e seleção operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<FuncionarioDTO>>>> filter(@RequestBody FuncionarioFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar funcionários com paginação por cursor", description = "Percorre grandes cadastros de pessoas usando cursor, útil para diretórios corporativos, tabelas extensas e navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<FuncionarioDTO>>>> filterByCursor(@RequestBody FuncionarioFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar funcionário em listas filtradas", description = "Informa em qual posição um funcionário aparece dentro do recorte filtrado, útil para retornar ao registro em catálogos e tabelas de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody FuncionarioFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar funcionários", description = "Retorna o cadastro completo de funcionários quando o consumidor precisa materializar toda a base de pessoas para conferência, exportação ou sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<FuncionarioDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar funcionários por IDs", description = "Recupera funcionários já referenciados por vínculos, formulários ou seleções anteriores usando uma lista conhecida de identificadores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<FuncionarioDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar funcionários para formulários", description = "Produz opções compactas de funcionários para campos de seleção, autocomplete e vínculos organizacionais orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody FuncionarioFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar funcionários selecionados", description = "Reidrata opções de funcionários já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter funcionário", description = "Retorna o detalhe de um funcionário para inspeção cadastral, auditoria de vínculo e composição de visões dependentes de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<FuncionarioDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar funcionário", description = "Cadastra uma nova pessoa vinculada à organização com dados de identificação, lotação, situação ativa, remuneração base e contatos para uso no domínio de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<FuncionarioDTO>> create(@jakarta.validation.Valid @RequestBody CreateFuncionarioDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar funcionário", description = "Atualiza dados cadastrais e organizacionais do funcionário preservando a coerência do cadastro, vínculos e históricos dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<FuncionarioDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateFuncionarioDTO dto) {
        return super.update(id, dto);
    }

    @PatchMapping("/{id}/profile")
    @UiSurface(
            id = "profile",
            kind = SurfaceKind.PARTIAL_FORM,
            scope = SurfaceScope.ITEM,
            title = "Atualizar perfil de contato",
            description = "Atualiza dados de apresentação e contato do funcionário usados em diretórios internos, comunicação operacional e atendimento de RH.",
            intent = "employee-contact-profile-maintenance",
            order = 20,
            tags = {"human-resources", "employee-profile", "contact-maintenance", "partial-update"}
    )
    @ResourceIntent(
            id = "employee-contact-profile-maintenance",
            title = "Manutenção de perfil de contato",
            description = "Mantém informações de apresentação e contato do funcionário para diretórios internos, comunicação operacional e atendimento de RH.",
            order = 20
    )
    @Operation(summary = "Atualizar perfil de contato do funcionário", description = "Ajusta dados de apresentação e contato usados em diretórios internos, comunicação operacional e atendimento de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<FuncionarioDTO>> updateProfile(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody UpdateFuncionarioProfileDTO dto
    ) {
        FuncionarioDTO updated = service.updateProfile(id, dto);
        // Publica links suficientes para que o consumidor consiga reentrar no recurso e no schema
        // da surface parcial sem conhecer detalhes internos do host.
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/profile", "patch", "request")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(updated, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/hero-profile")
    @UiSurface(
            id = "hero-profile",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Perfil 360",
            description = "Consolida identidade operacional, reputação, habilidades, equipe e base principal do funcionário para leitura contextual de RH.",
            intent = "employee-360",
            order = 40,
            tags = {"human-resources", "employee-360", "read-projection", "profile-context", "related-resource"},
            relatedChildResourceKey = "human-resources.vw-perfil-heroi",
            relatedChildResourcePath = ApiPaths.HumanResources.VW_PERFIL_HEROI,
            relatedChildParentField = "funcionarioId",
            relatedSelectable = true,
            relatedSelectionKeyField = "funcionarioId"
    )
    @ResourceIntent(
            id = "employee-360",
            title = "Perfil 360 do funcionário",
            description = "Apresenta uma leitura consolidada do funcionário para descoberta, contexto operacional e tomada de decisão em RH.",
            order = 40
    )
    @Operation(summary = "Obter perfil 360 do funcionário", description = "Retorna a visão consolidada do funcionário com identidade operacional, reputação, habilidades, equipe e base principal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil 360 retornado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Perfil 360 não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VwPerfilHeroiDTO>> getHeroProfile(@PathVariable Integer id) {
        VwPerfilHeroiDTO profile = perfilHeroiService.findById(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/hero-profile", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(profile, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/payroll-history")
    @UiSurface(
            id = "payroll-history",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Histórico de folha",
            description = "Apresenta ciclos recentes de folha de pagamento do funcionário com valores, eventos e classificações analíticas para acompanhamento de RH.",
            intent = "employee-payroll-intelligence",
            order = 50,
            tags = {"human-resources", "payroll", "folha-de-pagamento", "pagamento", "analytics", "read-projection", "related-resource"},
            relatedChildResourceKey = "human-resources.vw-analytics-folha-pagamento",
            relatedChildResourcePath = ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
            relatedChildParentField = "funcionarioId",
            relatedSelectable = true,
            relatedSelectionKeyField = "folhaPagamentoId"
    )
    @ResourceIntent(
            id = "employee-payroll-intelligence",
            title = "Inteligência de folha do funcionário",
            description = "Resume a evolução financeira recente do funcionário para acompanhamento analítico de folha e apoio a decisões de RH.",
            order = 50
    )
    @Operation(summary = "Obter histórico analítico de folha do funcionário", description = "Retorna os últimos registros analíticos de folha associados ao funcionário selecionado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico analítico retornado com sucesso.")
    })
    public ResponseEntity<RestApiResponse<List<VwAnalyticsFolhaPagamentoDTO>>> getPayrollHistory(@PathVariable Integer id) {
        List<VwAnalyticsFolhaPagamentoDTO> payrollHistory = analyticsFolhaPagamentoService.findLatestPayrollByFuncionarioId(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/payroll-history", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(payrollHistory, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/mission-participations")
    @UiSurface(
            id = "mission-participations",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Participações em missões",
            description = "Lista missões, papéis e resultados operacionais em que o funcionário participou para leitura contextual de RH e operações.",
            intent = "employee-mission-participations",
            order = 60,
            tags = {"human-resources", "operations", "missions", "mission-participations", "read-projection", "related-resource"},
            relatedChildResourceKey = "operations.missao-participantes",
            relatedChildResourcePath = ApiPaths.Operations.MISSAO_PARTICIPANTES,
            relatedChildParentField = "funcionarioId",
            relatedSelectable = true,
            relatedSelectionKeyField = "id",
            relatedChildOperations = {
                    RelatedResourceChildOperation.FILTER,
                    RelatedResourceChildOperation.LIST,
                    RelatedResourceChildOperation.CREATE,
                    RelatedResourceChildOperation.UPDATE,
                    RelatedResourceChildOperation.DELETE
            }
    )
    @ResourceIntent(
            id = "employee-mission-participations",
            title = "Missões do funcionário",
            description = "Apresenta as participações operacionais associadas ao funcionário para contexto, auditoria e acompanhamento corporativo.",
            order = 60
    )
    @Operation(summary = "Obter participações em missões do funcionário", description = "Retorna os vínculos de missões associados ao funcionário selecionado, incluindo título da missão, papel, ordem, liderança e resultado operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participações em missões retornadas com sucesso.")
    })
    public ResponseEntity<RestApiResponse<List<MissaoParticipanteDTO>>> getMissionParticipations(@PathVariable Integer id) {
        List<MissaoParticipanteDTO> participations = missaoParticipanteService.findByFuncionarioIdForEmployeeSurface(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/mission-participations", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(participations, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/dependents")
    @UiSurface(
            id = "dependents",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Dependentes e elegibilidade",
            description = "Lista dependentes vinculados ao funcionário para benefícios, conferência cadastral, atendimento de RH e governança de dados pessoais.",
            intent = "employee-dependent-governance",
            order = 70,
            tags = {"human-resources", "employee-governance", "dependents", "benefits", "privacy", "read-projection", "related-resource"},
            relatedChildResourceKey = "human-resources.dependentes",
            relatedChildResourcePath = ApiPaths.HumanResources.DEPENDENTES,
            relatedChildParentField = "funcionarioId",
            relatedSelectable = true,
            relatedSelectionKeyField = "id",
            relatedChildOperations = {
                    RelatedResourceChildOperation.FILTER,
                    RelatedResourceChildOperation.LIST,
                    RelatedResourceChildOperation.CREATE,
                    RelatedResourceChildOperation.UPDATE,
                    RelatedResourceChildOperation.DELETE
            }
    )
    @ResourceIntent(
            id = "employee-dependent-governance",
            title = "Governança de dependentes do funcionário",
            description = "Mostra vínculos familiares e dependentes para validar benefícios, elegibilidade e cuidados de privacidade dentro do cadastro de RH.",
            order = 70
    )
    @Operation(summary = "Obter dependentes do funcionário", description = "Retorna os dependentes vinculados ao funcionário selecionado para leitura de elegibilidade, benefícios e auditoria cadastral.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dependentes retornados com sucesso.")
    })
    public ResponseEntity<RestApiResponse<List<DependenteDTO>>> getDependents(@PathVariable Integer id) {
        List<DependenteDTO> dependents = dependenteService.findByFuncionarioIdForEmployeeSurface(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/dependents", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(dependents, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/address")
    @UiSurface(
            id = "address",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Endereço cadastral",
            description = "Mostra a localização cadastral do funcionário para atendimento de RH, contato administrativo e análise territorial com cuidado de privacidade.",
            intent = "employee-address-governance",
            order = 80,
            tags = {"human-resources", "employee-governance", "address", "territory", "privacy", "read-projection", "related-resource"},
            relatedChildResourceKey = "human-resources.enderecos",
            relatedChildResourcePath = ApiPaths.HumanResources.ENDERECOS,
            relatedChildParentField = "funcionarioId",
            relatedSelectable = true,
            relatedSelectionKeyField = "id",
            relatedChildOperations = {
                    RelatedResourceChildOperation.FILTER,
                    RelatedResourceChildOperation.LIST,
                    RelatedResourceChildOperation.CREATE,
                    RelatedResourceChildOperation.UPDATE,
                    RelatedResourceChildOperation.DELETE
            }
    )
    @ResourceIntent(
            id = "employee-address-governance",
            title = "Governança de endereço do funcionário",
            description = "Expõe a composição cadastral de localização do funcionário preservando a leitura de privacidade e uso administrativo.",
            order = 80
    )
    @Operation(summary = "Obter endereço do funcionário", description = "Retorna o endereço cadastral associado ao funcionário selecionado para atendimento administrativo e contexto territorial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço retornado com sucesso.")
    })
    public ResponseEntity<RestApiResponse<List<EnderecoDTO>>> getAddress(@PathVariable Integer id) {
        List<EnderecoDTO> addresses = enderecoService.findByFuncionarioIdForEmployeeSurface(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/address", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(addresses, hateoasOrNull(links)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover funcionário", description = "Exclui o registro do funcionário do cadastro operacional de RH conforme política administrativa do host.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover funcionários em lote", description = "Exclui múltiplos registros de funcionários em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}



