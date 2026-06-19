package com.example.praxis.apiquickstart.operations.dto;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UpdateSinaisSocorroDTO", description = "Comando para revisar um sinal de socorro existente, atualizando severidade, estado, localizacao ou narrativa de resposta mantendo sua identidade.")
public class UpdateSinaisSocorroDTO extends CreateSinaisSocorroDTO {
}

