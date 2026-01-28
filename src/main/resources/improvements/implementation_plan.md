# Enhancing Video Ingestion & Retrieval

This plan addresses the requirements for transcript quality, metadata extraction, and timestamp-based navigation.

## 1. Automated Grammar & Spelling Correction
Instead of a separate agent (which adds latency and cost), we will enhance the **Gemini System Prompt** to include strict quality controls. Gemini 1.5/2.0 already excels at this during the multimodal processing stage.

**Proposed Prompt Update:**
> "Transcribe the audio from this video. Ensure the transcript is grammatically correct and free of spelling errors. If it is a technical or tutorial video, use the correct industry terminology. Return the output in a structured format."

## 2. Structured Metadata Extraction
We will move away from plain text transcripts to a **Structured JSON Output**. This allows us to extract all necessary metadata in a single LLM pass.

**Proposed Data Structure:**
```json
{
  "video_id": "youtube_id",
  "summary": "Brief 1-2 sentence description",
  "keywords": ["yoga", "back pain", "stretch"],
  "segments": [
    {
       "start_timestamp": "02:30",
       "end_timestamp": "03:15",
       "heading": "Lower Back Stretch",
       "content": "Transcript of this specific segment..."
    }
  ]
}
```

## 3. Timestamp-based Retrieval (RAG)
To suggest specific sections (e.g., "minute 5 to 6"), we will implement **Semantic Segmenting**:

1.  **Ingestion**: Ask Gemini to chunk the video into logical segments (as shown in the JSON above).
2.  **Vector Store**: Store each segment as a **separate record** in `pgvector`.
3.  **Metadata**: Attach the `start_timestamp` and `video_url` to each record.
4.  **Retrieval**: When a user asks a question, the vector search will return the most relevant *segment*. The UI can then display: *"Found in 'Title' at 05:20"*.

## Verification Plan

### Automated Tests
- [ ] Verify Gemini returns valid JSON matching the new schema.
- [ ] Test pgvector retrieval to ensure metadata (timestamps) are preserved.

### Manual Verification
- [ ] Run ingestion for a 10-minute video and verify if the chunks correctly represent different topics.
- [ ] Search for a specific exercise and check if the suggested timestamp is accurate.
