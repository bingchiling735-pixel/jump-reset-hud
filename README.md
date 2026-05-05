# Jump Reset HUD (Fabric 1.21.4)

Client-side Minecraft HUD mod that measures jump reset timing in PvP.

## Features
- Detects hits via `hurtTime`.
- Measures `delay = now - lastHitTime` when jump is pressed.
- Classifies timing:
  - **0–50ms**: Perfect
  - **51–120ms**: Good
  - **121ms+**: Bad
- Auto-resets to `Waiting...` after 1 second of inactivity.
- Color-coded HUD text.
- Timing bar with center at 0ms and marker for measured timing.
- Draggable HUD position saved to config.
- Toggle keybind (`H`) to enable/disable the HUD.

## Config
Stored at `config/jumpresethud.json` with:
- `posX`, `posY`
- `offsetX`, `offsetY`
- `scale`
- `enabled`

## Requirements
- Java 17
- Fabric Loader
- Fabric API
- Minecraft 1.21.4
