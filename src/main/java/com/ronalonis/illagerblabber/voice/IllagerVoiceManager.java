package com.ronalonis.illagerblabber.voice;

import com.ronalonis.illagerblabber.IllagerBlabberConfig;
import com.ronalonis.illagerblabber.IllagerSounds;
import com.ronalonis.illagerblabber.voice.IllagerState.Combat;
import com.ronalonis.illagerblabber.voice.IllagerState.Passive;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IllagerVoiceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("assets.illagerblabber");
    private final AbstractIllager illager;
    private final IllagerType illagerType;
    private final Random threadSafeRandom = new Random();
    private final RandomSource random;
    private IllagerState currentState;
    private boolean hadTarget;
    private int soundCooldown;
    private int postHurtCooldown;
    private int postVictoryCooldown;
    private boolean isSpeaking;
    private int speakingTimer;
    private SoundEvent currentlyPlayingSound;
    private IllagerState currentSoundType;
    private boolean isShortHurtSound;
    private static final Supplier<SoundEvent>[] PILLAGER_AMBIENT_NOISE_SOUNDS;
    private static final Supplier<SoundEvent>[] PILLAGER_AMBIENT_TALK_SOUNDS;
    private static final Supplier<SoundEvent>[] PILLAGER_SPOTTED_SOUNDS;
    private static final Supplier<SoundEvent>[] PILLAGER_BATTLE_SOUNDS;
    private static final Supplier<SoundEvent>[] PILLAGER_HURT_SOUNDS;
    private static final Supplier<SoundEvent>[] PILLAGER_VICTORY_SOUNDS;
    private static final Supplier<SoundEvent>[] VINDICATOR_AMBIENT_NOISE_SOUNDS;
    private static final Supplier<SoundEvent>[] VINDICATOR_AMBIENT_TALK_SOUNDS;
    private static final Supplier<SoundEvent>[] VINDICATOR_SPOTTED_SOUNDS;
    private static final Supplier<SoundEvent>[] VINDICATOR_BATTLE_SOUNDS;
    private static final Supplier<SoundEvent>[] VINDICATOR_HURT_SOUNDS;
    private static final Supplier<SoundEvent>[] VINDICATOR_VICTORY_SOUNDS;
    private static final Supplier<SoundEvent>[] EVOKER_AMBIENT_NOISE_SOUNDS;
    private static final Supplier<SoundEvent>[] EVOKER_AMBIENT_TALK_SOUNDS;
    private static final Supplier<SoundEvent>[] EVOKER_BATTLE_SOUNDS;
    private static final Supplier<SoundEvent>[] EVOKER_HURT_SOUNDS;
    private static final Supplier<SoundEvent>[] EVOKER_SPOTTED_SOUNDS;
    private static final Supplier<SoundEvent>[] EVOKER_VICTORY_SOUNDS;
    private static final Map<SoundEvent, Integer> soundDurations;

    private static void initSoundDurations() {
        soundDurations.put((SoundEvent)IllagerSounds.PILLAGER_AMBIENT_NOISE_01.get(), 16);
        soundDurations.put((SoundEvent)IllagerSounds.PILLAGER_AMBIENT_NOISE_02.get(), 12);
        soundDurations.put((SoundEvent)IllagerSounds.PILLAGER_AMBIENT_NOISE_03.get(), 12);
    }

    public IllagerVoiceManager(AbstractIllager illager, IllagerType illagerType) {
        this.currentState = Passive.INSTANCE;
        this.hadTarget = false;
        this.soundCooldown = 0;
        this.postHurtCooldown = 0;
        this.postVictoryCooldown = 0;
        this.isSpeaking = false;
        this.speakingTimer = 0;
        this.currentlyPlayingSound = null;
        this.currentSoundType = null;
        this.isShortHurtSound = false;
        this.illager = illager;
        this.illagerType = illagerType;
        this.random = illager.level().random;
        String typeName = illagerType.name();
        LOGGER.info("VOICE MANAGER CREATED FOR {} AT {}, {}, {}", new Object[]{typeName, illager.getX(), illager.getY(), illager.getZ()});
    }

    public void update() {
        if (this.isSpeaking) {
            --this.speakingTimer;
            if (this.speakingTimer <= 0) {
                this.isSpeaking = false;
                this.currentlyPlayingSound = null;
                this.currentSoundType = null;
                this.isShortHurtSound = false;
                IllagerState var10000 = this.currentState;
                int var9;
                switch (this.currentState) {
                    case Passive p -> var9 = IllagerBlabberConfig.COOLDOWN_PASSIVE_MIN.get()
                            + this.threadSafeRandom.nextInt(IllagerBlabberConfig.COOLDOWN_PASSIVE_RANDOM.get());
                    case Combat c -> var9 = IllagerBlabberConfig.COOLDOWN_COMBAT_MIN.get()
                            + this.threadSafeRandom.nextInt(IllagerBlabberConfig.COOLDOWN_COMBAT_RANDOM.get());
                    case IllagerState.Spotted s -> var9 = IllagerBlabberConfig.COOLDOWN_SPOTTED_MIN.get()
                            + this.threadSafeRandom.nextInt(IllagerBlabberConfig.COOLDOWN_SPOTTED_RANDOM.get());
                    case IllagerState.Hurt h -> var9 = IllagerBlabberConfig.COOLDOWN_HURT_MIN.get()
                            + this.threadSafeRandom.nextInt(IllagerBlabberConfig.COOLDOWN_HURT_RANDOM.get());
                    case IllagerState.Victory v -> var9 = IllagerBlabberConfig.COOLDOWN_VICTORY_MIN.get()
                            + this.threadSafeRandom.nextInt(IllagerBlabberConfig.COOLDOWN_VICTORY_RANDOM.get());
                    default -> var9 = 40;
                }

                int baseCooldown = var9;
                this.soundCooldown = this.adjustCooldownBasedOnCrowding(this.illager, this.currentState, baseCooldown);
                if (this.currentState instanceof IllagerState.Victory) {
                    this.postVictoryCooldown = 100;
                    LOGGER.info("{} VICTORY COMPLETE - Setting post-victory cooldown", this.illagerType.name());
                }

                if (this.currentState instanceof IllagerState.Hurt) {
                    this.postHurtCooldown = 60;
                }
            }
        }

        if (this.soundCooldown > 0) {
            --this.soundCooldown;
        }

        if (this.postHurtCooldown > 0) {
            --this.postHurtCooldown;
        } else {
            if (this.postVictoryCooldown > 0) {
                --this.postVictoryCooldown;
                if (!(this.currentState instanceof IllagerState.Hurt)) {
                    return;
                }
            }

            if (this.shouldPlaySoundForState(this.currentState)) {
                if (this.isSpeaking) {
                    if (this.shouldInterruptCurrentSound(this.currentState)) {
                        this.isSpeaking = false;
                        this.currentlyPlayingSound = null;
                        this.currentSoundType = null;
                        this.isShortHurtSound = false;
                        this.playAppropriateSound();
                    }
                } else {
                    this.playAppropriateSound();
                }
            } else if (this.soundCooldown <= 0 && !this.isSpeaking) {
                this.playAppropriateSound();
            }

        }
    }

    private int adjustCooldownBasedOnCrowding(LivingEntity entity, IllagerState state, int baseCooldown) {
        if (state instanceof IllagerState.Hurt) {
            return baseCooldown;
        } else {
            Level world = entity.level();
            Class var10000;
            switch (this.illagerType) {
                case EVOKER -> var10000 = Evoker.class;
                case VINDICATOR -> var10000 = Vindicator.class;
                case PILLAGER -> var10000 = Pillager.class;
                default -> throw new MatchException((String)null, (Throwable)null);
            }

            Class<? extends AbstractIllager> entityClass = var10000;
            List<? extends AbstractIllager> nearbyIllagers = world.getEntitiesOfClass(entityClass, entity.getBoundingBox().inflate((double)15.0F), (e) -> true);
            int count = nearbyIllagers.size();
            if (count <= 1) {
                return baseCooldown;
            } else {
                double var16;
                switch (state) {
                    case IllagerState.Passive p -> var16 = 0.3;
                    case IllagerState.Combat c -> var16 = 0.2;
                    case IllagerState.Spotted s -> var16 = 0.15;
                    case IllagerState.Victory v -> var16 = 0.1;
                    default -> var16 = (double)0.25F;
                }

                double scaleFactor = var16;
                return (int)((double)baseCooldown * ((double)1.0F + (double)(count - 1) * scaleFactor));
            }
        }
    }

    private boolean shouldPlaySoundForState(IllagerState state) {
        return state instanceof IllagerState.Hurt || state instanceof IllagerState.Spotted;
    }

    private boolean shouldInterruptCurrentSound(IllagerState state) {
        if (!this.isSpeaking) {
            return false;
        } else if (state instanceof IllagerState.Hurt) {
            return !(this.currentSoundType instanceof IllagerState.Hurt);
        } else {
            return state instanceof IllagerState.Spotted && !(this.currentSoundType instanceof IllagerState.Hurt);
        }
    }

    public void setState(IllagerState state) {
        if (state instanceof IllagerState.Spotted && (this.illagerType == IllagerType.PILLAGER || this.illagerType == IllagerType.EVOKER)) {
            long currentTime = System.currentTimeMillis();
            long lastTime = IllagerVoiceRegistry.getLastGroupSpottedTime(this.illagerType);
            if (currentTime - lastTime < 3000L) {
                this.currentState = Combat.INSTANCE;
                return;
            }

            IllagerVoiceRegistry.setLastGroupSpottedTime(this.illagerType, currentTime);
        }

        this.currentState = state;
    }

    private void playAppropriateSound() {
        IllagerState var10000 = this.currentState;
        SoundEvent var8;
        switch (var10000) {
            case IllagerState.Combat c -> var8 = this.chooseCombatSound();
            case IllagerState.Spotted s -> var8 = this.chooseSpottedSound();
            case IllagerState.Hurt h -> var8 = this.chooseHurtSound();
            case IllagerState.Victory v -> var8 = this.chooseVictorySound();
            default -> var8 = this.choosePassiveSound();
        }

        SoundEvent sound = var8;
        this.playSound(sound);
    }

    private SoundEvent choosePassiveSound() {
        if (this.threadSafeRandom.nextInt(100) < 50) {
            SoundEvent var1;
            switch (this.illagerType) {
                case EVOKER -> var1 = this.getRandomSound(EVOKER_AMBIENT_NOISE_SOUNDS);
                case VINDICATOR -> var1 = this.getRandomSound(VINDICATOR_AMBIENT_NOISE_SOUNDS);
                case PILLAGER -> var1 = this.getRandomSound(PILLAGER_AMBIENT_NOISE_SOUNDS);
                default -> throw new MatchException((String)null, (Throwable)null);
            }

            return var1;
        } else {
            SoundEvent var10000;
            switch (this.illagerType) {
                case EVOKER -> var10000 = this.getRandomSound(EVOKER_AMBIENT_TALK_SOUNDS);
                case VINDICATOR -> var10000 = this.getRandomSound(VINDICATOR_AMBIENT_TALK_SOUNDS);
                case PILLAGER -> var10000 = this.getRandomSound(PILLAGER_AMBIENT_TALK_SOUNDS);
                default -> throw new MatchException((String)null, (Throwable)null);
            }

            return var10000;
        }
    }

    private SoundEvent chooseSpottedSound() {
        SoundEvent var10000;
        switch (this.illagerType) {
            case EVOKER -> var10000 = this.getRandomSound(EVOKER_SPOTTED_SOUNDS);
            case VINDICATOR -> var10000 = this.getRandomSound(VINDICATOR_SPOTTED_SOUNDS);
            case PILLAGER -> var10000 = this.getRandomSound(PILLAGER_SPOTTED_SOUNDS);
            default -> throw new MatchException((String)null, (Throwable)null);
        }

        return var10000;
    }

    private SoundEvent chooseCombatSound() {
        if (this.threadSafeRandom.nextInt(100) < 30) {
            SoundEvent var1;
            switch (this.illagerType) {
                case EVOKER -> var1 = this.getRandomSound(EVOKER_AMBIENT_NOISE_SOUNDS);
                case VINDICATOR -> var1 = this.getRandomSound(VINDICATOR_AMBIENT_NOISE_SOUNDS);
                case PILLAGER -> var1 = this.getRandomSound(PILLAGER_AMBIENT_NOISE_SOUNDS);
                default -> throw new MatchException((String)null, (Throwable)null);
            }

            return var1;
        } else {
            SoundEvent var10000;
            switch (this.illagerType) {
                case EVOKER -> var10000 = this.getRandomSound(EVOKER_BATTLE_SOUNDS);
                case VINDICATOR -> var10000 = this.getRandomSound(VINDICATOR_BATTLE_SOUNDS);
                case PILLAGER -> var10000 = this.getRandomSound(PILLAGER_BATTLE_SOUNDS);
                default -> throw new MatchException((String)null, (Throwable)null);
            }

            return var10000;
        }
    }

    private SoundEvent chooseHurtSound() {
        SoundEvent var10000;
        switch (this.illagerType) {
            case EVOKER -> var10000 = this.getRandomSound(EVOKER_HURT_SOUNDS);
            case VINDICATOR -> var10000 = this.getRandomSound(VINDICATOR_HURT_SOUNDS);
            case PILLAGER -> var10000 = this.getRandomSound(PILLAGER_HURT_SOUNDS);
            default -> throw new MatchException((String)null, (Throwable)null);
        }

        return var10000;
    }

    private SoundEvent chooseVictorySound() {
        SoundEvent var10000;
        switch (this.illagerType) {
            case EVOKER -> var10000 = this.getRandomSound(EVOKER_VICTORY_SOUNDS);
            case VINDICATOR -> var10000 = this.getRandomSound(VINDICATOR_VICTORY_SOUNDS);
            case PILLAGER -> var10000 = this.getRandomSound(PILLAGER_VICTORY_SOUNDS);
            default -> throw new MatchException((String)null, (Throwable)null);
        }

        return var10000;
    }

    private void playSound(SoundEvent sound) {
        IllagerState var10000 = this.currentState;
        float var6;
        switch (var10000) {
            case IllagerState.Hurt h -> var6 = 0.9F + this.threadSafeRandom.nextFloat() * 0.25F;
            default -> var6 = 0.9F + this.threadSafeRandom.nextFloat() * 0.1F;
        }

        float randomPitch = var6;
        this.illager.level().playSound((Player)null, this.illager.getX(), this.illager.getY(), this.illager.getZ(), sound, SoundSource.HOSTILE, 1.0F, randomPitch);
        this.isSpeaking = true;
        this.currentlyPlayingSound = sound;
        this.currentSoundType = this.currentState;
        this.speakingTimer = (Integer)soundDurations.getOrDefault(sound, 40);
        if (this.currentState instanceof IllagerState.Hurt && this.speakingTimer < 20) {
            this.isShortHurtSound = true;
        }

    }

    private SoundEvent getRandomSound(Supplier<SoundEvent>[] sounds) {
        return (SoundEvent)sounds[this.threadSafeRandom.nextInt(sounds.length)].get();
    }

    static {
        PILLAGER_AMBIENT_NOISE_SOUNDS = new Supplier[]{IllagerSounds.PILLAGER_AMBIENT_NOISE_01, IllagerSounds.PILLAGER_AMBIENT_NOISE_02, IllagerSounds.PILLAGER_AMBIENT_NOISE_03, IllagerSounds.PILLAGER_AMBIENT_NOISE_04, IllagerSounds.PILLAGER_AMBIENT_NOISE_05, IllagerSounds.PILLAGER_AMBIENT_NOISE_06, IllagerSounds.PILLAGER_AMBIENT_NOISE_07, IllagerSounds.PILLAGER_AMBIENT_NOISE_08, IllagerSounds.PILLAGER_AMBIENT_NOISE_09, IllagerSounds.PILLAGER_AMBIENT_NOISE_10, IllagerSounds.PILLAGER_AMBIENT_NOISE_11, IllagerSounds.PILLAGER_AMBIENT_NOISE_12, IllagerSounds.PILLAGER_AMBIENT_NOISE_13, IllagerSounds.PILLAGER_AMBIENT_NOISE_14};
        PILLAGER_AMBIENT_TALK_SOUNDS = new Supplier[]{IllagerSounds.PILLAGER_AMBIENT_TALK_01, IllagerSounds.PILLAGER_AMBIENT_TALK_02, IllagerSounds.PILLAGER_AMBIENT_TALK_03, IllagerSounds.PILLAGER_AMBIENT_TALK_04, IllagerSounds.PILLAGER_AMBIENT_TALK_05, IllagerSounds.PILLAGER_AMBIENT_TALK_06, IllagerSounds.PILLAGER_AMBIENT_TALK_07, IllagerSounds.PILLAGER_AMBIENT_TALK_08, IllagerSounds.PILLAGER_AMBIENT_TALK_09, IllagerSounds.PILLAGER_AMBIENT_TALK_10, IllagerSounds.PILLAGER_AMBIENT_TALK_11, IllagerSounds.PILLAGER_AMBIENT_TALK_12, IllagerSounds.PILLAGER_AMBIENT_TALK_13, IllagerSounds.PILLAGER_AMBIENT_TALK_14, IllagerSounds.PILLAGER_AMBIENT_TALK_15, IllagerSounds.PILLAGER_AMBIENT_TALK_16, IllagerSounds.PILLAGER_AMBIENT_TALK_17, IllagerSounds.PILLAGER_AMBIENT_TALK_18, IllagerSounds.PILLAGER_AMBIENT_TALK_19, IllagerSounds.PILLAGER_AMBIENT_TALK_20, IllagerSounds.PILLAGER_AMBIENT_TALK_21, IllagerSounds.PILLAGER_AMBIENT_TALK_22, IllagerSounds.PILLAGER_AMBIENT_TALK_23};
        PILLAGER_SPOTTED_SOUNDS = new Supplier[]{IllagerSounds.PILLAGER_SPOTTED_01, IllagerSounds.PILLAGER_SPOTTED_02, IllagerSounds.PILLAGER_SPOTTED_03, IllagerSounds.PILLAGER_SPOTTED_04, IllagerSounds.PILLAGER_SPOTTED_05, IllagerSounds.PILLAGER_SPOTTED_06};
        PILLAGER_BATTLE_SOUNDS = new Supplier[]{IllagerSounds.PILLAGER_BATTLE_01, IllagerSounds.PILLAGER_BATTLE_02, IllagerSounds.PILLAGER_BATTLE_03, IllagerSounds.PILLAGER_BATTLE_04, IllagerSounds.PILLAGER_BATTLE_05, IllagerSounds.PILLAGER_BATTLE_06, IllagerSounds.PILLAGER_BATTLE_07};
        PILLAGER_HURT_SOUNDS = new Supplier[]{IllagerSounds.PILLAGER_HURT_01, IllagerSounds.PILLAGER_HURT_02, IllagerSounds.PILLAGER_HURT_03, IllagerSounds.PILLAGER_HURT_04, IllagerSounds.PILLAGER_HURT_05, IllagerSounds.PILLAGER_HURT_06, IllagerSounds.PILLAGER_HURT_07, IllagerSounds.PILLAGER_HURT_08, IllagerSounds.PILLAGER_HURT_09, IllagerSounds.PILLAGER_HURT_10, IllagerSounds.PILLAGER_HURT_11, IllagerSounds.PILLAGER_HURT_12, IllagerSounds.PILLAGER_HURT_13, IllagerSounds.PILLAGER_HURT_14, IllagerSounds.PILLAGER_HURT_15, IllagerSounds.PILLAGER_HURT_16, IllagerSounds.PILLAGER_HURT_17, IllagerSounds.PILLAGER_HURT_18, IllagerSounds.PILLAGER_HURT_19, IllagerSounds.PILLAGER_HURT_20, IllagerSounds.PILLAGER_HURT_21, IllagerSounds.PILLAGER_HURT_22, IllagerSounds.PILLAGER_HURT_23};
        PILLAGER_VICTORY_SOUNDS = new Supplier[]{IllagerSounds.PILLAGER_VICTORY_01, IllagerSounds.PILLAGER_VICTORY_02, IllagerSounds.PILLAGER_VICTORY_03, IllagerSounds.PILLAGER_VICTORY_04, IllagerSounds.PILLAGER_VICTORY_05, IllagerSounds.PILLAGER_VICTORY_06, IllagerSounds.PILLAGER_VICTORY_07, IllagerSounds.PILLAGER_VICTORY_08, IllagerSounds.PILLAGER_VICTORY_09, IllagerSounds.PILLAGER_VICTORY_10, IllagerSounds.PILLAGER_VICTORY_11, IllagerSounds.PILLAGER_VICTORY_12, IllagerSounds.PILLAGER_VICTORY_13, IllagerSounds.PILLAGER_VICTORY_14, IllagerSounds.PILLAGER_VICTORY_15, IllagerSounds.PILLAGER_VICTORY_16, IllagerSounds.PILLAGER_VICTORY_17, IllagerSounds.PILLAGER_VICTORY_18, IllagerSounds.PILLAGER_VICTORY_19};
        VINDICATOR_AMBIENT_NOISE_SOUNDS = new Supplier[]{IllagerSounds.VINDICATOR_AMBIENT_NOISE_01, IllagerSounds.VINDICATOR_AMBIENT_NOISE_02, IllagerSounds.VINDICATOR_AMBIENT_NOISE_03, IllagerSounds.VINDICATOR_AMBIENT_NOISE_04, IllagerSounds.VINDICATOR_AMBIENT_NOISE_05, IllagerSounds.VINDICATOR_AMBIENT_NOISE_06, IllagerSounds.VINDICATOR_AMBIENT_NOISE_07, IllagerSounds.VINDICATOR_AMBIENT_NOISE_08, IllagerSounds.VINDICATOR_AMBIENT_NOISE_09, IllagerSounds.VINDICATOR_AMBIENT_NOISE_10, IllagerSounds.VINDICATOR_AMBIENT_NOISE_11, IllagerSounds.VINDICATOR_AMBIENT_NOISE_12, IllagerSounds.VINDICATOR_AMBIENT_NOISE_13, IllagerSounds.VINDICATOR_AMBIENT_NOISE_14, IllagerSounds.VINDICATOR_AMBIENT_NOISE_15, IllagerSounds.VINDICATOR_AMBIENT_NOISE_16, IllagerSounds.VINDICATOR_AMBIENT_NOISE_17, IllagerSounds.VINDICATOR_AMBIENT_NOISE_18, IllagerSounds.VINDICATOR_AMBIENT_NOISE_19, IllagerSounds.VINDICATOR_AMBIENT_NOISE_20, IllagerSounds.VINDICATOR_AMBIENT_NOISE_21, IllagerSounds.VINDICATOR_AMBIENT_NOISE_22, IllagerSounds.VINDICATOR_AMBIENT_NOISE_23, IllagerSounds.VINDICATOR_AMBIENT_NOISE_24, IllagerSounds.VINDICATOR_AMBIENT_NOISE_25, IllagerSounds.VINDICATOR_AMBIENT_NOISE_26, IllagerSounds.VINDICATOR_AMBIENT_NOISE_27, IllagerSounds.VINDICATOR_AMBIENT_NOISE_28, IllagerSounds.VINDICATOR_AMBIENT_NOISE_29, IllagerSounds.VINDICATOR_AMBIENT_NOISE_30, IllagerSounds.VINDICATOR_AMBIENT_NOISE_31};
        VINDICATOR_AMBIENT_TALK_SOUNDS = new Supplier[]{IllagerSounds.VINDICATOR_AMBIENT_TALK_01, IllagerSounds.VINDICATOR_AMBIENT_TALK_02, IllagerSounds.VINDICATOR_AMBIENT_TALK_03, IllagerSounds.VINDICATOR_AMBIENT_TALK_04, IllagerSounds.VINDICATOR_AMBIENT_TALK_05, IllagerSounds.VINDICATOR_AMBIENT_TALK_06, IllagerSounds.VINDICATOR_AMBIENT_TALK_07, IllagerSounds.VINDICATOR_AMBIENT_TALK_08, IllagerSounds.VINDICATOR_AMBIENT_TALK_09, IllagerSounds.VINDICATOR_AMBIENT_TALK_10, IllagerSounds.VINDICATOR_AMBIENT_TALK_11, IllagerSounds.VINDICATOR_AMBIENT_TALK_12, IllagerSounds.VINDICATOR_AMBIENT_TALK_13, IllagerSounds.VINDICATOR_AMBIENT_TALK_14, IllagerSounds.VINDICATOR_AMBIENT_TALK_15, IllagerSounds.VINDICATOR_AMBIENT_TALK_16, IllagerSounds.VINDICATOR_AMBIENT_TALK_17, IllagerSounds.VINDICATOR_AMBIENT_TALK_18, IllagerSounds.VINDICATOR_AMBIENT_TALK_19, IllagerSounds.VINDICATOR_AMBIENT_TALK_20, IllagerSounds.VINDICATOR_AMBIENT_TALK_21, IllagerSounds.VINDICATOR_AMBIENT_TALK_22, IllagerSounds.VINDICATOR_AMBIENT_TALK_23, IllagerSounds.VINDICATOR_AMBIENT_TALK_24};
        VINDICATOR_SPOTTED_SOUNDS = new Supplier[]{IllagerSounds.VINDICATOR_SPOTTED_01, IllagerSounds.VINDICATOR_SPOTTED_02, IllagerSounds.VINDICATOR_SPOTTED_03, IllagerSounds.VINDICATOR_SPOTTED_04, IllagerSounds.VINDICATOR_SPOTTED_05, IllagerSounds.VINDICATOR_SPOTTED_06, IllagerSounds.VINDICATOR_SPOTTED_07, IllagerSounds.VINDICATOR_SPOTTED_08, IllagerSounds.VINDICATOR_SPOTTED_09, IllagerSounds.VINDICATOR_SPOTTED_10, IllagerSounds.VINDICATOR_SPOTTED_11};
        VINDICATOR_BATTLE_SOUNDS = new Supplier[]{IllagerSounds.VINDICATOR_BATTLE_01, IllagerSounds.VINDICATOR_BATTLE_02, IllagerSounds.VINDICATOR_BATTLE_03, IllagerSounds.VINDICATOR_BATTLE_04, IllagerSounds.VINDICATOR_BATTLE_05, IllagerSounds.VINDICATOR_BATTLE_06, IllagerSounds.VINDICATOR_BATTLE_07, IllagerSounds.VINDICATOR_BATTLE_08, IllagerSounds.VINDICATOR_BATTLE_09, IllagerSounds.VINDICATOR_BATTLE_10, IllagerSounds.VINDICATOR_BATTLE_11, IllagerSounds.VINDICATOR_BATTLE_12, IllagerSounds.VINDICATOR_BATTLE_13};
        VINDICATOR_HURT_SOUNDS = new Supplier[]{IllagerSounds.VINDICATOR_HURT_01, IllagerSounds.VINDICATOR_HURT_02, IllagerSounds.VINDICATOR_HURT_03, IllagerSounds.VINDICATOR_HURT_04, IllagerSounds.VINDICATOR_HURT_05, IllagerSounds.VINDICATOR_HURT_06, IllagerSounds.VINDICATOR_HURT_07, IllagerSounds.VINDICATOR_HURT_08, IllagerSounds.VINDICATOR_HURT_09, IllagerSounds.VINDICATOR_HURT_10, IllagerSounds.VINDICATOR_HURT_11, IllagerSounds.VINDICATOR_HURT_12, IllagerSounds.VINDICATOR_HURT_13, IllagerSounds.VINDICATOR_HURT_14, IllagerSounds.VINDICATOR_HURT_15, IllagerSounds.VINDICATOR_HURT_16, IllagerSounds.VINDICATOR_HURT_17, IllagerSounds.VINDICATOR_HURT_18, IllagerSounds.VINDICATOR_HURT_19, IllagerSounds.VINDICATOR_HURT_20, IllagerSounds.VINDICATOR_HURT_21, IllagerSounds.VINDICATOR_HURT_22, IllagerSounds.VINDICATOR_HURT_23, IllagerSounds.VINDICATOR_HURT_24, IllagerSounds.VINDICATOR_HURT_25, IllagerSounds.VINDICATOR_HURT_26};
        VINDICATOR_VICTORY_SOUNDS = new Supplier[]{IllagerSounds.VINDICATOR_VICTORY_01, IllagerSounds.VINDICATOR_VICTORY_02, IllagerSounds.VINDICATOR_VICTORY_03, IllagerSounds.VINDICATOR_VICTORY_04, IllagerSounds.VINDICATOR_VICTORY_05, IllagerSounds.VINDICATOR_VICTORY_06, IllagerSounds.VINDICATOR_VICTORY_07, IllagerSounds.VINDICATOR_VICTORY_08, IllagerSounds.VINDICATOR_VICTORY_09, IllagerSounds.VINDICATOR_VICTORY_10, IllagerSounds.VINDICATOR_VICTORY_11, IllagerSounds.VINDICATOR_VICTORY_12, IllagerSounds.VINDICATOR_VICTORY_13};
        EVOKER_AMBIENT_NOISE_SOUNDS = new Supplier[]{IllagerSounds.EVOKER_AMBIENT_NOISE_01, IllagerSounds.EVOKER_AMBIENT_NOISE_02, IllagerSounds.EVOKER_AMBIENT_NOISE_03, IllagerSounds.EVOKER_AMBIENT_NOISE_04, IllagerSounds.EVOKER_AMBIENT_NOISE_05, IllagerSounds.EVOKER_AMBIENT_NOISE_06, IllagerSounds.EVOKER_AMBIENT_NOISE_07, IllagerSounds.EVOKER_AMBIENT_NOISE_08, IllagerSounds.EVOKER_AMBIENT_NOISE_09, IllagerSounds.EVOKER_AMBIENT_NOISE_10, IllagerSounds.EVOKER_AMBIENT_NOISE_11, IllagerSounds.EVOKER_AMBIENT_NOISE_12, IllagerSounds.EVOKER_AMBIENT_NOISE_13, IllagerSounds.EVOKER_AMBIENT_NOISE_14};
        EVOKER_AMBIENT_TALK_SOUNDS = new Supplier[]{IllagerSounds.EVOKER_AMBIENT_TALK_01, IllagerSounds.EVOKER_AMBIENT_TALK_02, IllagerSounds.EVOKER_AMBIENT_TALK_03, IllagerSounds.EVOKER_AMBIENT_TALK_04, IllagerSounds.EVOKER_AMBIENT_TALK_05, IllagerSounds.EVOKER_AMBIENT_TALK_06, IllagerSounds.EVOKER_AMBIENT_TALK_07, IllagerSounds.EVOKER_AMBIENT_TALK_08, IllagerSounds.EVOKER_AMBIENT_TALK_09, IllagerSounds.EVOKER_AMBIENT_TALK_10, IllagerSounds.EVOKER_AMBIENT_TALK_11, IllagerSounds.EVOKER_AMBIENT_TALK_12, IllagerSounds.EVOKER_AMBIENT_TALK_13, IllagerSounds.EVOKER_AMBIENT_TALK_14, IllagerSounds.EVOKER_AMBIENT_TALK_15, IllagerSounds.EVOKER_AMBIENT_TALK_16, IllagerSounds.EVOKER_AMBIENT_TALK_17, IllagerSounds.EVOKER_AMBIENT_TALK_18, IllagerSounds.EVOKER_AMBIENT_TALK_19, IllagerSounds.EVOKER_AMBIENT_TALK_20, IllagerSounds.EVOKER_AMBIENT_TALK_21, IllagerSounds.EVOKER_AMBIENT_TALK_22, IllagerSounds.EVOKER_AMBIENT_TALK_23, IllagerSounds.EVOKER_AMBIENT_TALK_24, IllagerSounds.EVOKER_AMBIENT_TALK_25, IllagerSounds.EVOKER_AMBIENT_TALK_26, IllagerSounds.EVOKER_AMBIENT_TALK_27, IllagerSounds.EVOKER_AMBIENT_TALK_28, IllagerSounds.EVOKER_AMBIENT_TALK_29, IllagerSounds.EVOKER_AMBIENT_TALK_30, IllagerSounds.EVOKER_AMBIENT_TALK_31, IllagerSounds.EVOKER_AMBIENT_TALK_32, IllagerSounds.EVOKER_AMBIENT_TALK_33};
        EVOKER_BATTLE_SOUNDS = new Supplier[]{IllagerSounds.EVOKER_BATTLE_01, IllagerSounds.EVOKER_BATTLE_02, IllagerSounds.EVOKER_BATTLE_03, IllagerSounds.EVOKER_BATTLE_04, IllagerSounds.EVOKER_BATTLE_05, IllagerSounds.EVOKER_BATTLE_06, IllagerSounds.EVOKER_BATTLE_07, IllagerSounds.EVOKER_BATTLE_08, IllagerSounds.EVOKER_BATTLE_09, IllagerSounds.EVOKER_BATTLE_10, IllagerSounds.EVOKER_BATTLE_11, IllagerSounds.EVOKER_BATTLE_12, IllagerSounds.EVOKER_BATTLE_13, IllagerSounds.EVOKER_BATTLE_14};
        EVOKER_HURT_SOUNDS = new Supplier[]{IllagerSounds.EVOKER_HURT_01, IllagerSounds.EVOKER_HURT_02, IllagerSounds.EVOKER_HURT_03, IllagerSounds.EVOKER_HURT_04, IllagerSounds.EVOKER_HURT_05, IllagerSounds.EVOKER_HURT_06, IllagerSounds.EVOKER_HURT_07, IllagerSounds.EVOKER_HURT_08, IllagerSounds.EVOKER_HURT_09, IllagerSounds.EVOKER_HURT_10, IllagerSounds.EVOKER_HURT_11, IllagerSounds.EVOKER_HURT_12, IllagerSounds.EVOKER_HURT_13, IllagerSounds.EVOKER_HURT_14, IllagerSounds.EVOKER_HURT_15, IllagerSounds.EVOKER_HURT_16, IllagerSounds.EVOKER_HURT_17, IllagerSounds.EVOKER_HURT_18, IllagerSounds.EVOKER_HURT_19};
        EVOKER_SPOTTED_SOUNDS = new Supplier[]{IllagerSounds.EVOKER_SPOTTED_01, IllagerSounds.EVOKER_SPOTTED_02, IllagerSounds.EVOKER_SPOTTED_03, IllagerSounds.EVOKER_SPOTTED_04, IllagerSounds.EVOKER_SPOTTED_05, IllagerSounds.EVOKER_SPOTTED_06};
        EVOKER_VICTORY_SOUNDS = new Supplier[]{IllagerSounds.EVOKER_VICTORY_01, IllagerSounds.EVOKER_VICTORY_02, IllagerSounds.EVOKER_VICTORY_03, IllagerSounds.EVOKER_VICTORY_04, IllagerSounds.EVOKER_VICTORY_05, IllagerSounds.EVOKER_VICTORY_06, IllagerSounds.EVOKER_VICTORY_07, IllagerSounds.EVOKER_VICTORY_08, IllagerSounds.EVOKER_VICTORY_09, IllagerSounds.EVOKER_VICTORY_10, IllagerSounds.EVOKER_VICTORY_11, IllagerSounds.EVOKER_VICTORY_12, IllagerSounds.EVOKER_VICTORY_13, IllagerSounds.EVOKER_VICTORY_14};
        soundDurations = new HashMap();
        initSoundDurations();
    }
}
