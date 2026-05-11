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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiService.class);

    private final RestClient openAiRestClient;

    @Value("${gemini.api-url}")
    private String apiUrl;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${chat.system-prompt}")
    private String systemPrompt;

    public OpenAiService(RestClient openAiRestClient) {
        this.openAiRestClient = openAiRestClient;
    }

    public String chat(List<Map<String, String>> messageHistory) {

        LOGGER.info("==============================================");
        LOGGER.info("INICIANDO CHAMADA GEMINI");
        LOGGER.info("Mensagens recebidas: {}", messageHistory != null ? messageHistory.size() : 0);
        LOGGER.info("Gemini apiUrl={}", apiUrl);
        LOGGER.info("Gemini apiKey presente? {}", apiKey != null && !apiKey.isBlank());
        LOGGER.info("System prompt presente? {}", systemPrompt != null && !systemPrompt.isBlank());
        LOGGER.info("==============================================");

        var contents = new ArrayList<GeminiContent>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            contents.add(new GeminiContent(
                    "user",
                    List.of(new GeminiPart(systemPrompt))
            ));
        }

        if (messageHistory != null) {
            for (Map<String, String> msg : messageHistory) {

                String contentText = msg.get("content");

                if (contentText == null || contentText.isBlank()) {
                    continue;
                }

                String role = normalizeRole(msg.get("role"));

                LOGGER.info("Adicionando mensagem role={} tamanho={}",
                        role,
                        contentText.length()
                );

                contents.add(new GeminiContent(
                        role,
                        List.of(new GeminiPart(contentText))
                ));
            }
        }

        if (contents.isEmpty()) {
            LOGGER.error("Nenhum conteúdo válido foi enviado ao Gemini.");

            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Não encontrei nenhuma mensagem válida para processar."
            );
        }

        var body = new GeminiRequest(
                contents,
                new GeminiGenerationConfig(0.7f)
        );

        GeminiResponse response;

        try {

            String url = UriComponentsBuilder
                    .fromHttpUrl(apiUrl)
                    .queryParam("key", apiKey)
                    .toUriString();

            LOGGER.info("==============================================");
            LOGGER.info("CHAMANDO GEMINI");
            LOGGER.info("URL: {}", apiUrl);
            LOGGER.info("Quantidade de conteúdos enviados: {}", contents.size());
            LOGGER.info("==============================================");

            response = openAiRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GeminiResponse.class);

            LOGGER.info("==============================================");
            LOGGER.info("RESPOSTA GEMINI RECEBIDA");
            LOGGER.info("Response null? {}", response == null);
            LOGGER.info("Candidates recebidos: {}",
                    response != null && response.getCandidates() != null
                            ? response.getCandidates().size()
                            : 0
            );
            LOGGER.info("==============================================");

        } catch (HttpClientErrorException.TooManyRequests ex) {

            LOGGER.warn("==============================================");
            LOGGER.warn("GEMINI 429 - LIMIT/QUOTA");
            LOGGER.warn("Body: {}", ex.getResponseBodyAsString());
            LOGGER.warn("==============================================");

            throw new ChatProcessingException(
                    ChatErrorCode.AI_QUOTA_EXCEEDED,
                    "No momento estou sem cota de uso da IA. Tente novamente mais tarde.",
                    ex
            );

        } catch (HttpClientErrorException ex) {

            LOGGER.error("==============================================");
            LOGGER.error("ERRO HTTP GEMINI");
            LOGGER.error("Status: {}", ex.getStatusCode());
            LOGGER.error("Body: {}", ex.getResponseBodyAsString());
            LOGGER.error("==============================================", ex);

            throw new ChatProcessingException(
                    ChatErrorCode.AI_HTTP_ERROR,
                    "Não consegui responder agora por uma falha na IA externa. Tente novamente em instantes.",
                    ex
            );

        } catch (RestClientException ex) {

            LOGGER.error("==============================================");
            LOGGER.error("FALHA DE COMUNICAÇÃO COM GEMINI");
            LOGGER.error("Mensagem: {}", ex.getMessage());
            LOGGER.error("==============================================", ex);

            throw new ChatProcessingException(
                    ChatErrorCode.AI_UNAVAILABLE,
                    "Não consegui me conectar ao serviço de IA agora. Tente novamente em instantes.",
                    ex
            );
        }

        String text = extractText(response);

        LOGGER.info("==============================================");
        LOGGER.info("TEXTO EXTRAÍDO COM SUCESSO");
        LOGGER.info("Tamanho da resposta: {}", text.length());
        LOGGER.info("Resposta IA: {}", text);
        LOGGER.info("==============================================");

        return text;
    }

    private String extractText(GeminiResponse response) {

        if (response == null ||
                response.getCandidates() == null ||
                response.getCandidates().isEmpty()) {

            LOGGER.error("Resposta Gemini vazia.");

            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Desculpe, não consegui processar sua mensagem."
            );
        }

        var candidate = response.getCandidates().get(0);

        if (candidate.getContent() == null ||
                candidate.getContent().getParts() == null ||
                candidate.getContent().getParts().isEmpty()) {

            LOGGER.error("Candidate Gemini sem parts.");

            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Desculpe, não consegui processar sua mensagem."
            );
        }

        String text = candidate.getContent().getParts().get(0).getText();

        if (text == null || text.isBlank()) {

            LOGGER.error("Texto Gemini vazio.");

            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Desculpe, não consegui processar sua mensagem."
            );
        }

        return text;
    }

    private String normalizeRole(String role) {

        if ("assistant".equalsIgnoreCase(role)) {
            return "model";
        }

        if ("model".equalsIgnoreCase(role)) {
            return "model";
        }

        return "user";
    }

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiResponse {

        private List<Candidate> candidates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {

        private GeminiContentResponse content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiContentResponse {

        private List<GeminiPartResponse> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiPartResponse {

        private String text;
    }
}