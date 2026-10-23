# Morning Bro ☕

Every morning, Morning Bro posts a friendly good-morning to one Discord channel, with a coffee and the day's weather in each city where the channel's members live:

> Good Morning — Καλημέρα! ☕☀️<br>
> Take it easy Friday! 😎
>
> 🇬🇷 **Athens** ☀️ 17–27°<br>
> 🇧🇪 **Brussels** 🌦️ 9–14° · ☔ 30%<br>
> 🇮🇹 **Milan** ⛅ 11–20°<br>
> <sub>[Weather data by Open-Meteo.com](https://open-meteo.com/)</sub>

The Friday line shows on Fridays only. If the weather can't be fetched, the greeting still goes out, with *"(weather is taking the day off)"* in place of the weather.

It runs on GitHub Actions, with no server to look after. For what it does and why, see [docs/PRODUCT.md](docs/PRODUCT.md); for how it's built, [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## How it runs

The **Morning** workflow starts every hour, at minute :37. Each run checks whether it was scheduled for the Post Time (08:00 Athens time by default) and only posts then, so the post stays at the same local time across daylight-saving changes. All other runs end without posting. The workflow's schedule doesn't need editing when you change the Post Time.

You can also make a **Forced Post** at any time (see below). It posts straight away, and it doesn't stop or replace that day's scheduled post.

If a run fails, because the Settings File has a mistake or Discord rejected the post, the workflow run fails and GitHub emails you. If only the weather is unavailable, the run doesn't fail: the greeting goes out without the weather.

## Set up the webhook secret

Morning Bro posts through a Discord webhook. Its URL is a secret: anyone who has it can post to the channel, so it never goes in the Settings File or in a commit.

1. **Create the webhook in Discord.** In the server, open **Server Settings → Integrations → Webhooks → New Webhook**. Pick the channel to post to, and click **Copy Webhook URL**. The webhook's name and picture don't matter: Morning Bro posts under its own name.
2. **Store it in GitHub.** In the repository, open **Settings → Secrets and variables → Actions → New repository secret**. Name it `DISCORD_WEBHOOK_URL` and paste the URL as its value.
3. **Check it works** with a Forced Post.

If the URL ever leaks, delete the webhook in Discord, create a new one, and update the secret.

## Make a Forced Post

In the repository, open **Actions → Morning → Run workflow**, keep the `master` branch, and click **Run workflow**. The message is posted within a minute or two. The run's log shows the settings it loaded and the message as plain text.

## Change the settings

Everything you can change lives in the Settings File, [`src/main/resources/application.yml`](src/main/resources/application.yml). Edit it, commit, and push to `master`: the next run uses it. There's no code to change.

| Setting | What it does |
|---|---|
| `schedule.hour` | The Post Time, a whole hour from 0 to 23 |
| `schedule.timezone` | The timezone of the Post Time, such as `Europe/Athens`. It also decides which day "today" is, and whether it's Friday |
| `cities` | One line per city: its `name`, `flag` emoji, and location as `lat` and `lon`, in the order they're shown |
| `greeting.text` | The greeting at the start of the message |
| `greeting.home-city` | The city whose weather emoji ends the greeting. It must be one of the `cities` names |
| `greeting.friday` | The extra line on Fridays |
| `greeting.fallback` | The line shown when the weather is unavailable |
| `weather.unlikely-rain-threshold` | Below this chance of rain, a rainy or stormy day shows `unlikely-rain`'s emoji instead, so a brief shower doesn't make the whole day look gloomy |
| `weather.rain-chance-display-threshold` | At or above this chance of rain, a city shows "☔ NN%" |
| `weather.emoji` | The emoji for each kind of weather |
| `weather.alert-emoji` | The extra emoji on very hot (high of 35° or more) and freezing (low of 0° or less) days |
| `weather.attribution`, `weather.attribution-url` | The credit to Open-Meteo and its link. Open-Meteo's terms require them, so keep both |
| `discord.username` | The name Morning Bro posts under |
| `discord.avatar-url` | A link to its profile picture, or `null` for the webhook's own |

Keys must be spelled exactly as above. A mistake, such as a missing emoji, a home city that isn't in the list, or an hour of 25, is caught before anything is posted: CI fails on the push that makes it, and the run's log says what's wrong. To check a change before you push it, do a Dry Run.

## Do a Dry Run

A Dry Run builds the message with today's real weather and prints it, without posting to Discord. It's how to try a change to the Settings File. It needs [Java 27](https://adoptium.net/) installed; the Maven wrapper in the repository downloads everything else.

Build it once, and again after each change to the Settings File:

```sh
./mvnw package -DskipTests
```

Then run it. `FORCE=true` builds the message even when it isn't the Post Time.

```sh
DRY_RUN=true FORCE=true java -jar target/morning-bro.jar
```

In PowerShell on Windows, use `.\mvnw.cmd package -DskipTests` to build, and this to run:

```powershell
$env:DRY_RUN = "true"; $env:FORCE = "true"; java -jar target/morning-bro.jar
```

The output shows the settings it loaded, the message as plain text, and then exactly what would be sent to Discord. A Windows terminal may show the emoji as `?`; the message itself is fine.

To try a Settings File without rebuilding, point `SETTINGS_PATH` at it, e.g. `SETTINGS_PATH=my-settings.yml`.

## Good to know

- **GitHub may start scheduled runs late, or skip them.** A run counts for the hour it was scheduled for, not the time it starts. So a late Post Time run still posts, just late, and a late run from the hour before never makes a second post. A run GitHub skips is a missed day: that's on purpose, as a missed day is better than two posts.
- **GitHub pauses scheduled workflows in a public repository after 60 days without a commit.** If the posts stop, check the Actions tab, re-enable the Morning workflow, or push any commit.
- **Every push runs CI,** which runs the tests and a Dry Run with the real Settings File, so a broken change fails there instead of at the next Post Time.

## Credits

Weather data by [Open-Meteo.com](https://open-meteo.com/), under the [Creative Commons Attribution 4.0 (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/) licence. Open-Meteo's free API is for non-commercial use, and each message credits it with a link.
