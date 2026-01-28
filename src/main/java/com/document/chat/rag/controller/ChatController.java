package com.document.chat.rag.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@CrossOrigin
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {

        this.chatClient = builder
                .defaultSystem("""
                        You are a professional Pilates instructor and AI assistant. 
                        Use the provided context (video transcript segments) to answer user questions. 
                        Each segment includes metadata: 'videoId', 'startTimestamp', and 'endTimestamp'.
                        
                        CRITICAL RULES:
                        1. Provide a 'videoId' and 'videoLink' from the retrieved context. (The videoLink is the full YouTube URL).
                        2. The 'description' should be a brief, encouraging introduction to the recommended exercises.
                        3. The 'details' should contain the specific instructions, ALWAYS citing the 'startTimestamp' (e.g., "At 05:20, the instructor...").
                        4. If multiple segments are found from different videos, choose the most relevant one for the videoId/Link but you can mention others in the 'details'.
                        5. If the context doesn't contain the answer, politely state that in the 'details' and leave videoId/videoLink as "N/A".
                        
                        You MUST return the response in a structured JSON format matching the ChatResponse schema.
                        """)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(new InMemoryChatMemory()),
                        new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    public record ChatResponse(
        String videoId,
        String description,
        String videoLink,
        String details
    ) {}

    @PostMapping("/chat")
    public ChatResponse chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .entity(ChatResponse.class);
    }

    @GetMapping("/stream")
    public Flux<String> chatWithStream(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

}
