package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.enums.EstadoCivil;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Filtro canonico de funcionarios.
 *
 * <p>Ele demonstra os principais padroes de busca do quickstart para recursos
 * cadastrais: texto livre, igualdade booleana, intervalos de data, filtros por
 * relacionamento, recortes numericos por faixa e uma janela relativa para
 * admissoes recentes.
 */
@Schema(
        name = "FuncionarioFilterDTO",
        description = "Criterios de busca no cadastro de colaboradores/herois; nao e a ficha Funcionario a editar. "
                + "Padrao completo: texto, FK, intervalo de data, faixa numerica e janela relativa IN_LAST_DAYS; "
                + "ver javadoc de classe. GenericFilter / POST /filter (demo).")
public class FuncionarioFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome Civil", controlType = FieldControlType.INPUT, maxLength = 200, order = 10, helpText = "Buscar funcionário por nome.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome legal para cracha e contrato; LIKE (case-insensitive depende de backend) (demo).")
    private String nomeCompleto;

    @UISchema(label = "CPF", controlType = FieldControlType.INPUT, maxLength = 20, order = 20, helpText = "Buscar por CPF.", icon = "fingerprint")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "CPF, com ou sem formatacao inserida; LIKE. Validacao forte ocorre na entrada de cadastro, nao no filtro (demo).")
    private String cpf;

    @UISchema(label = "Status", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 30, helpText = "Filtrar apenas ativos ou inativos.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Apenas ativos ou incluir desligados; EQUAL boolean (demo).")
    private Boolean ativo;

    @UISchema(label = "Período de Nascimento", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 35, helpText = "Buscar nascidos em um intervalo de tempo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataNascimento")
    @Schema(
            description = "Janela de nascimento para recortes por idade ou periodo; BETWEEN sobre dataNascimento. Dado sensivel em uso amplo (demo).")
    private List<LocalDate> dataNascimentoRange;

    @UISchema(label = "Cargos", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 40,
            multiple = true,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.CARGOS + "/options/filter", helpText = "Filtrar funcionários por um ou mais cargos.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "cargo.id")
    @Schema(
            description = "Conjunto de cargos aceitos para a busca; operacao IN sobre cargo.id (demo).")
    private List<Integer> cargoIdsIn;

    @UISchema(label = "Cargo", controlType = FieldControlType.INPUT, maxLength = 160, order = 45, helpText = "Filtrar funcionários pelo nome do cargo.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.LIKE, relation = "cargo.nome")
    @Schema(
            description = "Nome do cargo para recortes analiticos conectados; LIKE sobre cargo.nome (demo).")
    private String cargoNome;

    @UISchema(label = "Departamentos", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 50,
            multiple = true,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.DEPARTAMENTOS + "/options/filter", helpText = "Filtrar funcionários por um ou mais departamentos.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "departamento.id")
    @Schema(
            description = "Conjunto de departamentos aceitos para a busca; operacao IN sobre departamento.id (demo).")
    private List<Integer> departamentoIdsIn;

    @UISchema(label = "Departamento", controlType = FieldControlType.INPUT, maxLength = 160, order = 55, helpText = "Filtrar funcionários pelo nome do departamento.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.LIKE, relation = "departamento.nome")
    @Schema(
            description = "Nome do departamento para recortes analiticos conectados; LIKE sobre departamento.nome (demo).")
    private String departamentoNome;

    @UISchema(label = "Período de Admissão", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 60, helpText = "Buscar admissões em um intervalo de tempo.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "dataAdmissao")
    @Schema(
            description = "Janela de contratacao ou onboarding; BETWEEN sobre dataAdmissao (demo).")
    private List<LocalDate> dataAdmissaoRange;

    @UISchema(label = "E-mail", type = FieldDataType.EMAIL, controlType = FieldControlType.INPUT, maxLength = 200, order = 70, helpText = "Buscar por endereço de e-mail.", icon = "email")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Correio interno ou corporativo; LIKE. Dado sensivel, evitar vazamento em logs (demo).")
    private String email;

    @UISchema(label = "Telefone", controlType = FieldControlType.PHONE, maxLength = 30, order = 80, helpText = "Buscar por telefone de contato.", icon = "phone",
            mask = "+55 (00) 00000-0000",
            extraProperties = {
                    @ExtensionProperty(name = "phoneFormat", value = "international"),
                    @ExtensionProperty(name = "defaultCountry", value = "BR"),
                    @ExtensionProperty(name = "autoFormat", value = "true")
            })
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Telefone de contato; LIKE em digitos ou texto (demo).")
    private String telefone;

    @UISchema(label = "Faixa Salarial", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 90,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", helpText = "Informe o salário mínimo, máximo ou ambos.", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "salario")
    @Schema(
            description = "Faixa de remuneracao base vigente; BETWEEN sobre salario (demo).")
    private List<BigDecimal> salarioBetween;

    @UISchema(label = "Estado Civil", controlType = FieldControlType.SELECT, order = 100, helpText = "Filtrar pelo estado civil.", icon = "family_restroom")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Filtro por estado civil cadastral; EQUAL ao enum EstadoCivil (demo).")
    private EstadoCivil estadoCivil;

    @UISchema(label = "Admissões Recentes", type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, order = 110, helpText = "Admitidos nos últimos N dias.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.IN_LAST_DAYS, relation = "dataAdmissao")
    @Schema(
            description = "Corte movel para colaboradores admitidos nos ultimos N dias; operacao IN_LAST_DAYS sobre dataAdmissao (demo).")
    private Integer dataAdmissaoLastDays;

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public List<LocalDate> getDataNascimentoRange() { return dataNascimentoRange; }
    public void setDataNascimentoRange(List<LocalDate> dataNascimentoRange) { this.dataNascimentoRange = dataNascimentoRange; }
    public List<Integer> getCargoIdsIn() { return cargoIdsIn; }
    public void setCargoIdsIn(List<Integer> cargoIdsIn) { this.cargoIdsIn = cargoIdsIn; }
    public String getCargoNome() { return cargoNome; }
    public void setCargoNome(String cargoNome) { this.cargoNome = cargoNome; }
    public List<Integer> getDepartamentoIdsIn() { return departamentoIdsIn; }
    public void setDepartamentoIdsIn(List<Integer> departamentoIdsIn) { this.departamentoIdsIn = departamentoIdsIn; }
    public String getDepartamentoNome() { return departamentoNome; }
    public void setDepartamentoNome(String departamentoNome) { this.departamentoNome = departamentoNome; }
    public List<LocalDate> getDataAdmissaoRange() { return dataAdmissaoRange; }
    public void setDataAdmissaoRange(List<LocalDate> dataAdmissaoRange) { this.dataAdmissaoRange = dataAdmissaoRange; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public List<BigDecimal> getSalarioBetween() { return salarioBetween; }
    public void setSalarioBetween(List<BigDecimal> salarioBetween) { this.salarioBetween = salarioBetween; }
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
    public Integer getDataAdmissaoLastDays() { return dataAdmissaoLastDays; }
    public void setDataAdmissaoLastDays(Integer dataAdmissaoLastDays) { this.dataAdmissaoLastDays = dataAdmissaoLastDays; }
}
