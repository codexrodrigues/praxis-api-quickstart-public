package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.CreateLegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.dto.LegacyPayCodeAuditLineDTO;
import com.example.praxis.apiquickstart.hr.dto.LegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateLegacyPayCodeDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.LegacyPayCodeFilterDTO;
import com.example.praxis.apiquickstart.hr.service.LegacyPayCodeService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.controller.base.AbstractDuplicateDraftLegacyBackedResourceController;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.hateoas.Links;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ApiResource(value = ApiPaths.HumanResources.LEGACY_PAY_CODES, resourceKey = "human-resources.legacy-pay-codes")
@ApiGroup("human-resources")
public class LegacyPayCodeController extends AbstractDuplicateDraftLegacyBackedResourceController<
        LegacyPayCodeDTO,
        Integer,
        LegacyPayCodeFilterDTO,
        CreateLegacyPayCodeDTO,
        UpdateLegacyPayCodeDTO,
        LegacyPayCodeDTO> {

    private final LegacyPayCodeService service;

    public LegacyPayCodeController(LegacyPayCodeService service) {
        this.service = service;
    }

    @Override
    protected LegacyPayCodeService getService() {
        return service;
    }

    @Override
    protected Integer getResponseId(LegacyPayCodeDTO dto) {
        return dto.getId();
    }

    @GetMapping("/{id}/audit-lines")
    @UiSurface(
            id = "audit-lines",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Audit lines",
            description = "Related audit lines emitted by the legacy payroll-code adapter.",
            intent = "inspect-related-audit-lines",
            order = 70,
            tags = {"related-resource", "audit"},
            relatedChildResourceKey = "human-resources.legacy-pay-code-audit-lines",
            relatedChildResourcePath = "/api/human-resources/legacy-pay-code-audit-lines",
            relatedChildParentField = "payCodeId",
            relatedSelectable = true,
            relatedSelectionKeyField = "auditLineId"
    )
    public ResponseEntity<RestApiResponse<List<LegacyPayCodeAuditLineDTO>>> auditLines(@PathVariable Integer id) {
        List<LegacyPayCodeAuditLineDTO> lines = List.of(
                new LegacyPayCodeAuditLineDTO("AUD-" + id + "-001", id, "CREATE", "Payroll code imported from legacy catalog."),
                new LegacyPayCodeAuditLineDTO("AUD-" + id + "-002", id, "SYNC", "Legacy adapter confirmed catalog consistency.")
        );
        return ResponseEntity.ok(RestApiResponse.success(lines, Links.NONE));
    }
}
