package com.autofisher.mixin;

import com.autofisher.AutoFisherMod;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntity.class)
public abstract class MixinFishingBobberEntity {
    // Shadow the Yarn‐mapped int that counts ticks until catchable
    @Shadow private int waitCountdown;
    @Unique private int prevWaitCountdown = -1;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        FishingBobberEntity bobber = (FishingBobberEntity)(Object)this;
        World world = bobber.getWorld();
        if (!world.isClient) return;

        // when it transitions from >0 to 0 ⇒ a fish just bit
        if (prevWaitCountdown > 0 && waitCountdown == 0) {
            AutoFisherMod.onBobberBite(bobber);
        }
        prevWaitCountdown = waitCountdown;
    }
}
