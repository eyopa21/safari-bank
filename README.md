# External bank (mock)

A stand-in for the bank that the wallet service pays out to. It behaves the way the
spec describes the real bank: it accepts or rejects payouts, answers status queries
slowly, and sends outcome callbacks later, sometimes more than once.

## Endpoints

| Method | Path | What it does |
|---|---|---|
| POST | `/bank/payouts` | Submit a payout. Always 200: `{"result":"ACCEPTED","bank_ref":...}` or `{"result":"REJECTED","reason":...}`. Idempotent on `payout_id`. |
| GET | `/bank/payouts/{payout_id}` | The bank's view: `{"status":...}` with `IN_PROGRESS`, `SUCCEEDED`, `FAILED`, `REJECTED` or `NOT_FOUND`. Takes 1 to 3 seconds. |

Swagger UI: `http://localhost:9090/swagger-ui.html`

### Rules

- Account numbers matching `bank.payout.valid-account-pattern` (default `ACC-` plus 3+ digits)
  are accepted; anything else is `REJECTED` with `BENEFICIARY_ACCOUNT_INVALID`.
- Amount must be positive, otherwise `REJECTED` with `INVALID_AMOUNT`.
- The outcome (`SUCCEEDED` or `FAILED`) is decided during the submit call. About 5% fail
  (`bank.payout.failure-rate`). Accounts matching `bank.payout.failing-account-pattern`
  (default `ACC-9xx`) always fail, which is handy for tests.
- A payout reports `IN_PROGRESS` until its callback goes out, then its outcome.

### Callbacks

Posted to one of `bank.payout.callback.urls`, picked at random, as

```json
{"event_id":"...","payout_id":"P-7f3a91","bank_ref":"BR4F830B87B25B62A5","status":"SUCCEEDED"}
```

- Sent 1 to 10 seconds after the payout was accepted.
- 30% are sent 2 or 3 times concurrently with the same `event_id`.
- No 2xx within 2 seconds counts as failed; retried every 3 seconds, up to 5 times.
- Callbacks are stored in the `callback_outbox` table (one row per copy) in the same
  transaction as the payout, and a poller sends due rows. A restart does not lose them.

## Running

Needs the PostgreSQL started by `../Safari-test/compose.yaml` (same instance, database
`external_bank`). Then:

```
mvn spring-boot:run
```

Listens on port 9090. All knobs live in `src/main/resources/application.properties`
under `bank.payout.*`; override any of them on the command line, for example:

```
mvn spring-boot:run -Dspring-boot.run.arguments="--bank.payout.callback.urls=http://localhost:8080/mpesa/v1/callbacks/bank --bank.payout.callback.delay-max=3s"
```

## Tests

```
mvn test
```

Unit tests for the payout rules and the entity, plus a MockMvc test for the two endpoints.
No database is needed.

## Layout

```
api/        controller, request/response records
config/     properties record, HTTP client and executor beans, OpenAPI
domain/     Payout, CallbackOutbox, enums, repositories
service/    PayoutService (rules), CallbackDispatcher (outbox poller + HTTP)
db/changelog/  Liquibase changelogs (SQL format)
```
