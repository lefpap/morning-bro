# Operations

How Morning Bro runs every day, and how to look after it.

## Daily trigger (cron-job.org)

A free [cron-job.org](https://cron-job.org) job calls GitHub's [workflow dispatch API](https://docs.github.com/en/rest/actions/workflows#create-a-workflow-dispatch-event) every morning. Replace `<owner>/<repo>` with your repository (for example, your fork), `<branch>` with its default branch, and `<time-zone>` with the IANA time zone you want the time to follow. GitHub's own `schedule` event isn't used, because it started runs hours late (see [scheduling](architecture.md#scheduling)).

| Setting | Value |
|---|---|
| URL | `https://api.github.com/repos/<owner>/<repo>/actions/workflows/morning-bro.yml/dispatches` |
| Schedule | Every day at the time you want, time zone `<time-zone>`, e.g. `Europe/Athens` (follows daylight saving time) |
| Method | `POST` |
| Headers | `Authorization: Bearer <token>`, `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28`, `Content-Type: application/json` |
| Body | `{"ref":"<branch>","inputs":{"dry_run":"false"}}` |
| Notifications | On failure (after 1), on success after failure, on auto-disable |

The body must say `"dry_run":"false"`; a missing input falls back to the workflow default, a dry run. *Test run* in cron-job.org should return `204 No Content`, and the run then appears in `gh run list --workflow morning-bro.yml --event workflow_dispatch`.

### The GitHub token

The token is a [fine-grained personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) limited to your repository with the **Actions: Read and write** permission and nothing else (Metadata: Read-only is added automatically). It lives only in the cron-job.org job.

To rotate it (before it expires, or if it leaks): generate a new token with the same settings, replace the `Authorization` header value in the cron-job.org job, click *Test run*, then delete the old token on GitHub. GitHub's response header `github-authentication-token-expiration` shows the current token's expiry. API version `2022-11-28` stops working on 2028-03-10; update the header before then.

## Day-to-day commands

- Manual dry run: `gh workflow run morning-bro.yml`
- Manual real post (e.g. a re-send after an outage): `gh workflow run morning-bro.yml -f dry_run=false`
- Watch and inspect: `gh run list --workflow morning-bro.yml`, `gh run watch`, `gh run view <run-id> --log`
- Change the Discord channel: `gh secret set MORNING_BRO_DISCORD_WEBHOOK_URL` (it prompts for the value, so the URL never lands in your shell history).

## When something fails

- If the trigger call fails (expired token, GitHub down), cron-job.org emails the owner.
- If the run itself fails, GitHub emails whoever triggered it, which is the token's owner; the log says why.
- Known limits: see [product.md](product.md#known-limits).
