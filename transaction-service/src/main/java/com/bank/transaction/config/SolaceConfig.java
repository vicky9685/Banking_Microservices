package com.bank.transaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import jakarta.jms.ConnectionFactory;

/**
 * Solace JMS wiring.
 * Topics, not queues, are used so multiple subscribers (notification, audit,
 * downstream risk systems) can independently consume the same alert.
 */
@Configuration
@EnableJms
public class SolaceConfig {

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper mapper) {
        MappingJackson2MessageConverter c = new MappingJackson2MessageConverter();
        c.setObjectMapper(mapper);
        c.setTargetType(MessageType.TEXT);
        c.setTypeIdPropertyName("_type");
        return c;
    }

    @Bean
    public JmsTemplate solaceJmsTemplate(ConnectionFactory factory, MessageConverter converter) {
        JmsTemplate template = new JmsTemplate(factory);
        template.setMessageConverter(converter);
        template.setPubSubDomain(true);          // use topics
        template.setDeliveryPersistent(true);    // guaranteed messaging
        template.setExplicitQosEnabled(true);
        return template;
    }
}
