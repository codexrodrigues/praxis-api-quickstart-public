package com.example.praxis.apiquickstart.rulelab;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationRequest;
import com.example.praxis.apiquickstart.rulelab.dto.ExtraordinaryBenefitEvaluationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.DateTimeException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.command.ResourceCommandExecutionResult;
import org.praxisplatform.uischema.command.ResourceCommandOutcome;
import org.praxisplatform.uischema.command.ResourceCommandResponsePolicy;
import org.praxisplatform.uischema.controller.base.AbstractCollectionCommandResourceController;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Superficie HTTP QL-04 para avaliar uma solicitacao antes de sua persistencia. */
@ApiResource(
        value = ApiPaths.HumanResources.EXTRAORDINARY_BENEFIT_REQUESTS,
        resourceKey = "human-resources.extraordinary-benefit-requests",
        title = "Solicitacoes de beneficio extraordinario",
        description = "Avalia elegibilidade, limites, calendario e orcamento de beneficios excepcionais sem persistir ou executar a concessao.",
        icon = "volunteer_activism",
        visualTone = "support")
@ApiGroup("human-resources")
public class ExtraordinaryBenefitRequestController extends AbstractCollectionCommandResourceController {
    private final ExtraordinaryBenefitEvaluationService evaluationService;

    public ExtraordinaryBenefitRequestController(ExtraordinaryBenefitEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/actions/evaluate")
    @WorkflowAction(
            id = "evaluate",
            title = "Avaliar beneficio extraordinario",
            description = "Simula a decisao governada com fatos congelados e devolve evidencias do snapshot, sem criar pedido nem executar efeito.",
            scope = ActionScope.COLLECTION,
            order = 10,
            successMessage = "Avaliacao concluida sem persistencia",
            tags = {"human-resources", "benefits", "deterministic-rules", "simulation", "no-effects"})
    @Operation(
            summary = "Avaliar solicitacao de beneficio extraordinario",
            description = "Executa o RuleSet governado ativo sobre fatos congelados pelo host. A resposta informa decisao, motivos, hashes e eventual efeito apenas planejado; esta operacao nunca persiste a solicitacao.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Avaliacao deterministica concluida, inclusive para decisoes DENY, NOT_APPLICABLE, INCONCLUSIVE ou TECHNICAL_ERROR.",
                    content = @Content(schema = @Schema(implementation = ExtraordinaryBenefitEvaluationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Fatos ausentes, invalidos ou fuso horario desconhecido."),
            @ApiResponse(responseCode = "401", description = "Sessao de usuario ausente ou invalida."),
            @ApiResponse(responseCode = "403", description = "Action indisponivel para o contexto de seguranca atual."),
            @ApiResponse(responseCode = "412", description = "Nenhum snapshot governado esta ativo ou vigente no host.")
    })
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResponseEntity<RestApiResponse<ExtraordinaryBenefitEvaluationResponse>> evaluate(
            @Valid @RequestBody ExtraordinaryBenefitEvaluationRequest request) {
        return (ResponseEntity) executeCollectionCommand(
                "evaluate",
                request,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                command -> {
                    try {
                        ExtraordinaryBenefitEvaluationResponse response =
                                evaluationService.evaluate(request, resolveActorPermissions());
                        return ResourceCommandExecutionResult.success(
                                command,
                                null,
                                response,
                                Map.of(
                                        "decision", response.outcome().name(),
                                        "snapshotKey", response.snapshotKey(),
                                        "ruleSetVersion", response.ruleSetVersion()));
                    } catch (ExtraordinaryGrantRuleSnapshotUnavailableException exception) {
                        return ResourceCommandExecutionResult.failure(
                                command,
                                ResourceCommandOutcome.PRECONDITION_FAILED,
                                "No governed extraordinary-benefit snapshot is active and effective.",
                                Map.of("snapshotReady", false));
                    } catch (DateTimeException exception) {
                        return ResourceCommandExecutionResult.failure(
                                command,
                                ResourceCommandOutcome.VALIDATION_FAILED,
                                "The informed userTimeZone is not a valid IANA time zone.",
                                Map.of("field", "userTimeZone"));
                    }
                },
                Map.of("effectExecution", "disabled", "persistence", "disabled"));
    }

    private Set<String> resolveActorPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        Set<String> permissions = new LinkedHashSet<>();
        authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.isBlank())
                .forEach(permissions::add);
        if (permissions.contains("ROLE_ADMIN") || permissions.contains("BENEFIT_REQUEST")) {
            permissions.add("benefit:request");
        }
        return Set.copyOf(permissions);
    }
}
