package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.hr.enums.HabilidadeCategoria;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/**
 * Catalogo de habilidades disponiveis para associacao a funcionarios.
 *
 * <p>O quickstart separa a habilidade em recurso proprio para demonstrar
 * reuso por multiplos colaboradores e filtros semanticamente ricos.
 */
@Schema(
        name = "HabilidadeDTO",
        description = "Entrada do catalogo de competencias do universo demo: cada registo pode ser associada a varios colaboradores (N-N). "
                + "A categoria e o nivel de poder classificam a habilidade para filtros, missao e governanca de perfil.")
public class HabilidadeDTO {
    @Schema(
            description = "Chave da habilidade no catalogo. Usada em associacoes FuncionarioHabilidade e em criterios de missao; exposta em URLs.",
            example = "12")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, helpText = "Nome da competência no catálogo.", icon = "psychology")
    @Schema(
            description = "Designacao canónica da competencia (ex. Voo tatico, Telepatia de curto alcance) para exibicao e busca semantica.",
            example = "Muralha de forca")
    private String nome;

    @UISchema(label = "Categoria", controlType = FieldControlType.SELECT, helpText = "Classificação da habilidade (ex: TÉCNICA, COMPORTAMENTAL).", icon = "category")
    @Schema(
            description = "Eixo de classificacao: FISICA, MENTAL, TECNOLOGICA, MISTICA, ALIEN, BIOQUIMICA. Agrupa habilidades para relatorios e regras de alocacao.")
    private HabilidadeCategoria categoria;

    @Size(max = 2000)
    @UISchema(label = "Descrição", controlType = FieldControlType.TEXTAREA, maxLength = 2000, helpText = "Detalhes sobre o efeito e contexto de uso.", icon = "description")
    @Schema(
            description = "Narrativa livre do efeito, limitacoes e contexto de uso no demo; nao define sozinha a governanca (ver também missao e UI).")
    private String descricao;

    @Min(0)
    @UISchema(label = "Nível de Poder", type = FieldDataType.NUMBER, helpText = "Grau de impacto (escala interna).", icon = "bolt")
    @Schema(
            description = "Escala interna 0+ de potencia relativa (demo) para priorizar requisitos de missao e comparar candidatos; nao e salario nem antiguidade.",
            example = "7")
    private Integer nivelPoder;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public HabilidadeCategoria getCategoria() { return categoria; }
    public void setCategoria(HabilidadeCategoria categoria) { this.categoria = categoria; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getNivelPoder() { return nivelPoder; }
    public void setNivelPoder(Integer nivelPoder) { this.nivelPoder = nivelPoder; }
}
