# Enabling the native MPV player (and extra ExoPlayer codecs)

Wholphin ships with **two playback engines**:

| Engine | Works out of the box? | Notes |
|--------|-----------------------|-------|
| **ExoPlayer** (Media3) | Yes | Default backend. Uses Android's hardware decoders. Optional `ffmpeg`/`av1` software decoders add support for extra codecs. |
| **MPV** (`libmpv`) | **No, not by default** | Like VLC: brings its own decoders (FFmpeg internally), so it plays almost any container/codec (MKV, HEVC, AV1, weird audio, `.m3u8` HLS, etc.) in software, independent of what the TV's hardware supports. |

> **Why MPV needs setup:** `libmpv` is native C code compiled with the Android NDK. It is **not** part of this repository. It is published as a prebuilt Android library in a separate repo, [`wholphin-extensions`](https://github.com/damontecres/wholphin-extensions). When those binaries are absent, the build automatically falls back to the **`wholphin-mpv-stub`** module, which throws `MpvStubException` the moment you try to use MPV. That is the usual reason "MPV doesn't work".

The same applies to the optional ExoPlayer `ffmpeg` and `av1` software decoders.

---

## How the build decides where MPV comes from

From `app/build.gradle.kts` (priority order):

1. **Local AAR** — if `app/libs/wholphin-mpv-release.aar` exists, use it.
2. **Prebuilt from GitHub Packages** — if extension credentials are configured, download `wholphin-mpv` automatically.
3. **Stub** — otherwise use `:wholphin-mpv-stub` (compiles, but crashes when MPV is used).

You want to land on option **2** (easiest) or option **3 -> done via building from source** (option 1).

---

## Option A — Prebuilt binaries (recommended)

### 1. Create a GitHub Personal Access Token (classic)

- Go to GitHub -> **Settings** -> **Developer settings** -> **Personal access tokens** -> **Tokens (classic)** -> **Generate new token (classic)**.
- It **must be a classic token** (fine-grained tokens do not work with GitHub Packages maven).
- Select **only** the `read:packages` scope.
- Generate and copy the token (starts with `ghp_`).

### 2. Provide the credentials

You can use **either** of these locations (both are supported by this project):

**Option A1 — project `local.properties`** (gitignored, scoped to this repo):

```properties
WholphinExtensionsUsername=your_github_username
WholphinExtensionsPassword=ghp_your_classic_token_here
```

There is a ready-to-fill template at [`local.properties.example`](./local.properties.example):

```bash
cp local.properties.example local.properties
# then edit local.properties and paste your username + token
```

**Option A2 — global Gradle properties** (`~/.gradle/gradle.properties`, applies to all projects):

```properties
WholphinExtensionsUsername=your_github_username
WholphinExtensionsPassword=ghp_your_classic_token_here
```

> Never commit your real token. `local.properties` is already gitignored; the global file lives outside the repo.

### 3. Re-sync and build

In Android Studio: **File -> Sync Project with Gradle Files**, then build/run.

Confirm it worked by checking the Gradle log. You should see:

```
Using prebuilt libMPV
Using prebuilt ffmpeg decoder
Using prebuilt av1 decoder
```

instead of:

```
libMPV was NOT found, using stub library
```

---

## Option B — Build the extensions from source

If you prefer not to use a token, build the native artifacts yourself:

1. Clone [`wholphin-extensions`](https://github.com/damontecres/wholphin-extensions).
2. Build the media3 decoders and `libmpv` following that repo's README (requires the Android NDK).
3. Run `./gradlew assembleRelease` there to produce the `.aar` files.
4. Copy the AARs into this project's `app/libs/`:
   - `wholphin-mpv-release.aar`
   - `lib-decoder-ffmpeg-release.aar`
   - `lib-decoder-av1-release.aar`
5. Build Wholphin normally. The local AARs take top priority.

---

## Final, REQUIRED step in the app: select MPV

Even after the build includes the real `libmpv`, the **default backend is ExoPlayer**. You must switch it inside the app:

> **Settings -> Playback -> Player backend -> MPV** (or *Prefer MPV*).

If you had selected MPV *before* setting up the binaries, that is exactly when you hit the `MpvStubException` crash.

---

## Troubleshooting

- **`401 Unauthorized` / `403 Forbidden` while downloading `wholphin-mpv`:** the token is missing the `read:packages` scope, is fine-grained instead of classic, or the username is wrong.
- **Still says `using stub library`:** Gradle didn't pick up the credentials. Re-sync, and make sure the property names are exactly `WholphinExtensionsUsername` / `WholphinExtensionsPassword`.
- **MPV selected but crashes instantly:** you're still on the stub build (see above) — verify the Gradle log shows `Using prebuilt libMPV`.
- **A specific file won't play on ExoPlayer:** switch that item to MPV; libmpv handles most exotic codecs/containers in software.
