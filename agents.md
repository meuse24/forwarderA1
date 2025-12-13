# agents

## Purpose
- Document how to run and maintain AI agents for this project.
- Keep agent-related configs discoverable (repo: .claude/agents/).

## Files & locations
- .claude/agents/ – directory for agent configs/scripts (currently empty).
- .claude/settings.local.json – local settings used by the agent harness.

## Usage
- If the workspace is read-only, run /init in the Codex CLI to enable edits, then re-run the intended commands.
- Keep new agent docs/scripts in .claude/agents/.

## Conventions
- Prefer repo-relative paths in docs.
- Avoid committing secrets; use env vars or local config.
- Keep instructions concise; update this file when agent workflows change.