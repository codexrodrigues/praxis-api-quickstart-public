package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.IdentidadeSecretaDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateIdentidadeSecretaDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateIdentidadeSecretaDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.IdentidadeSecretaFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.IdentidadeSecreta;
import com.example.praxis.apiquickstart.hr.mapper.IdentidadeSecretaMapper;
import com.example.praxis.apiquickstart.hr.service.IdentidadeSecretaService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@ApiResource(value = ApiPaths.HumanResources.IDENTIDADES_SECRETAS, resourceKey = "human-resources.identidades-secretas")
@ApiGroup("human-resources")
public class IdentidadeSecretaController extends AbstractQuickstartCrudController<IdentidadeSecreta, IdentidadeSecretaDTO, Integer, IdentidadeSecretaFilterDTO, CreateIdentidadeSecretaDTO, UpdateIdentidadeSecretaDTO> {

    private final IdentidadeSecretaService service;
    private final IdentidadeSecretaMapper mapper;

    @Autowired
    public IdentidadeSecretaController(IdentidadeSecretaService service, IdentidadeSecretaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected IdentidadeSecretaService getService() { return service; }

    @Override
    protected IdentidadeSecretaDTO toDto(IdentidadeSecreta entity) { return mapper.toDto(entity); }

    @Override
    protected IdentidadeSecreta toEntity(IdentidadeSecretaDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(IdentidadeSecreta entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(IdentidadeSecretaDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar identidades operacionais por colaborador, codinome e exposição", description = "Lista codinomes vinculados a funcionários por titular, universo narrativo e exposição pública para diretórios internos, perfis protegidos e controle administrativo de sigilo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<IdentidadeSecretaDTO>>>> filter(@RequestBody IdentidadeSecretaFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer identidades operacionais em listas extensas", description = "Navega por identidades secretas usando cursor, preservando filtros de colaborador, codinome, universo e exposição pública em consultas incrementais e telas de administração de sigilo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<IdentidadeSecretaDTO>>>> filterByCursor(@RequestBody IdentidadeSecretaFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar identidade secreta dentro de um recorte de sigilo", description = "Informa a posição de uma identidade operacional em lista filtrada por colaborador, codinome, universo ou exposição pública para retomada de navegação em tabelas e diretórios internos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody IdentidadeSecretaFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar cadastro completo de identidades secretas", description = "Retorna todas as identidades operacionais cadastradas como referência de codinomes, universos e vínculos com funcionários para diretórios internos, auditoria e administração de sigilo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<IdentidadeSecretaDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar identidades secretas por identificadores conhecidos", description = "Recupera identidades operacionais já referenciadas em perfis, formulários, filtros salvos ou fluxos administrativos usando seus identificadores do cadastro de sigilo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<IdentidadeSecretaDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar identidades secretas para perfis protegidos", description = "Produz opções compactas de identidades operacionais para campos de seleção, busca, montagem de perfil protegido e filtros de exposição pública.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody IdentidadeSecretaFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de identidades secretas já selecionadas", description = "Reidrata opções de identidades operacionais escolhidas em formulários, perfis, filtros salvos ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de identidade secreta", description = "Retorna o vínculo entre funcionário, codinome, universo e exposição pública para consulta de sigilo, exibição de perfil protegido e auditoria administrativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<IdentidadeSecretaDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar identidade secreta de funcionário", description = "Cria uma identidade operacional com funcionário titular, codinome, universo e exposição pública para uso em perfis protegidos, diretórios internos e controle de sigilo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<IdentidadeSecretaDTO>> create(@jakarta.validation.Valid @RequestBody CreateIdentidadeSecretaDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar identidade secreta de funcionário", description = "Mantém codinome, universo, exposição pública e vínculo com funcionário para perfis protegidos, diretórios internos e administração de sigilo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<IdentidadeSecretaDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateIdentidadeSecretaDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover identidade secreta do cadastro", description = "Remove uma identidade operacional do cadastro administrativo quando os perfis protegidos exigem saneamento, revisão de sigilo ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover identidades secretas em lote", description = "Remove várias identidades operacionais em uma única chamada para saneamento administrativo, revisão de sigilo ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









