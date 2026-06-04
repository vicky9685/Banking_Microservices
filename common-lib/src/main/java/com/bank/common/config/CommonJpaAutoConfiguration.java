package com.bank.common.config;

import com.bank.common.audit.AuditChainVerifier;
import com.bank.common.audit.AuditLogger;
import com.bank.common.security.PiiCipher;
import com.bank.common.security.EncryptedStringConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@EntityScan(basePackages = "com.bank.common.audit")
@EnableJpaRepositories(basePackages = "com.bank.common.audit")
@Import({
    PiiCipher.class,
    EncryptedStringConverter.CipherInjector.class,
    AuditLogger.class,
    AuditChainVerifier.class
})
public class CommonJpaAutoConfiguration {
}
