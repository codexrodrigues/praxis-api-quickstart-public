package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "EnderecoFilterDTO",
        description = "Criterios de busca em enderecos residenciais (nao e o endereco a gravar). "
                + "Trata-se de procura cadastral e LGPD-aware no demo; GenericFilter / POST /filter.")
public class EnderecoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Logradouro", controlType = FieldControlType.INPUT, maxLength = 200, order = 10, helpText = "Buscar por rua ou avenida.", icon = "home")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Rua, avenida ou via; LIKE (substring).")
    private String logradouro;

    @UISchema(label = "Cidade", controlType = FieldControlType.INPUT, maxLength = 200, order = 20, helpText = "Buscar endereços por cidade.", icon = "location_city")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Municipio; LIKE. Usa-se com estado para recorte geografico (demo).")
    private String cidade;

    @UISchema(label = "Estado/UF", controlType = FieldControlType.INPUT, maxLength = 2, order = 30, helpText = "Buscar pela sigla do estado (UF).", icon = "map")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Unidade federativa (sigla); LIKE, tipicamente duas letras (demo).")
    private String estado;

    @UISchema(label = "Número", controlType = FieldControlType.INPUT, maxLength = 50, order = 35, helpText = "Filtrar por número da residência.", icon = "pin")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Numero predial; LIKE para acomodar sufixos alfanumericos (ex.: 1200A).")
    private String numero;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 40,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Localizar endereço de um colaborador específico.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Titular do endereco; EQUAL por id do Funcionario (FK).")
    private Integer funcionarioId;

    @UISchema(label = "Complemento", controlType = FieldControlType.INPUT, order = 50, helpText = "Filtrar por bloco ou apartamento.", icon = "add_location_alt")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Apto, bloco, andar; LIKE para achar variacoes de complemento (demo).")
    private String complemento;

    @UISchema(label = "Bairro", controlType = FieldControlType.INPUT, order = 60, helpText = "Buscar por bairro.", icon = "location_city")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Bairro ou distrito; LIKE.")
    private String bairro;

    @UISchema(label = "CEP", controlType = FieldControlType.INPUT, maxLength = 9, order = 70, helpText = "Buscar por CEP (apenas números ou formatado).", icon = "local_post_office")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "CEP sem formatacao fixa; LIKE para 8 digitos com ou sem hifen (cuidado com padrao de entrada) (demo).")
    private String cep;

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
}
