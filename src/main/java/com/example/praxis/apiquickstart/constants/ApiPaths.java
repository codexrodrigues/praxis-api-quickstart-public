package com.example.praxis.apiquickstart.constants;

/**
 * Registro central dos paths REST canonicos publicados pelo quickstart.
 *
 * <p>No {@code praxis-api-quickstart}, esta classe evita espalhar literais de URL por controllers,
 * DTOs e services. O uso principal aparece em dois pontos:</p>
 *
 * <ul>
 *   <li>{@code @ApiResource(value = ApiPaths....)} nos controllers, definindo o path base real do
 *   recurso HTTP;</li>
 *   <li>{@code @UISchema(endpoint = ApiPaths.... + "/options/filter")} em DTOs e filtros,
 *   publicando endpoints dinamicos que a UI pode consumir.</li>
 * </ul>
 *
 * <p>A relacao com o {@code praxis-metadata-starter} e estrutural, nao acidental. O starter usa o
 * path informado em {@code @ApiResource} como parte da descoberta canonica do recurso, da
 * documentacao OpenAPI e da resolucao de contratos como {@code /schemas/filtered}. Da mesma forma,
 * quando um DTO publica um {@code endpoint} em {@code @UISchema}, esse valor entra no bloco
 * {@code x-ui} consumido por runtimes como o {@code praxis-ui-angular}.</p>
 *
 * <p>Em outras palavras: {@code ApiPaths} nao e apenas um helper de organizacao. Ele funciona como
 * ponto de sincronizacao entre a URL operacional do quickstart e as superficies metadata-driven
 * derivadas pelo starter.</p>
 *
 * <p>Regra pratica de manutencao:</p>
 *
 * <ul>
 *   <li>mudar um path aqui muda o endpoint real exposto pelo app;</li>
 *   <li>essa mesma mudanca impacta OpenAPI, links de schema, discovery e endpoints publicados em
 *   {@code x-ui};</li>
 *   <li>por isso, alteracoes nesta classe devem acompanhar a revisao dos controllers consumidores e
 *   dos DTOs/filtros que compoem URLs derivadas a partir dessas constantes.</li>
 * </ul>
 */
public final class ApiPaths {
    public static final String BASE = "/api";

    /** Paths do dominio principal de recursos de RH. */
    public static final class HumanResources {
        private static final String BASE_PATH = BASE + "/human-resources";

        public static final String FUNCIONARIOS = BASE_PATH + "/funcionarios";
        public static final String FUNCIONARIOS_EMPLOYEE_LOOKUP_SOURCE = "employee";
        public static final String FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS =
                FUNCIONARIOS + "/option-sources/" + FUNCIONARIOS_EMPLOYEE_LOOKUP_SOURCE + "/options/filter";
        public static final String CARGOS = BASE_PATH + "/cargos";
        public static final String CARGOS_JOB_ROLE_LOOKUP_SOURCE = "jobRole";
        public static final String CARGOS_JOB_ROLE_LOOKUP_OPTIONS =
                CARGOS + "/option-sources/" + CARGOS_JOB_ROLE_LOOKUP_SOURCE + "/options/filter";
        public static final String DEPARTAMENTOS = BASE_PATH + "/departamentos";
        public static final String DEPARTAMENTOS_DEPARTMENT_LOOKUP_SOURCE = "department";
        public static final String DEPARTAMENTOS_DEPARTMENT_LOOKUP_OPTIONS =
                DEPARTAMENTOS + "/option-sources/" + DEPARTAMENTOS_DEPARTMENT_LOOKUP_SOURCE + "/options/filter";
        public static final String ENDERECOS = BASE_PATH + "/enderecos";
        public static final String DEPENDENTES = BASE_PATH + "/dependentes";
        public static final String FOLHAS_PAGAMENTO = BASE_PATH + "/folhas-pagamento";
        public static final String FOLHAS_PAGAMENTO_PAYROLL_LOOKUP_SOURCE = "payroll";
        public static final String FOLHAS_PAGAMENTO_PAYROLL_LOOKUP_OPTIONS =
                FOLHAS_PAGAMENTO + "/option-sources/" + FOLHAS_PAGAMENTO_PAYROLL_LOOKUP_SOURCE + "/options/filter";
        public static final String EVENTOS_FOLHA = BASE_PATH + "/eventos-folha";
        public static final String FERIAS_AFASTAMENTOS = BASE_PATH + "/ferias-afastamentos";
        public static final String HABILIDADES = BASE_PATH + "/habilidades";
        public static final String HABILIDADES_SKILL_LOOKUP_SOURCE = "skill";
        public static final String HABILIDADES_SKILL_LOOKUP_OPTIONS =
                HABILIDADES + "/option-sources/" + HABILIDADES_SKILL_LOOKUP_SOURCE + "/options/filter";
        public static final String INDENIZACOES = BASE_PATH + "/indenizacoes";
        public static final String MENCOES_MIDIA = BASE_PATH + "/mencoes-midia";
        public static final String REPUTACOES = BASE_PATH + "/reputacoes";
        public static final String IDENTIDADES_SECRETAS = BASE_PATH + "/identidades-secretas";
        public static final String HISTORICOS_SALARIAIS = BASE_PATH + "/historicos-salariais";
        public static final String HISTORICOS_CARGOS = BASE_PATH + "/historicos-cargos";
        public static final String FUNCIONARIO_HABILIDADES = BASE_PATH + "/funcionario-habilidades";
        public static final String VW_RANKING_REPUTACAO = BASE_PATH + "/vw-ranking-reputacao";
        public static final String VW_PERFIL_HEROI = BASE_PATH + "/vw-perfil-heroi";
        public static final String VW_ANALYTICS_FOLHA_PAGAMENTO = BASE_PATH + "/vw-analytics-folha-pagamento";

        private HumanResources() {}
    }

    /** Paths do dominio operacional usado pelo quickstart para recursos de operacoes. */
    public static final class Operations {
        private static final String BASE_PATH = BASE + "/operations";

        public static final String ACORDOS_REGULATORIOS = BASE_PATH + "/acordos-regulatorios";
        public static final String ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_SOURCE = "agreement";
        public static final String ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_OPTIONS =
                ACORDOS_REGULATORIOS + "/option-sources/" + ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_SOURCE + "/options/filter";
        public static final String BASES = BASE_PATH + "/bases";
        public static final String BASES_BASE_LOOKUP_SOURCE = "base";
        public static final String BASES_BASE_LOOKUP_OPTIONS =
                BASES + "/option-sources/" + BASES_BASE_LOOKUP_SOURCE + "/options/filter";
        public static final String BASE_ACESSOS = BASE_PATH + "/base-acessos";
        public static final String EQUIPES = BASE_PATH + "/equipes";
        public static final String EQUIPES_TEAM_LOOKUP_SOURCE = "team";
        public static final String EQUIPES_TEAM_LOOKUP_OPTIONS =
                EQUIPES + "/option-sources/" + EQUIPES_TEAM_LOOKUP_SOURCE + "/options/filter";
        public static final String EQUIPE_MEMBROS = BASE_PATH + "/equipe-membros";
        public static final String INCIDENTES = BASE_PATH + "/incidentes";
        public static final String INCIDENTES_INCIDENT_LOOKUP_SOURCE = "incident";
        public static final String INCIDENTES_INCIDENT_LOOKUP_OPTIONS =
                INCIDENTES + "/option-sources/" + INCIDENTES_INCIDENT_LOOKUP_SOURCE + "/options/filter";
        public static final String LICENCAS_OPERACAO = BASE_PATH + "/licencas-operacao";
        public static final String MISSOES = BASE_PATH + "/missoes";
        public static final String MISSOES_MISSION_LOOKUP_SOURCE = "mission";
        public static final String MISSOES_MISSION_LOOKUP_OPTIONS =
                MISSOES + "/option-sources/" + MISSOES_MISSION_LOOKUP_SOURCE + "/options/filter";
        public static final String MISSAO_EVENTOS = BASE_PATH + "/missao-eventos";
        public static final String MISSAO_PARTICIPANTES = BASE_PATH + "/missao-participantes";
        public static final String SINAIS_SOCORRO = BASE_PATH + "/sinais-socorro";
        public static final String VW_RESUMO_MISSOES = BASE_PATH + "/vw-resumo-missoes";

        private Operations() {}
    }

    /** Paths do dominio de ativos e alocacoes. */
    public static final class Assets {
        private static final String BASE_PATH = BASE + "/assets";

        public static final String EQUIPAMENTOS = BASE_PATH + "/equipamentos";
        public static final String EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE = "equipment";
        public static final String EQUIPAMENTOS_EQUIPMENT_LOOKUP_OPTIONS =
                EQUIPAMENTOS + "/option-sources/" + EQUIPAMENTOS_EQUIPMENT_LOOKUP_SOURCE + "/options/filter";
        public static final String EQUIPAMENTO_ALOCACOES = BASE_PATH + "/equipamento-alocacoes";
        public static final String VEICULOS = BASE_PATH + "/veiculos";
        public static final String VEICULOS_VEHICLE_LOOKUP_SOURCE = "vehicle";
        public static final String VEICULOS_VEHICLE_LOOKUP_OPTIONS =
                VEICULOS + "/option-sources/" + VEICULOS_VEHICLE_LOOKUP_SOURCE + "/options/filter";
        public static final String VEICULO_MISSAO_USOS = BASE_PATH + "/veiculo-missao-usos";

        private Assets() {}
    }

    /** Paths do piloto de procurement usado para validar entity lookup metadata-driven. */
    public static final class Procurement {
        private static final String BASE_PATH = BASE + "/procurement";

        public static final String COMPANIES = BASE_PATH + "/companies";
        public static final String COMPANIES_COMPANY_LOOKUP_SOURCE = "company";
        public static final String COMPANIES_COMPANY_LOOKUP_OPTIONS =
                COMPANIES + "/option-sources/" + COMPANIES_COMPANY_LOOKUP_SOURCE + "/options/filter";
        public static final String SUPPLIERS = BASE_PATH + "/suppliers";
        public static final String SUPPLIERS_SUPPLIER_LOOKUP_SOURCE = "supplier";
        public static final String SUPPLIERS_SUPPLIER_LOOKUP_OPTIONS =
                SUPPLIERS + "/option-sources/" + SUPPLIERS_SUPPLIER_LOOKUP_SOURCE + "/options/filter";
        public static final String SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE = "paymentTerm";
        public static final String SUPPLIERS_PAYMENT_TERMS_LOOKUP_OPTIONS =
                SUPPLIERS + "/option-sources/" + SUPPLIERS_PAYMENT_TERMS_LOOKUP_SOURCE + "/options/filter";
        public static final String CONTRACTS = BASE_PATH + "/contracts";
        public static final String CONTRACTS_CONTRACT_LOOKUP_SOURCE = "contract";
        public static final String CONTRACTS_CONTRACT_LOOKUP_OPTIONS =
                CONTRACTS + "/option-sources/" + CONTRACTS_CONTRACT_LOOKUP_SOURCE + "/options/filter";
        public static final String PRODUCTS = BASE_PATH + "/products";
        public static final String PRODUCTS_PRODUCT_LOOKUP_SOURCE = "product";
        public static final String PRODUCTS_PRODUCT_LOOKUP_OPTIONS =
                PRODUCTS + "/option-sources/" + PRODUCTS_PRODUCT_LOOKUP_SOURCE + "/options/filter";
        public static final String PURCHASE_ORDERS = BASE_PATH + "/purchase-orders";

        private Procurement() {}
    }

    /** Paths do dominio de inteligencia de risco e visoes derivadas. */
    public static final class RiskIntelligence {
        private static final String BASE_PATH = BASE + "/risk-intelligence";

        public static final String AMEACAS = BASE_PATH + "/ameacas";
        public static final String AMEACAS_THREAT_LOOKUP_SOURCE = "threat";
        public static final String AMEACAS_THREAT_LOOKUP_OPTIONS =
                AMEACAS + "/option-sources/" + AMEACAS_THREAT_LOOKUP_SOURCE + "/options/filter";
        public static final String VW_INDICADORES_INCIDENTES = BASE_PATH + "/vw-indicadores-incidentes";

        private RiskIntelligence() {}
    }

    private ApiPaths() {}
}
