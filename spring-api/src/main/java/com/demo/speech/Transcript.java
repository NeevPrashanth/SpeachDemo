package com.demo.speech;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcripts")
public class Transcript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String transcript;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
