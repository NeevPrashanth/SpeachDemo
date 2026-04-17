import { useEffect, useRef, useState } from "react";

const API_BASE = "http://localhost:8080/api/transcripts";

export default function App() {
  const [language, setLanguage] = useState("auto");
  const [keywords, setKeywords] = useState("");
  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [error, setError] = useState("");
  const mediaRecorderRef = useRef(null);
  const mediaStreamRef = useRef(null);
  const chunksRef = useRef([]);
  const languageOptions = [
    { value: "auto", label: "Auto detect" },
    { value: "en", label: "English" },
    { value: "hi", label: "Hindi" },
    { value: "es", label: "Spanish" },
    { value: "fr", label: "French" },
    { value: "de", label: "German" },
    { value: "it", label: "Italian" },
    { value: "pt", label: "Portuguese" },
    { value: "ja", label: "Japanese" },
    { value: "ko", label: "Korean" }
  ];

  async function loadTranscripts() {
    const res = await fetch(API_BASE);
    if (!res.ok) throw new Error("Failed to load transcripts");
    const data = await res.json();
    setItems(data);
  }

  useEffect(() => {
    loadTranscripts().catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    return () => {
      mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
    };
  }, []);

  async function uploadFile(file) {
    setError("");
    setIsLoading(true);

    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("language", language);
      formData.append("keywords", keywords);

      const res = await fetch(`${API_BASE}/upload`, {
        method: "POST",
        body: formData
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.error || "Upload failed");
      }

      const created = await res.json();
      setItems((prev) => [created, ...prev]);
    } catch (err) {
      setError(err.message || "Unexpected error");
    } finally {
      setIsLoading(false);
    }
  }

  async function uploadSelectedFile() {
    if (!selectedFile) {
      setError("Please choose an audio file to upload.");
      return;
    }

    await uploadFile(selectedFile);
    setSelectedFile(null);
    const input = document.getElementById("audio-file-input");
    if (input) input.value = "";
  }

  async function startRecording() {
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
      setError("Your browser does not support microphone recording.");
      return;
    }

    setError("");
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaStreamRef.current = stream;
      chunksRef.current = [];

      const recorder = new MediaRecorder(stream);
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };

      recorder.onstop = async () => {
        const recordedBlob = new Blob(chunksRef.current, {
          type: recorder.mimeType || "audio/webm"
        });
        chunksRef.current = [];
        const recordedFile = new File([recordedBlob], `recording-${Date.now()}.webm`, {
          type: recordedBlob.type || "audio/webm"
        });
        await uploadFile(recordedFile);
      };

      recorder.start();
      setIsRecording(true);
    } catch (_err) {
      setError("Microphone access was denied or unavailable.");
    }
  }

  function finishRecording() {
    const recorder = mediaRecorderRef.current;
    if (!recorder || recorder.state === "inactive") return;
    recorder.stop();
    setIsRecording(false);
    mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
  }

  return (
    <main className="page">
      <section className="card">
        <h1>Audio to Text POC</h1>
        <p>Click the microphone to start recording, then finish to transcribe.</p>
        <div className="controls">
          <label className="field">
            <span>Language</span>
            <select value={language} onChange={(e) => setLanguage(e.target.value)} disabled={isLoading}>
              {languageOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="field field-wide">
            <span>Custom Words (optional)</span>
            <input
              type="text"
              value={keywords}
              onChange={(e) => setKeywords(e.target.value)}
              placeholder="e.g. zolakarra, neevinfra"
              disabled={isLoading}
            />
          </label>
        </div>

        <div className="controls">
          <button
            type="button"
            className={`mic-button ${isRecording ? "recording" : ""}`}
            onClick={startRecording}
            disabled={isRecording || isLoading}
            aria-label="Start recording"
            title="Start recording"
          >
            <svg className="mic-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M12 15a3 3 0 0 0 3-3V7a3 3 0 1 0-6 0v5a3 3 0 0 0 3 3Zm5-3a1 1 0 1 0-2 0 3 3 0 1 1-6 0 1 1 0 1 0-2 0 5 5 0 0 0 4 4.9V19H9a1 1 0 1 0 0 2h6a1 1 0 1 0 0-2h-2v-2.1A5 5 0 0 0 17 12Z"
                fill="currentColor"
              />
            </svg>
          </button>
          <button type="button" onClick={finishRecording} disabled={!isRecording || isLoading}>
            Finish
          </button>
          {isRecording && <span className="recording-text">Recording...</span>}
          {isLoading && <span className="recording-text">Transcribing...</span>}
        </div>

        {error && <p className="error">{error}</p>}
      </section>

      <section className="card">
        <h2>Upload Audio File</h2>
        <p>Select an existing audio file and transcribe it using the same language settings.</p>
        <div className="controls">
          <input
            id="audio-file-input"
            type="file"
            accept="audio/*"
            onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
          />
          <button type="button" onClick={uploadSelectedFile} disabled={isLoading}>
            Upload File
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Transcript Box</h2>
        {items.length === 0 ? (
          <p>No transcripts yet.</p>
        ) : (
          <ul className="list">
            {items.map((t) => (
              <li key={t.id}>
                <small>Recorded at: {new Date(t.createdAt).toLocaleString()}</small>
                <p>{t.transcript}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
