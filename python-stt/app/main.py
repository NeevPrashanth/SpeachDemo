from tempfile import NamedTemporaryFile
import os

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
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
async def transcribe(
    file: UploadFile = File(...),
    language: str | None = Form(default=None),
    keywords: str | None = Form(default=None)
):
    if not file.filename:
        raise HTTPException(status_code=400, detail="Missing file")

    suffix = os.path.splitext(file.filename)[1] or ".wav"
    with NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    try:
        model = get_model()
        normalized_language = (language or "").strip().lower()
        if normalized_language in ("", "auto"):
            normalized_language = None

        keyword_hint = (keywords or "").strip()
        initial_prompt = None
        if keyword_hint:
            initial_prompt = (
                "Transcribe audio exactly. Keep the following words exactly as spoken without translation: "
                f"{keyword_hint}"
            )

        segments, _info = model.transcribe(
            tmp_path,
            language=normalized_language,
            initial_prompt=initial_prompt,
            condition_on_previous_text=False,
            temperature=0.0
        )
        text = " ".join(segment.text.strip() for segment in segments).strip()
        return {"text": text}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Transcription failed: {str(exc)}")
    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
