# Observed validation

Validated locally on macOS on 2026-10-10/11.

| Check | Observed result |
|---|---|
| Solidity contract tests | 6 passed, 0 failed |
| Java API tests | 7 passed, 0 failed |
| Java 17 embedded-Kafka integration with real local Ethereum | 1 passed, 0 failed |
| Angular production build | Passed with bundled Node 24 |
| Browser smoke | Submission, pending verification guard, unauthorized feedback, and 390px responsive viewport passed; no page errors |
| Docker Compose configuration | Parsed successfully; containers not executed |

The integration test executed record persistence, transactional outbox publication, an embedded Kafka broker, Web3j signing, a deployed Solidity contract, a mined receipt, matching on-chain digest, and safe duplicate anchoring. Its database was H2 in PostgreSQL mode. A PostgreSQL container runtime was not tested because the local Docker daemon was stopped.

Browser validation used a running Spring Boot API in H2 API-only mode and Angular's development server in an isolated headless Chrome session. The screenshots therefore show genuine pending records, not a simulated successful chain pipeline. The separate integration test validates anchoring.

Fourteen automated test cases passed across the Java and contract suites. These counts are not a coverage percentage, performance benchmark, production traffic claim, or security audit. GitHub Actions is configured, but its result is separate from these locally observed results.
