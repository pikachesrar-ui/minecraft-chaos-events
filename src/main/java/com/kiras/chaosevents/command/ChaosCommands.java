package com.kiras.chaosevents.command;

import com.kiras.chaosevents.core.ChaosSessionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ChaosCommands {

    private static final String PREFIX = "[Chaos Events] ";

    private ChaosCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("chaos")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("start")
                                .executes(context -> start(context.getSource())))
                        .then(Commands.literal("pause")
                                .executes(context -> pause(context.getSource())))
                        .then(Commands.literal("resume")
                                .executes(context -> resume(context.getSource())))
                        .then(Commands.literal("stop")
                                .executes(context -> stop(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource())))
        );
    }

    private static int start(CommandSourceStack source) {
        if (!ChaosSessionManager.start()) {
            source.sendFailure(Component.literal(
                    PREFIX + "система уже запущена или находится на паузе."
            ));
            return 0;
        }

        broadcast(source.getServer(),
                "Система запущена. Первый большой ивент начнётся через 5–10 минут.");
        return 1;
    }

    private static int pause(CommandSourceStack source) {
        if (!ChaosSessionManager.pause()) {
            source.sendFailure(Component.literal(
                    PREFIX + "поставить систему на паузу сейчас нельзя."
            ));
            return 0;
        }

        broadcast(source.getServer(), "Все таймеры приостановлены.");
        return 1;
    }

    private static int resume(CommandSourceStack source) {
        if (!ChaosSessionManager.resume()) {
            source.sendFailure(Component.literal(
                    PREFIX + "система сейчас не находится на паузе."
            ));
            return 0;
        }

        broadcast(source.getServer(), "Работа системы продолжена.");
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        if (!ChaosSessionManager.stop()) {
            source.sendFailure(Component.literal(
                    PREFIX + "система уже остановлена."
            ));
            return 0;
        }

        broadcast(source.getServer(), "Система полностью остановлена.");
        return 1;
    }

    private static int status(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal(
                        PREFIX + "текущее состояние: " + ChaosSessionManager.getStateName()
                ),
                false
        );
        return 1;
    }

    private static void broadcast(MinecraftServer server, String text) {
        Component message = Component.literal(PREFIX + text);
        server.getPlayerList().getPlayers()
                .forEach(player -> player.sendSystemMessage(message));
    }
}
