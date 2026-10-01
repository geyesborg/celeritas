# Agent guide: celeritas (fork)

Upstream Celeritas (embeddedt/celeritas, terrain + shader-pack renderer for
Minecraft 1.12.2) forked for Cleanroom. Remotes: `origin` =
github.com/geyesborg/celeritas (public, default branch `cleanroom`, full
history incl. upstream's), `upstream` = git.taumc.org/embeddedt/celeritas
(embeddedt's Forgejo; a GitHub fork of it isn't possible).
Branch model: `stonecutter` tracks upstream; all local work goes on the
`cleanroom` branch, branched from `8883da78`. Merge upstream with
`git fetch upstream && git merge upstream/stonecutter` on `cleanroom`.
Push `cleanroom` to `origin` only when the user asks; never push to
`upstream`. Keep `common/` diffs minimal for mergeability — changes belong in
`forge122/` and minimal root plumbing.

## Stack

- Upstream Stonecutter multi-version layout (common/ forge122/ forge1710/
  ornithe/ modern/ buildSrc/ plugins/), Gradle 9.7.1 wrapper
- `forge122` builds on CleanroomGradle `0.17.4` (settings plugin added in
  root `settings.gradle.kts`; `forge122/build.gradle` is Groovy, the .kts
  was replaced), local userdev `0.6.10` (`cg.repos.enableLocal=true`),
  JDK 25.0.5.7 via `gradle-dev.bat`, Java 25 source/target, no preview
- `common` compiles at its upstream Java level and is unpacked into the
  forge122 jar via `embed` (like upstream's shadowRemapJar). JOML is not
  embedded: Cleanroom ships org.joml:joml:1.10.9 and loads it first;
  no JVM downgrade for the 1.12.2 target

## Build

1. `../cleanroom-gradle-userdev/gradlew.bat publishCleanroomUserdevPublicationToMavenLocal`
   (only when the userdev checkout changes)
2. `gradle-dev.bat "-Pceleritas_target_versions=1.12.2" :forge122:build`
   — without the property `forge122` is not an included project (Stonecutter
   gating). Artifacts in `forge122/build/libs/`: `*-dev.jar` (dev runtime,
   MCP names), `*.jar` (reobfJar, SRG names), `*-sources.jar`.
3. Upstream toolchain still works for all targets:
   `gradlew.bat "-Pceleritas_target_versions=1.12.2" packageJar`
   (output `build/libs/<ver>/`; use JDK 21 JAVA_HOME for the launcher).
4. `gradle-dev.bat "-Pceleritas_target_versions=1.12.2" :forge122:runClient`
   — dev client in `forge122/run/cleanroom-client`; dev jar copied to `mods/`.

## Runtime notes

- `runClient` classpath must NOT contain `common-*.jar` or forge122's own
  classes/resources dirs (filtered in build.gradle). With common's jar on the
  app classpath the coremod/mixins split across app/Launch loaders and the
  crash-report graphics probe (`GL11.glGetString`) fatally aborts with
  "No context is current" — the real throwable is then invisible.
- LWJGL service selection (`LWJGLServiceProvider.createInstance`): probes
  `org.lwjgl.opengl.GL11C` → `LWJGL3Service`, else `LWJGL2Service`. On
  Cleanroom GL11C exists (lwjgly/LWJGL3 jars) so **LWJGL3Service** is
  selected — verified working (logged `[Celeritas] Using LWJGL3 service`).
- `mixins.celeritas.json` declares `compatibilityLevel: JAVA_25`; a lower
  level is only a DEBUG warning in CleanMix (mixins still apply).
- Dev jar is the one to run (`-dev.jar`, MCP-named mixins). The reobf jar
  has SRG names baked into mixin annotations and only works in production
  runtime, not the dev launch.
- `CeleritasVintageMixinPlugin`/`CeleritasLWJGLRelocationTransformer` are
  RetroFuturaBootstrap/lwjgl3ify compatibility — they warn `RFB class not
  found` on Cleanroom and are inert. Nothing else is Cleanroom-specific.
- `run/cleanroom-client/config/kirino_engine.cfg` controls Cleanroom's own
  Kirino render pipeline (`enableRenderDelegate`). World joins under
  Celeritas+gl46core have crashed inside
  `MeshletGpuRegistry.beginComputing` ("Failed to grow the write target
  vertex buffer") — Kirino, not Celeritas code — and also survived a join
  at least once; unresolved coexistence issue.
- gl46core detects Celeritas via `mixins.celeritas.json` on the launch
  classpath, skips its RenderGlobal/terrain mixins, and disables its
  terrain module (`CeleritasCompatAdapter`).

## Ours vs upstream's

- Ours (branch `cleanroom`): `forge122/build.gradle` (CleanroomGradle build),
  `gradle-dev.bat`, root `settings.gradle.kts` +2 lines (cleanroomgradle
  settings plugin), `gradle.properties` +2 lines (local userdev repo),
  `mixins.celeritas.json` compatibilityLevel, `LWJGLServiceProvider`
  service-selection log line, `.gitignore`/AGENTS.md.
- Upstream's: everything else, notably all of `common/` except the two-line
  service-selection println in `LWJGLServiceProvider`.
