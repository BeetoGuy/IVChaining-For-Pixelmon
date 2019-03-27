package ivchain.event;

import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.api.events.BeatWildPixelmonEvent;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.IVStore;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import ivchain.IVChain;
import ivchain.capability.CapabilityChainTracker;
import ivchain.capability.IChainTracker;
import ivchain.util.ChainingHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class IVChainEventHandler {
    public static class ForgeHandler {
        @SubscribeEvent
        public void attachCapability(AttachCapabilitiesEvent<Entity> evt) {
            if (evt.getObject() instanceof EntityPlayerMP) {
                evt.addCapability(ChainingHandler.CHAINING_LOCATION, new ICapabilitySerializable<NBTTagCompound>() {
                    IChainTracker tracker = CapabilityChainTracker.CHAIN_TRACKER.getDefaultInstance();

                    @Override
                    public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
                        return cap == CapabilityChainTracker.CHAIN_TRACKER;
                    }

                    @Override
                    public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
                        return cap == CapabilityChainTracker.CHAIN_TRACKER ? CapabilityChainTracker.CHAIN_TRACKER.cast(tracker) : null;
                    }

                    @Override
                    public NBTTagCompound serializeNBT() {
                        return ((NBTTagCompound) CapabilityChainTracker.CHAIN_TRACKER.getStorage().writeNBT(CapabilityChainTracker.CHAIN_TRACKER, tracker, null));
                    }

                    @Override
                    public void deserializeNBT(NBTTagCompound tag) {
                        CapabilityChainTracker.CHAIN_TRACKER.getStorage().readNBT(CapabilityChainTracker.CHAIN_TRACKER, tracker, null, tag);
                    }
                });
            }
        }

        @SubscribeEvent
        public void onPlayerClone(PlayerEvent.Clone evt) {
            if (!evt.isWasDeath() && evt.getEntityPlayer() instanceof EntityPlayerMP && evt.getOriginal() instanceof EntityPlayerMP) {
                NBTTagCompound tag = new NBTTagCompound();
                ChainingHandler.getPlayer((EntityPlayerMP) evt.getOriginal()).writeNBTValue(tag);
                ChainingHandler.getPlayer((EntityPlayerMP) evt.getEntityPlayer()).readNBTValue(tag);
            }
        }
    }

    public static class PixelHandler {
        @SubscribeEvent
        public void onPixelmonDefeat(BeatWildPixelmonEvent evt) {
            WildPixelmonParticipant pixelmon = evt.wpp;
            String name = pixelmon.getDisplayName();
            EntityPlayerMP player = evt.player;
            advanceChain(player, name);
        }

        @SubscribeEvent
        public void onPixelmonCatch(CaptureEvent.SuccessfulCapture evt) {
            EntityPixelmon pixelmon = evt.getPokemon();
            String name = pixelmon.getPixelmonWrapper().getNickname();
            EntityPlayerMP player = evt.player;
            advanceChain(player, name);
            if (getPlayer(player) != null) {
                byte chain = getPlayer(player).getChainValue();
                int guaranteedIVs = chain > 29 ? 4 : chain > 19 ? 3 : chain > 9 ? 2 : chain > 4 ? 1 : 0;
                if (guaranteedIVs > 0) {
                    List<StatsType> types = Lists.newArrayList(StatsType.HP, StatsType.Attack, StatsType.Defence, StatsType.SpecialAttack, StatsType.SpecialDefence, StatsType.Speed);
                    for (int i = 0; i < guaranteedIVs; i++) {
                        int place = IVChain.instance.rand.nextInt(types.size());
                        pixelmon.getPixelmonWrapper().getStats().ivs.set(types.get(place), IVStore.MAX_IVS);
                        types.remove(place);
                    }
                    evt.setPokemon(pixelmon);
                }
            }
        }

        private void advanceChain(EntityPlayerMP player, String pixelmonName) {
            if (getPlayer(player) != null) {
                String chain = getPlayer(player).getChainName();
                boolean continuesChain = pixelmonName.equals(chain);
                getPlayer(player).incrementChainValue(!continuesChain);
                if (!continuesChain) {
                    getPlayer(player).setChainName(pixelmonName);
                }
            }
        }

        private IChainTracker getPlayer(EntityPlayerMP player) {
            return player.getCapability(CapabilityChainTracker.CHAIN_TRACKER, EnumFacing.UP);
        }
    }
}
