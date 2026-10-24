<p align="center">
  <img src="assets/banner.png" alt="CleanTV" width="100%">
</p>

<h1 align="center">CleanTV</h1>

<p align="center"><b>English</b> · <a href="README.ru.md">Русский</a></p>

<p align="center">
  <b>Make your Android TV fast again with one sentence in Claude Code.</b><br>
  The skill debloats the TV, calibrates picture and sound, and swaps the ad-filled home screen for your own apps.
</p>

<p align="center">
  <a href="https://github.com/JohnWayneeee/cleantv/stargazers"><img src="https://img.shields.io/github/stars/JohnWayneeee/cleantv?style=flat&color=yellow" alt="Stars"></a>
  <img src="https://img.shields.io/badge/Android%20TV-Google%20TV-3DDC84?logo=androidtv&logoColor=white" alt="Android TV">
  <img src="https://img.shields.io/badge/Claude%20Code-skill-D97757" alt="Claude Code skill">
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT">
</p>

<p align="center">
  ⭐ <b>Found it useful? Star the repo.</b> It helps other owners of sluggish TVs find it.
</p>

---

## Before → after

Out of the box your TV shows ads instead of a home screen, runs a dozen background services and smooths and
sharpens films. On our CHiQ the stock equaliser added +41 at 5 kHz. All of that shares 1–3 GB of RAM with your apps.

Measured on two TVs:

| | Out of the box | After CleanTV |
|---|---|---|
| Free memory (CHiQ, 2.8 GB) | 1.42 GB | **1.77 GB** |
| Free memory (Harper, 1.8 GB) | 721 MB | **888 MB** |
| Home screen | ads and recommendations | **your apps on pitch black** |
| Picture | factory preset and "enhancers" | **calibrated for your room** |
| YouTube where the ISP throttles it | stalls | **plays, bypass for YouTube only** |

## What it does

- **Debloat.** Disables telemetry, ads and vendor junk. Asks what you use first. Removes nothing: one command brings everything back.
- **Speed.** Measures memory before and after. Speeds up animations, limits background work of rarely used apps.
- **Apple TV style launcher.** Pitch black background, a dock with your apps, a glass highlight, a clock, an HDMI inputs menu. On power-on it shows placeholder tiles at once instead of a black screen.
- **Calibrated picture.** Mode, colour temperature, gamma and backlight for your room's light. Switches off motion smoothing, noise reduction and the other "enhancers".
- **Sound.** Dolby, dialogue enhancement, volume levelling, night mode. For TV speakers, a soundbar, optical or Bluetooth.
- **YouTube without throttling.** Installs [ByeByeDPI](https://github.com/romanvht/ByeByeDPI) on the TV itself, no external servers. Only YouTube goes through it. The launcher starts it when YouTube opens and stops it on the home screen.
- **Checks on the TV.** The skill confirms each step with a screenshot from the TV or a command. At the end it closes network debugging and proves the port is shut.

Works on Android TV and Google TV: Xiaomi, TCL, Hisense, Sony, Philips, CHiQ/Changhong, Haier, Harper, Nvidia Shield and others.
Not for Samsung Tizen, LG webOS or Fire TV.

## Three steps

**1. Install the skill.** The easiest way: copy this prompt and send it to Claude Code. The agent installs everything.

```text
Install the CleanTV skill for Claude Code from https://github.com/JohnWayneeee/cleantv
1. Clone the repository into a temporary folder: git clone --depth 1 https://github.com/JohnWayneeee/cleantv
2. Copy skills/android-tv-setup to ~/.claude/skills/android-tv-setup
   (on Windows: %USERPROFILE%\.claude\skills\android-tv-setup). If the folder exists, ask me before replacing it.
3. Check that adb (Android SDK platform-tools) and Python 3 are installed. The launcher also needs
   JDK 17+ and Android SDK platform 35. If something is missing, tell me how to install it on my OS
   and install it only after I agree.
4. Delete the temporary folder.
5. Show me that ~/.claude/skills/android-tv-setup/SKILL.md exists and remind me
   to restart Claude Code and say "Set up my TV".
```

Or with Claude Code plugins:

```text
/plugin marketplace add JohnWayneeee/cleantv
/plugin install android-tv-setup@android-tv-setup
```

**2. Turn on debugging on the TV.** Settings → Device Preferences → About → press OK on "Build" 7 times.
Back → Developer options → turn on "USB debugging" and "Network debugging" (if listed).

**3. Tell Claude:** "Set up my TV".

The skill finds the TV on your network and asks 10–15 questions along the way: which services to keep, how bright
the room is, what bothers you in the sound. All questions: [`references/questions.md`](skills/android-tv-setup/references/questions.md).

<details>
<summary>What you need on the computer</summary>

- Android SDK platform-tools (`adb`);
- Python 3;
- for the launcher: Android SDK platform 35 and JDK 17+ (the JDK bundled with Android Studio works).

The TV and the computer must be on the same network. If the computer runs a VPN, exclude your home subnet from the tunnel.
</details>

## Safety

- The skill disables system packages (`pm disable-user`) and does not uninstall them.
- It leaves alone what the TV needs to show a picture, switch inputs or pair the remote.
- It backs up the TV before the first change. Each TV's data stays in `tvs/<model>/`, outside git.
- Undo everything with one command:

```sh
cd tvs/<model>
python ../../skills/android-tv-setup/scripts/restore.py
```

## Next

- Remote module: app buttons, the gear key, remapping, Bluetooth remotes.
- Picture and sound settings tables for popular boards: [`references/devices/`](skills/android-tv-setup/references/devices/). Tuned your TV? Send a PR with notes on its board.

---

<p align="center">
  ⭐ <b>CleanTV sped up your TV? Star the repo.</b>
</p>

<!-- Buy Me a Coffee: uncomment and put in your link
<p align="center">
  <a href="https://www.buymeacoffee.com/YOUR_BMC_NAME"><img src="https://img.shields.io/badge/Buy%20me%20a%20coffee-FFDD00?logo=buymeacoffee&logoColor=black" alt="Buy me a coffee"></a>
</p>
-->

## License

MIT. Onest font: SIL Open Font License 1.1.
