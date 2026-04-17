package com.demo.speech;

public record TranscriptResponse(Long id, String fileName, String transcript, String formattedHtml, String createdAt) {
    public static TranscriptResponse fromEntity(Transcript t, String formattedHtml) {
        return new TranscriptResponse(
                t.getId(),
                t.getFileName(),
                t.getTranscript(),
                formattedHtml,
                t.getCreatedAt().toString()
        );
    }
}
