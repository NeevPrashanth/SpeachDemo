package com.demo.speech;

public record TranscriptResponse(Long id, String fileName, String transcript, String createdAt) {
    public static TranscriptResponse fromEntity(Transcript t) {
        return new TranscriptResponse(
                t.getId(),
                t.getFileName(),
                t.getTranscript(),
                t.getCreatedAt().toString()
        );
    }
}
