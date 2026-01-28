package com.document.chat.rag.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class VideoIngestionService implements CommandLineRunner{
	
	  private static final Logger log = LoggerFactory.getLogger(VideoIngestionService.class);
	
	 private final VectorStore vectorStore;

	 private final VideoTransService videoTransService;
	 
	 public VideoIngestionService(VectorStore vectorStore,VideoTransService videoTransService) {
	        this.vectorStore = vectorStore;
			this.videoTransService = videoTransService;
	    }
	
		@Override
		public void run(String... args) throws Exception {

			/* String link = "https://www.youtube.com/watch?v=AcDnSLYdhbQ";
			
			long count = vectorStore.similaritySearch("*").size();
			log.info("result size  : {}", count);
			
			if(count == 0){
				videoTransService.processing(link);
			 log.info("Video processesing starts " );
			} */
		}
	
	
}
