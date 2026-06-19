package com.example.praxis.apiquickstart.operations.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
@JsonIgnoreProperties({"id", "ameacaNome"})
@Schema(
    name = "CreateMissaoDTO",
    description = "Comando para abrir uma missao operacional com titulo, objetivo, prioridade, status inicial, local, ameaca governada e janela prevista. A missao criada ainda nao inclui participantes, eventos ou contexto regulatorio derivado; esses recursos sao relacionados por surfaces e actions proprias."
)
public class CreateMissaoDTO extends MissaoDTO {
}

