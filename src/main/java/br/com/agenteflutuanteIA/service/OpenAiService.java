package br.com.agenteflutuanteia.service;

import br.com.agenteflutuanteia.exception.ChatErrorCode;
import br.com.agenteflutuanteia.exception.ChatProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiService.class);

    private final RestClient openAiRestClient;

    @Value("${gemini.model}")
    private String model;

    @Value("${chat.system-prompt}")
    private String systemPrompt;

    public OpenAiService(RestClient openAiRestClient) {
        this.openAiRestClient = openAiRestClient;
    }

    public String chat(List<Map<String, String>> messageHistory) {
        // Monta os conteúdos para Gemini (formato diferente do OpenAI)
        var contents = new java.util.ArrayList<GeminiContent>();

        // Adiciona system prompt como primeiro conteúdo
        var systemContent = new GeminiContent();
        systemContent.setParts(List.of(new GeminiPart(systemPrompt)));
        systemContent.setRole("user");
        contents.add(systemContent);

        // Adiciona histórico de mensagens
        for (Map<String, String> msg : messageHistory) {
            var content = new GeminiContent();
            content.setParts(List.of(new GeminiPart(msg.get("content"))));
            content.setRole(msg.get("role"));
            contents.add(content);
        }

        var body = new GeminiRequest(contents, new GeminiGenerationConfig(0.7f));

        GeminiResponse response;

        try {
            response = openAiRestClient.post()
                    .uri("/{model}:generateContent", model)
                    .body(body)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            LOGGER.warn("Gemini limit/quota excedido (429): {}", ex.getResponseBodyAsString());
            throw new ChatProcessingException(
                    ChatErrorCode.AI_QUOTA_EXCEEDED,
                    "No momento estou sem cota de uso da IA. Tente novamente mais tarde.",
                    ex
            );
        } catch (HttpClientErrorException ex) {
            LOGGER.error("Erro HTTP ao chamar Gemini. Status: {} Corpo: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ChatProcessingException(
                    ChatErrorCode.AI_HTTP_ERROR,
                    "Não consegui responder agora por uma falha na IA externa. Tente novamente em instantes.",
                    ex
            );
        } catch (RestClientException ex) {
            LOGGER.error("Falha de comunicação com Gemini", ex);
            throw new ChatProcessingException(
                    ChatErrorCode.AI_UNAVAILABLE,
                    "Não consegui me conectar ao serviço de IA agora. Tente novamente em instantes.",
                    ex
            );
        }

        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Desculpe, não consegui processar sua mensagem."
            );
        }

        var candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Desculpe, não consegui processar sua mensagem."
            );
        }

        return candidate.getContent().getParts().get(0).getText();
    }

    // Estruturas para requisição Gemini
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiRequest {
        private List<GeminiContent> contents;
        @JsonProperty("generationConfig")
        private GeminiGenerationConfig generationConfig;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiContent {
        private String role;
        private List<GeminiPart> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiPart {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiGenerationConfig {
        private Float temperature;
    }

    // Estruturas para resposta Gemini
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiResponse {
        private List<Candidate> candidates;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Candidate {
            private GeminiContent content;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class GeminiContent {
                private List<GeminiPart> parts;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                public static class GeminiPart {
                    private String text;
                }
            }
        }
    }

    // Manter classe legada para compatibilidade se necessário
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAiResponse {
        private List<Choice> choices;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            private Message message;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Message {
                private String role;
                private String content;
            }
        }
    }
}

