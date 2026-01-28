package com.document.chat.rag.controller;

import com.document.chat.rag.ingestion.LocalVideoTransService;
import com.document.chat.rag.ingestion.VideoTransService;
import com.document.chat.rag.ingestion.VideoVectorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.document.chat.rag.ingestion.VideoTransService.VideoTranscript;
import com.document.chat.rag.ingestion.VideoTransService.Segment;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class VideoTransController {

    private static final Logger log = LoggerFactory.getLogger(VideoTransController.class);

    private final VideoTransService videoTransService;
    private final LocalVideoTransService localVideoTransService;
    private final VideoVectorService videoVectorService;

    public VideoTransController(VideoTransService videoTransService, 
                                LocalVideoTransService localVideoTransService,
                                VideoVectorService videoVectorService) {

        this.videoTransService = videoTransService;
        this.localVideoTransService = localVideoTransService;
        this.videoVectorService = videoVectorService;
    }

    @PostMapping(value = "/transcript")
    public VideoTranscript videoUrl(@RequestBody String linkUrl) {

        log.info("linkUrl is : {} ", linkUrl);
        String withoutQuotes_link = linkUrl.replace("\"", "");
        log.info("withoutQuotes_line1 is : {} ", withoutQuotes_link);
        return videoTransService.processing(withoutQuotes_link);
    }

    @PostMapping(value = "/ingest")
    public String ingestTranscript(@RequestBody VideoTranscript transcript) {
        log.info("Ingesting transcript for video: {}", transcript.videoId());
        return videoVectorService.saveTranscript(transcript);
    }

    @GetMapping(value = "/ingest/count")
    public long getIngestCount() {
        return videoVectorService.count();
    }

    @PostMapping(value = "/transcript/local")
    public VideoTranscript localVideo(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Received local video file: {}", file.getOriginalFilename());
        VideoTranscript transcript = localVideoTransService.transcribe(file.getResource());
        log.info("Transcription received, saving to vector store...");
        videoVectorService.saveTranscript(transcript);
        return transcript;
    }

    @PostMapping(value = "/saveTranscript")
    public void saveTranscript(@RequestBody String transcriptJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode tree = objectMapper.readTree(transcriptJson);
            JsonNode textNode = tree.get("text");
            JsonNode videoIdNode = tree.get("videoId");
            
            String text = (textNode != null) ? textNode.textValue() : transcriptJson;
            String videoId = (videoIdNode != null) ? videoIdNode.textValue() : "unknown";

            VideoTranscript transcript = new VideoTranscript(
                videoId,
                "User updated transcript",
                new ArrayList<>(),
                List.of(new Segment("00:00", "end", "Full Transcript", text))
            );

            log.info("Save the updated transcript for video: {} ", videoId);
            videoVectorService.saveTranscript(transcript);
        } catch (Exception e) {
            log.error("Failed to save transcript: {}", e.getMessage());
            e.printStackTrace();
        }
    }

}
