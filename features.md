# Features Tracker

This document tracks the status of features in the Recall application, including what is currently implemented, what is planned for future releases, and what has been removed or deprecated.

## ✅ Verified / Implemented Features

### Core AI
- **Offline LLM Inference**: 
  - Powered by **MediaPipe** (Google `tasks-genai`).
  - Runs 100% on-device (CPU/GPU).
- **Supported Models**:
  - **Qwen 2.5 0.5B (Int8)**: Lite tier optimized for efficiency (~550MB). Best for devices with 2-4GB RAM.
  - **Qwen 2.5 1.5B (Int8)**: Standard tier balanced for reasoning (~1.5GB). Recommended for devices with 6GB+ RAM.
- **On-Device RAG (Retrieval-Augmented Generation)**: 
  - **Memory Retrieval**: Uses captured notes (Text/Audio/Image captions) as grounded context for the Chat LLM.
  - **Local Embedding**: Utilizes MediaPipe **Text Embedder** (Universal Sentence Encoder) for local vectorization.
  - **In-Memory Search**: High-performance Cosine Similarity implementation for semantic retrieval.
  - **Context Augmentation**: Dynamically injects the top 3 relevant memories into the LLM prompt to enable true "Recall".

### Chat Interface
- **Chat Screen**: Full conversational interface with user/AI bubbles.
  - **Dynamic Title Generation**: LLM automatically generates concise titles after the first exchange.
  - **Bubble Merging**: Adjacent messages from the same sender merge visually for better readability.
  - **Modern UI/UX**: Gradients, dark mode aesthetics, and smooth animations.
- **Session Management**: 
  - Automatic new session creation and robust cleanup of empty sessions.
  - History drawer with selection indicators and prominent "New Chat" action.
  - **Context Isolation**: Strict message collector management prevents "ghost conversation" switching and UI leaks.
- **Streaming Responses**: Real-time token generation using **Transient UI State** to prevent flickering and DB contention.
- **Keyboard Optimization**: `reverseLayout` and `adjustResize` support ensures messages stay visible when typing.

### Memory Capture (Feed)
- **Text Notes**: Capture text-based memories via `TextCaptureStrategy`.
- **Image Capture**: 
  - **Hybrid Acquisition**: Choice between **CameraX** (in-app viewfinder) and **System Photo Picker** (Gallery).
  - **Privacy First**: Uses scoped storage via `MediaStorageManager` and privacy-preserving pickers.
- **Audio Capture**:
  - **High Fidelity**: Records in AAC (`.m4a`) format for optimized quality/size.
  - **Visual Feedback**: Real-time **Waveform Visualization** using `AudioRecorder` amplitude data.
- **Feed UI**: Timeline view of captured memories with type-specific cards (Text/Image/Audio).

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
- **Long-Term Memory Management**:
  - *Semantic Deduplication*: Automatically merge or skip redundant memories to prevent clutter.
  - *Memory Decay/Relevance*: Implement aging scores for notes to prioritize recent or frequently accessed data.
  - *Knowledge Distillation*: User-triggered or scheduled "Daily/Weekly Summaries" to condense many small notes into master memories.
- **Context-Aware Chat**: 
  - *Stateful Sessions*: Upgrade to **LlmInferenceSession** for KV-cached, high-performance long-form conversations.
  - *Smart Restoration*: Restore the last ~10-20 turns of history when switching chats to warm the AI context.


### Model Support
- **Larger Models**: Support for Qwen 3B for devices with more RAM (8GB+).
- **Custom Model Import**: Allow users to import their own `.task` bundles.

---

## 🛑 Removed / Deprecated Features

- **MLC LLM Engine**: 
  - *Reason*: Replaced by **MediaPipe** for better compatibility, smaller binary size, and easier build process.
  - *Status*: Removed.
