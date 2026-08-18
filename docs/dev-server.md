# Local test server

The project uses [run-paper-maven-plugin](https://github.com/bluelhf/run-paper-maven-plugin) to
spin up a real Paper server with the freshly-built plugin already installed, for manual in-game
testing during development. Not part of the release build — purely a dev convenience.

## Running it

- **IntelliJ**: pick `Run Test Server` from the run configuration dropdown and press Run/Debug.
  It's a shared configuration checked in under `.run/`, so it shows up automatically for anyone
  who opens the project.
- **CLI**: `mvn run-paper:install verify run-paper:run-server`

Either way, this rebuilds the plugin jar (`verify` runs `package`) and (re)starts the server —
it does **not** run `clean`, so world data and installed plugins in `target/run/` survive between
runs. Deleting `target/run/` (e.g. via `mvn clean`) forces a fresh server download and setup.

The Minecraft EULA is auto-accepted for this local server (`acceptEula` in `pom.xml`) — this
only applies to `target/run/`, a local, non-distributed test instance.

## First run

The first run downloads the Paper 26.2 server jar, a JetBrains Runtime, and HotswapAgent
(needed for hot-swapping, see below) into `target/run/`. This can take a while; later runs
reuse what's already downloaded.

## Adding other plugins (e.g. LuckPerms)

`run-paper-maven-plugin` only manages the TES plugin jar itself. To test alongside other
plugins the real server depends on (per `docs/ROADMAP.md`, LuckPerms is a hard external
requirement), download their jars once and drop them into `target/run/plugins/` — they'll be
picked up on every subsequent run until `target/run/` is wiped, at which point they need to be
re-added.

## Hot-swapping

Hot-swap is enabled (`hotswap` in `pom.xml`), so code changes can be pushed to the running
server without a full restart:

1. Start the server via the run configuration above.
2. The server console prompts you to attach a debugger — attach IntelliJ's debugger to
   `localhost:5005` (or it may prompt automatically).
3. After editing code and rebuilding, a reload icon appears in the IDE to push the change to
   the live server.

Other tools/IDEs can attach a remote JVM debugger to port `5005` the same way.
