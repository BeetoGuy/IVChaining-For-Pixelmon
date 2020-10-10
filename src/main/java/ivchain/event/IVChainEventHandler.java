package ivchain.event;

import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.*;
import com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent;
import com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent;
import com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant;
import com.pixelmonmod.pixelmon.config.PixelmonConfig;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.IVStore;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import com.pixelmonmod.pixelmon.worldGeneration.dimension.ultraspace.UltraSpace;
import ivchain.IVChain;
import ivchain.IVConfig;
import ivchain.capability.CapabilityChainTracker;
import ivchain.capability.IChainTracker;
import ivchain.util.ChainingHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
            if (evt.getEntityPlayer() instanceof EntityPlayerMP && evt.getOriginal() instanceof EntityPlayerMP) {
                NBTTagCompound tag = new NBTTagCompound();
                ChainingHandler.getPlayer((EntityPlayerMP) evt.getOriginal()).writeNBTValue(tag);
                ChainingHandler.getPlayer((EntityPlayerMP) evt.getEntityPlayer()).readNBTValue(tag);
            }
        }
    }

    public static class PixelHandler {
        private static final StatsType[] STATS_TYPES = {StatsType.HP, StatsType.Attack, StatsType.Defence, StatsType.SpecialAttack, StatsType.SpecialDefence, StatsType.Speed};

        @SubscribeEvent
        public void onGrassEncounter(PixelmonBlockStartingBattleEvent evt) {
            if (!IVConfig.grassEncounterShinyIncrease) return;
            if (evt.wildPixelmon1 != null) {
                if (doShinyCheck(evt.wildPixelmon1, evt.player, evt.worldIn)) {
                    evt.wildPixelmon1.getPokemonData().setShiny(true);
                }
                doIVAllocation(evt.wildPixelmon1, evt.player, evt.wildPixelmon1.getPokemonName());
                doHAConversion(evt.wildPixelmon1, evt.player, evt.wildPixelmon1.getPokemonName());
            }
            if (evt.wildPixelmon2 != null) {
                if (doShinyCheck(evt.wildPixelmon2, evt.player, evt.worldIn)) {
                    evt.wildPixelmon2.getPokemonData().setShiny(true);
                }
                doIVAllocation(evt.wildPixelmon2, evt.player, evt.wildPixelmon2.getPokemonName());
                doHAConversion(evt.wildPixelmon2, evt.player, evt.wildPixelmon2.getPokemonName());
            }
        }

        @SubscribeEvent
        public void onFishing(FishingEvent.Reel evt) {
            if (evt.isPokemon()) {
                EntityPixelmon pixelmon = (EntityPixelmon)evt.optEntity.get();
                IChainTracker tracker = getPlayer(evt.player);
                doIVAllocation(pixelmon, evt.player, pixelmon.getPokemonName());
                doHAConversion(pixelmon, evt.player, pixelmon.getPokemonName());
                if (doShinyCheck(pixelmon, evt.player, evt.player.world)) {
                    pixelmon.getPokemonData().setShiny(true);
                }
            }
        }

        @SubscribeEvent
        public void onPixelmonDefeat(BeatWildPixelmonEvent evt) {
            WildPixelmonParticipant pixelmon = evt.wpp;
            String name = pixelmon.controlledPokemon.get(0).getPokemonName();
            EntityPlayerMP player = evt.player;
            advanceChain(player, name);
        }

        @SubscribeEvent
        public void onPixelmonCatch(CaptureEvent.SuccessfulCapture evt) {
            EntityPixelmon pixelmon = evt.getPokemon();
            String name = pixelmon.getPokemonName();
            EntityPlayerMP player = evt.player;
            advanceChain(player, name);
        }

        @SubscribeEvent
        public void onLegendarySpawn(LegendarySpawnEvent.DoSpawn evt) {
            if (evt.action.spawnLocation.cause instanceof EntityPlayerMP) {
                EntityPixelmon pixelmon = evt.action.getOrCreateEntity();
                EntityPlayerMP player = (EntityPlayerMP)evt.action.spawnLocation.cause;
                World world = player.getEntityWorld();
                if (doShinyCheck(pixelmon, player, world)) {
                    evt.action.getOrCreateEntity().getPokemonData().setShiny(true);
                }
                String name = pixelmon.getPokemonName();
                if (IVConfig.legendaryIVs) {
                    doIVAllocation(pixelmon, player, name);
                }
                doHAConversion(pixelmon, player, name);
            }
        }

        @SubscribeEvent
        public void onPixelmonSpawn(SpawnEvent evt) {
            if (evt.action.getOrCreateEntity() instanceof EntityPixelmon && evt.action.spawnLocation.cause instanceof EntityPlayerMP) {
                EntityPixelmon pixelmon = (EntityPixelmon)evt.action.getOrCreateEntity();
                EntityPlayerMP player = (EntityPlayerMP)evt.action.spawnLocation.cause;
                World world = player.getEntityWorld();
                if (doShinyCheck(pixelmon, player, world)) {
                    ((EntityPixelmon)evt.action.getOrCreateEntity()).getPokemonData().setShiny(true);
                }
                String name = pixelmon.getPokemonName();
                doIVAllocation(pixelmon, player, name);
                doHAConversion(pixelmon, player, name);
            }
        }

        private void doHAConversion(EntityPixelmon pixelmon, EntityPlayerMP player, String name) {
            if (!IVConfig.chainHA) return;
            if (getPlayer(player).getChainName().equals(name)) {
                if (pixelmon.getPokemonData().getAbilitySlot() != 2 && canHiddenAbility(player))
                    pixelmon.getPokemonData().setAbilitySlot(2);
            }
        }

        private void doIVAllocation(EntityPixelmon pixelmon, EntityPlayerMP player, String name) {
            if (getPlayer(player).getChainName().equals(name)) {
                byte chain = getPlayer(player).getChainValue();
                int guaranteedIVs = chain > 29 ? 4 : chain > 19 ? 3 : chain > 9 ? 2 : chain > 4 ? 1 : 0;
                if (guaranteedIVs > 0) {
                    doIVAllocation(pixelmon, guaranteedIVs);
                }
            }
        }

        private void doIVAllocation(EntityPixelmon pixelmon, int rolls) {
            List<StatsType> types = Lists.newArrayList();
            for (StatsType type : STATS_TYPES) {
                //If we want to skip already-perfect IVs, then we just don't add them to the list.
                if (IVConfig.easyMode && pixelmon.getPokemonData().getStats().ivs.get(type) == IVStore.MAX_IVS)
                    continue;
                types.add(type);
            } //If all the IVs are perfect, then this isn't worth going through.
            if (types.isEmpty()) return;
            for (int i = 0; i < rolls && !types.isEmpty(); i++) {
                int place = types.size() == 1 ? 0 : IVChain.instance.rand.nextInt(types.size());
                pixelmon.getPokemonData().getStats().ivs.set(types.get(place), IVStore.MAX_IVS);
                types.remove(place);
            }
            pixelmon.updateStats();
        }

        private boolean doShinyCheck(EntityPixelmon pixelmon, EntityPlayerMP player, World world) {
            String name = pixelmon.getPokemonName();
            if (player != null && getPlayer(player).getChainName().equals(name)) {
                if (canGetShiny(player, world)) {
                    return true;
                }
            }
            return false;
        }

        private boolean canGetShiny(EntityPlayerMP player, World world) {
            byte chain = getPlayer(player).getChainValue();
            int chance = chain <= 10 ? 0 : chain <= 20 ? 5 : chain <= 30 ? 9 : 13;
            if (chance > 0) {
                if (Pixelmon.storageManager.getParty(player).getShinyCharm().isActive())
                    chance *= 3;
                int shinyChance = Math.round(PixelmonConfig.getShinyRate(world.provider.getDimension()));
                if (shinyChance > 0)
                    return IVChain.instance.rand.nextInt(shinyChance) <= chance;
            }
            return false;
        }

        private boolean canHiddenAbility(EntityPlayerMP player) {
            byte chain = getPlayer(player).getChainValue();
            int chance = chain < 10 ? 0 : chain < 20 ? 5 : chain < 30 ? 10 : 15;
            return chance > 0 && IVChain.instance.rand.nextInt(isUltraSpace(player) && PixelmonConfig.ultraSpaceHiddenAbilityModifier > 0.0F ? Math.max(Math.round(100 / PixelmonConfig.ultraSpaceHiddenAbilityModifier), 10) : 100) < chance;
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

        private static IChainTracker getPlayer(EntityPlayer player) {
            return player.getCapability(CapabilityChainTracker.CHAIN_TRACKER, EnumFacing.UP);
        }

        private boolean isUltraSpace(EntityPlayerMP player) {
            return player.dimension == UltraSpace.DIM_ID;
        }
    }
}
