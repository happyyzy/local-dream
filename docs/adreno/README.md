# Adreno + NPU Integration Logbook

This logbook tracks local-dream integration acceptance for the sd.cpp Adreno path and SDXL NPU compatibility.

## Scope

- App package: `io.github.xororz.localdream`
- Gate steps in this repo: Step36 / Step37 / Step38
- Related references:
  - `docs/adreno/fork_model_feature_matrix.md`
  - `docs/adreno/flags.md`
  - `docs/branching.md`

## Acceptance Snapshot

| Scenario | Target | Achieved | Status | Evidence |
|---|---:|---:|---:|---|
| FLUX.2-klein 512 txt2img (4-step) | `<= 40s` | `38.5s` | pass | `docs/adreno/steps/step38.md` |
| FLUX.2-klein 1024 txt2img (4-step) | `<= 142s` | `2m14s` | pass | `docs/adreno/steps/step38.md` |
| Z-Image turbo 512 txt2img (8-step) | `<= 100s` | `1m7s` | pass | `docs/adreno/steps/step38.md` |
| Z-Image turbo 1024 txt2img (8-step) | `<= 480s` | `7m16s` | pass | `docs/adreno/steps/step38.md` |
| FLUX.2-klein 512 edit (2 refs, 4-step) | `<= 100s` | `1m9s` | pass | `docs/adreno/steps/step38.md` |
| SDXL Base NPU 1024 (20-step) | `~30s class` | `33.3s` | pass | `docs/adreno/steps/step36.md` |

## Step Index

- Step36: SDXL NPU 1024 compatibility and performance recovery
  - doc: `docs/adreno/steps/step36.md`
  - output assets: `benchmarks/images/localdream_sdxl_1769073919488.png`, `benchmarks/images/localdream_sdxl_1769073990211.png`, `benchmarks/images/localdream_sdxl_1769163563122.png`
- Step37: model card download UX + HF dispatch manifests
  - doc: `docs/adreno/steps/step37.md`
  - UI assets: `docs/adreno/assets/step37/`
- Step38: branch hygiene + full acceptance replay
  - doc: `docs/adreno/steps/step38.md`
  - output assets: `docs/adreno/assets/step38/`

## Notes

- This repository keeps acceptance summaries, lightweight UI/output assets, and replayable step docs.
