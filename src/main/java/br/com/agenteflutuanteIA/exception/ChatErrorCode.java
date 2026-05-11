package br.com.agenteflutuanteia.exception;

public enum ChatErrorCode {
    AI_QUOTA_EXCEEDED("chat.error.ai_quota_exceeded"),
    AI_HTTP_ERROR("chat.error.ai_http_error"),
    AI_UNAVAILABLE("chat.error.ai_unavailable"),
    AI_EMPTY_RESPONSE("chat.error.ai_empty_response"),
    INTERNAL_ERROR("chat.error.internal_error");

    private final String messageKey;

    ChatErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}

