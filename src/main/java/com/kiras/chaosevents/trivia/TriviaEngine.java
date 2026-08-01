package com.kiras.chaosevents.trivia;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Independent 6-12 minute trivia cycle with a 20 second answer window. */
public final class TriviaEngine {
    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_DELAY_SECONDS = 6 * 60;
    private static final int MAX_DELAY_SECONDS = 12 * 60;
    private static final int ANSWER_WINDOW_TICKS = 20 * TICKS_PER_SECOND;
    private static final String PREFIX = "[Викторина] ";
    private static final List<TriviaQuestion> QUESTIONS = TriviaQuestionBank.QUESTIONS;

    private static boolean active;
    private static TriviaQuestion currentQuestion;
    private static int ticksRemaining;
    private static int lastQuestionIndex = -1;

    private TriviaEngine() {
    }

    public static synchronized void startSession() {
        active = true;
        currentQuestion = null;
        scheduleNextQuestion();
    }

    public static void tick(MinecraftServer server) {
        TriviaQuestion timedOut = null;
        boolean startQuestion = false;

        synchronized (TriviaEngine.class) {
            if (!active) {
                return;
            }

            if (ticksRemaining > 0) {
                ticksRemaining--;
            }

            if (ticksRemaining > 0) {
                return;
            }

            if (currentQuestion == null) {
                startQuestion = true;
            } else {
                timedOut = currentQuestion;
                currentQuestion = null;
                scheduleNextQuestion();
            }
        }

        if (startQuestion) {
            beginRandomQuestion(server);
        } else if (timedOut != null) {
            broadcast(server, "Время вышло. Правильный ответ: " + timedOut.primaryAnswer());
            applyHiddenGroupPunishment(server);
        }
    }

    public static synchronized void stopSession() {
        active = false;
        currentQuestion = null;
        ticksRemaining = 0;
    }

    public static synchronized void reset() {
        active = false;
        currentQuestion = null;
        ticksRemaining = 0;
        lastQuestionIndex = -1;
    }

    public static boolean forceQuestion(MinecraftServer server) {
        synchronized (TriviaEngine.class) {
            if (!active) {
                return false;
            }
            currentQuestion = null;
        }
        beginRandomQuestion(server);
        return true;
    }

    /** Returns true only for the first correct chat message, so it can be hidden from normal chat. */
    public static boolean handleChatAnswer(MinecraftServer server, ServerPlayer player, String text) {
        TriviaQuestion answered;

        synchronized (TriviaEngine.class) {
            if (!active || currentQuestion == null || !currentQuestion.matches(text)) {
                return false;
            }
            answered = currentQuestion;
            currentQuestion = null;
            scheduleNextQuestion();
        }

        reward(player);
        broadcast(server, player.getGameProfile().getName() + " первым ответил правильно: " + answered.primaryAnswer());
        return true;
    }

    private static void beginRandomQuestion(MinecraftServer server) {
        TriviaQuestion selected;

        synchronized (TriviaEngine.class) {
            if (!active || QUESTIONS.isEmpty()) {
                return;
            }

            int index;
            do {
                index = ThreadLocalRandom.current().nextInt(QUESTIONS.size());
            } while (QUESTIONS.size() > 1 && index == lastQuestionIndex);

            lastQuestionIndex = index;
            selected = QUESTIONS.get(index);
            currentQuestion = selected;
            ticksRemaining = ANSWER_WINDOW_TICKS;
        }

        broadcast(server, "Категория: " + selected.category());
        broadcast(server, selected.prompt());
        broadcast(server, "Первый правильный ответ в чате за 20 секунд получит награду.");
        server.getPlayerList().getPlayers().forEach(player ->
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.PLAYERS, 1.0F, 1.15F));
    }

    private static void reward(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ItemStack reward;
        int roll = random.nextInt(100);

        if (roll < 2) {
            reward = new ItemStack(Items.DIAMOND, 1);
        } else if (roll < 15) {
            reward = new ItemStack(Items.EMERALD, random.nextInt(1, 3));
        } else if (roll < 35) {
            reward = new ItemStack(Items.GOLD_INGOT, random.nextInt(2, 6));
        } else if (roll < 60) {
            reward = new ItemStack(Items.IRON_INGOT, random.nextInt(3, 8));
        } else if (roll < 82) {
            reward = new ItemStack(Items.GOLDEN_CARROT, random.nextInt(2, 6));
        } else {
            reward = new ItemStack(Items.EXPERIENCE_BOTTLE, random.nextInt(3, 8));
        }

        if (!player.getInventory().add(reward.copy())) {
            player.drop(reward.copy(), false);
        }
        player.giveExperiencePoints(random.nextInt(3, 10));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.9F, 1.25F);
    }

    private static void applyHiddenGroupPunishment(MinecraftServer server) {
        int punishment = ThreadLocalRandom.current().nextInt(6);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            switch (punishment) {
                case 0 -> {
                    effect(player, MobEffects.DARKNESS, 12 * TICKS_PER_SECOND, 0);
                    effect(player, MobEffects.CONFUSION, 10 * TICKS_PER_SECOND, 0);
                }
                case 1 -> {
                    effect(player, MobEffects.MOVEMENT_SLOWDOWN, 18 * TICKS_PER_SECOND, 2);
                    effect(player, MobEffects.DIG_SLOWDOWN, 18 * TICKS_PER_SECOND, 1);
                }
                case 2 -> {
                    player.getFoodData().addExhaustion(12.0F);
                    effect(player, MobEffects.HUNGER, 15 * TICKS_PER_SECOND, 2);
                }
                case 3 -> {
                    effect(player, MobEffects.LEVITATION, 35, 2);
                    effect(player, MobEffects.SLOW_FALLING, 8 * TICKS_PER_SECOND, 0);
                }
                case 4 -> {
                    player.igniteForSeconds(4.0F);
                    effect(player, MobEffects.WEAKNESS, 15 * TICKS_PER_SECOND, 1);
                }
                default -> {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    Vec3 push = new Vec3(random.nextDouble(-1.4, 1.4), 0.75, random.nextDouble(-1.4, 1.4));
                    player.setDeltaMovement(player.getDeltaMovement().add(push));
                    effect(player, MobEffects.BLINDNESS, 8 * TICKS_PER_SECOND, 0);
                }
            }
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static void scheduleNextQuestion() {
        ticksRemaining = ThreadLocalRandom.current().nextInt(MIN_DELAY_SECONDS, MAX_DELAY_SECONDS + 1)
                * TICKS_PER_SECOND;
    }

    private static void broadcast(MinecraftServer server, String text) {
        Component message = Component.literal(PREFIX + text);
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
    }

    public static synchronized String getStatusText() {
        if (!active) {
            return "викторина остановлена";
        }
        int seconds = Math.max(0, (ticksRemaining + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
        if (currentQuestion != null) {
            return "идёт вопрос, осталось: " + format(seconds);
        }
        return "следующий вопрос примерно через: " + format(seconds);
    }

    public static int getQuestionCount() {
        return QUESTIONS.size();
    }

    private static String format(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
