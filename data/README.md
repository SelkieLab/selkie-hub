# data/ - runtime fixtures the host reads

Everything in this directory is **real input for the running app**, not a test fake: the
host reads it at startup and then keeps watching it, so editing a file while the window
is open updates the UI in about a second (no restart).

```text
data/
  tokens.json                 context-budget sample  -> TokenBridge
  plugins/<id>/manifest.json  one directory per plugin -> PluginBridge
```

## manifest.json

```json
{
  "id": "weather-kit",
  "name": "Weather Kit",
  "version": "1.4.0",
  "publisher": "SelkieLab",
  "description": "Adds a five-day forecast tool.",
  "official": true,
  "provides": ["tool", "prompt"],
  "permissions": ["net:http"],
  "token_policy": "schema-only"
}
```

Required: `id`, `name`. Everything else falls back (version `0.0.0`, publisher `""`,
`official` false, `token_policy` `schema-only`, empty `provides`/`permissions`).

QML cannot read `List<String>` reliably, so the bridge also publishes
`providesText` / `permissionsText` pre-joined with `", "`.

The shipped set is deliberately varied so the UI has something to show:

| plugin | kind | provides | permissions | token_policy |
|---|---|---|---|---|
| weather-kit | official | tool, prompt | net:http | schema-only |
| persona-injector | official | prompt, context | fs:read | schema-only |
| memory-vault | official | tool, prompt | fs:read, fs:write | **full-body** |
| telemetry-sink | third party | tool | net:http | **full-body** |
| theme-lab | third party | theme, prompt | fs:read | schema-only |

`net:http` makes the dev backend report a higher latency, and `full-body` makes it
report `degraded` - which is how the health column proves it is reading these files.
(memory-vault is `full-body` on purpose: the official health endpoint then returns at
least one `degraded` plugin, so the page has something real to colour.)

## tokens.json

```json
{ "used": 118400, "total": 200000,
  "persona": 24600, "recall": 19000, "tools": 41200, "history": 33600 }
```

Invariant: `persona + recall + tools + history == used`. `total` is the budget.

## Hot reload

`PluginBridge.tick()` / `TokenBridge.tick()` run once per frame on the render thread.
They fingerprint the files (count, bytes, newest mtime, path hash) and only re-read when
that fingerprint changed - editing a manifest, adding/removing a plugin directory, or
touching tokens.json is picked up on the next frame. Every re-read bumps `revision` and
prints one line, e.g.:

```text
[data] plugins revision=2 count=6 official=3 thirdParty=3 source=manifest
```

`data/local/` is git-ignored (machine-local state); everything else here is tracked.
