package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.util.List;

@Schema(
        name = "PlanejarEquipeMissaoDTO",
        description = "Plano de alocacao de equipe a uma missao: lista de participantes e papeis; "
                + "nao e entidade CRUD, somente corpo de acao de planejamento. OpenAPI 3.1 e x-ui (demo).")
public class PlanejarEquipeMissaoDTO {

    @Valid
    @Size(min = 1, max = 12)
    @UISchema(
            label = "Participantes",
            required = true,
            extraProperties = {
                    @ExtensionProperty(name = "type", value = "array"),
                    @ExtensionProperty(name = "controlType", value = "array"),
                    @ExtensionProperty(name = "array.itemType", value = "object"),
                    @ExtensionProperty(name = "array.mode", value = "cards"),
                    @ExtensionProperty(name = "array.itemSchemaRef", value = "#/components/schemas/PlanejarEquipeMissaoParticipanteDTO"),
                    @ExtensionProperty(name = "array.itemIdentityField", value = "id"),
                    @ExtensionProperty(name = "array.minItems", value = "1"),
                    @ExtensionProperty(name = "array.maxItems", value = "12"),
                    @ExtensionProperty(name = "array.addLabel", value = "Adicionar participante"),
                    @ExtensionProperty(name = "array.emptyState", value = "Nenhum participante planejado"),
                    @ExtensionProperty(name = "array.itemTitleTemplate", value = "{{funcionarioNome}} - {{papel}}"),
                    @ExtensionProperty(name = "array.deleteMode", value = "removeFromPayload"),
                    @ExtensionProperty(name = "array.operations", value = "{\"add\":true,\"edit\":true,\"remove\":true}"),
                    @ExtensionProperty(name = "array.collectionValidation.uniqueBy", value = "[\"funcionarioId\"]"),
                    @ExtensionProperty(name = "array.collectionValidation.exactlyOne", value = "{\"field\":\"principal\",\"value\":true,\"message\":\"Defina exatamente um participante principal.\"}")
            },
            icon = "flag"
    )
    @Schema(
            description = "Participantes a persistir: minimo 1, maximo 12; exatamente um `principal` na colecao.")
    private List<PlanejarEquipeMissaoParticipanteDTO> participantes;

    public List<PlanejarEquipeMissaoParticipanteDTO> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<PlanejarEquipeMissaoParticipanteDTO> participantes) {
        this.participantes = participantes;
    }
}
