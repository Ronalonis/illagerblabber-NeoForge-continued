package com.ronalonis.illagerblabber.mixin;

import com.ronalonis.illagerblabber.voice.IllagerType;
import com.ronalonis.illagerblabber.voice.IllagerVoiceRegistry;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Raider.class})
public class RaiderMixin {
    public RaiderMixin() {
    }

    @Inject(
            method = {"aiStep"},
            at = {@At("TAIL")}
    )
    private void onTickMovement(CallbackInfo ci) {
        if (!((Raider) (Object) this).level().isClientSide) {
            Object self = (Object) this;
            if (self instanceof Pillager) {
                Pillager pillager = (Pillager) self;
                IllagerVoiceRegistry.updateIllager(pillager, IllagerType.PILLAGER);
            } else if (self instanceof Evoker) {
                Evoker evoker = (Evoker) self;
                IllagerVoiceRegistry.updateIllager(evoker, IllagerType.EVOKER);
            } else if (self instanceof Vindicator) {
                Vindicator vindicator = (Vindicator) self;
                IllagerVoiceRegistry.updateIllager(vindicator, IllagerType.VINDICATOR);
            }
        }

    }
}
