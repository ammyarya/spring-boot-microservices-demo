package com.example.notification.consumer;

import com.example.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    @Autowired
    private EmailService emailService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void consumeNotification(String message) {
        log.info("Notification Service received: {}", message);

        // 1. Send Email (Placeholder recipient)
        emailService.sendSimpleEmail("user@example.com", "Document Processing Update", message);

        // 2. Real-time update via WebSocket
        messagingTemplate.convertAndSend("/topic/updates", message);
        log.info("WebSocket notification pushed to /topic/updates");
    }
}
