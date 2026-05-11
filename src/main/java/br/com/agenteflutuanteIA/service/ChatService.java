package br.com.agenteflutuanteia.service;

import br.com.agenteflutuanteia.entity.ChatMessage;
import br.com.agenteflutuanteia.entity.ChatSession;
import br.com.agenteflutuanteia.exception.ChatErrorCode;
import br.com.agenteflutuanteia.exception.ChatProcessingException;
import br.com.agenteflutuanteia.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final AiService aiService;

    public ChatMessage processMessage(String sessionId, String userContent) {
        // Busca ou cria a sessão
        ChatSession session = chatSessionRepository.findBySessionId(sessionId)
                .orElseGet(() -> ChatSession.builder()
                        .sessionId(sessionId)
                        .build());

        // Salva mensagem do usuário
        ChatMessage userMessage = ChatMessage.builder()
                .role("user")
                .content(userContent)
                .build();
        session.getMessages().add(userMessage);

        // Monta histórico para o provedor de IA
        List<Map<String, String>> history = session.getMessages().stream()
                .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                .toList();

        ChatMessage assistantMessage;

        try {
            // Chama o provedor de IA
            String aiContent = aiService.chat(history);

            // Salva resposta da IA
            assistantMessage = ChatMessage.builder()
                    .role("assistant")
                    .content(aiContent)
                    .build();
        } catch (ChatProcessingException ex) {
            LOGGER.warn("Falha de negócio no processamento da IA. code={} sessionId={}", ex.getErrorCode(), sessionId);
            assistantMessage = ChatMessage.builder()
                    .role("assistant")
                    .content(ex.getUserMessage())
                    .error(true)
                    .errorCode(ex.getErrorCode().name())
                    .errorMessageKey(ex.getErrorCode().getMessageKey())
                    .build();
        } catch (Exception ex) {
            LOGGER.error("Falha inesperada ao processar mensagem. sessionId={}", sessionId, ex);
            assistantMessage = ChatMessage.builder()
                    .role("assistant")
                    .content("Desculpe, ocorreu um erro interno ao processar sua mensagem.")
                    .error(true)
                    .errorCode(ChatErrorCode.INTERNAL_ERROR.name())
                    .errorMessageKey(ChatErrorCode.INTERNAL_ERROR.getMessageKey())
                    .build();
        }

        session.getMessages().add(assistantMessage);
        session.setUpdatedAt(LocalDateTime.now());

        chatSessionRepository.save(session);

        return assistantMessage;
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return chatSessionRepository.findBySessionId(sessionId)
                .map(ChatSession::getMessages)
                .orElse(List.of());
    }

    public void clearSession(String sessionId) {
        chatSessionRepository.findBySessionId(sessionId)
                .ifPresent(chatSessionRepository::delete);
    }
}

