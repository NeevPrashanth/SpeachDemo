# Audio-to-Text POC (React + Spring Boot + Python faster-whisper + MySQL)

## Architecture
- `frontend` (React + Vite)
- `spring-api` (Spring Boot REST API + MySQL persistence)
- `python-stt` (FastAPI + Whisper transcription)
- `mysql` (database)

## Endpoints
- `POST /api/transcripts/upload` (Spring) multipart form field: `file`
- `GET /api/transcripts` (Spring)
- `POST /transcribe` (Python STT internal)

## Run with Docker
```bash
docker compose up --build
```

Apps:
- React UI: `http://localhost:5173`
- Spring API: `http://localhost:8080/api/transcripts`
- Python STT: `http://localhost:8000/health`
- MySQL: `localhost:3307`

## MySQL table
The table is auto-created by JPA:
- `transcripts(id, file_name, transcript, created_at)`

## Notes
- First transcription takes longer because Whisper model downloads and warms up.
- This is a POC optimized for file upload (batch), not live streaming.
