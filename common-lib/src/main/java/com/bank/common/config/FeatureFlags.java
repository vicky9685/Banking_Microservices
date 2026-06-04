package com.bank.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Typed feature flags. Every messaging/scheduling/integration surface is gated on these
 * so the same image can run in a "light" local profile (no Kafka, no Solace) without
 * dead-letter spam or background noise.
 *
 * Defaults: ON. To disable, set the matching environment variable:
 *   APP_FEATURES_KAFKA_ENABLED=false
 *   APP_FEATURES_SOLACE_ENABLED=false
 *   APP_FEATURES_SCHEDULER_ENABLED=false
 *   APP_FEATURES_CAMUNDA_ENABLED=false   (workflow-service only)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.features")
public class FeatureFlags {
    private Toggle kafka = new Toggle();
    private Toggle solace = new Toggle();
    private Toggle scheduler = new Toggle();
    private Toggle camunda = new Toggle();

    @Data
    public static class Toggle {
        /** Master switch — turns off all beans annotated with @ConditionalOnProperty for this feature. */
        private boolean enabled = true;
    }
}
