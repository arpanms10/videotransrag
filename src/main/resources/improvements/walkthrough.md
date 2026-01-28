# Walkthrough: Enhanced Video RAG with Metadata & Timestamps

I have updated the ingestion pipeline and retrieval logic to support high-quality transcripts, structured metadata, and timestamped navigation.

## Changes Made

### 1. Structured Ingestion Pipeline
Refactored `VideoTransService` and `LocalVideoTransService` to use a sophisticated Gemini prompt that returns structured JSON. This eliminates grammar/spelling errors and extracts metadata in a single pass.

- **Data Models**: Added `VideoTranscript` and `Segment` records to represent the video structure.
- **Enhanced Prompting**: Instructed Gemini to provide summaries, keywords, and time-stamped segments.
- **Semantic Chunking**: Instead of arbitrary token splitting, the system now splits the video into logical segments (e.g., specific exercises or topics) as defined by the AI.

### 2. Metadata Persistence
Updated the `insertDocuments` method to store each segment as a separate document in `pgvector` with rich metadata:
- `videoId`
- `startTimestamp` / `endTimestamp`
- `heading`
- `summary`
- `keywords`

### 3. Timestamp-Aware Retrieval
Enhanced `ChatController` with a specialized system prompt that instructs the AI to:
- Cite specific timestamps (e.g., "At 02:30...") when answering questions.
- Provide time ranges for requested information.
- Use the semantic segments to guide the user precisely.

## Proof of Work

### Code Integration
The changes have been integrated across three main components:
1. [VideoTransService.java](file:///Users/arpanmuk/Projects/videotransrag/src/main/java/com/document/chat/rag/ingestion/VideoTransService.java) - Core logic for structured processing.
2. [LocalVideoTransService.java](file:///Users/arpanmuk/Projects/videotransrag/src/main/java/com/document/chat/rag/ingestion/LocalVideoTransService.java) - Parallel logic for local files.
3. [ChatController.java](file:///Users/arpanmuk/Projects/videotransrag/src/main/java/com/document/chat/rag/controller/ChatController.java) - Retrieval and AI persona.

### Compilation Status
The project compiles successfully with the new changes:
```text
[INFO] --- compiler:3.13.0:compile (default-compile) @ rag ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 8 source files with javac [debug parameters release 21] to target/classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

## Next Steps
- **Production Testing**: Upload a 5+ minute video to verify how Gemini chunks the content.
- **UI Enhancement**: Update the mobile/web frontend to render the returned `VideoTranscript` JSON and provide clickable links that jump to the specific `startTimestamp`.
## Local Setup Guide

To run this project locally, follow these steps:

### 1. Prerequisites
- **Java 21**: Ensure you have JDK 21 installed.
- **Docker**: Required to run the `pgvector` database container.
- **Google Cloud Credentials**: Ensure you have `gcloud` authenticated or `GOOGLE_APPLICATION_CREDENTIALS` set for Gemini.
```bash
gcloud auth application-default login
```

### 2. Run pgvector Database

If you are running the database for the first time:
```bash
docker run -d --name postgres-ai -e POSTGRES_PASSWORD=postgres -p 5432:5432 ankane/pgvector
```

If the container already exists but is stopped, start it with:
```bash
docker start postgres-ai
```

To check if the database is running:
```bash
docker ps -a --filter name=postgres-ai
```

### 3. Run the Application
Use the Maven wrapper to start the Spring Boot application:

```bash
./mvnw spring-boot:run
```

The application will automatically:
1.  Connect to the local PostgreSQL instance.
2.  Initialize the vector schema (`documentgpt_vectors` table).
3.  Be ready to ingest videos and answer queries via the API.

---

## Database Inspection (pgAdmin)

You can use **pgAdmin** or any other SQL client to inspect the vector store and the processed transcripts.

### 1. Register Server in pgAdmin
- **Host name/address**: `localhost`
- **Port**: `5432`
- **Maintenance database**: `postgres`
- **Username**: `postgres`
- **Password**: `postgres`

### 2. View Vector Data
Once connected, you can browse the `documentgpt_vectors` table in the `public` schema. To see all ingested segments and their embeddings, run:

```sql
SELECT 
    id, 
    content, 
    metadata->>'heading' as heading, 
    metadata->>'startTimestamp' as start,
    metadata->>'endTimestamp' as "end"
FROM documentgpt_vectors;
```

---

## API Integration: Chat vs Stream

Depending on your UI requirements, you can choose between blocking calls or real-time streaming.

| Feature | `/chat` (Blocking) | `/stream` (Streaming) |
| :--- | :--- | :--- |
| **Method** | `POST` | `GET` |
| **Response Type** | `String` (Plain Text) | `text/event-stream` (or SSE chunked) |
| **UX Feel** | "Loading..." spinner until done. | "Typing..." effect (Live updates). |
| **Best For** | Short answers, background processing. | Core conversational interface. |

### 1. Calling the /chat Endpoint
Use this for simple requests where you don't mind waiting for the full answer.

```javascript
async function askChat(message) {
  const response = await fetch(`http://localhost:8080/chat?message=${encodeURIComponent(message)}`, {
    method: 'POST'
  });
  const text = await response.text();
  console.log("Full Answer:", text);
}
```

### 2. Calling the /stream Endpoint
Use this for a premium AI experience where tokens appear as they are generated.

```javascript
async function askStream(message) {
  const response = await fetch(`http://localhost:8080/stream?message=${encodeURIComponent(message)}`);
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  
  let fullText = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value, { stream: true });
    fullText += chunk;
    
    // Update your UI state here (e.g., setChatText(fullText))
    console.log("Partial Chunk:", chunk);
  }
}
```

---

## Decoupled Ingestion API

To provide more flexibility, the transcription (LLM) and ingestion (Vector DB) logic have been decoupled into separate services.

### 1. Transcription Only
- **Endpoint**: `POST /transcript`
- **Behavior**: Returns structured JSON (videoId, summary, keywords, segments) without saving to the database. This allows the UI to display/review the transcript before committing to the vector store.

### 2. Ingestion Only
- **Endpoint**: `POST /ingest`
- **Behavior**: Accepts the structured JSON returned by `/transcript` and saves it to `pgvector`.
- **Implementation**: Handled by the new `VideoVectorService`.

### 3. Usage Example (UI Flow)
1. Call `/transcript` to get the video data.
2. (Optional) Let the user edit headings or content in the UI.
3. Call `/ingest` with the (modified) JSON to save it.

---

## Troubleshooting

### "Failed to obtain JDBC Connection"
- **Cause**: The Docker container is likely stopped.
- **Fix**: Run `docker start postgres-ai`.

### "Web server failed to start. Port 8080 was already in use."
- **Cause**: Another process (possibly an old instance of the app) is running on port 8080.
- **Fix**: Find and kill the process:
  ```bash
  lsof -i :8080
  kill -9 <PID>
  ```
