package com.flowstudio.auth_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name:erp.main.exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // --- DLQ Configuration ---
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange("erp.dlx");
    }

    @Bean
    public org.springframework.amqp.core.Queue deadLetterQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable("q.dlq").build();
    }

    @Bean
    public org.springframework.amqp.core.Binding deadLetterBinding() {
        return org.springframework.amqp.core.BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("#");
    }
    // -----------------------

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
