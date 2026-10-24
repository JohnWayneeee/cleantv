<p align="center">
  <img src="assets/banner.png" alt="CleanTV" width="100%">
</p>

<h1 align="center">CleanTV</h1>

<p align="center"><a href="README.md">English</a> · <b>Русский</b></p>

<p align="center">
  <b>Ваш Android TV снова быстрый: хватит одной фразы в Claude Code.</b><br>
  Скилл чистит ТВ, калибрует картинку и звук и меняет рекламный главный экран на ваши приложения.
</p>

<p align="center">
  <a href="https://github.com/JohnWayneeee/cleantv/stargazers"><img src="https://img.shields.io/github/stars/JohnWayneeee/cleantv?style=flat&color=yellow" alt="Stars"></a>
  <img src="https://img.shields.io/badge/Android%20TV-Google%20TV-3DDC84?logo=androidtv&logoColor=white" alt="Android TV">
  <img src="https://img.shields.io/badge/Claude%20Code-skill-D97757" alt="Claude Code skill">
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT">
</p>

<p align="center">
  ⭐ <b>Помог? Поставьте звезду.</b> Так проект найдут другие владельцы тормозящих телевизоров.
</p>

---

## Было → стало

Из коробки ваш ТВ показывает рекламу вместо главного экрана, держит в фоне десяток сервисов, сглаживает
и «подкрашивает» фильмы. На нашем CHiQ заводской эквалайзер задирал 5 кГц на +41. Всё это делит с вашими приложениями 1–3 ГБ памяти.

Замеры на двух ТВ:

| | Из коробки | После CleanTV |
|---|---|---|
| Свободная память (CHiQ, 2.8 ГБ) | 1.42 ГБ | **1.77 ГБ** |
| Свободная память (Harper, 1.8 ГБ) | 721 МБ | **888 МБ** |
| Главный экран | реклама и рекомендации | **ваши приложения на чёрном фоне** |
| Картинка | заводской пресет и «улучшайзеры» | **калибровка под комнату** |
| YouTube в России | тормозит | **работает, обход только для YouTube** |

## Что он делает

- **Чистка.** Отключает телеметрию, рекламу и мусор производителя. Сначала спрашивает, чем вы пользуетесь. Ничего не удаляет: всё возвращается одной командой.
- **Скорость.** Замеряет память до и после. Ускоряет анимации, ограничивает фон редких приложений.
- **Лаунчер в стиле Apple TV.** Чёрный фон, док с вашими приложениями, стеклянная подсветка, часы, меню HDMI-входов. При включении сразу показывает плитки-заглушки, без чёрного экрана.
- **Калибровка картинки.** Режим, цветовая температура, гамма, подсветка под освещение комнаты. Отключает плавность, шумодав и прочие «улучшайзеры».
- **Звук.** Dolby, усиление речи, выравнивание громкости, ночной режим. Под динамики ТВ, саундбар, оптику или Bluetooth.
- **YouTube без замедления.** Ставит [ByeByeDPI](https://github.com/romanvht/ByeByeDPI) прямо на ТВ, без внешних серверов. Через обход идёт только YouTube. Лаунчер включает его при запуске YouTube и выключает на главном экране.
- **Проверка на ТВ.** Скилл подтверждает каждый шаг скриншотом с ТВ или командой. В конце закрывает отладку по сети и проверяет, что порт закрыт.

Работает на Android TV и Google TV: Xiaomi, TCL, Hisense, Sony, Philips, CHiQ/Changhong, Haier, Harper, Nvidia Shield и другие.
Не работает на Samsung Tizen, LG webOS и Fire TV.

## Запуск за 3 шага

**1. Установите скилл.** Проще всего: скопируйте промт ниже и отправьте его в Claude Code. Агент всё поставит сам.

```text
Установи скилл CleanTV для Claude Code из https://github.com/JohnWayneeee/cleantv
1. Склонируй репозиторий во временную папку: git clone --depth 1 https://github.com/JohnWayneeee/cleantv
2. Скопируй папку skills/android-tv-setup в ~/.claude/skills/android-tv-setup
   (на Windows: %USERPROFILE%\.claude\skills\android-tv-setup). Если папка уже есть, спроси меня, заменить ли её.
3. Проверь, что на компьютере есть adb (Android SDK platform-tools) и Python 3. Для лаунчера ещё нужны
   JDK 17+ и Android SDK platform 35. Если чего-то нет, скажи, как поставить это на моей ОС,
   и ставь только после моего согласия.
4. Удали временную папку.
5. Покажи, что файл ~/.claude/skills/android-tv-setup/SKILL.md на месте, и напомни мне
   перезапустить Claude Code и написать «Настрой мой телевизор».
```

Или через плагины Claude Code:

```text
/plugin marketplace add JohnWayneeee/cleantv
/plugin install android-tv-setup@android-tv-setup
```

**2. Включите отладку на ТВ.** Настройки → Настройки устройства → Об устройстве → 7 раз нажмите OK на «Сборка».
Назад → Для разработчиков → включите «Отладку по USB» и «Сетевую отладку» (если есть).

**3. Скажите Claude:** «Настрой мой телевизор».

Дальше скилл сам найдёт ТВ в сети и задаст 10–15 вопросов по ходу работы: какие сервисы оставить, какой свет в комнате, что мешает в звуке.
Все вопросы: [`references/questions.md`](skills/android-tv-setup/references/questions.md).

<details>
<summary>Что нужно на компьютере</summary>

- Android SDK platform-tools (`adb`);
- Python 3;
- для лаунчера: Android SDK platform 35 и JDK 17+ (подойдёт JDK из Android Studio).

ТВ и компьютер должны быть в одной сети. Если на компьютере VPN, исключите домашнюю подсеть из туннеля.
</details>

## Безопасность

- Скилл отключает системные пакеты (`pm disable-user`) и не удаляет их.
- Пакеты, без которых ТВ не покажет картинку, не переключит вход или не найдёт пульт, скилл не трогает.
- Перед первым изменением скилл делает бэкап. Данные каждого ТВ лежат в `tvs/<модель>/`, вне git.
- Откат всего одной командой:

```sh
cd tvs/<модель>
python ../../skills/android-tv-setup/scripts/restore.py
```

## Дальше

- Модуль пульта: кнопки приложений, шестерёнка, переназначение, Bluetooth-пульты.
- Таблицы настроек картинки и звука для популярных плат: [`references/devices/`](skills/android-tv-setup/references/devices/). Настроили свой ТВ? Пришлите PR с заметками по плате.

---

<p align="center">
  ⭐ <b>CleanTV ускорил ваш ТВ? Поставьте звезду.</b>
</p>

<p align="center">
  <a href="https://www.buymeacoffee.com/JohnWayne"><img src="https://img.shields.io/badge/Buy%20me%20a%20coffee-FFDD00?logo=buymeacoffee&logoColor=black" alt="Buy me a coffee"></a>
  <br><br>
  <img src="assets/bmc-qr.png" alt="QR-код для кофе" width="160">
</p>

## Лицензия

MIT. Шрифт Onest: SIL Open Font License 1.1.
