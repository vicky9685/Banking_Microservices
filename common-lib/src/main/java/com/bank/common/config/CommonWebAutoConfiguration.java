package com.bank.common.config;

import com.bank.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
    GlobalExceptionHandler.class,
    FeatureFlags.class
})
public class CommonWebAutoConfiguration {
}
