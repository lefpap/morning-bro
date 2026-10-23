# Issue tracker: Local Markdown

Issues and specs for this repo live as markdown files in `.scratch/`. The folder is git-ignored: tasks stay local and never go to GitHub. Don't create GitHub issues for this repo.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`
- The spec is `.scratch/<feature-slug>/spec.md`
- Implementation issues are one file per ticket at `.scratch/<feature-slug>/issues/<NN>-<type>-<slug>.md`, never a single combined tickets file
- `<NN>` is unique across the whole repo, not per feature, because branches, commits and PRs refer to tickets by number alone. A new ticket takes the next number after the highest one in any `.scratch/*/issues/`
- `<type>` is the ticket's category, the same type its branch and commits use: `feat`, `fix`, `refactor`, `docs`, `test`, `chore` or `ci`. It's also recorded as a `Type:` line just above `Status:`, and in the heading: `# <NN> [<type>]: <Title>`
- State is recorded as a `Status:` line near the top of each issue file (e.g. `open`, `in-progress`, `done`)
- Comments and conversation history append to the bottom of the file under a `## Comments` heading

## When a skill says "publish to the issue tracker"

Create a new file under `.scratch/<feature-slug>/` (creating the directory if needed).

## When a skill says "fetch the relevant ticket"

Read the file at the referenced path. The user will normally pass the path or the issue number directly.
