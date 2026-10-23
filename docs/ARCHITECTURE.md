# Architecture

How Morning Bro is built. For what it does and why, see [PRODUCT.md](PRODUCT.md). For running and configuring it, see the [README](../README.md).

## Overview

Morning Bro is a **short-lived batch job**, not a service. A scheduler starts it, it decides whether it's time to post, fetches today's forecast for all cities, composes one message, posts it to Discord, and exits. There is no server, database, or persisted state.

```mermaid
flowchart LR
    S["⏰ GitHub Actions<br/>scheduled workflow"] -->|starts| A["☕ Morning Bro<br/>Java app"]
    A -->|"1 · GET today's forecast<br/>(all cities, one request)"| W["🌤️ Open-Meteo API"]
    A -->|"2 · POST message"| D["💬 Discord webhook"]
    D --> C["#channel"]
```

## Technology stack

| Concern | Choice |
|---|---|
| Language | Java 27, without preview features |
| Build | Maven, Shade plugin for a single runnable jar |
| JSON | Jackson (`jackson-databind`), mapping to records |
| Config file | YAML via Jackson (`jackson-dataformat-yaml`), mapped to the same settings records |
| HTTP | `java.net.http.HttpClient` (JDK) |
| Tests | JUnit 5, AssertJ, Mockito (forecast source only) |
| Scheduling, secrets | GitHub Actions |
| Weather data | Open-Meteo (free, no API key, one request for all cities) |
| Delivery | Discord channel webhook (send-only) |

No framework, and no Discord bot account. The job runs for seconds a day and only sends, so wiring is done by hand in the entry point.

## Code organization

Code is organized **by feature**. Each package owns one capability and exposes a small public API; everything else is package-private.

```
MorningBro.java                entry point: wiring + daily flow, a compact source file in the unnamed package
io/github/lefpap/morningbro/
├── settings/
│   ├── Settings.java          loads application.yml + env vars, validates, hands each feature its own part
│   ├── SettingsFile.java      the Settings File exactly as written, before validation
│   └── SettingsException.java a mistake in the Settings File or the env vars
├── schedule/
│   ├── PostTime.java          Post Time + Reference Timezone: the slot a run was scheduled for, whether it's due,
│   │                          and its Forecast Day
│   └── ScheduleException.java SCHEDULED_CRON isn't a daily cron entry
├── weather/
│   ├── City.java              name, flag, latitude, longitude
│   ├── WeatherSettings.java   rain thresholds, emoji per condition, the Attribution and its link
│   ├── Condition.java         enum: CLEAR, RAIN, SNOW, …, UNLIKELY_RAIN, UNKNOWN
│   ├── TemperatureAlert.java  enum: HOT, FREEZING
│   ├── Forecast.java          day, weather code, min/max temp, rain chance
│   ├── WeatherService.java    one Forecast per city for the Forecast Day; rejects partial or unexpected forecasts
│   ├── OpenMeteoClient.java   the only class that knows Open-Meteo: HTTP + response records
│   ├── WeatherException.java  the forecast couldn't be fetched, or came back partial or unexpected
│   └── WeatherEmoji.java      forecast → Condition + TemperatureAlert (rules) → emoji (from settings)
├── messaging/
│   ├── GreetingSettings.java  greeting, Friday line, fallback note, home city
│   ├── MorningMessage.java    sealed, platform-neutral data: WithWeather (Greeting, Home City emoji, Friday Line,
│   │                          Weather Report, Attribution) or Fallback (Greeting, Friday Line, Fallback Note); nothing formatted
│   ├── CityForecast.java      one city's entry in the Weather Report: city, emoji, rounded low/high, Rain Chance to show
│   ├── MorningMessageFactory.java  the product rules: forecasts → MorningMessage, or the Fallback Message
│   │                          when there's no forecast for the Home City (an empty map means the weather is unavailable)
│   ├── MessagePublisher.java  interface: delivers a MorningMessage on one platform
│   ├── console/
│   │   ├── ConsoleMessageComposer.java   MorningMessage → plain text, the Attribution and its URL as the last line
│   │   └── ConsoleMessagePublisher.java  prints its composer's text to the run log
│   └── discord/
│       ├── DiscordSettings.java   webhook URL, bot name, avatar
│       ├── DiscordException.java  the post failed or Discord rejected it
│       ├── DiscordMessageComposer.java   MorningMessage → webhook payload, all plain text in Discord's markdown: greeting lines
│       │                          first (they show in notifications), one City Forecast per line, Attribution as a link in subtext
│       ├── WebhookPayload.java    the payload record, as Discord's webhook API takes it
│       └── DiscordMessagePublisher.java  POSTs its composer's payload to the Channel; with DRY_RUN, prints it instead
```

### Rules
- **Dependencies point one way:** `messaging → weather`, and each platform sub-package → `messaging`. Platform sub-packages never know about each other, `weather` knows nothing about the message, and nothing depends on `MorningBro`.
- **The message is platform-neutral; publishers are not.** `MorningMessageFactory` applies the product rules once and produces a `MorningMessage` with nothing formatted. Each platform has a sub-package of `messaging` (`console`, `discord`) with a composer that structures and formats the message for that platform, and a `MessagePublisher` that delivers what its composer makes. The publisher is given its composer; `MorningBro` wires the two together. Another platform means another sub-package next to them.
- **No global settings object.** Each feature has a settings record. `Settings` builds them, and `MorningBro` passes each feature only its own. Features never read the Settings File or environment variables directly.
- **Rules in code, presentation in config.** Which weather codes count as rain, when rain becomes unlikely, and when a temperature alert applies are product rules in `WeatherEmoji`. Which emoji represents each `Condition` and `TemperatureAlert` comes from the config file.
- **The entry point picks the forecast source.** `MorningBro` wires `new WeatherService(new OpenMeteoClient())`; everything else uses `WeatherService` and never names the source.
- **A weather failure is not a failed run.** `WeatherService` throws `WeatherException`; `MorningBro` logs it for the Owner and carries on with no forecasts, so the Fallback Message goes out. Nothing else catches it.
- **One level deep, except platforms.** No sub-packages until a feature outgrows a handful of classes. The one exception is `messaging`, which keeps each platform in its own sub-package.
- **Product rules live in pure classes** (`PostTime`, `WeatherEmoji`, `MorningMessageFactory`, the composers), so they're testable without network access.
- **`MorningBro` only wires.** It's a compact source file, so no test can call it: anything with a branch goes in a named class. The one decision it keeps is that a Forced Post ignores the Post Time.

## Application flow

1. **Trigger.** GitHub Actions runs the job hourly at minute :37 and passes the cron entry as `SCHEDULED_CRON`, or runs it manually (which sets `FORCE=true`).
2. **Load settings.** `Settings` reads `application.yml` and the env vars, and validates them. A missing file, unknown key, or invalid value fails the run.
3. **Time gate.** `PostTime` works out the instant the run was scheduled for, or uses now without `SCHEDULED_CRON`, and checks whether that instant is in the Post Time hour. If not, and the run isn't forced, the app exits with success. The same instant gives the Forecast Day.
4. **Fetch.** `WeatherService` gets the Forecast Day for all cities in one request through `OpenMeteoClient`, which maps the response to `Forecast` records. `WeatherService` checks there is one per city, each for the Forecast Day. Unexpected or partial data is a `WeatherException`: `MorningBro` logs why the weather is unavailable and carries on with no forecasts (an empty map).
5. **Build the message.** `MorningMessageFactory` builds the `MorningMessage`: the greeting with the Home City's emoji, the Friday Line on Fridays, and one City Forecast per city. With no forecast for the Home City, which includes no forecasts at all, it builds the Fallback Message instead: the greeting text, the Friday Line on Fridays, and the Fallback Note, with no Weather Report. It never builds a partly filled Weather Report.
6. **Show it in the run log.** `ConsoleMessagePublisher` prints the message as plain text, so the Owner can check it.
7. **Publish.** `DiscordMessagePublisher` turns the message into a webhook payload and POSTs it. With `DRY_RUN=true`, the payload is printed instead. A failed or rejected post is a `DiscordException`, and `MorningBro` exits non-zero.
8. **Report.** A failed post or invalid config exits non-zero. The workflow run fails, and GitHub notifies the owner. An unavailable weather does not: the Fallback Message still goes out.

## Scheduling

GitHub Actions cron runs in UTC only. The workflow therefore runs **every hour**, and the app is the single authority on when to post ([ADR-0001](adr/0001-app-decides-when-to-post.md)). The Post Time is a whole hour in the Reference Timezone, so the post stays at the same local time across daylight-saving changes.

A run is judged by the slot it was **scheduled** for, not by when it started ([ADR-0004](adr/0004-judge-the-post-time-by-the-scheduled-slot.md)). GitHub often starts runs late, so the workflow lists 24 cron entries, one per UTC hour at minute :37, and passes the one that fired as `SCHEDULED_CRON`. `PostTime` turns it into the latest matching instant at or before now. So a late run from the hour before never posts, and a late Post Time run still posts, for its own Forecast Day. The entries never change when the Owner changes the Post Time. A `concurrency` group stops overlapping runs from running side by side.

The Post Time on a Forecast Day is resolved with `ZonedDateTime.of(day.atTime(hour, 0), timezone)`: in an hour the clock repeats it posts once, and in an hour the clock skips it posts in the hour after the gap.

## Configuration

All non-secret settings live in **`src/main/resources/application.yml`**. Changing a text, emoji, city, or the Post Time is an edit and a commit, with no code change. The file is bundled into the jar, and the morning workflow builds the jar on every run, so a committed change applies from the next run. `SETTINGS_PATH` can point to a file on disk instead. Tests build their test cases from `src/test/resources/application-test.yml`.

```yaml
schedule:
  hour: 8
  timezone: Europe/Athens

cities:
  - { name: Athens,   flag: "🇬🇷", lat: 37.98, lon: 23.73 }
  - { name: Brussels, flag: "🇧🇪", lat: 50.85, lon: 4.35 }
  - { name: Milan,    flag: "🇮🇹", lat: 45.46, lon: 9.19 }

greeting:
  text: "Good Morning — Καλημέρα! ☕"
  home-city: Athens                     # its weather emoji is appended to the greeting
  friday: "Take it easy Friday! 😎"
  fallback: "(weather is taking the day off)"

weather:
  unlikely-rain-threshold: 40           # below this, rain/storm shows as unlikely-rain
  rain-chance-display-threshold: 20     # at or above this, "☔ NN%" is shown
  emoji:
    clear: "☀️"
    mostly-clear: "🌤️"
    partly-cloudy: "⛅"
    overcast: "☁️"
    fog: "🌫️"
    drizzle: "🌦️"
    rain: "🌧️"
    snow: "🌨️"
    thunderstorm: "⛈️"
    unknown: "🌡️"
    unlikely-rain: "🌦️"
  alert-emoji:                          # shown after the condition emoji, never instead of it
    hot: "🥵"                           # high ≥ 35°
    freezing: "🥶"                      # low ≤ 0°
  attribution: "Weather data by Open-Meteo.com"   # the forecast source's credit and link, which its terms require
  attribution-url: "https://open-meteo.com/"

discord:
  username: "Morning Bro"
  avatar-url: null
```

Validation (at startup, fails the run): there are no missing or unknown keys, every `Condition` and `TemperatureAlert` has an emoji, there is at least one city and no city is listed twice, `home-city` is one of `cities`, the hour is 0–23, the timezone is known, and coordinates are in range. The env vars are checked too: `FORCE` and `DRY_RUN` are `true` or `false`, and `DISCORD_WEBHOOK_URL` is set unless it's a Dry Run. A `SCHEDULED_CRON` that isn't a daily entry like `37 5 * * *` fails the run as well.

Environment variables, for secrets and runtime flags only:

| Variable | Source | Default |
|---|---|---|
| `DISCORD_WEBHOOK_URL` | GitHub secret | required unless `DRY_RUN` |
| `SETTINGS_PATH` | env var | the bundled `application.yml` |
| `FORCE` | set by the workflow on manual runs | `false` |
| `DRY_RUN` | env var | `false` |
| `SCHEDULED_CRON` | set by the workflow on scheduled runs: the cron entry that fired | unset: the run is judged by now |

Secrets never go in `application.yml`.

## Build and CI

- **`ci.yml`** runs on every push and PR: `mvn verify`, then a dry run of the built jar with the real `application.yml`, so a broken config fails CI before it reaches a morning run.
- **`morning.yml`** runs hourly at :37 and on demand: builds with `-DskipTests` and runs the jar. A broken test can't cancel a morning post; CI catches it at push time.
- Tests mirror the feature packages (`src/test/java/io/github/lefpap/morningbro/<feature>/`) and make no network calls.
  - `OpenMeteoClientTest` runs the real client against a local `HttpServer` that serves responses recorded from Open-Meteo, in `src/test/resources/open-meteo/`. Each file in `broken/` is `athens.json` with one value changed.
  - `WeatherServiceTest` mocks `OpenMeteoClient` with Mockito. Mockito is loaded as a `-javaagent` through surefire's `argLine`, so running tests from an IDE may need that agent added to the run configuration.
  - `DiscordMessagePublisherTest` runs the real publisher against a local `HttpServer` standing in for the webhook.
  - `PostTimeTest` passes fixed instants instead of reading the clock: late runs from the hour before and from the Post Time, a slot on the previous UTC day, no `SCHEDULED_CRON`, a malformed one, and both daylight-saving changes in the Reference Timezone, including a Post Time in the repeated and the skipped hour.
  - `MorningMessageFactoryTest` builds `Forecast`s by hand, and each composer's test builds `MorningMessage`s by hand, so neither needs a mock.
  - `MorningBro` and `ConsoleMessagePublisher` have no unit tests: one only wires features together, the other only prints `ConsoleMessageComposer`'s text. CI's dry run of the built jar covers them.

## Constraints

- GitHub cron is best-effort. Runs can start minutes late and are occasionally skipped.
- Scheduled workflows are paused in public repositories after 60 days without commits.
- Open-Meteo's free tier is non-commercial and requires attribution with a link, shown as a small link below the Weather Report.
- Open-Meteo's daily weather code is the day's *worst* condition, which is why the emoji rules use the Rain Chance to soften it.
