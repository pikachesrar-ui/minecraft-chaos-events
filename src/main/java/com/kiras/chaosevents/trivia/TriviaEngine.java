package com.kiras.chaosevents.trivia;

import com.kiras.chaosevents.config.ChaosConfigCategory;
import com.kiras.chaosevents.config.ChaosConfigEntry;
import com.kiras.chaosevents.config.ChaosConfigManager;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Independent 6-12 minute trivia cycle with a 15 second answer window. */
public final class TriviaEngine {
    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_DELAY_SECONDS = 6 * 60;
    private static final int MAX_DELAY_SECONDS = 12 * 60;
    private static final int ANSWER_WINDOW_TICKS = 15 * TICKS_PER_SECOND;
    private static final String PREFIX = "[Викторина] ";
    private static final List<TriviaQuestion> QUESTIONS = TriviaQuestionBank.QUESTIONS;
    private static final Set<UUID> WRONG_ANSWER_PUNISHED = new HashSet<>();

    private static boolean active;
    private static TriviaQuestion currentQuestion;
    private static int ticksRemaining;
    private static String lastQuestionId;

    private TriviaEngine() {
    }

    public static synchronized void startSession() {
        active = true;
        currentQuestion = null;
        WRONG_ANSWER_PUNISHED.clear();
        lastQuestionId = null;
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
                WRONG_ANSWER_PUNISHED.clear();
                scheduleNextQuestion();
            }
        }

        if (startQuestion) {
            if (!beginRandomQuestion(server)) {
                synchronized (TriviaEngine.class) {
                    scheduleNextQuestion();
                }
            }
        } else if (timedOut != null) {
            broadcast(server, "Время вышло. Правильный ответ: " + timedOut.primaryAnswer());
            applyHiddenGroupPunishment(server);
        }
    }

    public static synchronized void stopSession() {
        active = false;
        currentQuestion = null;
        ticksRemaining = 0;
        WRONG_ANSWER_PUNISHED.clear();
    }

    public static synchronized void reset() {
        active = false;
        currentQuestion = null;
        ticksRemaining = 0;
        lastQuestionId = null;
        WRONG_ANSWER_PUNISHED.clear();
    }

    public static boolean forceQuestion(MinecraftServer server) {
        synchronized (TriviaEngine.class) {
            if (!active) {
                return false;
            }
            currentQuestion = null;
            WRONG_ANSWER_PUNISHED.clear();
        }
        return beginRandomQuestion(server);
    }

    /** Every chat message during a question is treated as an answer and hidden from normal chat. */
    public static boolean handleChatAnswer(MinecraftServer server, ServerPlayer player, String text) {
        TriviaQuestion answered = null;
        boolean punishWrongAnswer = false;

        synchronized (TriviaEngine.class) {
            if (!active || currentQuestion == null) {
                return false;
            }

            if (currentQuestion.matches(text)) {
                answered = currentQuestion;
                currentQuestion = null;
                WRONG_ANSWER_PUNISHED.clear();
                scheduleNextQuestion();
            } else {
                punishWrongAnswer = WRONG_ANSWER_PUNISHED.add(player.getUUID());
            }
        }

        if (answered == null) {
            if (punishWrongAnswer) {
                applyWrongAnswerPunishment(player);
                player.sendSystemMessage(Component.literal(PREFIX
                        + "Неверный ответ. Наказание применено, но можно попробовать ответить ещё раз."));
            } else {
                player.sendSystemMessage(Component.literal(PREFIX
                        + "Неверно. Повторное наказание за этот вопрос не накладывается."));
            }
            return true;
        }

        RewardResult reward = reward(player);
        Component result = Component.literal(PREFIX
                        + player.getGameProfile().getName()
                        + " первым ответил правильно: " + answered.primaryAnswer() + ". Награда: ")
                .append(reward.item().getHoverName())
                .append(Component.literal(" ×" + reward.item().getCount()
                        + " и " + reward.experience() + " очк. опыта."));
        broadcast(server, result);
        return true;
    }

    private static boolean beginRandomQuestion(MinecraftServer server) {
        TriviaQuestion selected;

        synchronized (TriviaEngine.class) {
            if (!active || QUESTIONS.isEmpty()) {
                return false;
            }

            List<TriviaQuestion> enabled = QUESTIONS.stream()
                    .filter(question -> ChaosConfigManager.isEnabled(
                            ChaosConfigCategory.TRIVIA, configId(question)))
                    .toList();
            if (enabled.isEmpty()) {
                currentQuestion = null;
                return false;
            }

            List<TriviaQuestion> candidates = enabled;
            if (enabled.size() > 1 && lastQuestionId != null) {
                List<TriviaQuestion> withoutLast = enabled.stream()
                        .filter(question -> !configId(question).equals(lastQuestionId))
                        .toList();
                if (!withoutLast.isEmpty()) {
                    candidates = withoutLast;
                }
            }

            selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            lastQuestionId = configId(selected);
            currentQuestion = selected;
            ticksRemaining = ANSWER_WINDOW_TICKS;
            WRONG_ANSWER_PUNISHED.clear();
        }

        broadcast(server, "Категория: " + selected.category());
        broadcast(server, selected.prompt());
        broadcast(server, "Первый правильный ответ в чате за 15 секунд получит награду. Ошибка наказывается.");
        server.getPlayerList().getPlayers().forEach(player ->
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.PLAYERS, 1.0F, 1.15F));
        return true;
    }

    private static RewardResult reward(ServerPlayer player) {
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
        int experience = random.nextInt(3, 10);
        player.giveExperiencePoints(experience);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.9F, 1.25F);
        return new RewardResult(reward.copy(), experience);
    }

    private static void applyWrongAnswerPunishment(ServerPlayer player) {
        int punishment = ThreadLocalRandom.current().nextInt(6);
        switch (punishment) {
            case 0 -> {
                effect(player, MobEffects.WITHER, 8 * TICKS_PER_SECOND, 1);
                effect(player, MobEffects.WEAKNESS, 20 * TICKS_PER_SECOND, 2);
            }
            case 1 -> {
                effect(player, MobEffects.DARKNESS, 20 * TICKS_PER_SECOND, 0);
                effect(player, MobEffects.CONFUSION, 15 * TICKS_PER_SECOND, 1);
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, 15 * TICKS_PER_SECOND, 2);
            }
            case 2 -> {
                player.getFoodData().addExhaustion(18.0F);
                effect(player, MobEffects.HUNGER, 25 * TICKS_PER_SECOND, 3);
            }
            case 3 -> {
                player.igniteForSeconds(6.0F);
                effect(player, MobEffects.WEAKNESS, 20 * TICKS_PER_SECOND, 2);
            }
            case 4 -> {
                effect(player, MobEffects.LEVITATION, 60, 4);
                effect(player, MobEffects.SLOW_FALLING, 10 * TICKS_PER_SECOND, 0);
            }
            default -> {
                player.giveExperiencePoints(-10);
                effect(player, MobEffects.BLINDNESS, 12 * TICKS_PER_SECOND, 0);
                effect(player, MobEffects.CONFUSION, 12 * TICKS_PER_SECOND, 1);
            }
        }
    }

    private static void applyHiddenGroupPunishment(MinecraftServer server) {
        int punishment = ThreadLocalRandom.current().nextInt(6);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            switch (punishment) {
                case 0 -> {
                    effect(player, MobEffects.DARKNESS, 25 * TICKS_PER_SECOND, 0);
                    effect(player, MobEffects.CONFUSION, 20 * TICKS_PER_SECOND, 1);
                    effect(player, MobEffects.MOVEMENT_SLOWDOWN, 15 * TICKS_PER_SECOND, 2);
                }
                case 1 -> {
                    effect(player, MobEffects.MOVEMENT_SLOWDOWN, 30 * TICKS_PER_SECOND, 3);
                    effect(player, MobEffects.DIG_SLOWDOWN, 30 * TICKS_PER_SECOND, 2);
                    effect(player, MobEffects.WEAKNESS, 30 * TICKS_PER_SECOND, 1);
                }
                case 2 -> {
                    player.getFoodData().setSaturation(0.0F);
                    player.getFoodData().addExhaustion(24.0F);
                    effect(player, MobEffects.HUNGER, 30 * TICKS_PER_SECOND, 3);
                }
                case 3 -> {
                    effect(player, MobEffects.LEVITATION, 70, 3);
                    effect(player, MobEffects.SLOW_FALLING, 12 * TICKS_PER_SECOND, 0);
                }
                case 4 -> {
                    player.igniteForSeconds(7.0F);
                    effect(player, MobEffects.WITHER, 8 * TICKS_PER_SECOND, 0);
                    effect(player, MobEffects.WEAKNESS, 25 * TICKS_PER_SECOND, 2);
                }
                default -> {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    Vec3 push = new Vec3(random.nextDouble(-2.2, 2.2), 1.15,
                            random.nextDouble(-2.2, 2.2));
                    player.setDeltaMovement(player.getDeltaMovement().add(push));
                    effect(player, MobEffects.BLINDNESS, 15 * TICKS_PER_SECOND, 0);
                    effect(player, MobEffects.CONFUSION, 15 * TICKS_PER_SECOND, 1);
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
        broadcast(server, Component.literal(PREFIX + text));
    }

    private static void broadcast(MinecraftServer server, Component message) {
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

    public static List<ChaosConfigEntry> getConfigEntries() {
        List<ChaosConfigEntry> result = new ArrayList<>(QUESTIONS.size());
        for (TriviaQuestion question : QUESTIONS) {
            result.add(new ChaosConfigEntry(
                    configId(question),
                    question.prompt(),
                    "Категория: " + question.category()
            ));
        }
        return List.copyOf(result);
    }

    private static String configId(TriviaQuestion question) {
        return question.category() + "::" + question.prompt();
    }

    private static String format(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private record RewardResult(ItemStack item, int experience) {
    }
}
