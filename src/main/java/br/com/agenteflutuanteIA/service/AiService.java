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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiService.class);

    private static final int MAX_RETRIES = 3;

    private final RestClient aiRestClient;

    @Value("${gemini.api-url}")
    private String apiUrl;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${chat.system-prompt}")
    private String systemPrompt;

    public AiService(RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    public String chat(List<Map<String, String>> messageHistory) {
        LOGGER.info("==============================================");
        LOGGER.info("INICIANDO CHAMADA IA");
        LOGGER.info("Provider atual: Gemini");
        LOGGER.info("Modelo configurado: {}", model);
        LOGGER.info("Mensagens recebidas: {}", messageHistory != null ? messageHistory.size() : 0);
        LOGGER.info("API URL configurada? {}", apiUrl != null && !apiUrl.isBlank());
        LOGGER.info("API Key presente? {}", apiKey != null && !apiKey.isBlank());
        LOGGER.info("System prompt presente? {}", systemPrompt != null && !systemPrompt.isBlank());
        LOGGER.info("==============================================");

        var contents = montarConteudoGemini(messageHistory);

        if (contents.isEmpty()) {
            LOGGER.error("Nenhum conteúdo válido foi enviado para a IA.");

            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Não encontrei nenhuma mensagem válida para processar."
            );
        }

        var body = new GeminiRequest(
                contents,
                new GeminiGenerationConfig(0.7f)
        );

        GeminiResponse response = chamarIaComRetry(body, contents.size());

        String text = extractText(response);

        LOGGER.info("==============================================");
        LOGGER.info("TEXTO EXTRAÍDO COM SUCESSO");
        LOGGER.info("Tamanho da resposta: {}", text.length());
        LOGGER.info("Prévia da resposta IA: {}", limitarTexto(text, 300));
        LOGGER.info("==============================================");

        return text;
    }

    private List<GeminiContent> montarConteudoGemini(List<Map<String, String>> messageHistory) {
        var contents = new ArrayList<GeminiContent>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            contents.add(new GeminiContent(
                    "user",
                    List.of(new GeminiPart(systemPrompt))
            ));
        }

        if (messageHistory == null || messageHistory.isEmpty()) {
            return contents;
        }

        for (Map<String, String> msg : messageHistory) {
            if (msg == null) {
                continue;
            }

            String contentText = msg.get("content");

            if (contentText == null || contentText.isBlank()) {
                continue;
            }

            String role = normalizeRole(msg.get("role"));

            LOGGER.info("Adicionando mensagem role={} tamanho={}", role, contentText.length());

            contents.add(new GeminiContent(
                    role,
                    List.of(new GeminiPart(contentText))
            ));
        }

        return contents;
    }

    private GeminiResponse chamarIaComRetry(GeminiRequest body, int totalContents) {
        String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl)
                .queryParam("key", apiKey)
                .toUriString();

        for (int tentativa = 1; tentativa <= MAX_RETRIES; tentativa++) {
            try {
                LOGGER.info("==============================================");
                LOGGER.info("CHAMANDO IA EXTERNA");
                LOGGER.info("Provider: Gemini");
                LOGGER.info("Tentativa: {}/{}", tentativa, MAX_RETRIES);
                LOGGER.info("URL sem chave: {}", apiUrl);
                LOGGER.info("Quantidade de conteúdos enviados: {}", totalContents);
                LOGGER.info("==============================================");

                GeminiResponse response = aiRestClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(GeminiResponse.class);

                LOGGER.info("==============================================");
                LOGGER.info("RESPOSTA DA IA RECEBIDA");
                LOGGER.info("Response null? {}", response == null);
                LOGGER.info("Candidates recebidos: {}",
                        response != null && response.getCandidates() != null
                                ? response.getCandidates().size()
                                : 0
                );
                LOGGER.info("==============================================");

                return response;

            } catch (HttpServerErrorException.ServiceUnavailable ex) {
                LOGGER.warn("IA retornou 503 - serviço/modelo indisponível ou alta demanda.");
                LOGGER.warn("Tentativa {}/{}", tentativa, MAX_RETRIES);
                LOGGER.warn("Body: {}", ex.getResponseBodyAsString());

                if (tentativa == MAX_RETRIES) {
                    throw new ChatProcessingException(
                            ChatErrorCode.AI_UNAVAILABLE,
                            "O modelo de IA está em alta demanda no momento. Tente novamente em alguns instantes.",
                            ex
                    );
                }

                aguardarAntesDeTentarNovamente(tentativa);

            } catch (HttpServerErrorException.BadGateway |
                     HttpServerErrorException.GatewayTimeout |
                     HttpServerErrorException.InternalServerError ex) {

                LOGGER.warn("IA retornou erro temporário {}.", ex.getStatusCode());
                LOGGER.warn("Tentativa {}/{}", tentativa, MAX_RETRIES);
                LOGGER.warn("Body: {}", ex.getResponseBodyAsString());

                if (tentativa == MAX_RETRIES) {
                    throw new ChatProcessingException(
                            ChatErrorCode.AI_UNAVAILABLE,
                            "A IA externa está instável no momento. Tente novamente em alguns instantes.",
                            ex
                    );
                }

                aguardarAntesDeTentarNovamente(tentativa);

            } catch (HttpClientErrorException.TooManyRequests ex) {
                LOGGER.warn("IA retornou 429 - limite/cota excedido.");
                LOGGER.warn("Body: {}", ex.getResponseBodyAsString());

                throw new ChatProcessingException(
                        ChatErrorCode.AI_QUOTA_EXCEEDED,
                        "No momento estou sem cota de uso da IA. Tente novamente mais tarde.",
                        ex
                );

            } catch (HttpClientErrorException ex) {
                LOGGER.error("Erro HTTP ao chamar IA externa.");
                LOGGER.error("Status: {}", ex.getStatusCode());
                LOGGER.error("Body: {}", ex.getResponseBodyAsString(), ex);

                throw new ChatProcessingException(
                        ChatErrorCode.AI_HTTP_ERROR,
                        "Não consegui responder agora por uma falha na IA externa. Tente novamente em instantes.",
                        ex
                );

            } catch (RestClientException ex) {
                LOGGER.error("Falha de comunicação com IA externa. Mensagem: {}", ex.getMessage(), ex);

                throw new ChatProcessingException(
                        ChatErrorCode.AI_UNAVAILABLE,
                        "Não consegui me conectar ao serviço de IA agora. Tente novamente em instantes.",
                        ex
                );
            }
        }

        throw new ChatProcessingException(
                ChatErrorCode.AI_UNAVAILABLE,
                "Não consegui obter resposta da IA no momento."
        );
    }

    private void aguardarAntesDeTentarNovamente(int tentativa) {
        try {
            long tempoEsperaMs = switch (tentativa) {
                case 1 -> 1500L;
                case 2 -> 3000L;
                default -> 5000L;
            };

            LOGGER.info("Aguardando {} ms antes de tentar novamente...", tempoEsperaMs);
            Thread.sleep(tempoEsperaMs);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new ChatProcessingException(
                    ChatErrorCode.AI_UNAVAILABLE,
                    "A tentativa de comunicação com a IA foi interrompida.",
                    ex
            );
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null ||
                response.getCandidates() == null ||
                response.getCandidates().isEmpty()) {

            LOGGER.error("Resposta da IA vazia.");

            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Desculpe, não consegui processar sua mensagem."
            );
        }

        var candidate = response.getCandidates().get(0);

        if (candidate.getContent() == null ||
                candidate.getContent().getParts() == null ||
                candidate.getContent().getParts().isEmpty()) {

            LOGGER.error("Candidate da IA sem parts.");

            throw new ChatProcessingException(
                    ChatErrorCode.AI_EMPTY_RESPONSE,
                    "Desculpe, não consegui processar sua mensagem."
            );
        }

        String text = candidate.getContent().getParts().get(0).getText();

        if (text == null || text.isBlank()) {
            LOGGER.error("Texto retornado pela IA está vazio.");

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

    private String limitarTexto(String texto, int limite) {
        if (texto == null) {
            return "";
        }

        if (texto.length() <= limite) {
            return texto;
        }

        return texto.substring(0, limite) + "...";
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