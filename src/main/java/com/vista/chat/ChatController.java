package com.vista.chat;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/models")
    public List<ModelOption> models() {
        return chatService.availableModels();
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String model = body.getOrDefault("model", "");
        String message = body.getOrDefault("message", "");
        String reply = chatService.chat(model, message);
        return Map.of("reply", reply);
    }

    @PostMapping("/chat/reset")
    public Map<String, String> reset() 
    {
        chatService.resetConversation();
        return Map.of("status", "cleared");
    }
}