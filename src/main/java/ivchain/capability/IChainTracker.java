package ivchain.capability;

import net.minecraft.nbt.NBTTagCompound;

public interface IChainTracker {
    String getChainName();

    void setChainName(String name);

    byte getChainValue();

    void incrementChainValue(boolean chainBreak);

    void setChainValue(byte value);

    default NBTTagCompound writeNBTValue(NBTTagCompound tag) {
        tag.setString("ChainName", getChainName());
        tag.setByte("ChainValue", getChainValue());
        return tag;
    }

    default void readNBTValue(NBTTagCompound tag) {
        if (tag.hasKey("ChainName"))
            setChainName(tag.getString("ChainName"));
        if (tag.hasKey("ChainValue"))
            setChainValue(tag.getByte("ChainValue"));
    }
}
