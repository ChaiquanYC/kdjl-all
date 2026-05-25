package com.kdjl.server.websocket;

import com.kdjl.server.security.JwtTokenProvider;
import com.kdjl.server.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Controller
public class ChatHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private final SimpMessagingTemplate messaging;
    private final CacheService cache;

    public ChatHandler(SimpMessagingTemplate messaging, CacheService cache) {
        this.messaging = messaging;
        this.cache = cache;
    }

    @MessageMapping("/chat")
    public void handleChat(@Payload ChatPayload payload, Principal principal) {
        String senderName = principal.getName();
        long senderId = Long.parseLong(principal.getName());

        // Rate limit: 3 messages per 2 seconds
        if (!cache.tryAcquire("chat", String.valueOf(senderId), 3, Duration.ofSeconds(2))) {
            messaging.convertAndSendToUser(
                principal.getName(), "/queue/error",
                Map.of("message", "发言太频繁，请稍后再试"));
            return;
        }

        ChatMessage msg = new ChatMessage(
            UUID.randomUUID().toString(),
            senderId,
            senderName,
            payload.content(),
            payload.channel() != null ? payload.channel() : "world",
            System.currentTimeMillis()
        );

        switch (msg.channel()) {
            case "world" -> messaging.convertAndSend("/topic/chat", msg);
            case "guild" -> messaging.convertAndSend("/topic/guild", msg);
            case "team" -> messaging.convertAndSend("/topic/team", msg);
            default -> messaging.convertAndSend("/topic/chat", msg);
        }

        cache.addChatMessage(msg.channel(), msg.content());
    }

    public record ChatPayload(String content, String channel) {}
    public record ChatMessage(String id, long senderId, String senderName, String content, String channel, long timestamp) {}
}
