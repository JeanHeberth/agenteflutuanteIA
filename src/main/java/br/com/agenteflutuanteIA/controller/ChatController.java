package br.com.agenteflutuanteia.controller;

import br.com.agenteflutuanteia.entity.ChatMessage;
import br.com.agenteflutuanteia.exception.ChatErrorCode;
import br.com.agenteflutuanteia.service.ChatService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket endpoint: o frontend envia para /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload ChatRequest request) {
        ChatMessage response;

        try {
            response = chatService.processMessage(request.getSessionId(), request.getContent());
        } catch (Exception ex) {
            LOGGER.error("Erro ao processar mensagem WebSocket para sessionId={}", request.getSessionId(), ex);
            response = ChatMessage.builder()
                    .role("assistant")
                    .content("Desculpe, ocorreu um erro ao processar sua mensagem. Tente novamente em instantes.")
                    .error(true)
                    .errorCode(ChatErrorCode.INTERNAL_ERROR.name())
                    .errorMessageKey(ChatErrorCode.INTERNAL_ERROR.getMessageKey())
                    .build();
        }

        messagingTemplate.convertAndSend("/topic/chat/" + request.getSessionId(), response);
    }

    /**
     * REST endpoint: buscar histórico da sessão
     */
    @GetMapping("/history/{sessionId}")
    public List<ChatMessage> getHistory(@PathVariable String sessionId) {
        return chatService.getHistory(sessionId);
    }

    /**
     * REST endpoint: limpar sessão
     */
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
    }

    @Data
    @NoArgsConstructor
    public static class ChatRequest {
        private String sessionId;
        private String content;
    }
}

