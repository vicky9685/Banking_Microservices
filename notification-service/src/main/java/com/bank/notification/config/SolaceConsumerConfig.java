package com.bank.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import jakarta.jms.ConnectionFactory;

@Configuration
@EnableJms
public class SolaceConsumerConfig {

    @Bean
    public MessageConverter jmsConverter(ObjectMapper mapper) {
        MappingJackson2MessageConverter c = new MappingJackson2MessageConverter();
        c.setObjectMapper(mapper);
        c.setTargetType(MessageType.TEXT);
        c.setTypeIdPropertyName("_type");
        return c;
    }

    @Bean
    public DefaultJmsListenerContainerFactory solaceListenerFactory(
            ConnectionFactory cf, MessageConverter converter) {
        DefaultJmsListenerContainerFactory f = new DefaultJmsListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(converter);
        f.setPubSubDomain(true);
        f.setSessionTransacted(true);
        f.setConcurrency("2-5");
        return f;
    }
}
