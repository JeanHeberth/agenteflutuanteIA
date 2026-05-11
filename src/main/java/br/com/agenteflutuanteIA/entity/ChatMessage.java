package br.com.agenteflutuanteia.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String role; // "user" ou "assistant"

    private String content;

    @Builder.Default
    private boolean error = false;

    private String errorCode;

    private String errorMessageKey;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

