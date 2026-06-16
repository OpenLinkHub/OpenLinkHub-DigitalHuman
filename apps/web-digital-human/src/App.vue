<template>
  <main class="app-shell">
    <aside class="conversation-sidebar">
      <div class="brand-block">
        <div class="brand-mark">OLH</div>
        <div>
          <p>OpenLinkHub</p>
          <strong>Digital Human</strong>
        </div>
      </div>

      <button class="new-chat-button" type="button" @click="startNewConversation">
        <span>+</span>
        新建对话
      </button>

      <nav class="conversation-list" aria-label="对话记录">
        <button
          v-for="conversation in conversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ active: conversation.id === activeConversationId }"
          type="button"
          @click="activeConversationId = conversation.id"
        >
          <span class="conversation-title">{{ conversation.title }}</span>
          <small>{{ conversation.time }}</small>
        </button>
      </nav>

      <div class="runtime-card">
        <p>Runtime</p>
        <strong>{{ statusText }}</strong>
        <span>{{ wsUrl }}</span>
      </div>
    </aside>

    <section class="chat-workspace">
      <header class="chat-header">
        <div>
          <p class="eyebrow">Realtime ASR + LightRAG</p>
          <h1>数字人对话流</h1>
        </div>
        <div class="header-actions">
          <button class="ghost-button" type="button" @click="clearMessages">清空记录</button>
          <div class="connection-pill" :data-state="connectionState">
            <span />
            {{ statusText }}
          </div>
        </div>
      </header>

      <section ref="messageViewport" class="message-viewport" aria-live="polite">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="[`role-${message.role}`, { streaming: message.status === 'streaming' }]"
        >
          <div class="avatar">{{ avatarLabel(message.role) }}</div>
          <div class="message-bubble">
            <div class="message-meta">
              <strong>{{ roleLabel(message.role) }}</strong>
              <span>{{ message.time }}</span>
            </div>
            <p>{{ message.speechText || message.content }}</p>
            <div
              v-if="message.role === 'assistant' && message.ttsState && message.ttsState !== 'idle'"
              class="voice-playback"
              :data-state="message.ttsState"
            >
              <span>{{ ttsStateText(message.ttsState) }}</span>
              <button
                v-if="message.audioUrl"
                type="button"
                @click="playMessageAudio(message)"
              >
                重播语音
              </button>
            </div>
            <div
              v-if="message.role === 'assistant' && (message.thinking || message.references)"
              class="answer-extras"
            >
              <details v-if="message.thinking">
                <summary>查看思考过程</summary>
                <pre>{{ message.thinking }}</pre>
              </details>
              <details v-if="message.references">
                <summary>查看引用资料</summary>
                <pre>{{ message.references }}</pre>
              </details>
            </div>
            <div v-if="message.status === 'streaming'" class="typing-indicator" aria-label="回答生成中">
              <span />
              <span />
              <span />
            </div>
          </div>
        </article>
      </section>

      <footer class="composer-panel">
        <div v-if="errorMessage" class="error-line">{{ errorMessage }}</div>
        <div class="composer">
          <button
            class="voice-button"
            :class="{ recording: isRecording }"
            :disabled="isBusy && !isRecording"
            type="button"
            @click="toggleRecording"
          >
            <span class="voice-dot" />
            {{ isRecording ? '停止语音' : '语音输入' }}
          </button>
          <textarea
            v-model="draft"
            rows="1"
            placeholder="输入问题，或点击语音输入后直接说话..."
            @keydown.enter.exact.prevent="sendTextQuestion"
            @input="resizeComposer"
          />
          <button
            class="send-button"
            :disabled="!canSendText"
            type="button"
            @click="sendTextQuestion"
          >
            发送
          </button>
        </div>
      </footer>
    </section>

    <aside class="signal-sidebar">
      <section class="signal-card voice-card">
        <div class="panel-title">
          <span>Voice Stream</span>
          <small>PCM 16kHz / mono</small>
        </div>
        <div class="meter" aria-hidden="true">
          <span
            v-for="bar in meterBars"
            :key="bar"
            :style="{ transform: `scaleY(${barScale(bar)})` }"
          />
        </div>
        <p class="live-caption">{{ liveText || '实时字幕会显示在这里' }}</p>
        <div class="intent-result">
          <span>{{ intentStatusText }}</span>
          <strong>{{ intentResult.name || '等待识别' }}</strong>
          <small>{{ intentResult.detail || '识别结果会显示在这里' }}</small>
        </div>
      </section>

      <section class="signal-card">
        <div class="panel-title">
          <span>Pipeline</span>
          <small>{{ answerStatusText }}</small>
        </div>
        <ol class="pipeline-list">
          <li :class="{ active: isRecording, done: lastQuestion }">
            <span>1</span>
            <div>
              <strong>语音采集</strong>
              <p>{{ isRecording ? '正在监听麦克风' : '等待启动' }}</p>
            </div>
          </li>
          <li :class="{ active: Boolean(liveText), done: Boolean(lastQuestion) }">
            <span>2</span>
            <div>
              <strong>ASR 识别</strong>
              <p>{{ lastQuestion || '等待完整句子' }}</p>
            </div>
          </li>
          <li :class="{ active: answerState === 'querying' || answerState === 'handled', done: answerState === 'completed' }">
            <span>3</span>
            <div>
              <strong>意图处理</strong>
              <p>{{ answerStatusText }}</p>
            </div>
          </li>
        </ol>
      </section>

      <section class="signal-card tips-card">
        <div class="panel-title">
          <span>Controls</span>
          <small>Web 对话</small>
        </div>
        <div class="shortcut-grid">
          <span>Enter</span>
          <p>发送文本问题</p>
          <span>Shift Enter</span>
          <p>输入换行</p>
          <span>Voice</span>
          <p>语音识别后自动判断指令、工具或知识库</p>
        </div>
        <button
          class="tts-toggle"
          :data-enabled="ttsEnabled"
          type="button"
          @click="toggleTts"
        >
          <span />
          TTS {{ ttsEnabled ? '开启' : '关闭' }}
        </button>
      </section>
    </aside>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue';
import { PcmAudioStreamer } from './audio/pcmAudioStreamer';

const DEFAULT_BACKEND_PORT = 8080;
const wsUrl = import.meta.env.VITE_RUNTIME_WS_URL
  || `ws://${window.location.hostname}:${DEFAULT_BACKEND_PORT}/ws/runtime/conversation`;

const activeConversationId = ref('today');
const conversations = ref([
  { id: 'today', title: '数字人知识问答', time: '当前会话' },
  { id: 'voice-lab', title: '语音链路调试', time: '样例' },
  { id: 'rag-plan', title: 'RAG 能力规划', time: '样例' }
]);
const messages = ref([
  {
    id: crypto.randomUUID(),
    role: 'assistant',
    content: '你好，我是 OpenLinkHub 数字人运行时。你可以直接输入问题，也可以点击语音输入，我会把识别结果送入知识库问答流程。',
    rawContent: '你好，我是 OpenLinkHub 数字人运行时。你可以直接输入问题，也可以点击语音输入，我会把识别结果送入知识库问答流程。',
    speechText: '你好，我是 OpenLinkHub 数字人运行时。你可以直接输入问题，也可以点击语音输入，我会把识别结果送入知识库问答流程。',
    thinking: '',
    references: '',
    audioUrl: '',
    audioMimeType: '',
    ttsState: 'idle',
    status: 'completed',
    time: formatTime()
  }
]);
const draft = ref('');
const connectionState = ref('idle');
const isRecording = ref(false);
const errorMessage = ref('');
const liveText = ref('');
const lastQuestion = ref('');
const answerState = ref('idle');
const intentStatusText = ref('等待意图识别');
const intentResult = ref({ name: '', detail: '' });
const ttsEnabled = ref(true);
const level = ref(0);
const socket = ref(null);
const streamer = ref(null);
const currentAudio = ref(null);
const activeAssistantMessageId = ref(null);
const lastCompletedAssistantMessageId = ref(null);
const messageViewport = ref(null);
const meterBars = Array.from({ length: 28 }, (_, index) => index);

const isBusy = computed(() => ['connecting', 'stopping'].includes(connectionState.value));
const isAnswering = computed(() => answerState.value === 'querying');
const canSendText = computed(() => draft.value.trim().length > 0 && !isAnswering.value);
const answerStatusText = computed(() => {
  const labels = {
    idle: '待机',
    querying: '检索与生成中',
    handled: '已处理',
    completed: '已完成',
    error: '异常'
  };
  return labels[answerState.value] || answerState.value;
});
const statusText = computed(() => {
  const labels = {
    idle: '待机',
    connecting: '连接中',
    connected: '已连接',
    listening: '监听中',
    stopping: '停止中',
    closed: '已停止',
    error: '异常'
  };
  return labels[connectionState.value] || connectionState.value;
});

function avatarLabel(role) {
  return role === 'user' ? '你' : 'AI';
}

function roleLabel(role) {
  return role === 'user' ? '你' : 'OpenLinkHub Assistant';
}

function ttsStateText(state) {
  const labels = {
    idle: '等待语音',
    synthesizing: '语音合成中',
    ready: '语音已就绪',
    playing: '正在播报',
    ended: '播报完成',
    error: '语音播报异常'
  };
  return labels[state] || labels.idle;
}

function formatTime() {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date());
}

function barScale(index) {
  const center = (meterBars.length - 1) / 2;
  const distance = Math.abs(index - center) / center;
  const shaped = 1 - distance * 0.76;
  return Math.max(0.1, shaped * (0.2 + level.value));
}

async function sendTextQuestion() {
  const question = draft.value.trim();
  if (!question || isAnswering.value) {
    return;
  }

  stopCurrentPlayback();
  draft.value = '';
  appendUserMessage(question);
  liveText.value = '';
  lastQuestion.value = question;
  intentStatusText.value = '正在判断处理类型';
  errorMessage.value = '';

  try {
    const ws = await ensureSocket();
    ws.send(JSON.stringify({ type: 'query', question, ttsEnabled: ttsEnabled.value }));
  } catch (error) {
    connectionState.value = 'error';
    errorMessage.value = error?.message || '无法发送问题。';
  }
}

async function toggleRecording() {
  if (isRecording.value) {
    await stopConversation();
  } else {
    await startConversation();
  }
}

async function startConversation() {
  stopCurrentPlayback();
  errorMessage.value = '';
  liveText.value = '';
  answerState.value = 'idle';
  intentStatusText.value = '等待意图识别';
  connectionState.value = 'connecting';

  try {
    const ws = await ensureSocket();
    ws.send(JSON.stringify({ type: 'start', sampleRate: 16000, ttsEnabled: ttsEnabled.value }));

    streamer.value = new PcmAudioStreamer({
      onAudioFrame: (frame) => {
        if (socket.value?.readyState === WebSocket.OPEN) {
          socket.value.send(frame);
        }
      },
      onLevel: (nextLevel) => {
        level.value = nextLevel;
      }
    });
    await streamer.value.start();

    isRecording.value = true;
    connectionState.value = 'listening';
  } catch (error) {
    await cleanup();
    connectionState.value = 'error';
    errorMessage.value = error?.message || '无法启动实时语音对话。';
  }
}

async function stopConversation() {
  connectionState.value = 'stopping';
  isRecording.value = false;
  await streamer.value?.stop();
  streamer.value = null;
  level.value = 0;

  if (socket.value?.readyState === WebSocket.OPEN) {
    socket.value.send(JSON.stringify({ type: 'stop' }));
  }
  connectionState.value = socket.value?.readyState === WebSocket.OPEN ? 'connected' : 'closed';
}

async function ensureSocket() {
  if (socket.value?.readyState === WebSocket.OPEN) {
    connectionState.value = isRecording.value ? 'listening' : 'connected';
    return socket.value;
  }

  connectionState.value = 'connecting';
  const ws = new WebSocket(wsUrl);
  ws.binaryType = 'arraybuffer';
  socket.value = ws;
  ws.onmessage = handleServerMessage;
  ws.onclose = () => {
    if (!['stopping', 'error'].includes(connectionState.value)) {
      connectionState.value = 'closed';
    }
    isRecording.value = false;
  };
  ws.onerror = () => {
    connectionState.value = 'error';
    errorMessage.value = 'WebSocket 连接异常，请确认后端服务已启动。';
  };

  await waitForOpen(ws);
  connectionState.value = 'connected';
  return ws;
}

function waitForOpen(ws) {
  return new Promise((resolve, reject) => {
    ws.addEventListener('open', resolve, { once: true });
    ws.addEventListener('error', () => reject(new Error('无法连接数字人运行时 WebSocket 服务。')), { once: true });
  });
}

function handleServerMessage(event) {
  const message = JSON.parse(event.data);
  if (message.type === 'started') {
    connectionState.value = 'listening';
  } else if (message.type === 'recognition') {
    liveText.value = message.text || liveText.value;
    if (message.sentenceEnd && message.text) {
      stopCurrentPlayback();
      lastQuestion.value = message.text;
      appendUserMessage(message.text);
    }
  } else if (message.type === 'intent_result') {
    applyIntentResult(message);
  } else if (message.type === 'answer_started') {
    lastQuestion.value = message.question || lastQuestion.value;
    intentStatusText.value = '处理类型：知识库查询';
    answerState.value = 'querying';
    activeAssistantMessageId.value = appendAssistantMessage('', 'streaming');
  } else if (message.type === 'answer_delta') {
    appendAssistantDelta(message.text || '');
    answerState.value = 'querying';
  } else if (message.type === 'answer_completed') {
    completeAssistantMessage(message.text);
    answerState.value = 'completed';
  } else if (message.type === 'command_result') {
    intentStatusText.value = `处理类型：指令执行 / ${message.command || 'command'}`;
    appendAssistantMessage(message.text || '指令已接收。');
    answerState.value = 'handled';
  } else if (message.type === 'tool_result') {
    intentStatusText.value = `处理类型：内置工具 / ${message.intent || 'tool'}`;
    appendAssistantMessage(message.text || '工具调用已完成。');
    answerState.value = 'handled';
  } else if (message.type === 'rag_rejected') {
    intentStatusText.value = '处理类型：知识库查询 / 未发送';
    appendAssistantMessage(message.text || '知识库查询内容太短，请补充完整问题。', 'error');
    answerState.value = 'error';
  } else if (message.type === 'tts_started') {
    updateLatestAssistantMessage((target) => {
      target.ttsState = 'synthesizing';
    });
  } else if (message.type === 'tts_config') {
    ttsEnabled.value = Boolean(message.enabled);
  } else if (message.type === 'tts_stop') {
    stopCurrentPlayback();
    appendAssistantMessage(message.text || '已停止播报。');
    answerState.value = 'handled';
  } else if (message.type === 'tts_audio') {
    attachAndPlayAudio(message.audio, message.mimeType);
  } else if (message.type === 'tts_skipped') {
    updateLatestAssistantMessage((target) => {
      target.ttsState = 'idle';
    });
  } else if (message.type === 'tts_error') {
    updateLatestAssistantMessage((target) => {
      target.ttsState = 'error';
    });
    errorMessage.value = message.message || 'TTS 语音合成异常。';
  } else if (message.type === 'answer_error') {
    answerState.value = 'error';
    errorMessage.value = message.message || 'LightRAG 问答异常。';
    completeAssistantMessage(errorMessage.value, 'error');
  } else if (message.type === 'answer_skipped') {
    errorMessage.value = '当前回答仍在生成，请稍后再问。';
    appendAssistantMessage(errorMessage.value, 'error');
  } else if (message.type === 'error') {
    connectionState.value = 'error';
    errorMessage.value = message.message || '数字人运行时异常。';
    appendAssistantMessage(errorMessage.value, 'error');
  }
}

function applyIntentResult(message) {
  const labels = {
    COMMAND: '指令执行',
    DATE_TIME: '日期时间',
    WEATHER: '天气工具',
    RAG: '知识库查询'
  };
  const kind = message.kind || '';
  const slots = formatSlots(message.slots);
  intentStatusText.value = `意图识别：${labels[kind] || kind || '未知'}`;
  intentResult.value = {
    name: message.name || 'unknown',
    detail: slots ? `参数：${slots}` : `文本：${message.normalizedText || lastQuestion.value || '-'}`
  };
}

function formatSlots(slots) {
  if (!slots || typeof slots !== 'object') {
    return '';
  }
  return Object.entries(slots)
    .map(([key, value]) => `${key}=${value}`)
    .join('，');
}

async function toggleTts() {
  ttsEnabled.value = !ttsEnabled.value;
  if (socket.value?.readyState === WebSocket.OPEN) {
    socket.value.send(JSON.stringify({ type: 'config', ttsEnabled: ttsEnabled.value }));
  }
}

function appendUserMessage(content) {
  messages.value.push({
    id: crypto.randomUUID(),
    role: 'user',
    content,
    rawContent: content,
    speechText: content,
    thinking: '',
    references: '',
    audioUrl: '',
    audioMimeType: '',
    ttsState: 'idle',
    status: 'completed',
    time: formatTime()
  });
  scrollToBottom();
}

function appendAssistantMessage(content, status = 'completed') {
  const id = crypto.randomUUID();
  const parts = parseAssistantAnswer(content);
  messages.value.push({
    id,
    role: 'assistant',
    content: parts.speechText,
    rawContent: content,
    speechText: parts.speechText,
    thinking: parts.thinking,
    references: parts.references,
    audioUrl: '',
    audioMimeType: '',
    ttsState: 'idle',
    status,
    time: formatTime()
  });
  if (status === 'completed') {
    lastCompletedAssistantMessageId.value = id;
  }
  scrollToBottom();
  return id;
}

function appendAssistantDelta(delta) {
  if (!activeAssistantMessageId.value) {
    activeAssistantMessageId.value = appendAssistantMessage('', 'streaming');
  }
  const target = messages.value.find((message) => message.id === activeAssistantMessageId.value);
  if (target) {
    target.rawContent = `${target.rawContent || ''}${delta}`;
    applyAssistantAnswerParts(target, target.rawContent);
  }
  scrollToBottom();
}

function completeAssistantMessage(content, status = 'completed') {
  const target = messages.value.find((message) => message.id === activeAssistantMessageId.value);
  if (target) {
    const rawContent = content || target.rawContent || target.content || '已完成，但没有返回文本。';
    applyAssistantAnswerParts(target, rawContent);
    target.status = status;
    lastCompletedAssistantMessageId.value = target.id;
  } else if (content) {
    appendAssistantMessage(content, status);
  }
  activeAssistantMessageId.value = null;
  scrollToBottom();
}

function applyAssistantAnswerParts(message, rawContent) {
  const parts = parseAssistantAnswer(rawContent);
  message.rawContent = rawContent;
  message.speechText = parts.speechText || (message.status === 'streaming' ? '正在整理回答...' : '已完成，但没有返回正文。');
  message.content = message.speechText;
  message.thinking = parts.thinking;
  message.references = parts.references;
}

function attachAndPlayAudio(base64Audio, mimeType = 'audio/mpeg') {
  if (!base64Audio) {
    return;
  }
  updateLatestAssistantMessage((target) => {
    if (target.audioUrl) {
      URL.revokeObjectURL(target.audioUrl);
    }
    target.audioMimeType = mimeType;
    target.audioUrl = audioBlobUrl(base64Audio, mimeType);
    target.ttsState = 'ready';
    playMessageAudio(target);
  });
}

function stopCurrentPlayback() {
  currentAudio.value?.pause();
  currentAudio.value = null;
  messages.value.forEach((message) => {
    if (message.ttsState === 'playing' || message.ttsState === 'ready') {
      message.ttsState = 'ended';
    }
  });
}

function audioBlobUrl(base64Audio, mimeType) {
  const binary = atob(base64Audio);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return URL.createObjectURL(new Blob([bytes], { type: mimeType }));
}

function playMessageAudio(message) {
  if (!message.audioUrl) {
    return;
  }
  currentAudio.value?.pause();
  const audio = new Audio(message.audioUrl);
  currentAudio.value = audio;
  message.ttsState = 'playing';
  audio.onended = () => {
    message.ttsState = 'ended';
  };
  audio.onerror = () => {
    message.ttsState = 'error';
    errorMessage.value = '浏览器播放语音失败。';
  };
  audio.play().catch(() => {
    message.ttsState = 'error';
    errorMessage.value = '浏览器阻止了自动播放，请点击“重播语音”。';
  });
}

function updateLatestAssistantMessage(updater) {
  const target = messages.value.find((message) => message.id === lastCompletedAssistantMessageId.value)
    || messages.value.find((message) => message.id === activeAssistantMessageId.value);
  if (target) {
    updater(target);
  }
}

function parseAssistantAnswer(rawContent) {
  const raw = rawContent || '';
  const thinkingBlocks = [];
  let working = raw.replace(/<think\b[^>]*>([\s\S]*?)<\/think>/gi, (_, thinking) => {
    thinkingBlocks.push(thinking.trim());
    return '';
  });

  working = working.replace(/<think\b[^>]*>[\s\S]*$/i, (thinking) => {
    thinkingBlocks.push(thinking.replace(/<think\b[^>]*>/i, '').trim());
    return '';
  });

  const referencesMatch = working.match(/(?:^|\n)\s*(?:#{1,6}\s*)?References\s*:?\s*\n?/i);
  let references = '';
  if (referencesMatch?.index !== undefined) {
    references = working.slice(referencesMatch.index + referencesMatch[0].length).trim();
    working = working.slice(0, referencesMatch.index).trim();
  }

  return {
    speechText: cleanupAnswerText(working),
    thinking: cleanupAnswerText(thinkingBlocks.filter(Boolean).join('\n\n')),
    references: cleanupAnswerText(references)
  };
}

function cleanupAnswerText(text) {
  return (text || '')
    .replace(/<\/?think\b[^>]*>/gi, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function clearMessages() {
  messages.value.forEach((message) => {
    if (message.audioUrl) {
      URL.revokeObjectURL(message.audioUrl);
    }
  });
  currentAudio.value?.pause();
  currentAudio.value = null;
  messages.value = [];
  liveText.value = '';
  lastQuestion.value = '';
  answerState.value = 'idle';
  intentStatusText.value = '等待意图识别';
  intentResult.value = { name: '', detail: '' };
  errorMessage.value = '';
  activeAssistantMessageId.value = null;
  lastCompletedAssistantMessageId.value = null;
}

function startNewConversation() {
  clearMessages();
  messages.value.push({
    id: crypto.randomUUID(),
    role: 'assistant',
    content: '新的会话已开始。输入问题或使用语音，我会继续把对话接到数字人运行时。',
    rawContent: '新的会话已开始。输入问题或使用语音，我会继续把对话接到数字人运行时。',
    speechText: '新的会话已开始。输入问题或使用语音，我会继续把对话接到数字人运行时。',
    thinking: '',
    references: '',
    audioUrl: '',
    audioMimeType: '',
    ttsState: 'idle',
    status: 'completed',
    time: formatTime()
  });
}

function resizeComposer(event) {
  const target = event.target;
  target.style.height = 'auto';
  target.style.height = `${Math.min(target.scrollHeight, 148)}px`;
}

function scrollToBottom() {
  nextTick(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        if (messageViewport.value) {
          messageViewport.value.scrollTop = messageViewport.value.scrollHeight;
        }
      });
    });
  });
}

async function cleanup() {
  await streamer.value?.stop();
  streamer.value = null;
  currentAudio.value?.pause();
  currentAudio.value = null;
  messages.value.forEach((message) => {
    if (message.audioUrl) {
      URL.revokeObjectURL(message.audioUrl);
    }
  });
  socket.value?.close();
  socket.value = null;
  isRecording.value = false;
  level.value = 0;
}

onBeforeUnmount(cleanup);
</script>
