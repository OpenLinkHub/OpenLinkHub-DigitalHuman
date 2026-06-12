<template>
  <main class="shell">
    <section class="voice-console">
      <div class="header">
        <div>
          <p class="eyebrow">DashScope FunASR Realtime</p>
          <h1>实时语音对话</h1>
        </div>
        <div class="status" :data-state="connectionState">{{ statusText }}</div>
      </div>

      <div class="signal-panel">
        <div class="meter" aria-hidden="true">
          <span
            v-for="bar in meterBars"
            :key="bar"
            :style="{ transform: `scaleY(${barScale(bar)})` }"
          />
        </div>
        <button
          class="talk-button"
          :class="{ recording: isRecording }"
          :disabled="isBusy && !isRecording"
          @click="toggleRecording"
        >
          <span>{{ isRecording ? '停止对话' : '开始对话' }}</span>
        </button>
      </div>

      <div class="transcript-grid">
        <section class="transcript-block">
          <div class="block-title">实时字幕</div>
          <p class="live-text">{{ liveText || '等待语音输入' }}</p>
        </section>
        <section class="transcript-block">
          <div class="block-title">最终文本</div>
          <p class="final-text">{{ finalText || '完整句子会在这里沉淀' }}</p>
        </section>
      </div>

      <div v-if="errorMessage" class="error-line">{{ errorMessage }}</div>

      <footer class="footer">
        <span>WebSocket: {{ wsUrl }}</span>
        <span>PCM 16kHz / mono / 100ms frame</span>
      </footer>
    </section>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue';
import { FunAsrPcmStreamer } from './audio/funasrPcmStreamer';

const DEFAULT_BACKEND_PORT = 8080;
const wsUrl = import.meta.env.VITE_FUNASR_WS_URL
  || `ws://${window.location.hostname}:${DEFAULT_BACKEND_PORT}/ws/funasr/realtime`;

const connectionState = ref('idle');
const isRecording = ref(false);
const errorMessage = ref('');
const liveText = ref('');
const finalText = ref('');
const level = ref(0);
const socket = ref(null);
const streamer = ref(null);
const meterBars = Array.from({ length: 36 }, (_, index) => index);

const isBusy = computed(() => ['connecting', 'stopping'].includes(connectionState.value));
const statusText = computed(() => {
  const labels = {
    idle: '待机',
    connecting: '连接中',
    listening: '监听中',
    stopping: '停止中',
    closed: '已停止',
    error: '异常'
  };
  return labels[connectionState.value] || connectionState.value;
});

function barScale(index) {
  const center = (meterBars.length - 1) / 2;
  const distance = Math.abs(index - center) / center;
  const shaped = 1 - distance * 0.72;
  return Math.max(0.08, shaped * (0.18 + level.value));
}

async function toggleRecording() {
  if (isRecording.value) {
    await stopConversation();
  } else {
    await startConversation();
  }
}

async function startConversation() {
  errorMessage.value = '';
  liveText.value = '';
  finalText.value = '';
  connectionState.value = 'connecting';

  try {
    const ws = new WebSocket(wsUrl);
    ws.binaryType = 'arraybuffer';
    socket.value = ws;
    ws.onmessage = handleServerMessage;
    ws.onclose = () => {
      if (connectionState.value !== 'stopping') {
        connectionState.value = 'closed';
      }
      isRecording.value = false;
    };
    ws.onerror = () => {
      connectionState.value = 'error';
      errorMessage.value = 'WebSocket 连接异常，请确认后端服务已启动。';
    };

    await waitForOpen(ws);
    ws.send(JSON.stringify({ type: 'start', sampleRate: 16000 }));

    streamer.value = new FunAsrPcmStreamer({
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

  if (socket.value?.readyState === WebSocket.OPEN) {
    socket.value.send(JSON.stringify({ type: 'stop' }));
    window.setTimeout(() => socket.value?.close(), 600);
  }
  connectionState.value = 'closed';
}

async function cleanup() {
  await streamer.value?.stop();
  streamer.value = null;
  socket.value?.close();
  socket.value = null;
  isRecording.value = false;
}

function waitForOpen(ws) {
  return new Promise((resolve, reject) => {
    ws.onopen = resolve;
    ws.onerror = () => reject(new Error('无法连接 FunASR WebSocket 服务。'));
  });
}

function handleServerMessage(event) {
  const message = JSON.parse(event.data);
  if (message.type === 'recognition') {
    liveText.value = message.text || liveText.value;
    if (message.sentenceEnd && message.text) {
      finalText.value = message.text;
    }
  } else if (message.type === 'error') {
    connectionState.value = 'error';
    errorMessage.value = message.message || 'FunASR 实时识别异常。';
  }
}
</script>
