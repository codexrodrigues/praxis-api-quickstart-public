package com.example.praxis.apiquickstart.core.dto.filter.support;

public final class QuickstartRelativePeriodUiOptions {

    public static final String DEFAULT_OPTIONS_JSON = """
            [
              {"label":"Today","value":"today","icon":"calendar_today"},
              {"label":"Yesterday","value":"yesterday","icon":"keyboard_double_arrow_left"},
              {"label":"Last 7 days","value":"last7","icon":"date_range"},
              {"label":"Last 30 days","value":"last30","icon":"date_range"},
              {"label":"This month","value":"thisMonth","icon":"event"},
              {"label":"Last month","value":"lastMonth","icon":"event"},
              {"label":"This quarter","value":"thisQuarter","icon":"bar_chart"},
              {"label":"This year","value":"thisYear","icon":"ads_click"}
            ]
            """;

    private QuickstartRelativePeriodUiOptions() {
    }
}

