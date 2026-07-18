package com.example.praxis.apiquickstart.core.dto.filter.support;

public final class QuickstartBusinessDateRangeUiOptions {

    public static final String PAYROLL_PAYMENT_SHORTCUTS_JSON = """
            [
              "today",
              "thisWeek",
              "thisMonth",
              {
                "id": "payroll-q1-2026",
                "label": "Folha Q1 2026",
                "startDate": "2026-01-01",
                "endDate": "2026-03-31",
                "timeZone": "America/Sao_Paulo",
                "icon": "account_balance",
                "description": "Periodo fiscal de folha Q1/2026 resolvido pelo dominio de pagamento.",
                "tone": "info",
                "effectiveFrom": "2026-01-01",
                "effectiveTo": "2026-12-31"
              },
              {
                "id": "payroll-legal-audit-window-2026",
                "label": "Auditoria trabalhista 2026",
                "startDate": "2026-04-01",
                "endDate": "2026-04-30",
                "timeZone": "America/Sao_Paulo",
                "icon": "gavel",
                "description": "Janela juridica de conferencia de pagamentos ja resolvida pelo backend.",
                "tone": "warning"
              }
            ]
            """;

    public static final String FOOTER_INLINE_PRESETS_JSON = """
            {
              "enabled": true,
              "position": "footer"
            }
            """;

    public static final String EXPLICIT_INLINE_OVERLAY_JSON = """
            {
              "applyMode": "explicit",
              "actions": {
                "cancel": {"label": "Cancelar"},
                "apply": {"label": "Aplicar"}
              }
            }
            """;

    private QuickstartBusinessDateRangeUiOptions() {
    }
}
