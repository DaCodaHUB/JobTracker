# JobTracker - Offline-First Android Application

JobTracker is a professional-grade Android application designed for managing job applications with a robust, offline-first synchronization engine. It demonstrates modern Android development practices, including Material 3 theming, Shared Element Transitions, and a reliable Single Source of Truth (SSOT) architecture.

## 🚀 Key Features

- **Offline-First Architecture**: View, create, and modify job applications without an internet connection.
- **Robust Synchronization**: Automatic background synchronization using WorkManager and GraphQL mutations.
- **Conflict Resolution UI**: Interactive side-by-side comparison for resolving divergent local and server states.
- **Modern UI/UX**: Built entirely with Jetpack Compose, featuring Material 3 Dynamic Color and seamless Shared Element Transitions.
- **Real-time Updates**: Support for GraphQL subscriptions to keep the local database in sync with remote changes.

## 🏗️ Architecture & Technology Stack

### Core Architecture
- **Single Source of Truth (SSOT)**: The local Room database is the authoritative source for the UI.
- **Unidirectional Data Flow (UDF)**: MVI-style state management using `StateFlow` and centralized event handling.
- **Repository Pattern**: Abstracts data sources (Room + Apollo GraphQL) and manages the complex reconciliation logic.

### Technology Stack
- **UI**: Jetpack Compose (1.7.0+), Material Design 3.
- **Local Database**: Room with custom TypeConverters for complex domain enums.
- **Remote API**: Apollo GraphQL (v5.0) with custom engine configuration.
- **Background Tasks**: WorkManager with Hilt integration for reliable data "flushing".
- **Dependency Injection**: Hilt (Dagger) with custom Dispatcher modules.
- **Networking**: OkHttp 5 with logging interceptors.

## 🔄 Synchronization Engine

The app uses a sophisticated state machine to manage data consistency:

1.  **Local Write**: Operations immediately update Room and mark the entity with a `SyncStatus` (`PENDING_CREATE`, `PENDING_UPDATE`, or `PENDING_DELETE`).
2.  **Background Flush**: `SyncJobApplicationsWorker` iterates through pending items and executes the corresponding GraphQL mutations.
3.  **Version-Based Reconciliation**: Uses optimistic locking (version numbers) to detect conflicts.
4.  **Manual Conflict Resolution**: If the server rejects a change due to a version mismatch, the item is marked as `CONFLICT`, and the user is prompted to choose between local and server versions via a comparison dialog.

## 📁 Project Structure

```text
com.dangle.jobtracker
├── data/          # Local Database, Apollo configuration, Repository, and Workers
├── di/            # Hilt Modules
├── domain/        # Pure Kotlin Domain Models
├── ui/            # Screens, Components, and Theme
└── util/          # Utilities
graphql-server/    # Node.js GraphQL backend with Subscriptions support
```

## 🧪 Testing Strategy

The project maintains a high standard of reliability through comprehensive testing:

- **DAO Tests**: Instrumented tests (`androidTest`) using an in-memory Room database to verify SQLite queries and atomic updates.
- **Repository Tests**: Unit tests using **MockK** for dependency isolation and **Turbine** for verifying Flow emissions.
- **Worker Tests**: Integration-style unit tests using `TestListenableWorkerBuilder` and **Robolectric** to simulate network failures and verify WorkManager retry logic.

### Running Tests
- **Unit Tests**: `./gradlew :app:testDebugUnitTest`
- **Instrumented Tests**: `./gradlew :app:connectedDebugAndroidTest`

## 🛠️ Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17
- Node.js (for the backend server)

### Backend Setup
The project includes a mock GraphQL backend server in the `graphql-server/` directory.

1.  Navigate to the directory:
    ```bash
    cd graphql-server
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```
3.  Start the server:
    ```bash
    npm start
    ```
    The server will be running at `http://localhost:4000/graphql`.

### Android App Configuration
Create a `local.properties` file in the root directory and point it to your machine's IP address (not localhost, as the emulator needs to reach your host):

```properties
api.url="http://<YOUR_IP_ADDRESS>:4000/graphql"
```

## 📄 License
This project is for demonstration and professional development purposes.
