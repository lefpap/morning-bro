# Morning Bro

Morning Bro is a small daily ritual for a group of friends. Around 07:50 Athens time it posts one message in their Discord channel: a greeting plus a quick weather check for each friend's location, so everyone can decide on a jacket or an umbrella. It should feel like a friend posting, not a dashboard.

## The message

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

The rules:

- The header is the morning text followed directly (no space) by the home location's weather emoji. On Fridays only, the Friday text is added after it, following a single space.
- There is one block per location, in the order they are configured: the country flag, the location name, then a line of `{emoji} · {min}°–{max}° · 💧 {rain}%`.
- Temperatures are in °C, rounded half-up. Rain is the day's highest precipitation probability.
- If a location's weather can't be fetched, its block shows `(weather is taking the day off)` instead of the weather line.
- If the home location's weather fails, the header simply has no emoji.
- "Today" and "Friday" are decided in the configured time zone (Europe/Athens by default), not UTC.

## Weather emoji

The emoji comes from the WMO weather code of the day:

| WMO codes | Emoji |
|---|---|
| 0 | ☀️ |
| 1, 2 | ⛅ |
| 3 | ☁️ |
| 45, 48 | 🌫️ |
| 51–57, 80–82 | 🌦️ |
| 61–67 | 🌧️ |
| 71–77, 85, 86 | 🌨️ |
| 95–99 | ⛈️ |
| anything else | 🌡️ |

Accepted quirk: the daily code is the day's most severe condition, so a line like `⛈️ · 18°–26° · 💧 15%` can happen.

## Behavior you can rely on

- The greeting always goes out, even if some or all of the weather fails.
- Each external call is tried once. There are no retries.
- Late and manual runs always post. A manual re-send plus a late scheduled run can therefore post twice, and that is accepted.
- A Discord failure fails the run, so GitHub emails whoever last edited the schedule (scheduled runs) or triggered the run (manual runs), which here is the owner.
- The webhook URL never appears in logs or error messages.

## What it doesn't do

- No bot commands or replies.
- No DMs or mentions.
- No database or stored state.
- No web UI.
- No randomized greetings.

## Known limits

- GitHub may delay or skip a scheduled run, with no email.
- On public repositories, GitHub disables scheduled workflows after 60 days without activity. This is not relevant while the repository is private.
- Discord rejects messages over 2000 characters. That fails the run instead of truncating the message.

## Changing it

Adding a location or changing the greeting is a config edit in `src/main/resources/application.yaml`. See the [README's configuration section](../README.md#configuration) for the keys.
