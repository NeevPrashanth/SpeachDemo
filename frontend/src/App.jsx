import { useEffect, useState } from "react";

const API_BASE = "http://localhost:8080/api/transcripts";

export default function App() {
  const [file, setFile] = useState(null);
  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  async function loadTranscripts() {
    const res = await fetch(API_BASE);
    if (!res.ok) throw new Error("Failed to load transcripts");
    const data = await res.json();
    setItems(data);
  }

  useEffect(() => {
    loadTranscripts().catch((e) => setError(e.message));
  }, []);

  async function onUpload(e) {
    e.preventDefault();
    if (!file) {
      setError("Please choose an audio file.");
      return;
    }

    setError("");
    setIsLoading(true);

    try {
      const formData = new FormData();
      formData.append("file", file);

      const res = await fetch(`${API_BASE}/upload`, {
        method: "POST",
        body: formData
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.error || "Upload failed");
      }

      await loadTranscripts();
      setFile(null);
      const input = document.getElementById("audio-file");
      if (input) input.value = "";
    } catch (err) {
      setError(err.message || "Unexpected error");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="page">
      <section className="card">
        <h1>Audio to Text POC</h1>
        <p>Upload an audio file (.wav/.mp3/.m4a) and store transcript in MySQL.</p>

        <form onSubmit={onUpload}>
          <input
            id="audio-file"
            type="file"
            accept="audio/*"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />
          <button type="submit" disabled={isLoading}>
            {isLoading ? "Transcribing..." : "Upload & Transcribe"}
          </button>
        </form>

        {error && <p className="error">{error}</p>}
      </section>

      <section className="card">
        <h2>Transcripts</h2>
        {items.length === 0 ? (
          <p>No transcripts yet.</p>
        ) : (
          <ul className="list">
            {items.map((t) => (
              <li key={t.id}>
                <strong>{t.fileName}</strong>
                <small>{new Date(t.createdAt).toLocaleString()}</small>
                <p>{t.transcript}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
