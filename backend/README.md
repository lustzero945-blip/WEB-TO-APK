# LUST Web-to-APK Production Backend Architecture

Welcome to the **LUST (Lust URL Studio & Technology) APK Compilation Ecosystem Backend** – a complete, production-grade asynchronous, full-stack compilation system. It is designed to take custom client website target configurations, generate a dedicated native Android Jetpack Compose WebView wrapper project, compile it using native Gradle daemons, and package/sign it using standard Java Keystore signatures.

---

## 🏗️ Architectural Topology Blueprint

The system consists of independent containerized modern microservices orchestrating dynamic compilation sequences:

```
                      +---------------------------------------+
                      |       JSON Web Token (JWT) Guard      |
                      +---------------------------------------+
                                          |
                                          v
+------------------+         +-------------------------+         +----------------------+
|   REST Gateway   |  ---->  |  Redis Task Broker Bus  |  ---->  | Celery Build Workers |
|  (Python FastAPI)|         |        (Redis:7)        |         | (JDK 17 + Gradle v8) |
+------------------+         +-------------------------+         +----------------------+
         |                                                                   |
         v                                                                   v
+------------------+                                                 +----------------------+
|  User PostgreSQL |                                                 | Solid Storage Engine |
|   Persistence    |                                                 |  (Local/S3 Buckets)  |
+------------------+                                                 +----------------------+
```

1. **REST API Gateway (FastAPI)**: Lightweight gateway facilitating client routing (identity authentication, project setups, custom icons upload, keystores registry, compilation status tracking).
2. **Asynchronous Message Bus (Redis)**: High-speed publish/subscribe message broker distributing builder jobs.
3. **Background Build Runners (Celery)**: Persistent task processors packaged with OpenJDK-17 and Android platform Gradle compilation tools. They fetch tasks, scaffold projects, call Gradle builds, capture live logs, and sign compiled APK packages.
4. **Relational Engine (PostgreSQL)**: Handles relational profiles (Users, projects, builds, certificated keystores metadata).
5. **Storage Abstraction Layer**: Isolated user directories enforcing individual quotas (e.g., 100MB accounts) and automatic expired APK pruning.

---

## 🗄️ Relational Database Layout (PostgreSQL)

The persistence engine contains four interlocked models:

### 1. User Model (`users`)
- Matches credentials and structures secure ownership boundaries.
- **Attributes**: `id`, `username`, `email`, `password_hash`, `created_at`.

### 2. Project Model (`projects`)
- Houses Web-To-APK parameters (dimensions, client features, target layout).
- **Attributes**: `id`, `user_id`, `name`, `website_url`, `package_name`, `version`, `icon_path`, `configuration_json`, `created_at`, `updated_at`.

### 3. Build Model (`builds`)
- Tracks compilation logs and progress state.
- **Attributes**: `id`, `project_id`, `status` (Enum), `progress_percentage`, `build_logs` (Stream database cell), `apk_path`, `error_message`, `started_at`, `finished_at`.

### 4. Keystore Model (`keystores`)
- Holds custom `.jks` or `.keystore` certificate signatures, encrypting passphrases via AES-GCM prior to database saves.
- **Attributes**: `id`, `user_id`, `encrypted_file_path`, `alias`, `encrypted_password`, `created_at`.

---

## 🚦 Endpoints & REST Interface Spec

All endpoints reside under the `/api` namespace. Bearer tokens must accompany authenticated routing.

### 🔐 Authentication Router `/api/auth`
- `POST /register`: Registers unique developer credentials.
- `POST /login`: Standard OAuth2 Form callback returning Bearer JWT tokens.

### 🌐 Project Configuration Router `/api/projects`
- `POST /projects`: Saves a new project configuration. Checks package validation against Android Reverse-DNS (e.g. `com.demo.app`) and URL structures.
- `GET /projects`: Fetches active user configs.
- `PUT /projects/{id}`: Mutates configurations dynamically.
- `DELETE /projects/{id}`: Purges projects and local storage assets.
- `POST /projects/{id}/icon`: Uploads and overrides launcher assets, verifying extensions (`.png`, `.webp`, `.jpg`) and sizes (<5MB).

### ⚙️ Asynchronous Build Router `/api/builds`
- `POST /projects/{id}/build`: Queues a compile job. Accepts an optional `keystore_id` payload.
- `GET /builds/{id}/status`: Polling state endpoint returning percentage metrics.
- `GET /builds/{id}/logs`: Returns captured stdout compilations console logs streams.
- `GET /builds/{id}/download-url`: Returns temporary signed download ticket valid for 1 hour.
- `GET /builds/download/{token}`: Served file-response downloading signed APK.

### 🔑 Keystore Certificates Router `/api/keystores`
- `POST /keystores`: Registers certificate files (`.jks`, `.keystore`). Encrypts passwords with Fernet keys.
- `GET /keystores`: Lists credentials.
- `DELETE /keystores/{id}`: Revokes certificate files.

---

## 🌪️ Build Queue State-Machine Transitions

The Celery worker executes the state management sequences during compilation:

```
[QUEUED] (0%) -> Standby queue loop in Redis.
  |
  v
[PREPARING] (10%) -> Formulate secure workspace directory.
  |
  v
[GENERATING_PROJECT] (25%) -> Scaffold Android project (Gradle configurations, Kotlin files, assets).
  |
  v
[CONFIGURING_GRADLE] (40%) -> Set buildSDK targets, app version, dynamically link packages.
  |
  v
[BUILDING_APK] (60%) -> Run shell subprocess "gradle :app:assembleRelease", stream lines to Postgres cell.
  |
  v
[SIGNING_APK] (85%) -> Decrypt certificate passwords, align keys, and sign compiling packages using apksigner.
  |
  v
[COMPLETED] (100%) -> Close workspace, register APK download permissions, release cache storage.
```

---

## 🛠️ Local Deployments & Booting

Spin up the entire system local setup under Docker using:

```bash
# Navigate to backend path
cd backend

# Boot services using docker-compose
docker-compose up --build
```

### Manual Development Setup (No Docker)

```bash
# Set up a Python Virtual Environment
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Launch PostgreSQL and Redis locally, then start services:
# Start FastAPI Web service
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Start Celery build worker
celery -A app.tasks.worker.celery_app worker --loglevel=info
```
