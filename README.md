# Recall - Smart Memory App

Recall is a privacy-focused Android application that runs a powerful Large Language Model (LLM) entirely on-device using Google MediaPipe.

## 🚀 Key Features

-   **100% Offline AI**: Runs strictly on-device with no data leaving your phone.
-   **On-Device RAG (Retrieval-Augmented Generation)**: Automatically remembers and utilizes your saved notes/memories during chat conversations for grounded responses.
-   **Safe Multi-Session Chat**: Robust synchronization prevents message misrouting even when switching conversations rapidly.
-   **Production-Grade UI**: Uses "Transient UI State" for flicker-free streaming responses and `reverseLayout` for perfect soft-keyboard handling.
-   **MediaPipe Backend**: Uses Google's efficient `tasks-genai` library with **Stateful Sessions** and KV-caching.
-   **Qwen 2.5 0.5B**: Powered by Qwen-0.5B-Instruct (Int8), providing a smart 550MB footprint.

## 🛠️ Architecture

Recall follows Modern Android Architecture (MVI/MVVM) with a focus on thread-safety and offline-first reliability:

-   **Transient UI State**: Streaming LLM output is held in a temporary state before persisting, ensuring DB performance and UI stability.
-   **On-Device Vector Search**: Uses MediaPipe `TextEmbedder` (Universal Sentence Encoder) to convert memories into 512D vectors, stored efficiently as BLOBs in Room.
-   **Semantic Retrieval**: `VectorSearchService` performs in-memory Cosine Similarity search to find the most relevant context for your queries.
-   **Stateful Engine**: `MediaPipeAIEngine` manages long-form context via `LlmInferenceSession`, complete with history truncation to prevent OOM.
-   **Clean Architecture**: Separation of concerns between `AIEngine`, `VectorSearchService`, `ChatRepository`, and the UI.
-   **Room DB**: Reactive message persistence with strict collector management to prevent data leaks.

### Engineering Highlights
-   **Mutex-Guarded Inference**: Serialized requests to the single-instance LLM engine prevent corrupted outputs.
-   **Stateless Title Generation**: Concurrent task-specific generation that doesn't pollute the main conversation context.
-   **Keyboard Resilience**: `adjustResize` and reverse indexing ensure the input field and latest messages are always visible.

## 📱 Getting Started

### Prerequisites
-   Android Studio Koala or newer.
-   JDK 17+.
-   Physical Android device (Emulator support for GPU/NPU is limited).

### Building
1.  Clone the repo:
    ```bash
    git clone https://github.com/Black-J08/Recall.git
    ```
2.  Open in Android Studio.
3.  Sync Gradle.
4.  Run on device: `./gradlew installDebug`.

### Usage
1.  **Download Model**: On first launch, go to **Settings** and tap download on "Qwen 2.5 0.5B".
2.  **Chat**: Navigate to the Chat screen and start typing.

## 📦 Adding New Models

To add a new MediaPipe-compatible model:
1.  **Host**: Upload the `.bin` or `.task` file to a GitHub Release.
2.  **Config**: Add a new `AIModel` entry in `AIModel.kt`:
    ```kotlin
    val NEW_MODEL = AIModel(
        id = "new_model_id",
        displayName = "New Model 1B",
        filename = "model_file.task",
        downloadUrl = "https://github.com/.../model_file.task",
        ...
    )
    ```

## 🤝 Contributing
1.  Fork the Project.
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the Branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.
