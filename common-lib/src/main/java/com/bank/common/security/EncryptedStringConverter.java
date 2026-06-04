package com.bank.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Applied via @Convert(converter = EncryptedStringConverter.class) on PII columns.
 * Encrypts on write, decrypts on read — transparent to the rest of the code.
 *
 * Hibernate instantiates AttributeConverters outside the Spring context, so we
 * grab the PiiCipher bean via a static accessor populated at startup.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile PiiCipher CIPHER;

    @Component
    public static class CipherInjector implements ApplicationContextAware {
        @Autowired private PiiCipher cipher;
        @Override public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            CIPHER = cipher;
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return CIPHER == null ? attribute : CIPHER.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return CIPHER == null ? dbData : CIPHER.decrypt(dbData);
    }
}
