# Privacy: screen recognition, microphone, camera

Mandatory. Run it on every TV right after the debloat batches, even when the owner only asked for the launcher.
Say why in one line: smart TVs recognise what is on screen (ACR), including HDMI inputs, and sell the viewing
history for ads; some keep a microphone or a camera available to vendor apps.

What is known, so you can answer the owner without overstating it:
- ACR samples the picture (or sound) several times a second and sends fingerprints, not full video. A 2024
  UC Davis / UCL study measured Samsung sampling every 500 ms and LG every 10 ms, also on HDMI and antenna
  input. On Android TV / Google TV it is usually Samba TV (Sony, TCL, Philips and others) or a vendor service.
- Vendors earn on viewing data, which is why TVs are cheap. Texas sued Samsung, Sony, LG, Hisense and TCL
  over ACR in December 2025 (allegations, not rulings).
- Toggles are not proof: the owner switches them off, then we check the packages and app ops ourselves.

## 1. Scan

```sh
python <skill>/scripts/privacy.py scan
```

It prints ACR/ad/analytics candidates by name, every app that holds `RECORD_AUDIO` or `CAMERA`, and whether the
TV reports a microphone or camera. Read each candidate with `dumpsys package <pkg>` (debloat.md section 1).
Typical finds: `*samba*`, "interactive TV" services, vendor `*.acr*`, `*.ads*`,
`*.analytics*`, "user experience programme" apps. Names vary per firmware: trust
services, receivers and permissions over the name.

## 2. Menu toggles (owner, with a screenshot each)

Ask Q-R1. The owner switches these off in the TV menu; take a screenshot of each screen as proof.
Paths differ per model and year: search the settings screen, do not insist on one path.

| Where | What to switch off |
|---|---|
| Sony / Philips / TCL Android TV | "Samba Interactive TV" / "Interactive TV" / "Viewing information" (Settings → Privacy, Device Preferences or Initial setup) |
| Google TV / Android TV 12+ | Privacy → Usage & Diagnostics off; Privacy → Ads → "Delete advertising ID" (or "Opt out of Ads Personalization" on older builds) |
| Vendor privacy screen | "Viewing information services", "Content recognition", "User experience programme", "Personalised recommendations" |
| Voice | "Hey Google" / hands-free voice / far-field mic toggle; a physical mic switch on the set if it has one |

Reading the setting back with `snapshot.py` (save, toggle, diff) finds the key so the next TV of this family can
be checked by command. Note the key in `references/devices/<board>.md`.

## 3. Disable ACR packages

Packages proven to be ACR, ad or viewing-data services go into their own batch,
`debloat/batchN-privacy.txt`, and through `disable.py` like any other batch. Screenshot Home and play one
video after it. If `disable.py` refuses (core, factory or Wi-Fi owner), leave the package and rely on the menu
toggle and DNS (section 5).

## 4. Microphone and camera

Ask Q-R2 and Q-R3. Block the mic and camera for every vendor app the owner does not use them for:

```sh
python <skill>/scripts/privacy.py deny <pkg> [<pkg> ...]
python <skill>/scripts/privacy.py scan     # proves RECORD_AUDIO=ignore / CAMERA=ignore
```

- Keep the mic for the voice assistant the owner kept in Q-D1/Q-D2 (`katniss`, `tv.assistant`, the vendor one),
  and for apps they named (video calls, karaoke).
- With no mic and no camera feature on the TV, say so and only block what holds the permission anyway:
  a remote or an accessory can add a microphone later.
- A camera on the set (pop-up or clip-on): block it for everything but the call app the owner uses, and tell
  them to push it down or cover it. Software cannot prove a camera is off; the shutter can.
- App ops survive reboots; `restore.py` puts the original modes back.

## 5. Network

Recommend the ad-blocking Private DNS (Q-L5, debloat.md "Optional: ad-blocking DNS") here: it also blocks
many ACR and tracking hosts the packages above did not cover. Same warnings: check playback, VPN/DPI-bypass apps.

A block list ages: firmware updates add hosts. A reader running AdGuard Home saw an LG webOS TV with every
telemetry toggle off still call `eic-connect-client.lgthinq.com`, `eic.tv.wiselg.com`, `fr.nextlgsdp.com` and
`lgtvonline.lge.com` at startup. After two webOS updates it also called `prov-lg.alphonso.tv` (Alphonso, LG's
ACR company), which his 18-month-old list did not have. His list blocks `*.lgsmartplatform.com`, `*.lgeapi.com`,
`*.lgsmartad.com`, `*.lgtviot.com`, `*.lgthinq.com`, `*.lge.com`, `*.lgtvcommon.com`, `*.lgtvsdp.com` and
allows only the firmware hosts `snu.lge.com` and `su-ssl.lge.com`.

When the owner runs their own DNS filter (AdGuard Home, Pi-hole), suggest the same shape for the TV: deny
everything from the TV's IP except firmware updates and the streaming apps they use, redirect port 53 on the
router so hardcoded DNS cannot bypass it, and block outbound DoT/DoH from the TV. Check playback in every app
after; a too-tight list breaks apps, not the TV.

## 6. Report

Tell the owner: which ACR/ad packages were disabled, which toggles they switched off (with the screenshots),
which apps lost the mic or camera and which kept them, and whether Private DNS is on. Put the same into the
TV's README under "Privacy".
