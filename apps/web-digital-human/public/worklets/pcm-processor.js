class PcmProcessor extends AudioWorkletProcessor {
  constructor(options) {
    super();
    this.targetSampleRate = options.processorOptions?.targetSampleRate || 16000;
    this.frameSamples = options.processorOptions?.frameSamples || 1600;
    this.ratio = sampleRate / this.targetSampleRate;
    this.source = [];
    this.sourceIndex = 0;
    this.frame = new Int16Array(this.frameSamples);
    this.frameIndex = 0;
    this.levelTick = 0;
  }

  process(inputs) {
    const channel = inputs[0]?.[0];
    if (!channel || channel.length === 0) {
      return true;
    }

    let sum = 0;
    for (let index = 0; index < channel.length; index += 1) {
      const sample = channel[index];
      this.source.push(sample);
      sum += sample * sample;
    }

    this.levelTick += 1;
    if (this.levelTick >= 4) {
      this.levelTick = 0;
      this.port.postMessage({
        type: 'level',
        level: Math.min(1, Math.sqrt(sum / channel.length) * 8)
      });
    }

    while (this.sourceIndex < this.source.length) {
      const sample = this.source[Math.floor(this.sourceIndex)] || 0;
      this.frame[this.frameIndex] = this.toInt16(sample);
      this.frameIndex += 1;
      this.sourceIndex += this.ratio;

      if (this.frameIndex === this.frameSamples) {
        const payload = this.frame.buffer.slice(0);
        this.port.postMessage({ type: 'frame', payload }, [payload]);
        this.frame = new Int16Array(this.frameSamples);
        this.frameIndex = 0;
      }
    }

    const consumed = Math.floor(this.sourceIndex);
    if (consumed > 2048) {
      this.source.splice(0, consumed);
      this.sourceIndex -= consumed;
    }

    return true;
  }

  toInt16(sample) {
    const clamped = Math.max(-1, Math.min(1, sample));
    return clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff;
  }
}

registerProcessor('pcm-processor', PcmProcessor);
