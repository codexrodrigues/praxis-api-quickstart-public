package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.operations.repository.AcordosRegulatorioRepository;
import com.example.praxis.apiquickstart.operations.repository.BaseAcessoRepository;
import com.example.praxis.apiquickstart.operations.repository.LicencasOperacaoRepository;
import com.example.praxis.apiquickstart.operations.repository.MissaoRepository;
import com.example.praxis.apiquickstart.hr.service.FolhasPagamentoService;
import com.example.praxis.apiquickstart.rulelab.ExtraordinaryBenefitRequestQueryService;
import org.praxisplatform.uischema.capability.ResourceStateSnapshot;
import org.praxisplatform.uischema.capability.ResourceStateSnapshotProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Component
/**
 * Resolve snapshots de estado usados por surfaces, actions e capabilities contextuais.
 *
 * <p>O {@code praxis-metadata-starter} consegue publicar discovery contextual quando alguem
 * informa, para um recurso concreto, qual e o seu estado atual. Este provider e o adaptador local
 * do quickstart para essa extensao: ele traduz ids de recursos reais em
 * {@link ResourceStateSnapshot} consumidos na avaliacao de availability.</p>
 *
 * <p>Na pratica, esta classe mostra como um host Praxis conecta a semantica de negocio local
 * (status, validade, ativo/inativo, agenda de pagamento) ao contrato canonico de discovery da
 * plataforma, sem empurrar essa regra para o frontend.</p>
 */
public class QuickstartResourceStateSnapshotProvider implements ResourceStateSnapshotProvider {

    private static final String ACORDOS_REGULATORIOS_RESOURCE_KEY = "operations.acordos-regulatorios";
    private static final String MISSOES_RESOURCE_KEY = "operations.missoes";
    private static final String LICENCAS_OPERACAO_RESOURCE_KEY = "operations.licencas-operacao";
    private static final String BASE_ACESSOS_RESOURCE_KEY = "operations.base-acessos";
    private static final String FOLHAS_PAGAMENTO_RESOURCE_KEY = "human-resources.folhas-pagamento";
    private static final String EXTRAORDINARY_BENEFIT_REQUESTS_RESOURCE_KEY =
            "human-resources.extraordinary-benefit-requests";

    private final Map<String, Function<Object, Optional<ResourceStateSnapshot>>> resolvers;

    public QuickstartResourceStateSnapshotProvider(
            AcordosRegulatorioRepository acordosRegulatorioRepository,
            MissaoRepository missaoRepository,
            LicencasOperacaoRepository licencasOperacaoRepository,
            BaseAcessoRepository baseAcessoRepository,
            FolhasPagamentoService folhasPagamentoService,
            ExtraordinaryBenefitRequestQueryService extraordinaryBenefitRequestQueryService
    ) {
        this.resolvers = Map.of(
                ACORDOS_REGULATORIOS_RESOURCE_KEY,
                resourceId -> resolveAcordoRegulatorioSnapshot(acordosRegulatorioRepository, resourceId),
                MISSOES_RESOURCE_KEY,
                resourceId -> resolveMissaoSnapshot(missaoRepository, resourceId),
                LICENCAS_OPERACAO_RESOURCE_KEY,
                resourceId -> resolveLicencaSnapshot(licencasOperacaoRepository, resourceId),
                BASE_ACESSOS_RESOURCE_KEY,
                resourceId -> resolveBaseAcessoSnapshot(baseAcessoRepository, resourceId),
                FOLHAS_PAGAMENTO_RESOURCE_KEY,
                folhasPagamentoService::resolveStateSnapshot,
                EXTRAORDINARY_BENEFIT_REQUESTS_RESOURCE_KEY,
                extraordinaryBenefitRequestQueryService::resolveStateSnapshot
        );
    }

    @Override
    public Optional<ResourceStateSnapshot> resolve(String resourceKey, Object resourceId) {
        Function<Object, Optional<ResourceStateSnapshot>> resolver = resolvers.get(resourceKey);
        if (resolver == null) {
            return Optional.empty();
        }
        return resolver.apply(resourceId);
    }

    private Optional<ResourceStateSnapshot> resolveAcordoRegulatorioSnapshot(
            AcordosRegulatorioRepository repository,
            Object resourceId
    ) {
        Integer id = coerceInteger(resourceId);
        if (id == null) {
            return Optional.empty();
        }
        return repository.findStatusById(id)
                .map(status -> ResourceStateSnapshot.of(status.name()));
    }

    private Optional<ResourceStateSnapshot> resolveMissaoSnapshot(
            MissaoRepository repository,
            Object resourceId
    ) {
        Integer id = coerceInteger(resourceId);
        if (id == null) {
            return Optional.empty();
        }
        return repository.findStatusById(id)
                .map(status -> ResourceStateSnapshot.of(status.name()));
    }

    private Optional<ResourceStateSnapshot> resolveLicencaSnapshot(
            LicencasOperacaoRepository repository,
            Object resourceId
    ) {
        Integer id = coerceInteger(resourceId);
        if (id == null) {
            return Optional.empty();
        }
        LocalDate today = LocalDate.now();
        return repository.findValiditySnapshotById(id)
                .map(snapshot -> {
                    LocalDate validoDe = snapshot.getValidoDe();
                    LocalDate validoAte = snapshot.getValidoAte();
                    if (validoDe != null && validoDe.isAfter(today)) {
                        return ResourceStateSnapshot.of("FUTURA");
                    }
                    if (validoAte != null && validoAte.isBefore(today)) {
                        return ResourceStateSnapshot.of("EXPIRADA");
                    }
                    if (validoAte != null && !validoAte.isAfter(today.plusDays(14))) {
                        return ResourceStateSnapshot.of("A_EXPIRAR");
                    }
                    return ResourceStateSnapshot.of("ATIVA");
                });
    }

    private Optional<ResourceStateSnapshot> resolveBaseAcessoSnapshot(
            BaseAcessoRepository repository,
            Object resourceId
    ) {
        Integer id = coerceInteger(resourceId);
        if (id == null) {
            return Optional.empty();
        }
        return repository.findAtivoById(id)
                .map(ativo -> ResourceStateSnapshot.of(Boolean.TRUE.equals(ativo) ? "ATIVO" : "INATIVO"));
    }

    /** Converte ids externos para inteiro quando o recurso do quickstart usa chave numerica. */
    private Integer coerceInteger(Object resourceId) {
        if (resourceId instanceof Integer integerId) {
            return integerId;
        }
        if (resourceId instanceof Number number) {
            return number.intValue();
        }
        if (resourceId instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

