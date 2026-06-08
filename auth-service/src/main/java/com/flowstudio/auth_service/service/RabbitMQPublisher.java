package com.flowstudio.auth_service.service;

import com.flowstudio.auth_service.dto.AuthResponse;
import com.flowstudio.auth_service.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.flowstudio.auth_service.security.JwtProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final JwtProvider jwtProvider;

    @Value("${rabbitmq.exchange.name:erp.main.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key.user.created:auth.user.created}")
    private String userCreatedRoutingKey;

    public void publishUserCreatedEvent(User user) {
        log.info("Publishing user.created event for user: {}", user.getEmail());

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", user.getId().toString());
        payload.put("username", user.getUsername());
        payload.put("email", user.getEmail());
        payload.put("roles", user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList()));
        payload.put("createdAt", user.getCreatedAt().toString());

        String serviceToken = jwtProvider.generateTokenFromEmail("auth-service@flowstudio.ma", "auth-service", java.util.List.of("SYSTEM"));

        rabbitTemplate.convertAndSend(exchangeName, userCreatedRoutingKey, payload, message -> {
            message.getMessageProperties().setHeader("X-Service-Token", serviceToken);
            return message;
        });
        log.info("Event published successfully to exchange: {}, routing key: {}", exchangeName, userCreatedRoutingKey);
    }
}
