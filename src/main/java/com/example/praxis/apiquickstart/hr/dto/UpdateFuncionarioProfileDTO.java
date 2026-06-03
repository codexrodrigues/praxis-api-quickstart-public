package com.example.praxis.apiquickstart.hr.dto;

import com.example.praxis.apiquickstart.hr.enums.EstadoCivil;
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
 * Payload do PATCH de perfil publico/operacional do funcionario.
 *
 * <p>Esse contrato e menor que o DTO principal porque o endpoint de profile
 * demonstra uma alteracao parcial orientada a experiencia do usuario, sem
 * expor salario, lotacao ou outros campos estritamente de RH administrativo.
 */
@Schema(
        name = "UpdateFuncionarioProfileDTO",
        description =
                "Payload parcial (PATCH) do perfil publico/operacional do heroi; nao expoe salario, lotacao "
                        + "nem campos exclusivos de RH administrativo.")
public class UpdateFuncionarioProfileDTO {

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome completo", required = true, maxLength = 200, group = "Perfil", order = 10, helpText = "Novo nome de exibiÃ§Ã£o pÃºblica do colaborador.", icon = "badge")
    @Schema(
            description = "Nome civil ou como o heroi deseja aparecer em listas e identificacao de missao.")
    private String nomeCompleto;

    @NotBlank
    @Email
    @Size(max = 200)
    @UISchema(label = "Email", type = FieldDataType.EMAIL, required = true, maxLength = 200, group = "Perfil", order = 20, helpText = "Novo endereÃ§o de e-mail operacional.", icon = "email")
    @Schema(
            description = "Endereco de correio operacional; ancora convocacoes e alertas.")
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
            helpText = "Novo telefone de contato de emergÃªncia.",
            icon = "phone"
    )
    @Schema(
            description = "Telefone em formato E.164 para contato de emergencia e 2FA.")
    private String telefone;

    @Size(max = 300)
    @UISchema(label = "Foto (URL)", type = FieldDataType.URL, maxLength = 300, group = "Perfil", order = 40, helpText = "URL atualizada da foto de perfil.", icon = "link")
    @Schema(
            description = "URL publica da foto de perfil (armazenamento ja resolvido pelo cliente de upload).")
    private String fotoPerfilUrl;

    @UISchema(label = "Estado civil", controlType = FieldControlType.SELECT, group = "Perfil", order = 50, helpText = "Nova situaÃ§Ã£o civil.", icon = "family_restroom")
    @Schema(
            description = "Estado civil para beneficio e documentacao; EstadoCivil.")
    private EstadoCivil estadoCivil;

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

    public EstadoCivil getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(EstadoCivil estadoCivil) {
        this.estadoCivil = estadoCivil;
    }
}
