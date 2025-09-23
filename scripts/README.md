# 🛠️ Monopoly Zapped --- Asset Stub Scripts

These scripts help you **work without distributing proprietary assets**
(Hasbro images/sounds) while keeping the project **buildable**.\
They generate **placeholder files ("stubs")** from the existing
resources or from a **list of filenames**.

## 📦 Scripts

-   `generate_raw_struct.sh`\
    Creates **dummy audio files** mirroring `res/raw/` into
    `res/raw-struct/`:

    -   `.wav` → valid WAV (1s of silence, generated with Python stdlib)
    -   `.mp3` → valid MP3 (1s of silence via `ffmpeg` if available),
        otherwise empty file
    -   other extensions → empty file

-   `save_drawable_list.sh`\
    Extracts the **filenames** present in `res/drawable-nodpi/` and
    saves them into `drawable_files.txt`.\
    This manifest can be versioned and reused later **without** keeping
    the original images.

-   `generate_drawable_from_list.sh`\
    Reads `drawable_files.txt` and creates **valid transparent XML
    drawables** in `res/drawable-nodpi-struct/`.\
    Handles **Nine-Patch** (`*.9.png`): the stub is named `name.xml`
    (the `.9` is removed).

-   `generate_raw_from_list.sh`\
    Reads `raw_files.txt` and creates **dummy audio files** in
    `res/raw-struct/` (same rules as `generate_raw_struct.sh`).

------------------------------------------------------------------------

## ✅ Requirements

-   Unix shell (bash), `sed`, `ls` (standard)
-   **Python 3** (for generating silent WAVs)
-   (Optional) **ffmpeg** installed and available in `PATH` (for
    generating valid MP3s)

------------------------------------------------------------------------

## 🗂️ Expected folder structure (by default)

Scripts expect the following paths (you can adjust them at the top of
each script):

    app/src/main/res/
    ├── drawable-nodpi/            # real images (not versioned)
    ├── drawable-nodpi-struct/     # placeholder XML drawables (generated)
    ├── raw/                       # real sounds (not versioned)
    └── raw-struct/                # placeholder sounds (generated)

Manifest files (lists of names) at the repo root or next to the scripts:

    drawable_files.txt
    raw_files.txt

> 💡 Recommended organization (cleaner):\
> - `scripts/` for the `.sh` files\
> - `manifests/` for `*_files.txt`\
> - `stubs/` for the `*-struct/` folders\
> Then update the variables at the top of the scripts (`RES_ROOT`,
> `LIST_FILE`, `RAW_DST`, etc.).

------------------------------------------------------------------------

## 🚀 Usage scenarios

### A) Generate **only** audio stubs from `res/raw/`

Use `generate_raw_struct.sh`:

``` bash
chmod +x generate_raw_struct.sh
./generate_raw_struct.sh
```

Result: dummy files in `res/raw-struct/`.

------------------------------------------------------------------------

### B) Freeze the **list** of images (to work without assets)

1)  Save the list from `res/drawable-nodpi/`:

``` bash
chmod +x save_drawable_list.sh
./save_drawable_list.sh
```

→ creates `drawable_files.txt`

2)  (Later / elsewhere) regenerate stubs **without** original images:

``` bash
chmod +x generate_drawable_from_list.sh
./generate_drawable_from_list.sh
```

→ creates transparent XMLs in `res/drawable-nodpi-struct/`.

------------------------------------------------------------------------

### C) Generate audio stubs **from a list** (without original sounds)

1)  Create/edit `raw_files.txt` (one name per line):

```{=html}
<!-- -->
```
    intro_music.mp3
    click.wav
    error.wav
    ambiance_menu.mp3

2)  Generate:

``` bash
chmod +x generate_raw_from_list.sh
./generate_raw_from_list.sh
```

→ creates files in `res/raw-struct/` (valid WAVs, MP3s via ffmpeg if
available, otherwise empty).

------------------------------------------------------------------------

## 🔧 Notes & tips

-   **Android & drawables:**\
    Files in `res/drawable*` must be **valid images** or **XML
    drawables**.\
    Empty `.png` files break the build → hence the generation of
    **transparent XML stubs**.

-   **Nine-Patch (`*.9.png`):**\
    The stub generated removes `.9` so you can still reference
    `@drawable/name` (`panel.9.png` → `panel.xml`).

-   **MP3 without ffmpeg:**\
    The script creates an **empty file** and prints a warning. The app
    **may crash** if it tries to play it.\
    Install `ffmpeg` to generate a valid 1s silent MP3.

-   **Version control:**\
    → Version the **lists** (`drawable_files.txt`, `raw_files.txt`).\
    → Do **not** version the real assets.\
    → Generated stubs (`*-struct/`): commit them only if you want a
    ready-to-run "debug profile", otherwise ignore them.

-   **Gradle (optional, debug builds with stubs):**\
    You can add a `sourceSet` for debug that includes the stubs to run
    the app without real assets.

------------------------------------------------------------------------

## ⚖️ Legal

These scripts do not include **any proprietary assets**.\
They only generate **placeholders** to ease development and testing.

**This project is not affiliated with Hasbro.**\
"Monopoly" and "Monopoly Zapped" are trademarks of Hasbro, Inc.
