# Stop Node UI Reference

This mockup shows the current stop-state behavior already wired into the app:

- When walking stops, a bottom sheet slides up with `Left`, `More`, and `Right`.
- Pressing `More` expands quick node options: `Room`, `Toilet`, `Hall`, `Lab`, and `Modify`.
- When walking resumes, the stop sheet goes away again.

Generated mockup image:

- `docs/stop-node-ui-reference.svg`
- `docs/stop-node-ui-reference.png` can also be produced later with `tools/generate_stop_node_reference.py` if local image execution is allowed.

Primary code locations:

- `app/src/main/java/com/ashutosh/pathdrawingapp/ui/screens/IndoorMapScreen.kt`
- `app/src/main/java/com/ashutosh/pathdrawingapp/ui/components/StopDetectedDialog.kt`
