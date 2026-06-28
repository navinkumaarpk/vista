package com.vista.chat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.vista.prompt.PromptLibrary;
import com.vista.tools.VistaTools;

@Service
public class ChatService {

    private final ChatClient ollamaClient;     // null if Ollama starter absent
    private final ChatClient anthropicClient;  // null if Anthropic starter absent
    private final List<ModelOption> options = new ArrayList<>();
    private final VistaTools vistaTools;

    private static final String SESSION_ID = "vista-default-session";  // single session for now
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final ChatMemory chatMemory;
    private final PromptLibrary prompts;

    public ChatService(ObjectProvider<OllamaChatModel> ollama,
                       ObjectProvider<AnthropicChatModel> anthropic,
                       VistaTools vistaTools,
                       ChatMemory chatMemory,
                       PromptLibrary prompts) {
        OllamaChatModel om = ollama.getIfAvailable();
        AnthropicChatModel am = anthropic.getIfAvailable();
        this.vistaTools = vistaTools;

        this.chatMemory = chatMemory;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        this.ollamaClient = (om != null) ? ChatClient.builder(om).defaultAdvisors(new SimpleLoggerAdvisor()).build() : null;
        this.anthropicClient = (am != null) ? ChatClient.builder(am).defaultAdvisors(new SimpleLoggerAdvisor()).build() : null;

        if (ollamaClient != null) {
            options.add(new ModelOption("gptoss", "gpt-oss 20B (local)", "ollama", "gpt-oss:20b"));
            options.add(new ModelOption("gemma", "Gemma 4 12B (local)", "ollama", "gemma4:12b"));
        }
        if (anthropicClient != null) {
            options.add(new ModelOption("claude", "Claude (Anthropic API)", "anthropic", "claude-sonnet-4-6"));
        }

        this.prompts = prompts;
    }

    public List<ModelOption> availableModels() {
        return List.copyOf(options);
    }

    public String chat(String modelKey, String message) {
        ModelOption opt = options.stream()
                .filter(o -> o.key().equals(modelKey))
                .findFirst()
                .orElse(null);
        if (opt == null) {
            return "Unknown model key: " + modelKey;
        }
        try {
            return switch (opt.provider()) {
                case "ollama" -> ollamaClient.prompt()
                        .advisors(a -> a.advisors(memoryAdvisor).param(ChatMemory.CONVERSATION_ID, SESSION_ID))
                        .options(OllamaChatOptions.builder().model(opt.modelName()))
                        .tools(vistaTools)
                        .user(message)
                        .system(prompts.get("chat-system"))
                        .call()
                        .content();
                case "anthropic" -> anthropicClient.prompt()
                        .advisors(a -> a.advisors(memoryAdvisor).param(ChatMemory.CONVERSATION_ID, SESSION_ID))
                        .tools(vistaTools)
                        .user(message)
                        .system(prompts.get("chat-system"))
                        .call()
                        .content();
                default -> "Unsupported provider: " + opt.provider();
            };
        } catch (Exception e) {
            return "Error calling " + opt.label() + ": " + e.getMessage();
        }
    }

    public <T> T structured(String modelKey, String prompt, Class<T> type) {
        ModelOption opt = options.stream()
                .filter(o -> o.key().equals(modelKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown model key: " + modelKey));
    
        return switch (opt.provider()) {
            case "ollama" -> ollamaClient.prompt()
                    .options(OllamaChatOptions.builder().model(opt.modelName()))
                    .user(prompt)
                    .system(prompts.get("chat-system"))
                    .call()
                    .entity(type);
            case "anthropic" -> anthropicClient.prompt()
                    .user(prompt)
                    .system(prompts.get("chat-system"))
                    .call()
                    .entity(type);
            default -> throw new IllegalStateException("Unsupported provider: " + opt.provider());
        };
    }

    public void resetConversation() 
    {
        chatMemory.clear(SESSION_ID);
    }
}
