package com.ronalonis.illagerblabber.mixin;

import com.ronalonis.illagerblabber.IllagerSounds;
import com.ronalonis.illagerblabber.voice.IllagerVoiceRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Pillager.class})
public abstract class PillagerMixin extends AbstractIllager {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("assets.illagerblabber");

    protected PillagerMixin(EntityType<? extends AbstractIllager> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(
            method = {"getHurtSound"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void onHurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        Pillager pillager = (Pillager) (Object) this;
        IllagerVoiceRegistry.setHurtState(pillager);
        cir.setReturnValue((SoundEvent) null);
    }

    @Inject(
            method = {"getCelebrateSound"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void onGetCelebratingSound(CallbackInfoReturnable<SoundEvent> cir) {
        LOGGER.info("PILLAGER RAID VICTORY DETECTED!");
        Pillager pillager = (Pillager) (Object) this;
        IllagerVoiceRegistry.setVictoryState(pillager);
        cir.setReturnValue((SoundEvent)IllagerSounds.SILENCE.get());
    }

    @Inject(
            method = {"getAmbientSound"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void onGetAmbientSound(CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue((SoundEvent) null);
    }
}
