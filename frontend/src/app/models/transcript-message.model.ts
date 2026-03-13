export interface TranscriptMessage {
  role: 'user' | 'assistant';
  text: string;
  timestamp: Date;
}
