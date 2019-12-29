package ivchain.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public class CapabilityChainTracker {
    @CapabilityInject(IChainTracker.class)
    public static Capability<IChainTracker> CHAIN_TRACKER = null;

    public static class ChainTrackingImpl<T extends IChainTracker> implements Capability.IStorage<IChainTracker> {
        @Override
        public NBTBase writeNBT(Capability<IChainTracker> cap, IChainTracker tracker, EnumFacing side) {
            return tracker.writeNBTValue(new NBTTagCompound());
        }

        @Override
        public void readNBT(Capability<IChainTracker> cap, IChainTracker tracker, EnumFacing side, NBTBase nbt) {
            tracker.readNBTValue((NBTTagCompound)nbt);
        }
    }

    public static class ChainTrackerDefault implements IChainTracker {
        private String chainName = "";
        private byte chainValue = 0;

        @Override
        public String getChainName() {
            return chainName;
        }

        @Override
        public void setChainName(String name) {
            chainName = name;
        }

        @Override
        public byte getChainValue() {
            return chainValue;
        }

        @Override
        public void incrementChainValue(boolean reset) {
            if (reset)
                chainValue = 1;
            else if (chainValue < 30)
                chainValue++;
        }

        @Override
        public void setChainValue(byte value) {
            chainValue = (byte)Math.max(0, value);
        }

        @Override
        public boolean isDirty() {
            return false;
        }

        @Override
        public void incrementEncounterValue(short value) {

        }

        @Override
        public short getEncounterValue(short value) {
            return 0;
        }

        @Override
        public NBTTagCompound getPokedexMap() {
            return new NBTTagCompound();
        }

        @Override
        public void readPokedexMap(NBTTagCompound tag) {}
    }
}
