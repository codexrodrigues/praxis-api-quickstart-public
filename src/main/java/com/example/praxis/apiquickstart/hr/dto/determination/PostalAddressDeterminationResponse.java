package com.example.praxis.apiquickstart.hr.dto.determination;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado deterministico do diretorio postal demonstrativo do host.
 *
 * <p>Os campos derivados continuam sujeitos a nova validacao no comando final de create/update.</p>
 */
@Schema(
        name = "PostalAddressDeterminationResponse",
        description = "Campos de localizacao derivados deterministicamente do CEP pelo backend, sem persistir o formulario.")
public record PostalAddressDeterminationResponse(
        @Schema(description = "Via publica associada ao CEP no diretorio do host.", example = "Avenida Paulista")
        String logradouro,
        @Schema(description = "Bairro associado ao CEP no diretorio do host.", example = "Bela Vista")
        String bairro,
        @Schema(description = "Municipio associado ao CEP no diretorio do host.", example = "Sao Paulo")
        String cidade,
        @Schema(description = "Unidade federativa associada ao CEP no diretorio do host.", example = "SP")
        String estado,
        @Schema(description = "Versao imutavel do conjunto de fatos usado para a derivacao.", example = "quickstart-postal-directory-v1")
        String decisionVersion
) {
}
