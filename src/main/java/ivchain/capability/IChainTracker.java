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
        //tag.setTag("Pokedex", getPokedexMap());
        return tag;
    }

    default void readNBTValue(NBTTagCompound tag) {
        if (tag.hasKey("ChainName"))
            setChainName(tag.getString("ChainName"));
        if (tag.hasKey("ChainValue"))
            setChainValue(tag.getByte("ChainValue"));/*
        if (tag.hasKey("Pokedex"))
            readPokedexMap(tag.getCompoundTag("Pokedex"));*/
    }

    void incrementEncounterValue(short natDex);

    short getEncounterValue(short natDex);

    NBTTagCompound getPokedexMap();

    void readPokedexMap(NBTTagCompound tag);

    boolean isDirty();
}
