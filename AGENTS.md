# AGENTS.md

## Agent skills

### Issue tracker

Issues are tracked as local markdown files under `.scratch/<feature>/`, which is git-ignored, so nothing goes to GitHub Issues. See `docs/agents/ISSUE-TRACKER.md`.

### Domain docs

`docs/GLOSSARY.md` and `docs/adr/`, alongside `docs/PRODUCT.md` and `docs/ARCHITECTURE.md`. See `docs/agents/DOMAIN.md`.

### Git

Work for a ticket goes on a branch named `<type>/<NN>-<ticket-slug>`, e.g. `feat/01-load-settings-file`. Every commit subject starts with the type and the ticket number it belongs to: `<type>(#NN): <summary>`, e.g. `feat(#01): Load the Settings File`.
