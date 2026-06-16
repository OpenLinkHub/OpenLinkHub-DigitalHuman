# OpenLinkHub DigitalHuman

中文 | [English](#english)

OpenLinkHub DigitalHuman 是 OpenLinkHub 生态中的实时语音交互与数字人智能体项目。项目从浏览器实时 ASR 起步，逐步演进为覆盖 ASR、VAD、LLM、TTS、RAG、数字人渲染、工具调用和 OpenLinkHub 业务系统联动的完整对话式智能体平台。

本仓库采用多服务工作区结构。API 网关、服务编排、适配器和企业系统集成优先使用 Java；本地 AI 模型、VAD、RAG、推理 Worker、数字人渲染等能力可按需要使用 Python 实现。

## 项目愿景

长期目标是构建一个面向物联网、运维、大屏、客服和行业场景的智能数字人，使其具备：

- 听得懂：通过浏览器、设备或其他实时音频输入进行语音识别。
- 想得明白：结合 ASR、LLM、RAG 和业务上下文理解用户意图。
- 说得出来：通过实时 TTS 输出语音回复。
- 动得起来：驱动数字人口型、表情、手势和状态反馈。
- 干得了活：调用设备查询、告警处理、任务管理、大屏控制等 OpenLinkHub 工具。
- 接得住业务：支持云服务和本地模型等可插拔 AI Provider。

## 当前状态

第一个已落地功能点是实时 ASR：

- Vue 浏览器麦克风采集。
- 基于 `AudioWorklet` 的 `16kHz mono PCM Int16` 实时音频流。
- Spring Boot WebSocket 实时音频帧入口。
- DashScope FunASR 作为当前 ASR Provider。
- ASR final 文本自动调用 LightRAG 生成回答，并在前端流式展示。
- 保留 REST 文件上传识别，便于测试。
- 后端 API 集成 Knife4j/OpenAPI 文档。

数字人运行时服务位置：

```text
services/runtime/digital-human-runtime
```

Vue 实时语音前端位置：

```text
apps/web-digital-human
```

## 目录结构

```text
OpenLinkHub-DigitalHuman/
├── apps/
│   └── web-digital-human/                 # Vue 前端：实时语音、字幕、未来数字人展示
├── services/
│   └── runtime/
│       └── digital-human-runtime/         # Spring Boot 数字人运行时：ASR、RAG、TTS 和会话编排
├── packages/                               # 前端共享包、SDK、UI 组件
├── shared/                                 # 共享协议、Schema、Prompt、跨服务契约
├── docs/
│   ├── architecture/                       # 架构说明和设计图
│   └── deployment/                         # 本地、Docker、生产部署说明
├── infra/                                  # Docker Compose、Kubernetes、观测、网关配置
└── scripts/                                # 开发和 CI 辅助脚本
```

## 服务规划

### 前端应用

| 路径 | 技术栈 | 职责 |
|---|---|---|
| `apps/web-digital-human` | Vue 3 + Vite | 实时麦克风采集、WebSocket 通信、字幕展示、后续数字人播放和控制 |

### Java 服务

| 规划服务 | 建议路径 | 职责 |
|---|---|---|
| 数字人运行时服务 | `services/runtime/digital-human-runtime` | 实时 WebSocket、ASR、RAG、后续 TTS、会话状态和 Provider 编排 |
| 独立对话编排服务 | `services/conversation/realtime-orchestrator` | 当 runtime 服务变大后，可拆出的会话状态、打断控制、ASR/LLM/TTS 编排服务 |
| 工具网关服务 | `services/tools/openlinkhub-tool-gateway` | OpenLinkHub 设备、告警、任务、大屏和业务 API 调用 |
| API Gateway/BFF | `services/gateway/digital-human-gateway` | 面向前端的 API 聚合、鉴权、路由和协议兼容 |

### Python 服务

| 规划服务 | 建议路径 | 职责 |
|---|---|---|
| VAD Worker | `services/vad/silero-vad-service` | 语音开始/结束检测、打断触发 |
| 本地 ASR Worker | `services/asr/faster-whisper-service` 或 `services/asr/local-funasr-service` | 本地或私有化 ASR 模型推理 |
| RAG 服务 | `services/rag/knowledge-service` | 文档解析、向量化、检索、重排 |
| 数字人渲染服务 | `services/avatar/realtime-avatar-service` | 口型同步、视频生成、WebRTC/HTTP-FLV 输出 |
| TTS Worker | `services/tts/cosyvoice-service` | 本地流式 TTS、音色克隆、音频分片 |

## Provider 适配策略

ASR、TTS、LLM、RAG 和数字人渲染都应被视为可替换 Provider，而不是硬编码依赖。

ASR 的稳定项目级契约建议为：

```text
Browser/Device PCM Stream
  -> Realtime ASR Gateway
  -> ASR Provider Adapter
  -> Recognition Events
```

ASR Provider 可包括：

- `dashscope-funasr`：当前云端实时 ASR 实现。
- `whisper`：云端或 OpenAI API 兼容 Whisper 实现。
- `faster-whisper`：本地 GPU/CPU Worker。
- `local-funasr`：私有化部署 FunASR。
- `third-party-asr`：讯飞、火山、Azure、Google 等商业 Provider。

前端不应感知具体 ASR Provider，只需要依赖实时对话协议：

```json
{"type":"start","sampleRate":16000}
```

音频以 PCM 二进制帧发送，识别事件以统一 JSON 返回：

```json
{
  "type": "recognition",
  "requestId": "provider-request-id",
  "text": "recognized text",
  "beginTime": 0,
  "endTime": 1200,
  "sentenceBegin": false,
  "sentenceEnd": true,
  "completeResult": false
}
```

## Roadmap

### 阶段 1：实时语音对话闭环

- Vue 实时麦克风采集。
- WebSocket PCM 音频流。
- 可插拔 ASR Provider 边界。
- DashScope FunASR 作为第一个 ASR 适配器。
- 增加 VAD，实现说话开始/结束检测。
- 增加 OpenAI 兼容的 LLM 流式回复。
- 增加 TTS 流式输出。
- 支持用户在助手说话时打断。
- 维护会话状态：`IDLE`、`LISTENING`、`THINKING`、`SPEAKING`、`INTERRUPTED`、`ERROR`。

### 阶段 2：意图识别与工具调用

- 增加工具注册表和工具 Schema。
- 支持常见 OpenLinkHub 操作的规则指令。
- 支持 LLM Tool/Function Calling。
- 集成设备状态、告警查询、任务操作、大屏控制等 OpenLinkHub API。
- 增加权限校验和风险分级确认。
- 记录工具调用审计日志。

### 阶段 3：数字人语音联动

- 增加 TTS 到数字人播放链路。
- 集成 MuseTalk、Wav2Lip、livetalking 或商业数字人 API。
- 支持 WebRTC 或 HTTP-FLV 视频输出。
- 同步前端和数字人状态：idle、listening、thinking、speaking、interrupted、error。

### 阶段 4：表情、手势与情绪表达

- 让 LLM 回复携带可选 emotion 和 gesture 元数据。
- 增加 neutral、smile、serious、thinking、surprised、apologetic 等表情标签。
- 增加 nod、shake head、wave、point、explain、alert、idle loop 等动作库。
- 将告警、设备状态等业务事件联动到数字人表现。

### 阶段 5：业务系统深度集成

- 深度集成 OpenLinkHub IoT、告警、大屏、任务、日志和权限。
- 提供面向指挥中心、客服和运维场景的语音工作流。
- 增加多租户和基于角色的访问控制。
- 增加延迟、错误率、Provider 健康度和对话质量观测。

### 阶段 6：多模态智能体平台

- 增加摄像头和图像输入。
- 增加屏幕和大屏理解能力。
- 增加长期记忆和知识个性化。
- 支持多数字人形象和场景模板。
- 提供 ASR、LLM、TTS、RAG、工具和数字人渲染的插件化 Provider 管理。

## 本地开发

### 启动数字人运行时后端

```bash
cd services/runtime/digital-human-runtime
mvn spring-boot:run
```

本地 API Key 放在被忽略的外部配置文件中：

```text
services/runtime/digital-human-runtime/config/application-prod.yml
```

不要提交本地密钥。

### 启动 Vue 前端

```bash
cd apps/web-digital-human
npm install
npm run dev
```

访问：

```text
http://localhost:5173/
```

前端默认 WebSocket 地址：

```text
ws://localhost:8080/ws/runtime/conversation
```

需要时可覆盖：

```bash
VITE_RUNTIME_WS_URL=ws://localhost:8080/ws/runtime/conversation npm run dev
```

## 安全说明

- 不要提交 `application-prod.yml`、`.env`、API Key、模型凭证或云访问令牌。
- Provider 密钥应保存在本地配置、环境变量或 Secret Manager 中。
- 设备控制、配置变更等高风险工具调用必须进行显式确认和权限校验。
- 对话日志可能包含敏感业务数据，应按生产安全要求保护。

## 许可证

本项目遵循仓库中的 License 文件。

---

## English

[中文](#openlinkhub-digitalhuman) | English

OpenLinkHub DigitalHuman is the realtime voice interaction and digital human agent project for the OpenLinkHub ecosystem. It starts with browser-based realtime ASR and will evolve into a full conversational agent platform covering ASR, VAD, LLM, TTS, RAG, digital human rendering, tool calling, and OpenLinkHub business system integration.

This repository is organized as a multi-service workspace. Java is preferred for API gateways, orchestration, adapters, and enterprise integration; Python can be used for local AI models, VAD, RAG pipelines, inference workers, and digital human rendering.

## Vision

The long-term goal is to build an intelligent digital human for IoT, operations, dashboards, customer service, and industry scenarios. It should be able to:

- Listen through realtime browser, device, or external audio input.
- Understand user intent with ASR, LLM, RAG, and business context.
- Speak through realtime TTS.
- Drive a digital human avatar with lip sync, emotion, gesture, and state feedback.
- Call OpenLinkHub tools such as device query, alarm handling, task management, and dashboard control.
- Support pluggable AI providers, including cloud services and local models.

## Current Status

The first implemented capability is realtime ASR:

- Browser microphone capture in Vue.
- `AudioWorklet`-based `16kHz mono PCM Int16` streaming.
- Spring Boot WebSocket endpoint for realtime audio frames.
- DashScope FunASR realtime adapter.
- REST upload recognition retained for file-based testing.
- Knife4j/OpenAPI documentation for backend APIs.

The digital human runtime service is located at:

```text
services/runtime/digital-human-runtime
```

The Vue realtime voice frontend is located at:

```text
apps/web-digital-human
```

## Repository Layout

```text
OpenLinkHub-DigitalHuman/
├── apps/
│   └── web-digital-human/                 # Vue frontend for realtime voice and future avatar UI
├── services/
│   └── runtime/
│       └── digital-human-runtime/         # Spring Boot runtime for ASR, RAG, TTS, and conversation orchestration
├── packages/                               # Shared frontend packages, SDKs, UI components
├── shared/                                 # Shared contracts, schemas, prompts, protocol definitions
├── docs/
│   ├── architecture/                       # Architecture notes and diagrams
│   └── deployment/                         # Local, Docker, and production deployment docs
├── infra/                                  # Docker Compose, Kubernetes, observability, gateway config
└── scripts/                                # Developer and CI helper scripts
```

## Service Plan

### Frontend

| Path | Technology | Responsibility |
|---|---|---|
| `apps/web-digital-human` | Vue 3 + Vite | Realtime microphone capture, WebSocket communication, transcript UI, future avatar playback and controls |

### Java Services

| Planned Service | Suggested Path | Responsibility |
|---|---|---|
| Digital human runtime service | `services/runtime/digital-human-runtime` | Realtime WebSocket, ASR, RAG, future TTS, session state, and provider orchestration |
| Standalone conversation service | `services/conversation/realtime-orchestrator` | A future split-out service for session state, interruption, and ASR/LLM/TTS coordination when the runtime grows |
| Tool gateway service | `services/tools/openlinkhub-tool-gateway` | OpenLinkHub device, alarm, task, dashboard, and business API tool calls |
| API gateway/BFF | `services/gateway/digital-human-gateway` | Frontend-facing API aggregation, auth, routing, protocol compatibility |

### Python Services

| Planned Service | Suggested Path | Responsibility |
|---|---|---|
| VAD worker | `services/vad/silero-vad-service` | Speech start/end detection, interruption trigger |
| Local ASR worker | `services/asr/faster-whisper-service` or `services/asr/local-funasr-service` | Local/private ASR model inference |
| RAG service | `services/rag/knowledge-service` | Document ingestion, embedding, retrieval, reranking |
| Digital human renderer | `services/avatar/realtime-avatar-service` | Lip sync, avatar video generation, WebRTC/HTTP-FLV output |
| TTS worker | `services/tts/cosyvoice-service` | Local streaming TTS, voice cloning, audio chunks |

## Provider Adapter Strategy

ASR, TTS, LLM, RAG, and avatar rendering should be replaceable providers instead of hard-coded dependencies.

For ASR, the stable project-level contract should be:

```text
Browser/Device PCM Stream
  -> Realtime ASR Gateway
  -> ASR Provider Adapter
  -> Recognition Events
```

ASR provider adapters may include:

- `dashscope-funasr`: current cloud realtime ASR implementation.
- `whisper`: cloud or OpenAI-compatible Whisper implementation.
- `faster-whisper`: local GPU/CPU worker.
- `local-funasr`: private FunASR deployment.
- `third-party-asr`: iFlytek, Volcano, Azure, Google, or other commercial providers.

The frontend should not depend on a specific ASR provider. It should only know the realtime conversation protocol:

```json
{"type":"start","sampleRate":16000}
```

Binary audio frames are sent as PCM, and recognition events are returned as normalized JSON:

```json
{
  "type": "recognition",
  "requestId": "provider-request-id",
  "text": "recognized text",
  "beginTime": 0,
  "endTime": 1200,
  "sentenceBegin": false,
  "sentenceEnd": true,
  "completeResult": false
}
```

## Roadmap

### Phase 1: Realtime Voice Conversation Loop

- Realtime microphone capture in Vue.
- WebSocket PCM streaming.
- Pluggable ASR provider boundary.
- DashScope FunASR as the first ASR adapter.
- Add VAD for speech start/end detection.
- Add LLM streaming responses with OpenAI-compatible APIs.
- Query LightRAG from final ASR text and stream answers back to the frontend.
- Add TTS streaming output.
- Support user interruption while the assistant is speaking.
- Maintain per-session state: `IDLE`, `LISTENING`, `THINKING`, `SPEAKING`, `INTERRUPTED`, `ERROR`.

### Phase 2: Intent Recognition and Tool Calling

- Add tool registry and tool schema definitions.
- Support rule-based commands for common OpenLinkHub operations.
- Support LLM tool/function calling for flexible natural language commands.
- Integrate OpenLinkHub APIs for device status, alarm query, task operations, and dashboard control.
- Add permission checks and risk-based confirmation.
- Record tool call audit logs.

### Phase 3: Digital Human Voice Linkage

- Add TTS-to-avatar playback flow.
- Integrate lip-sync digital human services such as MuseTalk, Wav2Lip, livetalking, or commercial APIs.
- Support WebRTC or HTTP-FLV video output.
- Synchronize frontend states with avatar states: idle, listening, thinking, speaking, interrupted, error.

### Phase 4: Emotion, Gesture, and Expression

- Let LLM responses include optional emotion and gesture metadata.
- Add expression labels such as neutral, smile, serious, thinking, surprised, apologetic.
- Add gesture libraries such as nod, shake head, wave, point, explain, alert, idle loop.
- Connect business events to avatar behavior, especially alarms and device state changes.

### Phase 5: Business System Integration

- Deeply integrate OpenLinkHub IoT, alarms, dashboards, tasks, logs, and permissions.
- Provide operator-oriented voice workflows for command centers, customer service, and operations.
- Add multi-tenant and role-based access control.
- Add observability for latency, error rate, provider health, and conversation quality.

### Phase 6: Multimodal Agent Platform

- Add camera and image input.
- Add screen and dashboard understanding.
- Add long-term memory and knowledge personalization.
- Support multiple avatars and scenario templates.
- Provide plugin-based provider management for ASR, LLM, TTS, RAG, tools, and avatar rendering.

## Local Development

### Start Digital Human Runtime Backend

```bash
cd services/runtime/digital-human-runtime
mvn spring-boot:run
```

Keep local API keys in an ignored external config file:

```text
services/runtime/digital-human-runtime/config/application-prod.yml
```

Do not commit local secrets.

### Start Vue Frontend

```bash
cd apps/web-digital-human
npm install
npm run dev
```

Open:

```text
http://localhost:5173/
```

The default frontend WebSocket URL is:

```text
ws://localhost:8080/ws/runtime/conversation
```

Override it when needed:

```bash
VITE_RUNTIME_WS_URL=ws://localhost:8080/ws/runtime/conversation npm run dev
```

## Security Notes

- Never commit `application-prod.yml`, `.env`, API keys, model credentials, or cloud access tokens.
- Keep provider keys in local config, environment variables, or a secret manager.
- High-risk tool calls, such as device control or configuration changes, must require explicit confirmation and permission checks.
- Conversation logs may contain sensitive operational data and should be protected accordingly.

## License

This project follows the license file in this repository.
