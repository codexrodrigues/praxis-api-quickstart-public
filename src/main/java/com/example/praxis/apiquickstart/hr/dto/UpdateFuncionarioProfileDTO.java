package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/**
 * Payload do PATCH de apresentação e contato do funcionário.
 *
 * <p>Esse contrato e menor que o DTO principal porque o endpoint de profile
 * demonstra uma alteração parcial orientada ao diretório corporativo, sem
 * expor remuneração, lotação, documentos ou dados administrativos.
 */
@Schema(
        name = "UpdateFuncionarioProfileDTO",
        description =
                "Comando parcial para atualizar nome de exibição, e-mail, telefone e foto usados no diretório corporativo. Não altera remuneração, lotação, documentos ou dados administrativos.")
public class UpdateFuncionarioProfileDTO {

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome completo", required = true, maxLength = 200, group = "Perfil", order = 10, helpText = "Novo nome de exibição pública do colaborador.", icon = "badge")
    @Schema(description = "Nome usado para identificar o colaborador no diretório e nas comunicações operacionais.")
    private String nomeCompleto;

    @NotBlank
    @Email
    @Size(max = 200)
    @UISchema(label = "Email", type = FieldDataType.EMAIL, required = true, maxLength = 200, group = "Perfil", order = 20, helpText = "Novo endereço de e-mail operacional.", icon = "email")
    @Schema(description = "Endereço de e-mail usado em comunicações e alertas operacionais.")
    private String email;

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^\\+?\\d{8,15}$", message = "Telefone invalido (use formato E.164: +5581999999999)")
    @UISchema(
            label = "Telefone",
            maxLength = 30,
            required = true,
            group = "Perfil",
            order = 30,
            controlType = FieldControlType.PHONE,
            mask = "+55 (00) 00000-0000",
            extraProperties = {
                    @ExtensionProperty(name = "phoneFormat", value = "international"),
                    @ExtensionProperty(name = "defaultCountry", value = "BR"),
                    @ExtensionProperty(name = "autoFormat", value = "true")
            },
            helpText = "Novo telefone de contato operacional.",
            icon = "phone"
    )
    @Schema(description = "Telefone em formato E.164 usado para contato operacional.")
    private String telefone;

    @Size(max = 300)
    @UISchema(label = "Foto (URL)", type = FieldDataType.URL, maxLength = 300, group = "Perfil", order = 40, helpText = "URL atualizada da foto de perfil.", icon = "link")
    @Schema(
            description = "URL publica da foto de perfil (armazenamento ja resolvido pelo cliente de upload).")
    private String fotoPerfilUrl;

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

}
