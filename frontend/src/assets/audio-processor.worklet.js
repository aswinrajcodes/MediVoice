// MediVoice AudioWorklet Processor
// Runs inside AudioWorklet — no Angular dependencies allowed here
// Downsamples from browser sample rate to 16000Hz LINEAR16 PCM

class MediVoiceAudioProcessor extends AudioWorkletProcessor {

  process(inputs, outputs, parameters) {
    const input = inputs[0]?.[0];
    if (!input || input.length === 0) return true;

    // Downsample using linear interpolation
    const ratio = sampleRate / 16000;
    const outputLength = Math.floor(input.length / ratio);
    const downsampled = new Float32Array(outputLength);

    for (let i = 0; i < outputLength; i++) {
      const srcIndex = i * ratio;
      const floor = Math.floor(srcIndex);
      const frac = srcIndex - floor;
      const nextSample = floor + 1 < input.length ? input[floor + 1] : 0;
      downsampled[i] = input[floor] * (1 - frac) + nextSample * frac;
    }

    // Convert Float32 to Int16 PCM
    const pcm16 = new Int16Array(downsampled.length);
    for (let i = 0; i < downsampled.length; i++) {
      const s = Math.max(-1, Math.min(1, downsampled[i]));
      pcm16[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
    }

    // Post the Int16 buffer to the main thread
    this.port.postMessage(pcm16.buffer, [pcm16.buffer]);
    return true;
  }
}

registerProcessor('medivoice-audio-processor', MediVoiceAudioProcessor);
