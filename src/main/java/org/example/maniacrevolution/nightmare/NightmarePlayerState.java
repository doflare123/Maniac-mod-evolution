package org.example.maniacrevolution.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class NightmarePlayerState {
    float sanity = NightmareConfig.MAX_SANITY;
    long lastGazeTick = Long.MIN_VALUE;
    long abductionCooldownUntil;
    int mazeTrialsStarted;
    NightmareTrialType trialType = NightmareTrialType.NONE;
    UUID mazeId;
    BlockPos returnPos;
    BlockPos exitPos;
    BlockPos raceFinishPos;
    BlockPos cocoonPos;
    ServerLevel returnLevel;
    TrialArea trialArea;
    long trialEndsAt;
    long raceStartsAt;
    List<ItemStack> savedMainInventory;
    List<ItemStack> savedArmorInventory;
    List<ItemStack> savedOffhandInventory;

    boolean isInTrial() {
        return trialType != NightmareTrialType.NONE;
    }

    void clearTrial() {
        trialType = NightmareTrialType.NONE;
        mazeId = null;
        returnPos = null;
        exitPos = null;
        raceFinishPos = null;
        cocoonPos = null;
        returnLevel = null;
        trialArea = null;
        trialEndsAt = 0L;
        raceStartsAt = 0L;
    }
}
