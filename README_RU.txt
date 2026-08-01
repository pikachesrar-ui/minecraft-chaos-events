Chaos Events — патч команд управления

1. Закрой запущенный Minecraft.
2. Распакуй содержимое этого архива в корень проекта D:\KiraS\MinecraftChaosEvents.
3. Согласись на замену ChaosEvents.java.
4. В терминале IntelliJ выполни:
   .\gradlew.bat build
5. Затем запусти:
   .\gradlew.bat runClient
6. В мире с разрешёнными командами проверь:
   /chaos status
   /chaos start
   /chaos pause
   /chaos resume
   /chaos stop

Этот архив содержит текущий этап проекта: команды и состояние сессии. Таймеры и сами события будут добавлены в следующих версиях.
