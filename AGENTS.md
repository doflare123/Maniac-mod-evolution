# AGENTS.md

## Project

Maniac Revolution is a Minecraft Forge mod for Minecraft 1.20.1.

- Mod id: `maniacrev`
- Base package: `org.example.maniacrevolution`
- Forge: `47.2.18`
- Java: 17
- Mappings: official `1.20.1`
- Main mod class: `src/main/java/org/example/maniacrevolution/Maniacrev.java`
- Resources: `src/main/resources`

## Build And Run

Use Gradle wrapper commands from the repository root.

```powershell
$env:JAVA_HOME='C:\Users\ABOBUS\.jdks\ms-17.0.17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat compileJava --no-daemon
```

Other useful commands:

```powershell
.\gradlew.bat build --no-daemon
.\gradlew.bat runClient --no-daemon
```

If Gradle needs to download dependencies or the wrapper distribution, network access may be required.

## Project Map

- `hud/` contains custom HUD rendering and overlays.
- `event/` contains Forge event handlers for input, gameplay restrictions, drops, abilities, and client events.
- `events/` contains mod bus registrations such as custom GUI overlays.
- `network/` and `network/packets/` contain SimpleChannel setup and packets.
- `character/`, `perk/`, `mana/`, `downed/`, `nightmare/`, `shop/`, `stats/` contain gameplay systems.
- `gui/` and `client/screen/` contain custom screens.
- `resources/assets/maniacrev` contains textures, lang files, models, and sounds.
- `resources/data` contains datapack-style data.

## HUD Notes

The custom HUD is registered through Forge `RegisterGuiOverlaysEvent`, currently in:

- `src/main/java/org/example/maniacrevolution/events/HudEvents.java`
- `src/main/java/org/example/maniacrevolution/hud/CustomHud.java`

Vanilla HUD elements are hidden through `RenderGuiOverlayEvent.Pre`. The custom HUD and penalty slot behavior should apply only to survival/adventure players. Creative and spectator should keep vanilla HUD behavior and vanilla functionality unless the user explicitly asks otherwise.

## Working Rules

- Read the relevant code before changing behavior.
- Keep changes scoped to the user request.
- Do not revert or overwrite user changes unless the user explicitly asks.
- If a failure is in code unrelated to the current task, do not change that unrelated code automatically.
- If unrelated broken code blocks verification, report it clearly and offer a focused fix instead of silently expanding the task.
- If the right fix is ambiguous, ask or propose concrete options before editing.
- Prefer existing project patterns and Forge APIs already used in the repo.
- Use `apply_patch` for manual file edits.
- Avoid touching generated output, credentials, or unrelated resources.
- After edits, check the final diff and verify that only intended files changed.
- Run the narrowest useful build or compile check when practical, and report any blocker with exact file references.
