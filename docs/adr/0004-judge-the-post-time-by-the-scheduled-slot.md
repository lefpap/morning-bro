---
status: accepted
---

# Judge the Post Time by the scheduled slot, not the start time

ADR-0001 runs the workflow hourly and posts when the current hour is the Post Time. GitHub can start a scheduled run late, sometimes by most of an hour, and it is busiest at the top of the hour. So the run for the hour *before* the Post Time could start inside the Post Time hour and post, and the Post Time's own run would then post again: two Scheduled Posts on one Forecast Day. And the Post Time's own run, if it started after the hour had passed, posted nothing.

Instead, each run is judged by the time it was *scheduled* for. The workflow lists 24 cron entries, one per UTC hour at minute :37, so `github.event.schedule` names the slot. The workflow passes it to the app as `SCHEDULED_CRON`, and `PostTime` turns it into the scheduled instant: the latest time that entry matches at or before now. The run posts only if that instant is within the hour from the Post Time, and that instant also gives the Forecast Day. A run with no `SCHEDULED_CRON`, such as a Forced Post or a local run, uses now.

The Post Time for a Forecast Day is `ZonedDateTime.of(day.atTime(hour, 0), timezone)`. In an hour the clock repeats, that is the first of the two, and in an hour the clock skips, it is the hour after the gap.

## Consequences

- A late run posts late instead of not at all, and a late run from the hour before never posts. Each cron entry fires at most once a day, so lateness alone can't make a second Scheduled Post. Whether GitHub ever fires one entry twice isn't documented.
- A run is still a missed day if GitHub drops it, or starts it more than a day late.
- The 24 entries never change when the Owner changes the Post Time. The Settings File stays the only place it is set.
- A `concurrency` group makes overlapping runs wait instead of running side by side. While one runs, a newer pending run replaces an older pending one, so a queue of late runs can lose one of them.
- A Post Time in an hour the clock repeats posts once, and one in an hour the clock skips still posts, an hour later in local time.
- A malformed `SCHEDULED_CRON` fails the run, so the Owner is notified.
- This supersedes ADR-0001's consequence that a late run is a missed day. The rest of ADR-0001 stands.
