# Пользовательские скримеры

Chaos Events поддерживает до 10 пар «картинка + звук». Собственные скримеры можно хранить в
локальном каталоге `src/private/resources`, который участвует в сборке, но полностью игнорируется
Git. Поэтому `git pull` не затронет эти файлы, а они случайно не попадут в публичную версию.

Для каждого номера должны существовать оба файла:

```text
src/private/resources/assets/chaosevents/textures/gui/screamers/1.png
src/private/resources/assets/chaosevents/sounds/screamers/1.ogg
...
src/private/resources/assets/chaosevents/textures/gui/screamers/10.png
src/private/resources/assets/chaosevents/sounds/screamers/10.ogg
```

Номера могут идти с пропусками. Клиент случайно выбирает только те номера, у которых найдены оба
файла, и по возможности не повторяет предыдущую пару. Если ни одной полной пары нет, используется
встроенная нарисованная заглушка и рёв Хранителя.

Требования к ресурсам:

- изображение — PNG; оно растягивается на весь экран;
- звук — OGG Vorbis;
- регистр имени и путь должны совпадать точно;
- файлы из `src/private/resources` попадут только в локально собранный JAR и не попадут в GitHub Actions.

Для отдельного имени приватной версии выполните в Windows PowerShell:

```powershell
.\gradlew.bat clean build -Pmod_version=0.3.4-private.1
```

Готовый файл будет называться `build/libs/chaosevents-0.3.4-private.1.jar`. Перед обновлением
исходников используйте `git pull --ff-only`: приватные ресурсы останутся на месте.

Проверьте содержимое готового JAR в PowerShell:

```powershell
tar -tf build/libs/chaosevents-0.3.4-private.1.jar | Select-String 'screamers/[0-9]+\.(png|ogg)'
```
