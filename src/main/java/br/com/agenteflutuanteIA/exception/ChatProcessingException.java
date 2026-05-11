package br.com.agenteflutuanteia.exception;

public class ChatProcessingException extends RuntimeException {

    private final ChatErrorCode errorCode;
    private final String userMessage;

    public ChatProcessingException(ChatErrorCode errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public ChatProcessingException(ChatErrorCode errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public ChatErrorCode getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }
}

