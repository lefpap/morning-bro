# The app, not the scheduler, decides when to post

GitHub Actions cron only runs in UTC, so a fixed cron time would move the post by an hour whenever daylight saving changes (05:00 UTC is 08:00 in Athens in summer but 07:00 in winter). Instead, the workflow runs every hour, and the app posts only when the current hour in the Reference Timezone matches the Post Time. All other runs exit successfully without posting.

## Consequences

- There are 24 runs a day for one Scheduled Post. Each run is a few seconds, well within the free tier.
- The Post Time can only be set to a whole hour.
- With no stored state, a cron run that starts late enough to fall outside the Post Time hour is a missed day, not a late post. This matches the rule that a missed day is better than a duplicate.
