# ✚ MediVoice

[![Google Cloud](https://img.shields.io/badge/Google%20Cloud-Deployed-blue?logo=google-cloud)](https://cloud.google.com)
[![Gemini Live API](https://img.shields.io/badge/Gemini%20Live%20API-Powered-orange?logo=google)](https://ai.google.dev)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=spring)](https://spring.io)
[![Angular](https://img.shields.io/badge/Angular-21-red?logo=angular)](https://angular.dev)

> **Always Listening. Always Watching. Always Ready.**

**MediVoice** is a real-time AI medical symptom checker that uses **voice** and **vision** to help users understand their symptoms. Powered by the **Gemini Live API**, it provides calm, spoken guidance in real time — with automatic emergency detection.

Built for the **Gemini Live Agent Challenge** hackathon by Google on Devpost.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser (Angular)                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐   │
│  │  Voice    │  │  Camera  │  │Transcript│  │  Emergency    │   │
│  │  Panel    │  │  Feed    │  │  Display │  │  Panel        │   │
│  └────┬─────┘  └────┬─────┘  └──────────┘  └───────────────┘   │
│       │              │                                           │
│  ┌────┴──────────────┴─────────────────────────────────────┐    │
│  │              WebSocket Service (Binary + JSON)           │    │
│  │         AudioWorklet ←─→ PCM16 @ 16kHz                  │    │
│  └──────────────────────┬──────────────────────────────────┘    │
└─────────────────────────┼───────────────────────────────────────┘
                          │ WebSocket /ws
┌─────────────────────────┼───────────────────────────────────────┐
│                   Spring Boot Backend                            │
│  ┌──────────────────────┴──────────────────────────────────┐    │
│  │              WebSocketHandler (Core)                      │    │
│  │   Binary Audio → Gemini    JSON → Triage/Session Mgmt    │    │
│  └──┬──────────┬──────────┬──────────┬─────────────────────┘    │
│     │          │          │          │                           │
│  ┌──┴──┐   ┌──┴──┐   ┌──┴──┐   ┌──┴──┐                       │
│  │Gemini│   │Triage│   │Kafka │   │Neo4j│                       │
│  │Live  │   │Drools│   │Events│   │Graph│                       │
│  │API   │   │Rules │   │      │   │     │                       │
│  └──┬───┘   └─────┘   └──┬───┘   └──┬──┘                      │
│     │                     │          │                           │
└─────┼─────────────────────┼──────────┼──────────────────────────┘
      │                     │          │
  ┌───┴────┐          ┌────┴───┐  ┌───┴────┐
  │ Gemini │          │ Kafka  │  │ Neo4j  │
  │ 2.0    │          │ Topics │  │ Graph  │
  │ Flash  │          │   (4)  │  │  DB    │
  └────────┘          └────────┘  └────────┘
```

---

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Node.js 20+
- Docker Desktop (6GB+ RAM allocated)
- A Gemini API key from [Google AI Studio](https://aistudio.google.com)

### 1. Clone and configure

```bash
git clone https://github.com/aswinrajcodes/MediVoice.git
cd MediVoice
cp .env.example .env
# Edit .env and add your GEMINI_API_KEY
```

### 2. Start with Docker Compose

```bash
docker-compose up --build
```

Wait ~60 seconds for all services, then open **http://localhost:4200**

### 3. Development mode (without Docker)

**Backend:**
```bash
# Start infrastructure
docker-compose up -d postgres neo4j kafka zookeeper

# Run Spring Boot
cd backend
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npx ng serve
# Open http://localhost:4200
```

---

## 🔑 Environment Variables

| Variable | Description | Required |
|---|---|---|
| `GEMINI_API_KEY` | Google Gemini API key | ✅ |
| `GCP_PROJECT_ID` | Google Cloud project ID | For deployment |
| `POSTGRES_URL` | PostgreSQL connection URL | ✅ |
| `POSTGRES_USER` | PostgreSQL username | ✅ |
| `POSTGRES_PASSWORD` | PostgreSQL password | ✅ |
| `NEO4J_URI` | Neo4j Bolt connection URI | ✅ |
| `NEO4J_USER` | Neo4j username | ✅ |
| `NEO4J_PASSWORD` | Neo4j password | ✅ |
| `KAFKA_BROKERS` | Kafka bootstrap servers | ✅ |

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Primary language |
| Spring Boot | 3.2.5 | Application framework |
| Spring WebSocket | — | Real-time communication |
| Spring Data JPA | — | PostgreSQL ORM |
| Spring Data Neo4j | — | Graph database |
| Drools | 8.44 | Triage rules engine |
| Apache Kafka | — | Event streaming / audit |
| PostgreSQL | 15 | Session persistence |
| Neo4j | 5 | Symptom relationship graph |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| Angular | 21 | Frontend framework |
| TypeScript | 5.x | Primary language |
| Angular Signals | — | State management |
| AudioWorklet API | — | Mic capture & processing |
| Web Audio API | — | Audio playback |

### AI & Cloud
| Technology | Purpose |
|---|---|
| Gemini Live API | Real-time audio + vision AI |
| Model: `gemini-2.5-flash-native-audio-preview` | Multimodal live model |
| Google Cloud Run | Backend hosting |
| Google Kubernetes Engine | Full orchestration |

---

## 📊 Kafka Topics

| Topic | Purpose |
|---|---|
| `medivoice.sessions` | Session start/end events |
| `medivoice.symptoms` | Detected symptom events |
| `medivoice.triage` | Triage evaluation results |
| `medivoice.emergency` | HIGH severity emergency events |

---

## 🧠 Drools Triage Rules

### HIGH Severity (Immediate Emergency)
| Rule | Triggers |
|---|---|
| Cardiac | chest pain, chest pressure, heart attack |
| Respiratory | can't breathe, difficulty breathing, choking |
| Neurological | face drooping, slurred speech, stroke |
| Bleeding/Trauma | severe bleeding, lost consciousness |
| Poisoning | overdose, poisoning, took too many pills |
| Mental Health | self-harm, suicidal ideation |
| Anaphylaxis | throat closing, severe allergic reaction |

### MEDIUM Severity (Seek Care Soon)
| Rule | Triggers |
|---|---|
| High Fever | fever over 103°F |
| Severe Pain | 9-10/10 pain, unbearable |
| Blood | vomiting blood, blood in urine/stool |
| Head Injury | concussion signs, hit my head |

### Tense Classification
Historical mentions ("my grandfather died of...") and hypothetical questions ("what if I had...") are **NOT** flagged as emergencies.

---

## 🕸️ Neo4j Symptom Graph

```cypher
-- Schema
(:Symptom {name, severity_weight, body_region})
  -[:COMBINED_WITH]-> (:Symptom)
  -[:ASSOCIATED_WITH]-> (:Condition {name, severity, description})

-- Initial seed data: 14 symptom nodes, 6 condition nodes
-- Co-occurrence pairs: chest pain + shortness of breath (HIGH),
--   headache + stiff neck (HIGH), fever + rash (MEDIUM), etc.
```

---

## ☁️ Google Cloud Deployment

### Cloud Run (Recommended — fastest)

```bash
cd backend
gcloud builds submit --tag gcr.io/$PROJECT_ID/medivoice-backend
gcloud run deploy medivoice-backend \
  --image gcr.io/$PROJECT_ID/medivoice-backend \
  --platform managed --region us-central1 \
  --allow-unauthenticated --memory 1Gi --timeout 3600 \
  --set-env-vars GEMINI_API_KEY=$GEMINI_API_KEY
```

### Kubernetes / GKE

```bash
gcloud container clusters create medivoice-cluster \
  --region us-central1 --num-nodes 2
kubectl apply -f kubernetes/
kubectl get pods -n medivoice
```

---

## ⚕️ Medical Disclaimer

> **MediVoice is NOT a substitute for professional medical advice, diagnosis, or treatment.** Always seek the advice of your physician or other qualified health provider with any questions you may have regarding a medical condition. Never disregard professional medical advice or delay in seeking it because of something you heard from MediVoice. **If you think you may have a medical emergency, call 911 immediately.**

---

## 🏆 Hackathon

**Competition:** Gemini Live Agent Challenge — [Devpost](https://devpost.com)
**Category:** Live Agents
**Team:** Solo developer

### Mandatory Requirements ✅
- ✅ Gemini Live API (`gemini-2.5-flash-native-audio-preview`)
- ✅ Google GenAI SDK (Java)
- ✅ Google Cloud deployment
- ✅ Real-time audio + vision (multimodal)
- ✅ Barge-in / interruption handling

### Bonus Points ✅
- ✅ Infrastructure-as-code (Kubernetes YAML)
- ✅ Automated deployment scripts
- ✅ Event-driven architecture (Kafka)
- ✅ Graph database (Neo4j)
- ✅ Rules engine (Drools)

---

## 📄 License

MIT License — see [LICENSE](LICENSE)
