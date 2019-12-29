package ivchain.event;

import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.*;
import com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent;
import com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant;
import com.pixelmonmod.pixelmon.config.PixelmonConfig;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.BaseStats;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.IVStore;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import com.pixelmonmod.pixelmon.enums.EnumShinyCharm;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.worldGeneration.dimension.ultraspace.UltraSpace;
import ivchain.IVChain;
import ivchain.IVConfig;
import ivchain.IVRegistry;
import ivchain.capability.CapabilityChainTracker;
import ivchain.capability.IChainTracker;
import ivchain.util.ChainingHandler;
import ivchain.util.DexNavSaveManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentBase;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
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
                NBTTagCompound t = ChainingHandler.getPlayer((EntityPlayerMP) evt.getOriginal()).getPokedexMap();
                ChainingHandler.getPlayer((EntityPlayerMP) evt.getEntityPlayer()).readPokedexMap(t);
            }
        }

        @SubscribeEvent
        public void onPlayerLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent evt) {
            if (evt.player instanceof EntityPlayerMP) {
                DexNavSaveManager man = new DexNavSaveManager((EntityPlayerMP) evt.player);
                man.readFromFile();
            }
        }

        @SubscribeEvent
        public void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent evt) {
            if (evt.player instanceof EntityPlayerMP) {
                DexNavSaveManager man = new DexNavSaveManager((EntityPlayerMP) evt.player);
                man.writeToFile();
            }
        }
    }

    public static class PixelHandler {
        private static final StatsType[] STATS_TYPES = {StatsType.HP, StatsType.Attack, StatsType.Defence, StatsType.SpecialAttack, StatsType.SpecialDefence, StatsType.Speed};

        @SubscribeEvent
        public void onGrassEncounter(PixelmonBlockStartingBattleEvent evt) {
            if (!IVConfig.grassEncounterShinyIncrease) return;
            if (evt.wildPixelmon1 != null) {
                if (IVConfig.dexNav && playerIsHoldingDexNav(evt.player)) {
                    doDexNavBonuses(evt.player, evt.wildPixelmon1);
                }
                if (doShinyCheck(evt.wildPixelmon1, evt.player, evt.worldIn)) {
                    evt.wildPixelmon1.getPokemonData().setShiny(true);
                }
                doIVAllocation(evt.wildPixelmon1, evt.player, evt.wildPixelmon1.getPokemonName());
                doHAConversion(evt.wildPixelmon1, evt.player, evt.wildPixelmon1.getPokemonName());
            }
            if (evt.wildPixelmon2 != null) {
                if (IVConfig.dexNav && playerIsHoldingDexNav(evt.player)) {
                    doDexNavBonuses(evt.player, evt.wildPixelmon2);
                }
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
                if (IVConfig.dexNav && tracker != null && playerIsHoldingDexNav(evt.player)) {
                    doDexNavBonuses(evt.player, pixelmon);
                }
                doIVAllocation(pixelmon, evt.player, pixelmon.getPokemonName());
                doHAConversion(pixelmon, evt.player, pixelmon.getPokemonName());
                if (doShinyCheck(pixelmon, evt.player, evt.player.world)) {
                    pixelmon.getPokemonData().setShiny(true);
                }
            }
        }
/*
        @SubscribeEvent
        public void onPixelmonEncountered(BattleStartedEvent evt) {
            if (!IVConfig.chainHA) return;
            //Ensure that this is a PLAYER-WILD BATTLE
            if (evt.bc.containsParticipantType(WildPixelmonParticipant.class) && evt.bc.containsParticipantType(PlayerParticipant.class)) {
                BattleParticipant wild = evt.participant1[0] instanceof WildPixelmonParticipant ? evt.participant1[0] : evt.participant2[0];
                EntityPlayerMP player = evt.bc.getPlayers().get(0).player;
                PixelmonWrapper pixel = wild.controlledPokemon.get(0);
                String name = pixel.getPokemonName();
                if (!pixel.entity.getEntityData().hasKey("AttemptedHA") && getPlayer(player).getChainName().matches(name)) {
                    pixel.entity.getEntityData().setBoolean("AttemptedHA", true);
                    if (pixel.pokemon.getAbilitySlot() != 2 && canHiddenAbility(player))
                    wild.controlledPokemon.get(0).pokemon.setAbilitySlot(2);
                }
            }
        }*/

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
            advanceChain(player, name);/*
            if (getPlayer(player) != null) {
                byte chain = getPlayer(player).getChainValue();
                int guaranteedIVs = chain > 29 ? 4 : chain > 19 ? 3 : chain > 9 ? 2 : chain > 4 ? 1 : 0;
                if (guaranteedIVs > 0) {
                    List<StatsType> types = Lists.newArrayList();

                    for (StatsType type : STATS_TYPES) {
                        //If we want to skip already-perfect IVs, then we just don't add them to the list.
                        if (IVConfig.easyMode && pixelmon.getPokemonData().getStats().ivs.get(type) == IVStore.MAX_IVS)
                            continue;
                        types.add(type);
                    } //If all the IVs are perfect, then this isn't worth going through.
                    if (types.isEmpty()) return;
                    for (int i = 0; i < guaranteedIVs && !types.isEmpty(); i++) {
                        int place = types.size() == 1 ? 0 : IVChain.instance.rand.nextInt(types.size());
                        pixelmon.getPokemonData().getStats().ivs.set(types.get(place), IVStore.MAX_IVS);
                        types.remove(place);
                    }
                    evt.setPokemon(pixelmon);
                }
            }*/
        }

        @SubscribeEvent
        public void onLegendarySpawn(LegendarySpawnEvent.DoSpawn evt) {
            if (evt.action.spawnLocation.cause instanceof EntityPlayerMP) {
                EntityPixelmon pixelmon = evt.action.getOrCreateEntity();
                EntityPlayerMP player = (EntityPlayerMP)evt.action.spawnLocation.cause;
                World world = player.getEntityWorld();
                if (IVConfig.dexNav && playerIsHoldingDexNav(player)) {
                    doDexNavBonuses(player, pixelmon);
                }
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
                if (IVConfig.dexNav && playerIsHoldingDexNav(player)) {
                    doDexNavBonuses(player, pixelmon);
                }
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
                    /*
                    List<StatsType> types = Lists.newArrayList();
                    for (StatsType type : STATS_TYPES) {
                        //If we want to skip already-perfect IVs, then we just don't add them to the list.
                        if (IVConfig.easyMode && pixelmon.getPokemonData().getStats().ivs.get(type) == IVStore.MAX_IVS)
                            continue;
                        types.add(type);
                    } //If all the IVs are perfect, then this isn't worth going through.
                    if (types.isEmpty()) return;
                    for (int i = 0; i < guaranteedIVs && !types.isEmpty(); i++) {
                        int place = types.size() == 1 ? 0 : IVChain.instance.rand.nextInt(types.size());
                        pixelmon.getPokemonData().getStats().ivs.set(types.get(place), IVStore.MAX_IVS);
                        types.remove(place);
                    }
                    pixelmon.updateStats();*/
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
                if (Pixelmon.storageManager.getParty(player).getShinyCharmState().isActive())
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
                if (IVConfig.dexNav)
                    getPlayer(player).incrementEncounterValue((short) EnumSpecies.getPokedexNumber(pixelmonName));
            }
        }

        private void doDexNavBonuses(EntityPlayerMP player, EntityPixelmon pixelmon) {
            if (pixelmon != null) {
                if (EnumSpecies.legendaries.contains(pixelmon.getSpecies().getPokemonName()) || EnumSpecies.ultrabeasts.contains(pixelmon.getSpecies().getPokemonName()) || pixelmon.isBossPokemon())
                    return;
                Pokemon poke = pixelmon.getPokemonData();
                IChainTracker tracker = getPlayer(player);
                Random rand = IVChain.instance.rand;
                if (rand.nextInt(20) == 0) {
                    poke.setLevel(Math.min(100, poke.getLevel() + 10));
                }
                short searchLevel = tracker.getEncounterValue((short) poke.getSpecies().getNationalPokedexInteger());
                short shinyProbability = getShinyProbability(searchLevel);
                if (rand.nextDouble() < (shinyProbability * 0.01D) / 10000D) {
                    int attempts = rand.nextInt(100) < 4 ? 4 : 1;
                    if (Pixelmon.storageManager.getParty(player).getShinyCharmState().isActive())
                        attempts += 2;
                    int shinyRate = Math.round(PixelmonConfig.getShinyRate(player.dimension));
                    if (shinyRate > 0) {
                        for (int i = 0; i < attempts; i++) {
                            if (rand.nextInt(shinyRate) == 0) {
                                poke.setShiny(true);
                                break;
                            }
                        }
                    }
                }
                byte searchStage = getSearchStage(searchLevel);
                int chance = rand.nextInt(100);
                if (threestar[searchStage] > chance) {
                    doIVAllocation(pixelmon, 3);
                } else if (twostar[searchStage] > chance) {
                    doIVAllocation(pixelmon, 2);
                } else if (onestar[searchStage] > chance) {
                    doIVAllocation(pixelmon, 1);
                }
                chance = rand.nextInt(100);
                if (eggmove[searchStage] > chance) {
                    BaseStats stats = pixelmon.getPokemonData().getBaseStats();
                    if (stats != null && stats.getEggMoves() != null) {
                        ArrayList<Attack> eggMoves = stats.getEggMoves();
                        if (!eggMoves.isEmpty())
                            pixelmon.getPokemonData().getMoveset().set(0, eggMoves.get(rand.nextInt(eggMoves.size())));
                    }
                }
                chance = rand.nextInt(100);
                if (hiddenability[searchStage] > chance) {
                    pixelmon.getPokemonData().setAbilitySlot(2);
                }
            }
        }

        private byte[] onestar = {0, 14, 17, 17, 15, 8};
        private byte[] twostar = {0, 1, 9, 16, 17, 24};
        private byte[] threestar = {0, 0, 1, 7, 6, 12};
        private byte[] eggmove = {21, 46, 58, 63, 65, 83};
        private byte[] hiddenability = {0, 0, 5, 15, 20, 23};

        private byte getSearchStage(short searchLevel) {
            if (searchLevel > 99) return 5;
            else if (searchLevel > 49) return 4;
            else if (searchLevel > 24) return 3;
            else if (searchLevel > 9) return 2;
            else if (searchLevel > 4) return 1;
            else return 0;
        }

        private short getShinyProbability(short searchLevel) {
            if (searchLevel > 200) {
                short newLevel = (short)(searchLevel - 200);
                return (short)(800 + newLevel);
            } else if (searchLevel > 100) {
                short newLevel = (short)(searchLevel - 100);
                newLevel *= 2;
                return (short)(600 + newLevel);
            } else {
                return (short)(searchLevel * 6);
            }
        }

        private static IChainTracker getPlayer(EntityPlayer player) {
            return player.getCapability(CapabilityChainTracker.CHAIN_TRACKER, EnumFacing.UP);
        }

        private boolean isUltraSpace(EntityPlayerMP player) {
            return player.dimension == UltraSpace.DIM_ID;
        }

        private boolean playerIsHoldingDexNav(EntityPlayer player) {
            return player.getHeldItemMainhand().getItem() == IVRegistry.dexNav || player.getHeldItemOffhand().getItem() == IVRegistry.dexNav;
        }

        public static TextComponentBase getPokemonInformation(EntityPlayer player, Pokemon poke) {
            IChainTracker tracker = getPlayer(player);
            String info = I18n.translateToLocalFormatted("ivchain.dexnav.pokecheck.0");
            if (tracker != null) {
                short track = tracker.getEncounterValue((short)poke.getSpecies().getNationalPokedexInteger());
                if (track > 0)
                    info = track >= 5 ? getFullInformation(player, poke) : track >= 3 ? getThirdStageInformation(player, poke) : track == 2 ? getSecondStageInformation(player, poke) : getFirstStageInformation(player, poke);
            }
            return new TextComponentString(info);
        }

        private static String getFirstStageInformation(EntityPlayer player, Pokemon poke) {
            return I18n.translateToLocalFormatted("ivchain.dexnav.pokecheck.1", poke.getDisplayName(), getPlayer(player).getEncounterValue((short)poke.getSpecies().getNationalPokedexInteger()));
        }

        private static String getSecondStageInformation(EntityPlayer player, Pokemon poke) {
            String name = poke.getDisplayName();
            String move = poke.getMoveset().get(0).getMove().getLocalizedName();
            return I18n.translateToLocalFormatted("ivchain.dexnav.pokecheck.2", name, getPlayer(player).getEncounterValue((short)poke.getSpecies().getNationalPokedexInteger()), move);
        }

        private static String getThirdStageInformation(EntityPlayer player, Pokemon poke) {
            String name = poke.getDisplayName();
            String move = poke.getMoveset().get(0).getMove().getLocalizedName();
            String ability = poke.getAbilityName();
            return I18n.translateToLocalFormatted("ivchain.dexnav.pokecheck.3", name, getPlayer(player).getEncounterValue((short)poke.getSpecies().getNationalPokedexInteger()), move, ability);
        }

        private static String getFullInformation(EntityPlayer player, Pokemon poke) {
            String name = poke.getDisplayName();
            String move = poke.getMoveset().get(0).getMove().getLocalizedName();
            String IVs = getPerfectIVs(poke);
            String ability = poke.getAbilityName();
            return I18n.translateToLocalFormatted("ivchain.dexnav.pokecheck.4", name, getPlayer(player).getEncounterValue((short)poke.getSpecies().getNationalPokedexInteger()), move, ability, IVs);
        }

        private static String getPerfectIVs(Pokemon poke) {
            int perfect = 0;
            for (int stat : poke.getStats().ivs.getArray()) {
                if (stat == 31) {
                    perfect++;
                }
            }
            return perfect >= 3 ? I18n.translateToLocal("ivchain.dexnav.3star") : perfect == 2 ? I18n.translateToLocal("ivchain.dexnav.2star") : perfect == 1 ? I18n.translateToLocal("ivchain.dexnav.1star") : I18n.translateToLocal("ivchain.dexnav.0star");
        }
    }
}
