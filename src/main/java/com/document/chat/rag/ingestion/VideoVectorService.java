package com.document.chat.rag.ingestion;

import com.document.chat.rag.ingestion.VideoTransService.VideoTranscript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;


@Service
public class VideoVectorService {

    private static final Logger log = LoggerFactory.getLogger(VideoVectorService.class);
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public VideoVectorService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Converts a VideoTranscript into AI Documents and saves them to the vector store.
     * Each segment is stored as a separate document with associated metadata.
     */
    public String saveTranscript(VideoTranscript transcript) {
        log.info("Saving structured segments to vector store for video: {}", transcript.videoId());
        try {
            List<Document> documents = transcript.segments().stream()
                .map(segment -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("videoId", transcript.videoId());
                    metadata.put("videoLink", transcript.videoId()); // Based on VideoTransService prompt, videoId contains the link
                    metadata.put("startTimestamp", segment.startTimestamp());
                    metadata.put("endTimestamp", segment.endTimestamp());
                    metadata.put("heading", segment.heading());
                    metadata.put("summary", transcript.summary());
                    metadata.put("keywords", transcript.keywords());

                    return new Document(segment.content(), metadata);
                })
                .collect(Collectors.toList());

            if (documents.isEmpty()) {
                throw new IllegalStateException("No segments were found in the transcript object.");
            }

            vectorStore.write(documents);
            log.info("Successfully saved {} segments to vector store!", documents.size());
            return "success";
        } catch (Exception e) {
            log.error("Failed to save transcript to vector store: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save transcript: " + e.getMessage(), e);
        }
    }

    public long count() {
        try {
            return jdbcTemplate.queryForObject("SELECT count(*) FROM exercise_vectors", Long.class);
        } catch (Exception e) {
            log.warn("Could not get count from exercise_vectors: {}", e.getMessage());
            return -1;
        }
    }
}
