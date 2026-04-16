package com.demo.speech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class TranscriptService {
    private final TranscriptRepository repository;
    private final SttClient sttClient;
    private final Path uploadDir;

    public TranscriptService(
            TranscriptRepository repository,
            SttClient sttClient,
            @Value("${app.upload-dir}") String uploadDir
    ) throws IOException {
        this.repository = repository;
        this.sttClient = sttClient;
        this.uploadDir = Path.of(uploadDir);
        Files.createDirectories(this.uploadDir);
    }

    public TranscriptResponse handleUpload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String safeName = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path savedPath = uploadDir.resolve(safeName);
        file.transferTo(savedPath);

        String text = sttClient.transcribe(savedPath.toFile());

        Transcript transcript = new Transcript();
        transcript.setFileName(file.getOriginalFilename());
        transcript.setTranscript(text);
        Transcript saved = repository.save(transcript);

        return TranscriptResponse.fromEntity(saved);
    }

    public List<TranscriptResponse> getAll() {
        return repository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(TranscriptResponse::fromEntity)
                .toList();
    }
}
