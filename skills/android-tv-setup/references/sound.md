# Sound

Ask Q-S1..Q-S4 first. Find the keys the same way as for picture:

```sh
python <skill>/scripts/setting.py list global sound_ hdmi_ encoded_surround
```

Map vendor enums with `snapshot.py` (picture.md step 2), apply with `setting.py`, verify in the sound menu by
screenshot. Keys that don't change the menu or the sound get restored and set through the menu instead.
Some TV apps keep sound settings in their own storage and leave stale `sound_*` keys in `settings global`
(Skyworth): if `snapshot.py diff` is empty after a menu change, drive the menu and list the changes in the
TV's README, since `restore.py` cannot undo them. Check sound per source too (apps vs tuner).

## Android-level keys (all brands)

| Key | Values |
|---|---|
| `encoded_surround_output` | 0 auto (follows the receiver's EDID), 1 never (always PCM), 2 always (bitstream), 3 manual (+ `encoded_surround_output_enabled_formats`) |
| `hdmi_control_enabled` | 1 = HDMI-CEC on (needed for ARC/eARC volume and power) |
| `hdmi_system_audio_control_enabled` | 1 = sound goes to the ARC device, TV remote controls its volume |

## Targets by output (Q-S1)

### TV speakers

| Item | Target |
|---|---|
| Sound mode | Standard / Movie / Smart; avoid Music, Dynamic, Vivid, "Clear Voice" as a mode |
| Equaliser | flat (all 0) or off |
| Bass enhancement | off or low |
| Virtual surround / DTS Virtual:X / Dolby virtualizer | off (small speakers sound phasey) unless the owner prefers it |
| Dialogue enhancement | on only for Q-S3 "dialogue hard to hear" |
| Auto volume / volume leveller (AVL) | on only for Q-S3 "volume jumps" |
| Dynamic range compression / night mode | Q-S4 |
| Speaker delay, balance | 0 |

### Soundbar or receiver over HDMI ARC/eARC

- CEC on, ARC/system audio on, eARC on if both devices support it.
- TV speakers off / external speaker as output.
- Digital output: Auto/Passthrough/Bitstream when the bar decodes Dolby/DTS (Q-S2); PCM for stereo-only bars,
  otherwise 5.1 content can be silent. `encoded_surround_output` 0 for decoding bars, 1 for PCM.
- Atmos needs eARC (TrueHD) or DD+ over ARC; plain ARC carries DD+ at most.
- All TV sound processing off; passthrough bypasses it anyway.
- Lip sync (Q-S3 "sound lags"): set the TV's digital audio delay to 0, then adjust on the bar or receiver.

### Optical

Dolby Digital / DTS core only (no DD+, Atmos, TrueHD). Output "Auto"/"Dolby Digital" for decoding bars,
PCM for stereo bars.

### Bluetooth

No Dolby; latency is fixed by the device. Turn processing off, AVL optional. Apps usually compensate A/V delay.

## Verify

```sh
python <skill>/scripts/tv.py 'dumpsys audio | grep -iE "devices|stream_music" | head'
python <skill>/scripts/tv.py 'dumpsys media.audio_flinger | grep -iE "output|format|AUDIO_FORMAT" | head -20'
```

Ask the owner to play something they know (a film with speech and an action scene) and confirm dialogue clarity,
level jumps and lip sync.
