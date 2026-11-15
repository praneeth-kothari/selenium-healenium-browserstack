# Selenium + Healenium + BrowserStack

**Self-healing test automation where the browser runs in the BrowserStack cloud and the healing brain runs locally.**

[![BrowserStack Tests](https://github.com/praneeth-kothari/selenium-healenium-browserstack/actions/workflows/browserstack-tests.yml/badge.svg)](https://github.com/praneeth-kothari/selenium-healenium-browserstack/actions/workflows/browserstack-tests.yml)

## Architecture

```
JUnit 5 tests
     │
     ▼
SelfHealingDriver (Healenium)          ← intercepts findElement, decides healing
     │ wraps
     ▼
RemoteWebDriver ──────────────────────► BrowserStack cloud (Chrome / Windows)
     │  element snapshots                    real browser, real session, video
     ▼
hlm-backend (docker-compose) ────────► postgres (healing store)
```

- **Phase 1 (healthy):** correct locators log in; Healenium records element info into the store
- **Phase 2 (broken):** deliberately wrong locators (`#user-name-wrong` …) are scored against the stored element info, **healed at runtime**, and login still succeeds — on a cloud session
- Session results are marked pass/fail on the BrowserStack dashboard via the REST API (plain `RemoteWebDriver`, no SDK magic)

## Tests

| Test | What it proves |
|---|---|
| `LoginHealingTest.phase1_healthyLogin` | Selenium + BrowserStack wiring works; store gets populated |
| `LoginHealingTest.phase2_brokenLocatorsAreHealed` | **Healenium recovers broken locators at runtime, in the cloud** |
| `InventoryTest.inventoryShowsSixProducts` | Plain healthy run on BrowserStack (no healing needed) |

## Run locally

```powershell
# 1. Start the healing store
docker compose up -d

# 2. Set your BrowserStack credentials
$env:BROWSERSTACK_USERNAME = "your-username"
$env:BROWSERSTACK_ACCESS_KEY = "your-access-key"

# 3. Run
mvn clean test
```

Optional env overrides: `BS_BROWSER` (Chrome), `BS_BROWSER_VERSION` (latest),
`BS_OS` (Windows), `BS_OS_VERSION` (11), `BS_BUILD`, `BS_SESSION`.

Sessions appear on your BrowserStack dashboard under build
`selenium-healenium-selfhealing`, marked pass/fail automatically.

## Run on GitHub Actions

1. Repo → **Settings → Secrets and variables → Actions**
2. Add `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY`
3. Trigger **BrowserStack Tests** (push to `src/**` or run manually)

Without the secrets the workflow stays green and prints a notice instead of failing.

## Why Healenium here and not in Playwright?

Healenium is a **Selenium-only** library — there is no Playwright port.
The Playwright-side equivalent lives in
[`agentic-test-automation`](https://github.com/praneeth-kothari/agentic-test-automation)
(the `Patch` Healer agent), so this profile demonstrates self-healing in **both** ecosystems.

## License

MIT
