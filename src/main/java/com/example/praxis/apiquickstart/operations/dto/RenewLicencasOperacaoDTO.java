package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.LicencaNivel;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

@Schema(
        name = "RenewLicencasOperacaoDTO",
        description = "Corpo de renovacao de licenca: nova classe e janela de vigencia (acao de domínio, nao filtro). "
                + "Materializa a revisao de autorizacao operacional sem criar uma nova licenca independente.")
public class RenewLicencasOperacaoDTO {

    @NotNull
    @UISchema(label = "Nivel", controlType = FieldControlType.SELECT, required = true, order = 10, icon = "trending_up")
    @Schema(
            description = "Nivel requerido apos aprovacao; LicencaNivel.")
    private LicencaNivel nivel;

    @NotNull
    @UISchema(label = "Valido De", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, required = true, order = 20, icon = "event")
    @Schema(
            description = "Inicio do novo periodo de validade.")
    private LocalDate validoDe;

    @UISchema(label = "Valido Ate", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, order = 30, icon = "event")
    @Schema(
            description = "Fim do periodo; nulo se a renovacao ainda nao tiver teto (politica do tenant).")
    private LocalDate validoAte;

    public LicencaNivel getNivel() {
        return nivel;
    }

    public void setNivel(LicencaNivel nivel) {
        this.nivel = nivel;
    }

    public LocalDate getValidoDe() {
        return validoDe;
    }

    public void setValidoDe(LocalDate validoDe) {
        this.validoDe = validoDe;
    }

    public LocalDate getValidoAte() {
        return validoAte;
    }

    public void setValidoAte(LocalDate validoAte) {
        this.validoAte = validoAte;
    }
}


