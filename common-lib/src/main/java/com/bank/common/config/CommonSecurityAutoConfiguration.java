package com.bank.common.config;

import com.bank.common.security.BankingSecurityConfig;
import com.bank.common.security.JwtTokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(name = "org.springframework.security.config.annotation.web.builders.HttpSecurity")
@Import({
    BankingSecurityConfig.class,
    JwtTokenProvider.class
})
public class CommonSecurityAutoConfiguration {
}
