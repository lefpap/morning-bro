---
status: accepted
---

# Open-Meteo for forecasts

We get forecasts from Open-Meteo because it's free, needs no API key or account, and returns the Forecast Day for every city in one request. Its free tier is non-commercial and requires Attribution, which the Weather Card shows.

## Consequences

- Open-Meteo's daily weather code is the day's *worst* condition, so a short shower marks the whole day as rain. The Unlikely Rain rule exists to soften this, using the Rain Chance. If the provider changes, check whether that rule is still needed.
- Only the `weather` package knows about Open-Meteo, so switching providers means replacing its client and mapping, not changing the message or the posting. A future switch should get its own ADR that supersedes this one.
