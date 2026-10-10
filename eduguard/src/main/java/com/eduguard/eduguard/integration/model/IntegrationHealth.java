package com.eduguard.eduguard.integration.model;

import java.util.List;

public record IntegrationHealth(
        IntegrationType integration,
        boolean enabled,
        IntegrationMode mode,
        List<IntegrationHealthState> states) {
}
