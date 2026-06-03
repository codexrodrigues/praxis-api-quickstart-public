package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.EnderecoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateEnderecoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateEnderecoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.EnderecoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Endereco;
import com.example.praxis.apiquickstart.hr.mapper.EnderecoMapper;
import com.example.praxis.apiquickstart.hr.service.EnderecoService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
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

@RestController
@ApiResource(value = ApiPaths.HumanResources.ENDERECOS, resourceKey = "human-resources.enderecos")
@ApiGroup("human-resources")
public class EnderecoController extends AbstractQuickstartCrudController<Endereco, EnderecoDTO, Integer, EnderecoFilterDTO, CreateEnderecoDTO, UpdateEnderecoDTO> {

    private final EnderecoService service;
    private final EnderecoMapper mapper;

    @Autowired
    public EnderecoController(EnderecoService service, EnderecoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected EnderecoService getService() { return service; }

    @Override
    protected EnderecoDTO toDto(Endereco entity) { return mapper.toDto(entity); }

    @Override
    protected Endereco toEntity(EnderecoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Endereco entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(EnderecoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar endereços cadastrais por colaborador e localidade", description = "Lista endereços vinculados a funcionários por titular, logradouro, número, bairro, cidade, UF e CEP para conferência cadastral, contato administrativo e rotinas internas de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<EnderecoDTO>>>> filter(@RequestBody EnderecoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer cadastro de endereços em listas extensas", description = "Navega por endereços de funcionários usando cursor, preservando filtros por titular e localidade em tabelas cadastrais, consultas de contato e fluxos administrativos de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<EnderecoDTO>>>> filterByCursor(@RequestBody EnderecoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar endereço dentro de um recorte cadastral", description = "Informa a posição de um endereço em uma lista filtrada por colaborador, logradouro ou localidade para retomada de navegação em tabelas cadastrais e atendimentos de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody EnderecoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar cadastro completo de endereços", description = "Retorna todos os endereços cadastrados como referência de localização de funcionários para conferência administrativa, exportação, sincronização e composição de telas internas de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<EnderecoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar endereços por identificadores conhecidos", description = "Recupera endereços já referenciados em formulários, atendimentos, filtros salvos ou seleções anteriores usando seus identificadores cadastrais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<EnderecoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar endereços para fluxos cadastrais de RH", description = "Produz opções compactas de endereços para campos de seleção, busca e preenchimento contextual em formulários administrativos vinculados ao funcionário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody EnderecoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de endereços já selecionadas", description = "Reidrata opções de endereços escolhidas em formulários, filtros salvos ou atendimentos de RH, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de endereço cadastral", description = "Retorna o endereço vinculado a um funcionário com logradouro, número, complemento, bairro, cidade, UF e CEP para inspeção cadastral, contato administrativo e composição de visões de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<EnderecoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar endereço de funcionário", description = "Cria um endereço cadastral vinculado ao funcionário titular para uso em contato administrativo, conferência de localização e registros internos de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EnderecoDTO>> create(@jakarta.validation.Valid @RequestBody CreateEnderecoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar endereço cadastral de funcionário", description = "Mantém dados de localização do funcionário, incluindo logradouro, número, complemento, bairro, cidade, UF e CEP para cadastros e fluxos administrativos de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EnderecoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateEnderecoDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover endereço do cadastro de funcionário", description = "Remove um endereço do cadastro administrativo quando a base de localização exige saneamento, revisão cadastral ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover endereços cadastrais em lote", description = "Remove múltiplos endereços em uma única chamada para saneamento administrativo, revisão cadastral ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









