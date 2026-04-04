# Repository Guidelines

## Project Structure & Module Organization
Core application code lives in `src/main/java/com/ali/dev/xonix`. UI and startup classes such as `XonixApp`, `Images`, and `KeyboardInput` sit in the root package; gameplay logic and data models are under `src/main/java/com/ali/dev/xonix/model`. Runtime assets and configuration live in `src/main/resources`, including `levels.json`, `logback.xml`, and sprite images in `src/main/resources/images`. Repository-level examples and reference files include `images/` screenshots, `levels1.json`, and `scores.txt`.

## Build, Test, and Development Commands
Use Maven from the repository root:

- `mvn clean package` builds the executable JAR and the dependency bundle in `target/`.
- `mvn clean package exec:java` compiles and launches the game with bundled levels.
- `mvn clean package exec:java -Dexec.args="-s levels1.json -l 0"` runs with a custom level file and starting level.
- `mvn -q test` runs the test phase; it currently passes with no committed test classes, so add tests with code changes.
- `java -jar target/xonix-1.0-SNAPSHOT-jar-with-dependencies.jar` runs the packaged build.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, braces on the same line, and `UpperCamelCase` for classes, enums, and records. Use `lowerCamelCase` for fields and methods, and keep constants in `UPPER_SNAKE_CASE` in `Config`. Keep packages under `com.ali.dev.xonix`; put game rules in `model` rather than in the Swing frame where possible.

## Testing Guidelines
There is no committed `src/test/java` tree yet. Add new tests under `src/test/java/com/ali/dev/xonix/...` and mirror the production package layout. Prefer focused unit tests around engine rules, collision handling, score calculation, and level parsing. Name test classes `*Test` so Maven picks them up automatically.

## Commit & Pull Request Guidelines
Recent commits use short, imperative summaries such as `extract Algo` and `command line parser`. Keep commit subjects brief, lowercase is acceptable, and describe one logical change per commit. For pull requests, include:

- a short description of gameplay or engine changes
- linked issue or task ID when available
- screenshots or a short video for UI-visible changes
- the exact verification command used, for example `mvn clean package` or `mvn -q test`

## Configuration Tips
Level data is JSON-driven. Prefer updating `src/main/resources/levels.json` for default gameplay and use `-s <file>` for local experiments instead of hardcoding values.
