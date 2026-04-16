from tempfile import NamedTemporaryFile
import os

from fastapi import FastAPI, UploadFile, File, HTTPException
from faster_whisper import WhisperModel

app = FastAPI(title="python-stt")
_model = None


def get_model():
    global _model
    if _model is None:
        _model = WhisperModel(
            os.getenv("WHISPER_MODEL", "base"),
            device=os.getenv("WHISPER_DEVICE", "cpu"),
            compute_type=os.getenv("WHISPER_COMPUTE_TYPE", "int8")
        )
    return _model


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...)):
    if not file.filename:
        raise HTTPException(status_code=400, detail="Missing file")

    suffix = os.path.splitext(file.filename)[1] or ".wav"
    with NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    try:
        model = get_model()
        segments, _info = model.transcribe(tmp_path)
        text = " ".join(segment.text.strip() for segment in segments).strip()
        return {"text": text}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Transcription failed: {str(exc)}")
    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
