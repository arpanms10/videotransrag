package com.document.chat.rag.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.document.chat.rag.ingestion.VideoTransService.VideoTranscript;
import com.document.chat.rag.ingestion.VideoTransService.Segment;
import java.util.Objects;

@Service
public class LocalVideoTransService {

    private static final Logger log = LoggerFactory.getLogger(LocalVideoTransService.class);

    private final ChatModel chatModel;

    public LocalVideoTransService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public VideoTranscript transcribe(Resource videoResource) {
        log.info("Transcribing video for structured metadata: {}", videoResource.getFilename());
        
        try {
            String promptText = """
                Please provide a complete and accurate transcript of the audio in this video file. 
                Additionally, provide a brief summary and key keywords for the video.
                Break the transcript into logical segments with start and end timestamps.
                Ensure the transcript is grammatically correct and free of spelling errors.
                
                Return the result EXACTLY in the following JSON format:
                {
                  "videoId": "local-file",
                  "summary": "1-2 sentence description",
                  "keywords": ["keyword1", "keyword2"],
                  "segments": [
                    {
                      "startTimestamp": "MM:SS",
                      "endTimestamp": "MM:SS",
                      "heading": "Section Heading",
                      "content": "Transcript text for this section"
                    }
                  ]
                }
                """;

            var userMessage = new UserMessage(
                promptText,
                List.of(new Media(MimeTypeUtils.parseMimeType("video/mp4"), videoResource))
            );

            ChatResponse response = chatModel.call(new Prompt(userMessage));
            String rawJson = response.getResult().getOutput().getText();

            // Clean JSON if Gemini adds markdown blocks
            if (rawJson.contains("```json")) {
                rawJson = rawJson.substring(rawJson.indexOf("```json") + 7);
                rawJson = rawJson.substring(0, rawJson.lastIndexOf("```"));
            } else if (rawJson.contains("```")) {
                rawJson = rawJson.substring(rawJson.indexOf("```") + 3);
                rawJson = rawJson.substring(0, rawJson.lastIndexOf("```"));
            }

            VideoTranscript transcript = objectMapper.readValue(rawJson, VideoTranscript.class);
            
            if (Objects.nonNull(transcript) && transcript.segments() != null && !transcript.segments().isEmpty()) {
                log.info("Transcription and metadata extraction completed successfully.");
                return transcript;
            } else {
                throw new IllegalStateException("Video couldn't be processed into structured format");
            }
        } catch (Exception e) {
            log.error("Failed to transcribe video: {}", e.getMessage(), e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }
}
