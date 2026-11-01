# Morning Bro

A daily good-morning and weather post for a group of friends' Discord channel.

```
Good Morning — Καλημέρα! ☕⛅ Take it easy Friday! 😎

🇬🇷 Athens
⛅ · 15°–21° · 💧 25%

🇬🇷 Ikaria
☀️ · 18°–23° · 💧 0%

🇧🇪 Brussels
🌦️ · 6°–11° · 💧 60%

🇮🇹 Milan
(weather is taking the day off)
```

**Docs:** [docs/product.md](docs/product.md) (what it does and why) and [docs/architecture.md](docs/architecture.md) (how it's built).

## How it works

GitHub Actions starts the app at 07:50 Europe/Athens. It fetches each location's forecast from Open-Meteo and posts one message to a Discord webhook. Then it exits. See the [architecture overview](docs/architecture.md#overview).

## Requirements

- JDK 27 (CI uses Temurin).
- Maven isn't needed; the Maven wrapper is included.
- A Discord channel where you can create webhooks.
- The [`gh` CLI](https://cli.github.com/) for operating it on GitHub.

## Setup

1. Clone the repo: `gh repo clone lefpap/morning-bro`, then `cd morning-bro`.
2. Create a webhook: Discord channel, *Edit Channel*, *Integrations*, *Webhooks*, *New Webhook*, *Copy Webhook URL*. It looks like `https://discord.com/api/webhooks/<id>/<token>`. Treat the URL as a password.
3. Local: `cp .env.example .env` and fill in `MORNING_BRO_DISCORD_WEBHOOK_URL`. `.env` is gitignored, and real environment variables override it.
4. GitHub: `gh secret set MORNING_BRO_DISCORD_WEBHOOK_URL`. It prompts for the value, so the URL never lands in your shell history.

## Configuration

Everything except run-on-startup lives in `src/main/resources/application.yaml`.

| Key | What it does | Default | Env var |
|---|---|---|---|
| `morning-bro.time-zone` | Decides what "today" and "Friday" mean | `Europe/Athens` | – |
| `morning-bro.home` | Name of the location whose weather emoji goes in the header | `Athens` | – |
| `morning-bro.message.morning` | The greeting | `Good Morning — Καλημέρα! ☕` | – |
| `morning-bro.message.friday` | Added to the header on Fridays | `Take it easy Friday! 😎` | – |
| `morning-bro.locations[]` | Locations to report on, in order: `name`, `country-code`, `latitude`, `longitude` | Athens, Ikaria, Brussels, Milan | – |
| `morning-bro.dry-run` | Log the message instead of posting it | `false` | `MORNING_BRO_DRY_RUN` |
| `morning-bro.run-on-startup` | Run the job as soon as the app starts (tests set it to `false`) | `true` (not in the yaml) | – |
| `spring.http.serviceclient.discord.base-url` | The Discord webhook URL (a secret) | empty | `MORNING_BRO_DISCORD_WEBHOOK_URL` |
| `spring.http.clients.connect-timeout` / `read-timeout` | HTTP timeouts for both clients | `5s` / `10s` | – |

Validation rules: see [docs/architecture.md](docs/architecture.md#configuration-and-secrets).

To add a location, append an entry to the list:

```yaml
morning-bro:
  locations:
    # …existing locations
    - name: Berlin
      country-code: DE
      latitude: 52.52
      longitude: 13.41
```

## Running locally

Dry run (logs the message, posts nothing, no webhook needed):

```bash
MORNING_BRO_DRY_RUN=true ./mvnw spring-boot:run
```

```powershell
$env:MORNING_BRO_DRY_RUN='true'; .\mvnw spring-boot:run; Remove-Item Env:MORNING_BRO_DRY_RUN
```

Real post (uses the webhook from `.env`):

```bash
./mvnw spring-boot:run
```

As a jar, like CI (the explicit name works in PowerShell too):

```bash
./mvnw -DskipTests package
java -jar target/morning-bro-0.0.1-SNAPSHOT.jar
```

## Tests

```bash
./mvnw test
```

Tests never touch the network and never start the job on application startup. CI runs `./mvnw verify` on every push and PR to `master`.

## Operating it

- Schedule: daily at 07:50 Europe/Athens (`.github/workflows/morning-bro.yml`; see [scheduling](docs/architecture.md#scheduling)).
- Manual dry run: `gh workflow run morning-bro.yml`
- Manual real post (e.g. a re-send after an outage): `gh workflow run morning-bro.yml -f dry_run=false`
- Watch and inspect: `gh run list --workflow morning-bro.yml`, `gh run watch`, `gh run view <run-id> --log`
- Scheduled runs only: `gh run list --workflow morning-bro.yml --event schedule`
- Failures: a failed run emails whoever last edited the schedule (scheduled runs) or triggered the run (manual runs), which here is the owner (GitHub's default notification); the log says why.
- Known limits: see [docs/product.md](docs/product.md#known-limits). Also, if the repo is public and GitHub disables the schedule after 60 days of inactivity, re-enable it with `gh workflow enable morning-bro.yml`.

## Project layout

```
src/main/java/io/github/lefpap/morningbro/
  weather/         Open-Meteo client and forecast mapping
  message/         builds the Discord message text
  notification/    Discord webhook client and publisher
src/main/resources/application.yaml   all configuration
.github/workflows/                    morning-bro.yml (schedule), ci.yml
docs/                                 product.md, architecture.md
```

More in [docs/architecture.md](docs/architecture.md#packages).
