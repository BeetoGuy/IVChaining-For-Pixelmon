package ivchain.util;

import ivchain.capability.CapabilityChainTracker;
import ivchain.capability.IChainTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class ChainingHandler {
    public static final ResourceLocation CHAINING_LOCATION = new ResourceLocation("ivchain", "chaining");

    public static IChainTracker getPlayer(EntityPlayerMP player) {
        return player.getCapability(CapabilityChainTracker.CHAIN_TRACKER, EnumFacing.UP);
    }
}
