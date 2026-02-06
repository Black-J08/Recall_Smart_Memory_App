# Recall - Smart Memory App

Recall is a privacy-focused Android application that runs a powerful Large Language Model (LLM) entirely on-device using Google MediaPipe.

## 🚀 Key Features

-   **100% Offline AI**: Runs strictly on-device with no data leaving your phone.
-   **Ultra-Low RAM Support**: Optimized for devices with as little as 2GB RAM.
-   **MediaPipe Backend**: Uses Google's efficient `tasks-genai` library for inference.
-   **Qwen 2.5 0.5B**: Powered by the Qwen-0.5B-Instruct model (Int8 Quantized), delivering smart responses in a tiny footprint (~550MB).
-   **Clean Architecture**: Built with Kotlin, Jetpack Compose, Hilt, and MVVM.

## 🛠️ Architecture

The app follows standard Android modern architecture principles:

-   **UI Layer**: Jetpack Compose + `ChatViewModel`.
-   **Domain Layer**: Clean interfaces for `AIEngine`, `SettingsRepository`, and `ChatRepository`.
-   **Data Layer**:
    -   `MediaPipeAIEngine`: Implementation of the AI interface using MediaPipe.
    -   `Room Database`: Local storage for chat history.
    -   `FileDownloader`: Resumable downloads for AI models.

### AI Engine Strategy
We migrated from MLC LLM to **MediaPipe** to improve device compatibility and simplify the build process.
-   **Engine**: `MediaPipeAIEngine` (wraps `LlmInference`).
-   **Model**: `.task` bundle (includes model + tokenizer).
-   **Download**: Models are hosted on GitHub Releases to bypass authentication issues.

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
