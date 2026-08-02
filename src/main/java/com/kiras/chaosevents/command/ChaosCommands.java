package com.kiras.chaosevents.command;

import com.kiras.chaosevents.core.ChaosSessionManager;
import com.kiras.chaosevents.event.BigEventEngine;
import com.kiras.chaosevents.prank.MicroPrankEngine;
import com.kiras.chaosevents.trivia.TriviaEngine;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ChaosCommands {
    private static final String PREFIX = "[Chaos Events] ";
    private ChaosCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("chaos")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("start").executes(context -> start(context.getSource())))
                        .then(Commands.literal("pause").executes(context -> pause(context.getSource())))
                        .then(Commands.literal("resume").executes(context -> resume(context.getSource())))
                        .then(Commands.literal("stop").executes(context -> stop(context.getSource())))
                        .then(Commands.literal("status").executes(context -> status(context.getSource())))
                        .then(Commands.literal("skip")
                                .executes(context -> skipBig(context.getSource()))
                                .then(Commands.literal("big").executes(context -> skipBig(context.getSource()))))
                        .then(Commands.literal("test")
                                .then(Commands.literal("big").executes(context -> testBig(context.getSource())))
                                .then(Commands.literal("speed").executes(context -> testSpeed(context.getSource())))
                                .then(Commands.literal("prank").executes(context -> testPrank(context.getSource())))
                                .then(Commands.literal("screamer").executes(context -> testScreamer(context.getSource())))
                                .then(Commands.literal("trivia").executes(context -> testTrivia(context.getSource())))
                                .then(Commands.literal("swap").executes(context -> testSwap(context.getSource()))))
        );
    }

    private static int start(CommandSourceStack source) {
        if (!ChaosSessionManager.start(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "система уже запущена или находится на паузе."));
            return 0;
        }
        broadcast(source.getServer(), "Система запущена: большие ивенты, микроподлянки и викторина работают независимо.");
        return 1;
    }

    private static int pause(CommandSourceStack source) {
        if (!ChaosSessionManager.pause(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "поставить систему на паузу сейчас нельзя."));
            return 0;
        }
        broadcast(source.getServer(), "Все таймеры и активные механики заморожены.");
        return 1;
    }

    private static int resume(CommandSourceStack source) {
        if (!ChaosSessionManager.resume(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "система сейчас не находится на паузе."));
            return 0;
        }
        broadcast(source.getServer(), "Работа системы продолжена.");
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        if (!ChaosSessionManager.stop(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "система уже остановлена."));
            return 0;
        }
        broadcast(source.getServer(), "Система полностью остановлена, временные эффекты очищаются.");
        return 1;
    }

    private static int status(CommandSourceStack source) {
        String text = PREFIX
                + "состояние: " + ChaosSessionManager.getStateName()
                + "; " + ChaosSessionManager.getBigEventStatus()
                + "; " + ChaosSessionManager.getMicroPrankStatus()
                + "; " + ChaosSessionManager.getTriviaStatus()
                + "; " + ChaosSessionManager.getSpatialStatus()
                + "; больших ивентов: " + BigEventEngine.getRegisteredEventCount()
                + "; микроподлянок: " + MicroPrankEngine.getRegisteredPrankCount()
                + "; вопросов: " + TriviaEngine.getQuestionCount();
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int skipBig(CommandSourceStack source) {
        if (!ChaosSessionManager.skipBigEvent(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "сейчас нет активного большого ивента для пропуска."));
            return 0;
        }
        broadcast(source.getServer(), "Текущий большой ивент пропущен. Начался обычный перерыв перед следующим.");
        return 1;
    }

    private static int testBig(CommandSourceStack source) {
        if (!ChaosSessionManager.forceBigEvent(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "сначала запусти систему командой /chaos start."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(PREFIX + "случайный большой ивент запущен принудительно."), false);
        return 1;
    }

    private static int testSpeed(CommandSourceStack source) {
        if (!ChaosSessionManager.forceAcceleratedTimeEvent(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "сначала запусти систему командой /chaos start и зайди в мир."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                PREFIX + "ивент ускорения мира запущен: мир работает на 200 TPS, игроки сохраняют обычную скорость."
        ), false);
        return 1;
    }

    private static int testPrank(CommandSourceStack source) {
        if (!ChaosSessionManager.forceMicroPrank(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "система не запущена или на сервере нет игроков."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(PREFIX + "случайная микроподлянка применена к одному игроку."), false);
        return 1;
    }

    private static int testScreamer(CommandSourceStack source) {
        ServerPlayer preferredTarget = source.getEntity() instanceof ServerPlayer player ? player : null;
        if (!MicroPrankEngine.forceScreamer(source.getServer(), preferredTarget)) {
            source.sendFailure(Component.literal(PREFIX + "на сервере нет игроков для проверки скримера."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(PREFIX + "случайный скример показан выбранному игроку."), false);
        return 1;
    }

    private static int testTrivia(CommandSourceStack source) {
        if (!ChaosSessionManager.forceTrivia(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "сначала запусти систему командой /chaos start."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(PREFIX + "вопрос викторины запущен принудительно."), false);
        return 1;
    }

    private static int testSwap(CommandSourceStack source) {
        if (!ChaosSessionManager.forceSpatialEvent(source.getServer())) {
            source.sendFailure(Component.literal(PREFIX + "для пространственного сдвига система должна работать и нужны минимум два игрока."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(PREFIX + "пространственный сдвиг запущен принудительно."), false);
        return 1;
    }

    private static void broadcast(MinecraftServer server, String text) {
        Component message = Component.literal(PREFIX + text);
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
    }
}
