# Sentinel setup and five-minute demo

1. Install Docker Desktop (or Docker Engine with Compose), then run `git clone https://github.com/sufibuildwith-py/Sentinel.git` and enter `Sentinel/`.
2. Copy `.env.example` to `.env` and replace `GEMINI_API_KEY`; add Razorpay **Test Mode** keys only for Payment Link execution.
3. Run `docker compose up --build` from the repository root and wait until all services are healthy.
4. Open [http://localhost:3000](http://localhost:3000) and confirm **TEST MODE / SYNTHETIC EVALUATION** is visible.
5. Open **Demo controls**, reset the state, inject the UPI outage, and open the created incident.
6. Run **Investigation**, build the recovery plan, and inspect every policy PASS/FAIL result.
7. If the verdict is HUMAN, approve it from **Approval queue** with an actor and reason; DENY remains non-executable.
8. With Razorpay enabled, execute once, pay the Test Mode link, refresh after the signed webhook, then verify metrics and audit; otherwise open **Evaluation Lab** and run the credential-free 464-case proof.
9. Stop with `docker compose down`; use `docker compose down --volumes` only when you intentionally want to erase local demo data.
