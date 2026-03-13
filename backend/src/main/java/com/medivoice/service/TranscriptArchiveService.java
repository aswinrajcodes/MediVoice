package com.medivoice.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Archives session transcripts to Google Cloud Storage for long-term storage.
 * Operates asynchronously so it doesn't block session close operations.
 */
@Service
@Slf4j
public class TranscriptArchiveService {

    @Value("${gcs.bucket:medivoice-transcripts}")
    private String bucketName;

    @Value("${gcs.enabled:false}")
    private boolean gcsEnabled;

    private Storage storage;

    /**
     * Archive a session transcript to GCS.
     * Object path: transcripts/{sessionId}/{timestamp}.txt
     */
    @Async
    public void archiveTranscript(String sessionId, String transcript) {
        if (!gcsEnabled) {
            log.debug("GCS archiving disabled — skipping transcript archive for session {}", sessionId);
            return;
        }

        if (transcript == null || transcript.isBlank()) {
            log.debug("No transcript to archive for session {}", sessionId);
            return;
        }

        try {
            Storage storageClient = getStorageClient();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String objectName = String.format("transcripts/%s/%s.txt", sessionId, timestamp);

            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("text/plain")
                    .build();

            storageClient.create(blobInfo, transcript.getBytes(StandardCharsets.UTF_8));

            log.info("Transcript archived to gs://{}/{}", bucketName, objectName);

        } catch (Exception e) {
            log.warn("Failed to archive transcript for session {} to GCS: {}", sessionId, e.getMessage());
            // Don't throw — archiving is non-critical and should not break session flow
        }
    }

    private Storage getStorageClient() {
        if (storage == null) {
            storage = StorageOptions.getDefaultInstance().getService();
        }
        return storage;
    }
}
