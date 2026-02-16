# EZWhere

EZWhere is a Paper 1.21.x plugin that records player location snapshots and displays them in a paginated GUI.

## Features

- `/where` prints your current block coordinates and world.
- Every `/where` call is saved to your personal history.
- `/where history` opens a 54-slot GUI with 44 history entries per page.
- Optional `/where history <player>` for users with `ezwhere.history.others`.
- Two storage backends:
  - `YAML` (default): `plugins/EZWhere/history/<uuid>.yml`
  - `SQLITE`: `plugins/EZWhere/history.db`
- Configurable max history size, GUI title, date format, yaw/pitch capture, and optional join/quit auto-record.

## Build

```bash
mvn clean package
```

Compiled jar will be in `target/`.

## Installation

1. Build the jar with Maven.
2. Copy `target/ezwhere-1.0.0.jar` into your Paper server `plugins/` folder.
3. Start/restart the server.
4. Edit generated config files in `plugins/EZWhere/` as needed.

## Commands

- `/where` - show current location and save snapshot.
- `/where history` - open your history GUI.
- `/where history <player>` - open another online player's history (requires permission).

## Permission

- `ezwhere.history.others` (default: op)

## Configuration

`config.yml`

```yaml
storage: YAML
max_history: 250
store_yaw_pitch: false
record_on_join: false
record_on_quit: false
date_time_format: "yyyy-MM-dd HH:mm:ss"
gui:
  title: "EZWhere History"
```

`messages.yml` contains all chat messages and can be customized with MiniMessage formatting.

## CI/CD

GitHub Actions workflow (`.github/workflows/build.yml`) will:
- Build on push to `main`
- Upload jar artifacts
- Create a GitHub Release and attach jar when tags starting with `v` are pushed (e.g. `v1.0.0`)
