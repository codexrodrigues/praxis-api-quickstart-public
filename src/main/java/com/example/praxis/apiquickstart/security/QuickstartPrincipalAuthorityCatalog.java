package com.example.praxis.apiquickstart.security;

import com.example.praxis.apiquickstart.hr.security.HrAnalyticsAuthorities;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Catalogo unico das authorities demonstrativas emitidas pelo host de referencia. */
@Component
public class QuickstartPrincipalAuthorityCatalog {

    public static final String AGGREGATE_ONLY_SUBJECT = "aggregate-only";

    private final String adminUser;
    private final String localDefaultUser;
    private final boolean corporateMode;

    public QuickstartPrincipalAuthorityCatalog(
            @Value("${spring.security.user.name:admin}") String adminUser,
            @Value("${praxis.ai.security.local-default-user:demo}") String localDefaultUser,
            @Value("${praxis.ai.security.corporate-mode:true}") boolean corporateMode) {
        this.adminUser = adminUser;
        this.localDefaultUser = localDefaultUser;
        this.corporateMode = corporateMode;
    }

    public Optional<PrincipalGrant> resolve(String subject) {
        if (matchesAdmin(subject) || matchesLocalReferencePrincipal(subject)) {
            return Optional.of(new PrincipalGrant(
                    subject,
                    "ADMIN",
                    List.of(
                            HrAnalyticsAuthorities.AGGREGATE_READ,
                            HrAnalyticsAuthorities.NOMINAL_READ,
                            HrAnalyticsAuthorities.EMPLOYEE_360_READ)));
        }
        if (AGGREGATE_ONLY_SUBJECT.equals(subject)) {
            return Optional.of(new PrincipalGrant(
                    subject,
                    "USER",
                    List.of(HrAnalyticsAuthorities.AGGREGATE_READ)));
        }
        return Optional.empty();
    }

    private boolean matchesAdmin(String subject) {
        return adminUser != null && adminUser.equals(subject);
    }

    /**
     * The local AI authoring profile deliberately accepts a header-resolved reference principal.
     * It must receive the same host authorities when the authoring engine calls protected
     * grounding surfaces on behalf of that principal. Corporate mode never enables this mapping.
     */
    private boolean matchesLocalReferencePrincipal(String subject) {
        return !corporateMode
                && localDefaultUser != null
                && !localDefaultUser.isBlank()
                && localDefaultUser.equals(subject);
    }

    public record PrincipalGrant(String subject, String role, List<String> authorities) {
        public PrincipalGrant {
            authorities = authorities == null ? List.of() : List.copyOf(authorities);
        }
    }
}
