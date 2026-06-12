const TARGET_SAMPLE_RATE = 16000;

export class FunAsrPcmStreamer {
  constructor({ onAudioFrame, onLevel }) {
    this.onAudioFrame = onAudioFrame;
    this.onLevel = onLevel;
    this.audioContext = null;
    this.mediaStream = null;
    this.sourceNode = null;
    this.workletNode = null;
    this.silentGain = null;
  }

  async start() {
    this.mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    });

    this.audioContext = new AudioContext();
    await this.audioContext.audioWorklet.addModule('/worklets/pcm-processor.js');

    this.sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream);
    this.workletNode = new AudioWorkletNode(this.audioContext, 'pcm-processor', {
      processorOptions: {
        targetSampleRate: TARGET_SAMPLE_RATE,
        frameSamples: 1600
      }
    });
    this.silentGain = this.audioContext.createGain();
    this.silentGain.gain.value = 0;

    this.workletNode.port.onmessage = (event) => {
      const { type, payload, level } = event.data;
      if (type === 'frame' && payload) {
        this.onAudioFrame?.(payload);
      }
      if (type === 'level') {
        this.onLevel?.(level);
      }
    };

    this.sourceNode.connect(this.workletNode);
    this.workletNode.connect(this.silentGain);
    this.silentGain.connect(this.audioContext.destination);
  }

  async stop() {
    this.workletNode?.disconnect();
    this.sourceNode?.disconnect();
    this.silentGain?.disconnect();
    this.mediaStream?.getTracks().forEach((track) => track.stop());

    if (this.audioContext && this.audioContext.state !== 'closed') {
      await this.audioContext.close();
    }

    this.audioContext = null;
    this.mediaStream = null;
    this.sourceNode = null;
    this.workletNode = null;
    this.silentGain = null;
  }
}
