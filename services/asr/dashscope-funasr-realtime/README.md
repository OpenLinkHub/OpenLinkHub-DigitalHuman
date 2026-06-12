# dashscope-funasr-realtime

Spring Boot service that integrates Alibaba Cloud Model Studio DashScope FunASR realtime speech recognition.

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

## Run

```bash
mvn spring-boot:run
```

## Realtime voice conversation

Start this Spring Boot backend from the ASR service root:

```bash
cd services/asr/dashscope-funasr-realtime
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
ws://localhost:8080/ws/funasr/realtime
```

If the backend runs on another host or port, override the WebSocket URL:

```bash
VITE_FUNASR_WS_URL=ws://localhost:8080/ws/funasr/realtime npm run dev
```

The final sentence text is emitted in the Vue page when FunASR marks a sentence as ended. This is the integration point for the later digital human flow:

```text
ASR final text -> LLM dialogue -> TTS -> digital human playback
```

## API

Health check:

```bash
curl http://localhost:8080/api/funasr/health
```

Knife4j API documentation:

```text
http://localhost:8080/doc.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs/funasr
```

Realtime recognition for an uploaded audio file:

```bash
curl -X POST http://localhost:8080/api/funasr/realtime/recognize \
  -F "file=@sample.pcm" \
  -F "format=pcm" \
  -F "sampleRate=16000"
```

The service reads the uploaded audio bytes, starts a DashScope FunASR realtime recognition session, sends audio frames chunk by chunk, stops the session, waits for completion, and returns sentence events plus the combined final text.

For PCM input, use mono 16-bit little-endian audio at the configured sample rate.
