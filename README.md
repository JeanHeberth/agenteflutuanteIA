# 🎈 Agente Flutuante IA

Widget de chat com IA flutuante em tempo real, construído com **Spring Boot + WebSocket (STOMP)** no backend e **React + Vite** no frontend, integrado ao **Google Gemini** e persistindo conversas no **MongoDB**.

---

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Stack](#stack)
- [Pré-requisitos](#pré-requisitos)
- [Configuração do Ambiente](#configuração-do-ambiente)
- [Rodando o Backend](#rodando-o-backend)
- [Rodando o Frontend](#rodando-o-frontend)
- [Estrutura do Projeto](#estrutura-do-projeto)

---

## Visão Geral

O projeto expõe um **balão flutuante** (🎈) fixado no canto inferior direito da tela. Ao clicar, abre uma janela de chat em tempo real que se comunica com o backend via WebSocket STOMP. As mensagens são processadas pelo Google Gemini e o histórico de cada sessão é salvo no MongoDB.

---

## Stack

| Camada     | Tecnologia                              |
|------------|-----------------------------------------|
| Backend    | Java 21, Spring Boot 3.2, Gradle        |
| WebSocket  | STOMP over SockJS                       |
| IA         | Google Gemini (`gemini-2.5-flash`)      |
| Banco      | MongoDB Atlas (ou local via Docker)     |
| Frontend   | React 19, TypeScript, Vite              |
| Estilização | CSS Modules                            |

---

## Pré-requisitos

- [Java 21+](https://adoptium.net/)
- [Node.js 20+](https://nodejs.org/)
- [Docker](https://www.docker.com/) *(opcional, para MongoDB local)*
- Conta no [Google AI Studio](https://aistudio.google.com/) para obter a `GEMINI_API_KEY`
- MongoDB Atlas ou local disponível

---

## Configuração do Ambiente

Crie um arquivo `.env` na raiz do projeto (`agenteflutuanteIA/`) com as seguintes variáveis:

```dotenv
# Banco de Dados
MONGODB_URI=mongodb+srv://<usuario>:<senha>@<cluster>.mongodb.net/<database>

# Servidor
SERVER_PORT=9998

# Google Gemini
GEMINI_API_KEY=sua_chave_aqui
```

> 💡 Para usar MongoDB local com Docker, rode `docker compose up -d` e use `MONGODB_URI=mongodb://localhost:27017/agenteflutuanteia`.

---

## Rodando o Backend

### 1. Via Gradle (desenvolvimento)

```bash
cd agenteflutuanteIA
./gradlew bootRun
```

### 2. Build + JAR

```bash
./gradlew clean build
java -jar build/libs/agenteflutuanteia-0.0.1-SNAPSHOT.jar
```

O backend sobe em: **http://localhost:9998**

---

## Rodando o Frontend

```bash
cd agenteflutuanteIA/frontend
npm install
npm run dev
```

O frontend sobe em: **http://localhost:3000**

> ⚡ O Vite está configurado com proxy para `/ws` e `/api` apontando para `http://localhost:9998`, então nenhuma configuração extra de CORS é necessária em desenvolvimento.

### Build de produção

```bash
cd agenteflutuanteIA/frontend
npm run build
```

---

## MongoDB com Docker (opcional)

Para subir uma instância local do MongoDB:

```bash
cd agenteflutuanteIA
docker compose up -d
```

Isso sobe o MongoDB na porta `27017` com o database `agenteflutuanteia` já criado.

---

## Estrutura do Projeto

```
agenteflutuanteIA/
├── src/
│   └── main/java/br/com/agenteflutuanteia/
│       ├── config/
│       │   ├── AiClientConfig.java       # RestClient para Gemini
│       │   ├── CorsConfig.java
│       │   └── WebSocketConfig.java      # STOMP broker
│       ├── controller/
│       │   └── ChatController.java       # @MessageMapping WebSocket
│       ├── entity/
│       │   ├── ChatMessage.java
│       │   └── ChatSession.java          # @Document MongoDB
│       ├── exception/
│       │   ├── ChatErrorCode.java        # Enum de códigos de erro
│       │   └── ChatProcessingException.java
│       ├── repository/
│       │   └── ChatSessionRepository.java
│       └── service/
│           ├── AiService.java            # Integração com Gemini
│           └── ChatService.java          # Orquestra o fluxo de chat
├── frontend/
│   └── src/
│       ├── components/
│       │   ├── ChatBubble.tsx            # Botão flutuante
│       │   ├── ChatDialog.tsx            # Janela de chat
│       │   └── ChatWidget.tsx            # Componente raiz
│       ├── hooks/
│       │   └── useWebSocket.ts           # Hook STOMP
│       └── types/
│           └── chat.ts
├── .env                                  # Variáveis de ambiente (não commitar)
├── build.gradle
├── docker-compose.yml
└── README.md
```

---

## Fluxo de Mensagens

```
Browser (React)
  └─ STOMP send → /app/chat.send
       └─ ChatController.handleMessage()
            └─ ChatService.processMessage()
                 ├─ Salva mensagem do usuário no MongoDB
                 ├─ AiService.chat() → Google Gemini API
                 └─ Salva resposta + retorna via /topic/chat/{sessionId}
```

