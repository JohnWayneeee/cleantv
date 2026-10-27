# Question bank

Ask each question at its phase, in the owner's language, with option/checklist UI when available.
Do not ask what you can read from the TV. Put a recommended option first and say why.
Russian wording is included because the skill was built with Russian-speaking owners; translate as needed.

## Connect (Q-C)

**Q-C1. Model and address.** Skip the parts the scan already answered.
- EN: What is the TV model? Do you know its IP (Settings > Network > status)?
- RU: Какая модель ТВ? Знаете его IP-адрес (Настройки → Сеть → статус)?

**Q-C2. Network path.** Ask separately from USB devices: "nothing plugged in" can be true while the network runs over a USB Ethernet adapter.
- EN: Is the TV on Wi-Fi, an Ethernet cable, or a USB Ethernet adapter?
- RU: ТВ подключён по Wi-Fi, кабелем в порт Ethernet или через USB-Ethernet адаптер?

**Q-C3. USB devices.** Warn: some boxes cut USB power while USB debugging is on.
- EN: What is plugged into the TV's USB ports?
- RU: Что подключено к USB-портам ТВ?

**Q-C4. Only when the LAN is unreachable from the PC.**
- EN: Is a VPN running on this computer? It can capture local traffic; please exclude your home subnet (split tunnelling).
- RU: На компьютере включён VPN? Он может перехватывать локальную сеть. Добавьте домашнюю подсеть в исключения (раздельное туннелирование).

## Debloat (Q-D)

**Q-D1. Google services to keep** (checklist; unticked are disabled):
Voice search / Assistant (`katniss`, `tv.assistant`) - Chromecast built-in (`mediashell`) - Google Play Games - Google TV / Play Movies (`videos`) - Ambient screensaver (`dreamx`).
- RU: Какими сервисами Google пользуетесь? Отмеченные оставлю, остальные отключу.

**Q-D2. Vendor features to keep** (checklist, fill from what the TV has): AirPlay receiver, screen mirroring / Miracast, vendor app store, firmware updates (recommend keeping), vendor voice assistant, e-manual.
- RU: Какие функции производителя оставить?

**Q-D3. Preloaded streaming apps** (checklist of the preloaded ones only; apps the owner installed are never touched): Netflix, Prime Video, YouTube Music, ...
- RU: Какими предустановленными сервисами пользуетесь?

**Q-D4. Unknown user-installed packages** (one question per unclear package; options "mine, keep" / "don't know, keep and note it").
- RU: На ТВ есть приложение <pkg> без иконки. Что это?

## Privacy (Q-R)

Mandatory phase (`privacy.md`). Show the `privacy.py scan` output in plain words first.

**Q-R1. Screen recognition and ads** (remind, then confirm with a screenshot of each screen): switch off Samba Interactive TV / viewing information / content recognition, Usage & Diagnostics, and delete the advertising ID. Recommend all of them; recommendations inside apps keep working.
- EN: The TV recognises what is on screen, HDMI included, and sends it for ads. Let's switch it off: open <path> and turn off <toggle>. I'll check it on the screenshot.
- RU: Телевизор распознаёт, что на экране, даже с HDMI, и отправляет это рекламщикам. Выключим: откройте <путь> и выключите <пункт>. Я проверю по скриншоту.

**Q-R2. Microphone** (checklist of apps holding it): keep for the voice assistant you use (recommended if you press the mic button) / keep for a call or karaoke app / block for everything.
- RU: Микрофон сейчас доступен этим приложениям: <список>. Кому оставить? Остальным запрещу.

**Q-R3. Camera** (only when the TV has one or an app holds the permission): block for all (recommended) / keep for a call app.
- RU: У ТВ есть камера или доступ к ней. Запретить всем (рекомендую) или оставить для видеозвонков? Шторку камеры лучше закрыть.

**Q-R4. Blocking DNS** (Android 9+): turn on (recommended: blocks ad and tracking hosts the TV calls even with the toggles off, every app keeps working, undo is one command) / no / I run my own DNS filter.
- RU: Включить DNS, который блокирует рекламу и слежку? Телевизор обращается к таким адресам даже с выключенными настройками. Приложения работают как раньше, отключается одной командой.

## Performance (Q-P)

Ask after measuring (`backup/meminfo.txt`, `top`). Show the numbers.

**Q-P1. Animations.** Options: `0.5` (quick, still smooth, recommended) / `off` (fastest, the launcher's spring highlight jumps) / keep.
- RU: Анимации системы: ускорить вдвое (рекомендую), выключить совсем или оставить?

**Q-P2. Extra memory savers** (checklist from the measured top consumers, with MB each), e.g. the assistant when the remote has no microphone, vendor background services, a setup wizard left in memory (force-stop only).
- RU: Что ещё отключить ради скорости? Всё обратимо.

**Q-P3. Background limits for rarely used apps** (checklist of installed streaming apps). They still open normally, they just don't run in the background.
- RU: Каким приложениям запретить работу в фоне? Открываться они будут как обычно.

**Q-P4. Compile apps now** (`bg-dexopt-job`, 5-15 minutes, TV must stay on). Recommended once after debloat.
- RU: Оптимизировать все приложения сейчас? Займёт 5-15 минут, ТВ не выключать.

**Q-P5. Background process limit** (only with 2 GB RAM or less and playback stops or reloads). The owner sets "At most 3/4 processes" in Developer Options; a reboot resets it. Not with ByeByeDPI or background music.
- RU: Ограничить фоновые процессы до 3–4? Видео будет стабильнее, но свёрнутые приложения будут открываться заново. Сбрасывается после перезагрузки.

## Launcher (Q-L)

**Q-L1. Dock size**: 4 (recommended) / 5 / 6 / 7.
- RU: Сколько слотов в доке?

**Q-L2. Dock apps and order**, left to right. List the launchable apps by name. If the owner names more apps than slots, use their count and say so.
- RU: Какие приложения поставить в док и в каком порядке?

**Q-L3. Google TV only, when the setup wizard owns the Wi-Fi network** (see launcher.md):
- A (recommended): owner forgets and re-joins Wi-Fi, then the wizard and the stock launcher are disabled - clean takeover.
- B: keep the stock launcher, intercept Home with an accessibility service - side effects (stock home flashes at boot, key repeat may break).
- RU: Как заменить стандартный лаунчер? A: переподключите Wi-Fi (рекомендую). B: перехват кнопки Home.

**Q-L4. Wi-Fi name on the pill** (Android 10+ needs location on): keep "Wi-Fi" (recommended) / turn location on.
- RU: Показывать имя Wi-Fi сети? Для этого нужна геолокация.

**Q-L6. Apps to hide from the grid** (checklist of the grid apps, split into several questions of 4). Hiding uses `hidden_apps`: the app stays installed and keeps serving remote keys and background work. Ask who uses the TV: for a child, an elderly person or a user with limited capacity, offer a short grid (or only the dock).
- RU: Какие приложения оставить на главном экране? Остальные скрою, работать они продолжат.

**Q-L5.** Moved to the privacy phase (Q-R4); do not ask twice.

## YouTube DPI bypass (Q-B)

Ask during the Launcher phase, only when `dpi-bypass.md` says to offer it (Russian timezone/locale, or YouTube is slow).

**Q-B1. YouTube on this TV**: yes, and it is slow or does not load - set up ByeByeDPI (recommended) / yes, it works fine / don't watch YouTube.
- RU: Смотрите YouTube на этом ТВ? Если тормозит или не грузится, поставлю ByeByeDPI: он обходит замедление прямо на ТВ, без внешних серверов.

**Q-B2. When it runs**: only while YouTube is open, started from the launcher (recommended) / always on, YouTube traffic only.
- RU: Включать обход только когда открыт YouTube (рекомендую) или держать включённым всегда, но только для YouTube?

**Q-B3. Remote YouTube key**: when the remote has a YouTube button and Q-B2 is "only while open", warn that the key bypasses the launcher.
- RU: Кнопка YouTube на пульте откроет YouTube без обхода. Открывайте его с главного экрана или выберите «всегда включён».

## Picture (Q-V)

**Q-V1. Room light while watching**: dark (evening, lights off) / dim / bright daylight / it changes.
- RU: Какое освещение обычно при просмотре: темно, приглушённый свет, яркий день, по-разному?

**Q-V2. Main content** (checklist): films and series / sport / TV channels / games (console) / PC.
- RU: Что в основном смотрите?

**Q-V3. Motion smoothing**: off - cinema look, recommended for films / low - a little smoother / high - very smooth, "soap opera" look.
- RU: Плавность движения: выключить (как в кино, рекомендую), слабая, сильная («эффект мыльной оперы»)?

**Q-V4. HDMI devices** (one per used port): console, PC, set-top box, soundbar/receiver, nothing.
- RU: Что подключено к HDMI-портам и к каким?

**Q-V5. Accuracy vs power**: accurate picture (light sensor and eco off, recommended) / save power.
- RU: Приоритет: точная картинка (рекомендую) или экономия энергии?

## Sound (Q-S)

**Q-S1. Output**: TV speakers / soundbar or receiver over HDMI ARC/eARC / optical / Bluetooth headphones or speaker.
- RU: Как слушаете звук: динамики ТВ, саундбар/ресивер по HDMI ARC/eARC, оптика, Bluetooth?

**Q-S2. Soundbar decoding** (only if a soundbar/receiver): Dolby Atmos / Dolby Digital and DTS / stereo only / don't know (give the model).
- RU: Саундбар поддерживает Dolby Atmos, Dolby Digital/DTS или только стерео? Если не знаете — модель.

**Q-S3. Complaints** (checklist): dialogue hard to hear / volume jumps between programmes and ads / weak bass / sound lags the picture / none.
- RU: Что мешает в звуке?

**Q-S4. Night listening**: yes, compress loud peaks at night / no.
- RU: Смотрите ночью тихо? Включить сжатие громких мест?

## Finish

**Q-F1.** Remind, not ask: switch off USB and network debugging, revoke authorisations, reboot. Then confirm with `adb-closed.py`.
- RU: Выключите «Отладку по USB» и «Сетевую отладку», отзовите авторизации, перезагрузите ТВ и напишите мне.
