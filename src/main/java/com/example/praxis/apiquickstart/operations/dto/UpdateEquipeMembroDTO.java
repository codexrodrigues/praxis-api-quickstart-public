package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateEquipeMembroDTO", description = "Comando para revisar o vinculo de membro de equipe, ajustando equipe, colaborador, papel ou vigencia sem alterar a identidade tecnica do registro.")
public class UpdateEquipeMembroDTO extends CreateEquipeMembroDTO {
}

