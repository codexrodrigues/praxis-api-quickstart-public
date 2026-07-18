package com.example.praxis.apiquickstart.hr.security;

import java.util.List;

/**
 * Minimal host contract for filters whose rows are governed by effective department scope.
 *
 * <p>The public filter remains owned by each resource. This interface only lets the host apply
 * the same server-resolved entitlement intersection without coupling security to one DTO.</p>
 */
public interface HrDepartmentScopedFilter {
    Integer getDepartamentoId();

    List<Integer> getDepartamentoIdsIn();

    void setDepartamentoIdsIn(List<Integer> departamentoIds);

    List<Integer> getFuncionarioIdsIn();
}
