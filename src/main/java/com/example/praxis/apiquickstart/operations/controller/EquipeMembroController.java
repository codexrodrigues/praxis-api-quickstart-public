package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.EquipeMembroDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateEquipeMembroDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateEquipeMembroDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.EquipeMembroFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.EquipeMembro;
import com.example.praxis.apiquickstart.operations.mapper.EquipeMembroMapper;
import com.example.praxis.apiquickstart.operations.service.EquipeMembroService;
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
@ApiResource(
        value = ApiPaths.Operations.EQUIPE_MEMBROS,
        resourceKey = "operations.equipe-membros",
        title = "Membros de equipes",
        description = "Vínculos de colaboradores com equipes, papéis e vigências que sustentam escala, responsabilidade e capacidade operacional.",
        icon = "user-round-check",
        visualTone = "operations"
)
@ApiGroup("operations")
/**
 * Controller de referência para vínculos entre equipes e seus membros.
 *
 * <p>Este recurso é didático porque mostra um caso clássico de relacionamento
 * operacional no qual a plataforma precisa expor filtros por papel, período e
 * status sem criar um contrato HTTP especial. No quickstart, ele orienta o uso
 * do padrão metadata-driven para tabelas de composição, seleção contextual e
 * reaproveitamento do vínculo por outros fluxos.</p>
 */
public class EquipeMembroController extends AbstractQuickstartCrudController<EquipeMembro, EquipeMembroDTO, Integer, EquipeMembroFilterDTO, CreateEquipeMembroDTO, UpdateEquipeMembroDTO> {

    private final EquipeMembroService service;
    private final EquipeMembroMapper mapper;

    @Autowired
    public EquipeMembroController(EquipeMembroService service, EquipeMembroMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected EquipeMembroService getService() { return service; }

    @Override
    protected EquipeMembroDTO toDto(EquipeMembro entity) { return mapper.toDto(entity); }

    @Override
    protected EquipeMembro toEntity(EquipeMembroDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(EquipeMembro entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(EquipeMembroDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar vínculos de membros de equipe", description = "Lista vínculos entre equipes e participantes por papel, status, período e missão associada para gestão de composição operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<EquipeMembroDTO>>>> filter(@RequestBody EquipeMembroFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar vínculos de membros de equipe com paginação por cursor", description = "Percorre vínculos de equipe em catálogos extensos usando cursor, útil para escalas operacionais e telas com navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<EquipeMembroDTO>>>> filterByCursor(@RequestBody EquipeMembroFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar vínculo de membro de equipe em listas filtradas", description = "Informa em qual posição um vínculo aparece dentro do recorte filtrado, útil para retomar a análise de composição em tabelas operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody EquipeMembroFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar vínculos de membros de equipe", description = "Retorna o cadastro completo de vínculos entre equipes e participantes quando o consumidor precisa materializar a composição operacional integral.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<EquipeMembroDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar vínculos de membros de equipe por IDs", description = "Recupera vínculos já referenciados por outro fluxo sem reaplicar filtros de equipe, pessoa ou papel.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<EquipeMembroDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar vínculos de membros de equipe para formulários", description = "Produz opções compactas de vínculos para formulários administrativos, lookup e seleção contextual dentro do domínio de equipes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody EquipeMembroFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar vínculos de membros de equipe selecionados", description = "Reidrata opções de vínculos já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter vínculo de membro de equipe", description = "Retorna o detalhe de um vínculo para inspeção de papel, vigência e participação na estrutura operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<EquipeMembroDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar vínculo de membro de equipe", description = "Cadastra a associação entre equipe e participante com papel, vigência e contexto operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EquipeMembroDTO>> create(@jakarta.validation.Valid @RequestBody CreateEquipeMembroDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar vínculo de membro de equipe", description = "Atualiza papel e metadados do vínculo sem alterar sua identidade na composição operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EquipeMembroDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateEquipeMembroDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover vínculo de membro de equipe", description = "Exclui a associação entre participante e equipe quando ela não deve mais compor escalas ou missões.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover vínculos de membros de equipe em lote", description = "Exclui múltiplos vínculos em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}











