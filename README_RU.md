<p align="center">
  <img src="docs/assets/banner.svg" alt="Баннер Chaos Events" width="100%">
</p>

<p align="center">
  Управляемый сервером мод для NeoForge, превращающий совместную игру в Minecraft в непредсказуемое шоу событий.
</p>

<p align="center">
  <a href="https://github.com/pikachesrar-ui/minecraft-chaos-events/actions/workflows/build.yml"><img alt="Сборка" src="https://img.shields.io/github/actions/workflow/status/pikachesrar-ui/minecraft-chaos-events/build.yml?branch=main&style=flat-square&label=build"></a>
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-62b47a?style=flat-square">
  <img alt="NeoForge 21.1.244+" src="https://img.shields.io/badge/NeoForge-21.1.244%2B-f47b20?style=flat-square">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-e76f00?style=flat-square">
  <a href="LICENSE"><img alt="Лицензия MIT" src="https://img.shields.io/badge/license-MIT-8b5cf6?style=flat-square"></a>
</p>

<p align="center">
  <a href="README.md">English</a> · <strong>Русский</strong>
</p>

## Что такое Chaos Events?

Во время активной сессии Chaos Events запускает несколько независимых систем. Большие мировые события, персональные нелетальные подлянки, викторины и пространственные обмены накладываются друг на друга и создают испытание для друзей, небольших серверов и стримов.

Мод не начинает работу сам. Оператор запускает сессию командой `/chaos start`, может заморозить все таймеры через `/chaos pause` и безопасно остановить активные механики командой `/chaos stop`.

> [!WARNING]
> Это ранняя публичная версия. Некоторые события намеренно меняют погоду, правила мира, позиции игроков, мобов и блоки. Перед игрой сделайте резервную копию важного мира.

## Возможности

- **Большие события** с таймером на полосе босса, случайными перерывами и защитой от быстрых повторов.
- **Микроподлянки** для отдельных игроков каждые 1–3 минуты: мешают играть, но не рассчитаны на намеренное убийство.
- **Викторина** каждые 6–12 минут: 15 секунд на ответ, награды и лёгкие наказания.
- **Пространственные обмены** между игроками и измерениями, включая механику совместной активации якорей для возвращения.
- **Ускорение мира** до 200 TPS с сохранением близкой к обычной скорости игроков.
- **Дополнительные катастрофы** из [Weather2](https://www.curseforge.com/minecraft/mc-mods/weather-storms-tornadoes) и [Oh My, Meteors!](https://www.curseforge.com/minecraft/mc-mods/oh-my-meteors), если эти моды установлены.
- Английская и русская локализация предметов; серверные сообщения пока преимущественно на русском языке.

## Требования

| Компонент | Версия |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.244` или более новая совместимая сборка для `1.21.1` |
| Java | `21` |
| Установка | На сервер и клиент |

Weather2 и Oh My, Meteors! необязательны. Мод автоматически проверяет их наличие во время запуска событий.

## Установка

1. Установите Minecraft `1.21.1` и совместимую версию NeoForge.
2. Скачайте JAR со страницы [Releases](https://github.com/pikachesrar-ui/minecraft-chaos-events/releases). До появления первого релиза соберите мод из исходного кода по инструкции ниже.
3. Поместите JAR в папку `mods` на сервере и у всех подключающихся игроков.
4. Запустите мир и выполните `/chaos start` с правами оператора.

## Команды

Для всех команд требуется второй уровень прав.

| Команда | Назначение |
| --- | --- |
| `/chaos start` | Запустить новую хаос-сессию |
| `/chaos pause` | Заморозить механики и все таймеры |
| `/chaos resume` | Продолжить сессию после паузы |
| `/chaos stop` | Остановить сессию и убрать временные эффекты |
| `/chaos status` | Показать состояние систем, таймеры и количество контента |
| `/chaos skip` | Завершить текущее большое событие и запустить перерыв |
| `/chaos test big` | Принудительно запустить случайное большое событие |
| `/chaos test speed` | Запустить ускорение мира |
| `/chaos test prank` | Применить случайную микроподлянку |
| `/chaos test screamer` | Показать скример выбранному игроку |
| `/chaos test trivia` | Запустить вопрос викторины |
| `/chaos test swap` | Запустить обмен позициями; нужно не менее двух игроков |

## Сборка из исходного кода

Установите JDK 21, клонируйте репозиторий и выполните:

```bash
./gradlew build
```

В Windows PowerShell:

```powershell
.\gradlew.bat build
```

Готовый JAR появится в `build/libs/`. Для разработки доступны команды `./gradlew runClient` и `./gradlew runServer`.

GitHub Actions проверяет сборку каждого коммита и pull request. Теги вида `v*` автоматически собирают проект и публикуют JAR в GitHub Releases.

## Участие в разработке

Сообщения об ошибках, идеи событий и pull request приветствуются. Перед изменениями прочитайте [CONTRIBUTING.md](CONTRIBUTING.md) и используйте шаблоны Issues.

## Лицензия

Chaos Events распространяется по [лицензии MIT](LICENSE). Это независимый проект сообщества, не связанный с Mojang Studios, Microsoft, NeoForge или авторами дополнительных интеграций. Minecraft является товарным знаком Microsoft Corporation.
