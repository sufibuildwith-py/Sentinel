# Sentinel

**An AI-powered incident investigation copilot for on-call engineers.**

When a production incident fires, Sentinel investigates it the way a senior SRE would — by pulling up similar past incidents before offering a diagnosis, instead of guessing from the incident description alone.

Built as a hands-on project to go deeper than "call an LLM API" — this implements a real Retrieval-Augmented Generation (RAG) pipeline from scratch in Java, with plans to grow into a multi-agent system with a proper evaluation harness.

---

## The problem

When an on-call engineer gets paged, the first 15–20 minutes usually go into manual detective work: has this happened before? What changed recently? What do the logs say? Sentinel automates the "has this happened before" step by retrieving the most relevant past incident from a runbook archive and grounding its diagnosis in that evidence — instead of the LLM guessing blind.

## How it works

```
Incident description
        │
        ▼
  Embed the incident (Gemini Embedding API)
        │
        ▼
  Compare against embedded runbook corpus (cosine similarity)
        │
        ▼
  Retrieve the most similar past incident
        │
        ▼
  Gemini generates a diagnosis, grounded in the retrieved evidence
        │
        ▼
  Structured response: hypothesis + what to check next
```

On startup, Sentinel reads a small corpus of past-incident "runbooks" from the `runbooks/` folder and embeds each one once, keeping them in memory. Every new incident is embedded and compared against that corpus to find the closest match before the LLM ever sees the request.

## Example

**Request**
```json
POST /investigate
{ "incident": "checkout is throwing errors after we deployed the new discount code feature" }
```

**Response**
```json
{
  "diagnosis": "Hypothesis: Based on the November 2025 incident, the backend is likely throwing a NullPointerException because the new discount code field is optional, and validation isn't handling null values. What to check next: 1) Search checkout service logs for NullPointerException since the deploy. 2) Compare error rates between transactions with vs without a discount code."
}
```

Sentinel correctly identified and cited the specific past incident that matched — not a generic guess — because it actually retrieved it first.

## Tech stack

- **Java 17 + Spring Boot** — REST API, dependency injection, layered architecture (controller / service / DTO)
- **Google Gemini API** — both generation (diagnosis) and embeddings (retrieval)
- **In-memory vector store** — cosine similarity search over embedded runbooks (no external vector DB yet — see roadmap)
- **Jackson** — JSON request/response handling

## Project structure

```
sentinel/
├── src/main/java/com/sentinel/
│   ├── controller/
│   │   └── InvestigateController.java   # orchestrates the RAG pipeline
│   ├── service/
│   │   ├── GeminiService.java           # calls Gemini for diagnosis generation
│   │   ├── EmbeddingService.java        # calls Gemini for embeddings
│   │   └── RunbookStore.java            # in-memory vector store + similarity search
│   └── dto/
│       ├── IncidentRequest.java
│       ├── InvestigationResponse.java
│       └── RunbookEntry.java
├── runbooks/                            # corpus of past-incident text files
└── src/main/resources/application.properties
```

## Running it locally

**1. Get a Gemini API key**
Free tier available at [aistudio.google.com](https://aistudio.google.com).

**2. Set it as an environment variable**

PowerShell:
```powershell
$env:GEMINI_API_KEY="your_key_here"
```

macOS/Linux:
```bash
export GEMINI_API_KEY="your_key_here"
```

**3. Run the app**
```bash
mvn spring-boot:run
```

On startup you should see each runbook get embedded:
```
Embedded runbook: incident-001-checkout-null-discount.txt
...
Loaded 5 runbooks into the vector store
```

**4. Send a request**
```bash
curl -X POST http://localhost:8080/investigate \
  -H "Content-Type: application/json" \
  -d '{"incident": "database CPU is at 95% and queries are timing out"}'
```

## Roadmap

- [x] **Phase 1** — Single-agent pipeline: incident in, LLM diagnosis out
- [x] **Phase 2** — RAG: embed + retrieve relevant past incidents before generating a diagnosis
- [ ] **Phase 3** — Multi-agent orchestration: a triage agent routes to specialist agents (change-correlation via GitHub API, log analysis, retrieval), with a synthesis agent combining their findings
- [ ] **Phase 4** — Evaluation harness (golden incident dataset, retrieval + faithfulness metrics), observability/tracing, and a proper vector database (pgvector) in place of the in-memory store

## Why this project

I'm a final-year B.Tech AI/ML student pivoting from general software engineering into AI engineering. Most portfolio projects in this space are thin LLM API wrappers — this one is my attempt to actually implement the patterns (RAG, and soon multi-agent orchestration + evals) that production AI systems are built on, in a stack (Java/Spring Boot) that most AI portfolios don't use.

## License

MIT
