# Features Tracker

This document tracks the status of features in the Recall application, including what is currently implemented, what is planned for future releases, and what has been removed or deprecated.

## ✅ Verified / Implemented Features

### Core AI
- **Offline LLM Inference**: 
  - Powered by **MediaPipe** (Google `tasks-genai`).
  - Runs 100% on-device (CPU/GPU).
- **Supported Model**:
  - **Qwen 2.5 0.5B (Int8)**: Highly optimized for mobile (~550MB). Verified working on devices with >= 2GB RAM.

### Chat Interface
- **Chat Screen**: Full conversational interface with user/AI bubbles.
  - **Dynamic Title Generation**: LLM automatically generates concise titles after the first exchange.
  - **Bubble Merging**: Adjacent messages from the same sender merge visually for better readability.
  - **Modern UI/UX**: Gradients, dark mode aesthetics, and smooth animations.
- **Session Management**: 
  - Automatic new session creation and robust cleanup of empty sessions.
  - History drawer with selection indicators and prominent "New Chat" action.
  - Data persisted in local Room database with concurrency-safe message routing.
- **Streaming Responses**: Real-time token generation with session-specific typing indicators.
- **Keyboard Optimization**: `reverseLayout` and `adjustResize` support ensures messages stay visible when typing.

### Memory Capture (Feed)
- **Text Notes**: Capture text-based memories via `TextCaptureStrategy`.
- **Feed UI**: Timeline view of captured memories.

### App Infrastructure
- **Model Management**: 
  - Download models from GitHub Releases.
  - Check download status (downloaded/not downloaded).
- **Background Model Download**:
  - *Goal*: Persistent downloads using `WorkManager` or `DownloadManager` that continue even if the app is closed.
- **Clean Architecture**: MVVM + Domain Layer + Hilt DI.

---

## 🚧 Planned / Future Features

### Advanced AI Capabilities
- **System Prompt Support (ChatML)**:
  - *Status*: Supported (via prompt wrapping).
  - *Capability*: Can define model behavior/persona by prepending ChatML system tags (`<|im_start|>system...`).
- **RAG (Retrieval Augmented Generation)**: 
  - *Goal*: Use captured memories (Text/Image/Audio) as grounded context for the Chat LLM.
  - *On-Device Embedding*: Utilize MediaPipe **Text Embedder** (Gecko) for local vectorization.
  - *Vector Search*: Integrate an on-device vector database (e.g., **ObjectBox 4.0**) for semantic retrieval.
  - *Context Injection*: Dynamically "stuff" retrieved memories into the LLM prompt to enable true "Recall".
- **Long-Term Memory Management**:
  - *Semantic Deduplication*: Automatically merge or skip redundant memories to prevent clutter.
  - *Memory Decay/Relevance*: Implement aging scores for notes to prioritize recent or frequently accessed data.
  - *Knowledge Distillation*: User-triggered or scheduled "Daily/Weekly Summaries" to condense many small notes into master memories.
- **Context-Aware Chat**: 
  - *Stateful Sessions*: Upgrade to **LlmInferenceSession** for KV-cached, high-performance long-form conversations.
  - *Smart Restoration*: Restore the last ~10-20 turns of history when switching chats to warm the AI context.

### Memory Capture Enhancements
- **Image Capture**: 
  - *Current Status*: `ImageCaptureStrategy` exists as a placeholder ("Coming soon").
  - *Plan*: Integrate CameraX or System Photo Picker.
- **Audio Capture**:
  - *Current Status*: `AudioCaptureStrategy` exists as a placeholder ("Coming soon").
  - *Plan*: Voice recording + Transcription (Whisper-tiny on-device?).

### Model Support
- **Larger Models**: Support for Qwen 1.5B or 3B for devices with more RAM (8GB+).
- **Custom Model Import**: Allow users to import their own `.task` bundles.

---

## 🛑 Removed / Deprecated Features

- **MLC LLM Engine**: 
  - *Reason*: Replaced by **MediaPipe** for better compatibility, smaller binary size, and easier build process.
  - *Status*: Removed.
