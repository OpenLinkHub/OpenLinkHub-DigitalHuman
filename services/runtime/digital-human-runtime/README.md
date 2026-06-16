# digital-human-runtime

Spring Boot runtime service for realtime digital human conversation. It currently integrates browser PCM streaming, DashScope FunASR ASR, and LightRAG question answering; TTS, admin configuration, and richer orchestration are planned next.

## Requirements

- JDK 17+
- Maven 3.9+
- FFmpeg available in `PATH` for m4a/mp4 upload transcoding
- DashScope API key

## Configuration

Set the API key with an environment variable:

```bash
export DASHSCOPE_API_KEY=<your-dashscope-api-key>
```

For local `prod` runs, you can also keep the key in an external Spring Boot config file that is ignored by Git:

```text
config/application-prod.yml
```

This file is loaded when the app starts from the project directory with the `prod` profile active, and it is not packaged into the jar.

Optional configuration lives in `src/main/resources/application.yml`:

```yaml
dashscope:
  funasr:
    model: fun-asr-realtime
    format: pcm
    sample-rate: 16000
```

LightRAG is used to answer final ASR questions. Defaults:

```yaml
lightrag:
  base-url: http://127.0.0.1:9621
  stream-path: /query/stream
  query-mode: mix
```

If LightRAG authentication is enabled, keep credentials in the ignored local `config/application-prod.yml` file. Either configure a bearer token:

```yaml
lightrag:
  bearer-token: <your-lightrag-access-token>
```

Or configure username/password so the service can login through `/login`:

```yaml
lightrag:
  username: <your-lightrag-username>
  password: <your-lightrag-password>
```

## Run

```bash
mvn spring-boot:run
```

## Realtime voice conversation

Start this Spring Boot backend from the runtime service root:

```bash
cd services/runtime/digital-human-runtime
mvn spring-boot:run
```

Start the workspace Vue frontend in another terminal:

```bash
cd apps/web-digital-human
npm install
npm run dev
```

Open the Vite URL, usually:

```text
http://localhost:5173/
```

The browser page captures microphone audio with `AudioWorklet`, converts it to `16kHz mono PCM Int16`, and streams 100ms binary frames to:

```text
ws://localhost:8080/ws/runtime/conversation
```

If the backend runs on another host or port, override the WebSocket URL:

```bash
VITE_RUNTIME_WS_URL=ws://localhost:8080/ws/runtime/conversation npm run dev
```

The final sentence text is emitted in the Vue page when FunASR marks a sentence as ended. This is the integration point for the later digital human flow:

```text
ASR final text -> LightRAG answer -> TTS -> digital human playback
```

## API

Health check:

```bash
curl http://localhost:8080/api/runtime/asr/health
```

Knife4j API documentation:

```text
http://localhost:8080/doc.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs/runtime
```

Realtime recognition for an uploaded audio file:

```bash
curl -X POST http://localhost:8080/api/runtime/asr/realtime/recognize \
  -F "file=@sample.pcm" \
  -F "format=pcm" \
  -F "sampleRate=16000"
```

The service reads the uploaded audio bytes, starts a DashScope FunASR realtime recognition session, sends audio frames chunk by chunk, stops the session, waits for completion, and returns sentence events plus the combined final text.

For PCM input, use mono 16-bit little-endian audio at the configured sample rate.
