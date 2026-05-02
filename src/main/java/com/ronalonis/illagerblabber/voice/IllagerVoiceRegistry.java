package com.ronalonis.illagerblabber.voice;

import com.ronalonis.illagerblabber.voice.IllagerState.Combat;
import com.ronalonis.illagerblabber.voice.IllagerState.Hurt;
import com.ronalonis.illagerblabber.voice.IllagerState.Passive;
import com.ronalonis.illagerblabber.voice.IllagerState.Spotted;
import com.ronalonis.illagerblabber.voice.IllagerState.Victory;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IllagerVoiceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("assets.illagerblabber");
    private static final ConcurrentHashMap<IllagerType, Long> lastGroupSpottedSoundTime = new ConcurrentHashMap();
    private static final ConcurrentHashMap<UUID, IllagerVoiceManager> voiceManagers = new ConcurrentHashMap();
    private static final ConcurrentHashMap<UUID, Boolean> hadTargetLastTick = new ConcurrentHashMap();
    private static final ConcurrentHashMap<UUID, Integer> victoryTimers = new ConcurrentHashMap();
    private static final ConcurrentHashMap<UUID, Integer> combatDebounceTimers = new ConcurrentHashMap();
    private static final ConcurrentHashMap<UUID, Long> lastProcessedTick = new ConcurrentHashMap();
    private static long currentGameTick = 0L;
    private static final Random safeRandom = new Random();
    private static final ConcurrentHashMap<UUID, UUID> lastPillagerTargets = new ConcurrentHashMap();
    private static final ConcurrentHashMap<UUID, UUID> lastVindicatorTargets = new ConcurrentHashMap();
    private static final ConcurrentHashMap<UUID, UUID> lastEvokerTargets = new ConcurrentHashMap();

    public IllagerVoiceRegistry() {
    }

    public static long getLastGroupSpottedTime(IllagerType type) {
        return (Long)lastGroupSpottedSoundTime.getOrDefault(type, 0L);
    }

    public static void setLastGroupSpottedTime(IllagerType type, long time) {
        lastGroupSpottedSoundTime.put(type, time);
    }

    public static IllagerVoiceManager getVoiceManager(AbstractIllager illager, IllagerType type) {
        return (IllagerVoiceManager)voiceManagers.computeIfAbsent(illager.getUUID(), (uuid) -> {
            LOGGER.info("CREATING NEW VOICE MANAGER FOR {}!", type.name());
            return new IllagerVoiceManager(illager, type);
        });
    }

    private static IllagerType getIllagerType(AbstractIllager illager) {
        if (illager instanceof Evoker) {
            return IllagerType.EVOKER;
        } else if (illager instanceof Vindicator) {
            return IllagerType.VINDICATOR;
        } else {
            return illager instanceof Pillager ? IllagerType.PILLAGER : IllagerType.EVOKER;
        }
    }

    public static void setHurtState(AbstractIllager entity) {
        UUID id = entity.getUUID();
        IllagerVoiceManager voiceManager = (IllagerVoiceManager)voiceManagers.get(id);
        if (voiceManager != null) {
            voiceManager.setState(Hurt.INSTANCE);
        }
    }

    public static void setVictoryState(AbstractIllager entity) {
        UUID id = entity.getUUID();
        IllagerVoiceManager voiceManager = (IllagerVoiceManager)voiceManagers.get(id);
        if (voiceManager != null) {
            voiceManager.setState(Victory.INSTANCE);
            victoryTimers.put(id, 100);
        }
    }

    public static void updateIllager(AbstractIllager illager, IllagerType illagerType) {
        UUID id = illager.getUUID();
        ++currentGameTick;
        if ((Long)lastProcessedTick.getOrDefault(id, 0L) == currentGameTick) {
            LOGGER.info("Entity {} already processed this tick, skipping", id);
        } else {
            lastProcessedTick.put(id, currentGameTick);
            synchronized(id.toString().intern()) {
                IllagerVoiceManager voiceManager = getVoiceManager(illager, illagerType);
                voiceManager.update();
                updateIllagerState(illager);
            }
        }
    }

    private static void updateIllagerState(AbstractIllager illager) {
        UUID id = illager.getUUID();
        IllagerType illagerType = getIllagerType(illager);
        IllagerVoiceManager voiceManager = (IllagerVoiceManager)voiceManagers.get(id);
        if (voiceManager != null) {
            boolean hadTarget = (Boolean)hadTargetLastTick.getOrDefault(id, false);
            int victoryTimer = (Integer)victoryTimers.getOrDefault(id, 0);
            int combatDebounceTimer = (Integer)combatDebounceTimers.getOrDefault(id, 0);
            if (victoryTimer > 0) {
                --victoryTimer;
                victoryTimers.put(id, victoryTimer);
            } else {
                if (combatDebounceTimer > 0) {
                    --combatDebounceTimer;
                    combatDebounceTimers.put(id, combatDebounceTimer);
                }

                boolean hasTarget = illager.getTarget() != null && illager.getTarget().isAlive();
                if (illagerType == IllagerType.VINDICATOR) {
                    UUID vindicatorId = illager.getUUID();
                    if (hasTarget) {
                        lastVindicatorTargets.put(vindicatorId, illager.getTarget().getUUID());
                    } else if (hadTarget && lastVindicatorTargets.containsKey(vindicatorId)) {
                        LOGGER.info("FORCING VINDICATOR VICTORY!");
                        voiceManager.setState(Victory.INSTANCE);
                        victoryTimers.put(id, 60);
                        hadTargetLastTick.put(id, false);
                        lastVindicatorTargets.remove(vindicatorId);
                        return;
                    }
                }

                if (illagerType == IllagerType.PILLAGER) {
                    UUID pillagerId = illager.getUUID();
                    if (hasTarget) {
                        lastPillagerTargets.putIfAbsent(pillagerId, illager.getTarget().getUUID());
                    } else if (hadTarget && lastPillagerTargets.containsKey(pillagerId)) {
                        voiceManager.setState(Victory.INSTANCE);
                        victoryTimers.put(id, 60);
                        hadTargetLastTick.put(id, false);
                        lastPillagerTargets.remove(pillagerId);
                        return;
                    }
                }

                if (illagerType == IllagerType.EVOKER) {
                    UUID evokerId = illager.getUUID();
                    if (hasTarget) {
                        lastEvokerTargets.putIfAbsent(evokerId, illager.getTarget().getUUID());
                    } else if (hadTarget && lastEvokerTargets.containsKey(evokerId)) {
                        voiceManager.setState(Victory.INSTANCE);
                        victoryTimers.put(id, 60);
                        hadTargetLastTick.put(id, false);
                        lastEvokerTargets.remove(evokerId);
                        return;
                    }
                }

                if (hasTarget) {
                    if (!hadTarget) {
                        voiceManager.setState(Spotted.INSTANCE);
                        hadTargetLastTick.put(id, true);
                    } else {
                        voiceManager.setState(Combat.INSTANCE);
                    }

                    combatDebounceTimer = 60 + safeRandom.nextInt(41);
                    combatDebounceTimers.put(id, combatDebounceTimer);
                } else if (hadTarget) {
                    if (combatDebounceTimer <= 0) {
                        voiceManager.setState(Victory.INSTANCE);
                        victoryTimer = 100;
                        victoryTimers.put(id, victoryTimer);
                        hadTargetLastTick.put(id, false);
                    } else {
                        voiceManager.setState(Combat.INSTANCE);
                    }
                } else {
                    voiceManager.setState(Passive.INSTANCE);
                }

                hadTargetLastTick.put(id, hasTarget);
            }
        }
    }
}
