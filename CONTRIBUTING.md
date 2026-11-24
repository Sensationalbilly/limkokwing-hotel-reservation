# Contributing

## Workflow
1. **Open an issue** for each feature or bug (e.g. "Add check-out confirmation"). Assign it to yourself.
2. **Create a branch** from `main`:
   `feature/<short-name>` for features, `fix/<short-name>` for bugs, `docs/<short-name>` for documentation.
3. **Commit small, focused changes** with clear messages, and reference the issue:
   `Add live price summary to booking form (#12)`
4. **Open a pull request** into `main`. Another team member reviews it before merging.
5. Never commit `config/db.properties` or other passwords.

## Code style
- Keep the layers separate: UI → controller → DAO. SQL only goes in `dao/`, and Swing code only in `ui/`.
- Use `PreparedStatement` parameters, never string-concatenated SQL.
- New pages extend `BasePanel`. New buttons extend `BaseButton`. New room types extend `Room`.
- When you change data, publish an `EventBus` topic so other screens refresh.
- Give every new control an accessible name or a linked label (`setLabelFor`), and make sure it works with the keyboard.

## Testing a change
Run `run.bat` (or `mvn compile exec:java`). Then sign in as both `admin` and `reception` and check the screens you touched.
