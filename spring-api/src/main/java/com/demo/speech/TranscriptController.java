package com.demo.speech;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transcripts")
public class TranscriptController {
    private final TranscriptService service;

    public TranscriptController(TranscriptService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public TranscriptResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        return service.handleUpload(file);
    }

    @GetMapping
    public List<TranscriptResponse> list() {
        return service.getAll();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, IOException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(Exception ex) {
        return Map.of("error", ex.getMessage());
    }
}
