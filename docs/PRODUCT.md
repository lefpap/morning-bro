# Morning Bro ☕

## Purpose

A small daily ritual for a Discord community spread across cities. Every morning, the channel gets a friendly "good morning" with a coffee, plus an at-a-glance view of the weather in each city where its members live. It should feel warm and personal, not like a weather report.

## Audience

Members of one Discord channel, currently based in **Athens**, **Brussels**, and **Milan**. They read the message on their phones, often straight from the notification.

## Core behavior

### When it posts
- Once a day, every day, at a **configurable time in a configurable reference timezone**.
- Default: **08:00 Athens time**.
- The configured time stays fixed in local time all year round, including across daylight-saving changes.
- **Never more than one message per day.** A missed day is better than a duplicate.
- The owner can trigger a post on demand at any time, for testing or for a special occasion.

### What it says
Each message has up to three parts:

1. **The greeting line**: the morning text followed by the **home city's** weather emoji: *"Good Morning — Καλημέρα! ☕☀️"*
   - The home city is configurable (default: Athens).
   - The greeting must be readable in a phone notification on its own.

2. **The Friday line**: on **Fridays** only, a second line: *"Take it easy Friday! 😎"*

3. **The weather card**: a compact, always-visible block below the greeting, with one entry per city, shown side by side where space allows. Each entry shows:
   - the city's flag and name
   - one emoji summarizing the day's weather
   - the day's low and high temperature, rounded to whole degrees Celsius
   - the chance of rain, but only when it's worth mentioning

**Example (a Friday):**

> Good Morning — Καλημέρα! ☕☀️
> Take it easy Friday! 😎
>
> | 🇬🇷 Athens | 🇧🇪 Brussels | 🇮🇹 Milan |
> |---|---|---|
> | ☀️ 17–27° | 🌦️ 9–14° · ☔ 30% | ⛅ 11–20° |
>
> *Forecast by \<weather source\>*

### Who it's from
- It posts under its own identity, named **"Morning Bro"**, so it doesn't look like a generic system message.
- It can optionally have its own profile picture.
- The weather card uses a warm, coffee-brown accent.

## Weather rules

### Weather emoji
The emoji should describe **what the day will feel like**, not the worst thing that could happen at some point in it.

| Condition | Emoji |
|---|---|
| Clear | ☀️ |
| Mostly clear | 🌤️ |
| Partly cloudy | ⛅ |
| Overcast | ☁️ |
| Fog | 🌫️ |
| Drizzle, or rain that's unlikely | 🌦️ |
| Rain or showers | 🌧️ |
| Snow | 🌨️ |
| Thunderstorm | ⛈️ |
| Anything unrecognized | 🌡️ |

**Overrides, applied in this order:**
1. **Very hot**: if the high is **35° or more**, show 🥵 regardless of conditions.
2. **Freezing**: if the low is **0° or below**, show 🥶, *unless it's snowing* (snow already says "cold").
3. **Unlikely rain**: if rain or a storm is forecast but the chance of rain is **below 40%**, show 🌦️ instead of 🌧️ or ⛈️. A brief shower shouldn't turn the whole day gloomy.

### Rain chance
- The day's highest chance of rain is shown as **"☔ NN%"** next to the temperatures.
- It is shown only when it's **20% or higher**. Dry days stay uncluttered.
- If the forecast doesn't include a rain chance, nothing is shown, and the emoji follows the forecast as-is.

### Attribution
The weather card credits its forecast source, as the source's terms require.

## When things go wrong

- **Weather unavailable:** the greeting still goes out, without the weather emoji, including the Friday line, with a light-hearted note: *"(weather is taking the day off)"*. There is no weather card. The ritual never skips because of the weather.
- **Posting fails:** the owner is notified, so a silent outage doesn't go unnoticed.
- **Partial or unexpected weather data:** treated as "weather unavailable". A message that is wrong or half-filled is never posted.

## Configuration (owner-facing)

All of these live in **one human-readable settings file**, so the owner can change them without touching code:
- the list of cities (name, flag, location)
- the home city whose weather emoji appears in the greeting line
- the posting time and the reference timezone (default: 08:00, Athens)
- the greeting, the Friday line, and the fallback note
- the emoji used for each weather condition and override
- the bot's name, picture, accent color, and attribution text
- the "unlikely rain" and "show rain chance" thresholds

A mistake in the settings (a missing emoji, an unknown home city, an invalid time) is caught before anything is posted, and the owner is notified. Secrets never live in the settings file.

## Non-goals (for now)

- No interaction: the bot doesn't respond to commands, replies, or reactions.
- No per-user settings or personalization.
- No forecasts beyond today, and no hourly breakdowns.
- No alerts or severe-weather warnings.
- One channel only.

## Ideas for later

- Rotating greetings in the members' languages (e.g. Καλημέρα, Goedemorgen, Bonjour, Good morning).
- Considering only waking hours when judging the weather, so a 3am storm doesn't count.
- More special days: Monday motivation, holidays, birthdays.
- Interactive use, such as asking the bot for the weather in any city on demand.
