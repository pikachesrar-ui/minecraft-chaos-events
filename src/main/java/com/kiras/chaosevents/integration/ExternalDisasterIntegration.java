package com.kiras.chaosevents.integration;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Optional runtime bridge for Weather2 and Oh My, Meteors.
 *
 * The external classes are accessed reflectively so Chaos Events still compiles
 * and loads when either third-party mod is absent. Their events are excluded
 * from random selection unless the required mod and an Overworld target exist.
 */
public final class ExternalDisasterIntegration {
    private static final String WEATHER2_MOD_ID = "weather2";
    private static final String METEORS_MOD_ID = "ohmymeteors";

    private static Object activeTornadoManager;
    private static Object activeTornado;

    private ExternalDisasterIntegration() {
    }

    public static boolean canStartTornado(MinecraftServer server) {
        return ModList.get().isLoaded(WEATHER2_MOD_ID) && !overworldPlayers(server).isEmpty();
    }

    public static boolean canStartMeteorShower(MinecraftServer server) {
        if (!ModList.get().isLoaded(METEORS_MOD_ID)) {
            return false;
        }

        try {
            Class<?> meteorUtils = Class.forName("me.emafire003.dev.ohmymeteors.util.MeteorUtils");
            Method canSpawn = meteorUtils.getMethod("canMeteorSpawn", ServerPlayer.class);
            for (ServerPlayer player : overworldPlayers(server)) {
                if (Boolean.TRUE.equals(canSpawn.invoke(null, player))) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ChaosEvents.LOGGER.error("Chaos Events could not inspect Oh My, Meteors", exception);
        }
        return false;
    }

    public static synchronized boolean startTornado(MinecraftServer server) {
        if (!canStartTornado(server)) {
            return false;
        }

        stopTornado();
        ServerPlayer target = randomPlayer(overworldPlayers(server));
        if (target == null) {
            return false;
        }

        try {
            Class<?> serverTickHandler = Class.forName("weather2.ServerTickHandler");
            Class<?> weatherManagerClass = Class.forName("weather2.weathersystem.WeatherManager");
            Class<?> managerServerClass = Class.forName("weather2.weathersystem.WeatherManagerServer");
            Class<?> weatherObjectClass = Class.forName("weather2.weathersystem.storm.WeatherObject");
            Class<?> stormObjectClass = Class.forName("weather2.weathersystem.storm.StormObject");

            Method getManager = serverTickHandler.getMethod("getWeatherManagerFor", ResourceKey.class);
            Object manager = getManager.invoke(null, target.serverLevel().dimension());
            if (manager == null) {
                return false;
            }

            Constructor<?> constructor = stormObjectClass.getConstructor(weatherManagerClass);
            Object storm = constructor.newInstance(manager);
            stormObjectClass.getMethod("setupStorm", Entity.class).invoke(storm, target);

            int f1Stage = stormObjectClass.getField("STATE_STAGE1").getInt(null);
            stormObjectClass.getField("levelCurIntensityStage").setInt(storm, f1Stage);
            stormObjectClass.getField("levelStormIntensityMax").setInt(storm, f1Stage);
            stormObjectClass.getField("alwaysProgresses").setBoolean(storm, false);

            int layer = stormObjectClass.getField("layer").getInt(storm);
            List<?> layers = (List<?>) stormObjectClass.getField("layers").get(null);
            double stormY = ((Number) layers.get(layer)).doubleValue();

            ThreadLocalRandom random = ThreadLocalRandom.current();
            double angle = random.nextDouble(Math.PI * 2.0);
            double distance = random.nextDouble(48.0, 73.0);
            Vec3 spawnPosition = new Vec3(
                    target.getX() + Math.cos(angle) * distance,
                    stormY,
                    target.getZ() + Math.sin(angle) * distance
            );

            stormObjectClass.getMethod("initPositions", Vec3.class).invoke(storm, spawnPosition);
            stormObjectClass.getMethod("aimStormAtClosestOrProvidedPlayer", Player.class).invoke(storm, target);
            managerServerClass.getMethod("addStormObject", weatherObjectClass).invoke(manager, storm);
            managerServerClass.getMethod("syncStormNew", weatherObjectClass).invoke(manager, storm);

            activeTornadoManager = manager;
            activeTornado = storm;
            ChaosEvents.LOGGER.info("Chaos Events spawned a Weather2 F1 tornado near {}",
                    target.getGameProfile().getName());
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            activeTornadoManager = null;
            activeTornado = null;
            ChaosEvents.LOGGER.error("Chaos Events could not spawn the Weather2 tornado", exception);
            return false;
        }
    }

    public static synchronized void stopTornado() {
        Object manager = activeTornadoManager;
        Object tornado = activeTornado;
        activeTornadoManager = null;
        activeTornado = null;

        if (manager == null || tornado == null) {
            return;
        }

        try {
            Class<?> weatherObjectClass = Class.forName("weather2.weathersystem.storm.WeatherObject");
            manager.getClass()
                    .getMethod("removeWeatherObjectAndSync", weatherObjectClass)
                    .invoke(manager, tornado);
            ChaosEvents.LOGGER.info("Chaos Events removed its Weather2 tornado");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ChaosEvents.LOGGER.warn("Chaos Events could not remove its Weather2 tornado cleanly", exception);
        }
    }

    public static boolean startMeteorShower(MinecraftServer server) {
        if (!ModList.get().isLoaded(METEORS_MOD_ID)) {
            return false;
        }

        try {
            Class<?> meteorUtils = Class.forName("me.emafire003.dev.ohmymeteors.util.MeteorUtils");
            Method canSpawn = meteorUtils.getMethod("canMeteorSpawn", ServerPlayer.class);
            List<ServerPlayer> candidates = new ArrayList<>();
            for (ServerPlayer player : overworldPlayers(server)) {
                if (Boolean.TRUE.equals(canSpawn.invoke(null, player))) {
                    candidates.add(player);
                }
            }

            ServerPlayer target = randomPlayer(candidates);
            if (target == null) {
                return false;
            }

            Method startShower = meteorUtils.getMethod(
                    "spawnMeteorShowerDelayedDirection",
                    ServerLevel.class,
                    Player.class
            );
            startShower.invoke(null, target.serverLevel(), target);
            ChaosEvents.LOGGER.info("Chaos Events started an Oh My, Meteors shower near {}",
                    target.getGameProfile().getName());
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ChaosEvents.LOGGER.error("Chaos Events could not start the Oh My, Meteors shower", exception);
            return false;
        }
    }

    private static List<ServerPlayer> overworldPlayers(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level().dimension().equals(Level.OVERWORLD))
                .toList();
    }

    private static ServerPlayer randomPlayer(List<ServerPlayer> players) {
        if (players.isEmpty()) {
            return null;
        }
        return players.get(ThreadLocalRandom.current().nextInt(players.size()));
    }
}
