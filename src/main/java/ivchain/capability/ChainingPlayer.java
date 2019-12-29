package ivchain.capability;

import com.google.common.collect.Maps;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;

public class ChainingPlayer implements IChainTracker {
    private String chainName = "";
    private byte chainValue = 0;
    private Map<Short, Short> DEXNAV = Maps.newHashMap();
    private boolean isDirty = false;

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
        else if (chainValue < 127)
            chainValue++;
    }

    @Override
    public void setChainValue(byte value) {
        chainValue = (byte)Math.max(0, value);
    }

    @Override
    public void incrementEncounterValue(short natDex) {
        if (natDex > 0) {
            if (!DEXNAV.containsKey(natDex)) {
                DEXNAV.put(natDex, (short) 1);
                isDirty = true;
            }
            else if (DEXNAV.get(natDex) < 999) {
                short chain = (short)(DEXNAV.get(natDex) + 1);
                DEXNAV.put(natDex, chain);
                isDirty = true;
            }
        }
    }

    @Override
    public short getEncounterValue(short natDex) {
        return DEXNAV.containsKey(natDex) ? DEXNAV.get(natDex) : 0;
    }

    @Override
    public NBTTagCompound getPokedexMap() {
        NBTTagCompound tag = new NBTTagCompound();
        if (!DEXNAV.isEmpty()) {
            int[] array = new int[809];
            for (int i = 0; i < array.length; i++) {
                short natDex = (short)(1 + i);
                array[i] = DEXNAV.containsKey(natDex) ? DEXNAV.get(natDex) : 0;
            }
            tag.setIntArray("NatDex", array);
            /*
            for (short key : DEXNAV.keySet()) {
                tag.setShort("" + key, DEXNAV.get(key));
            }*/
        }
        return tag;
    }

    @Override
    public void readPokedexMap(NBTTagCompound tag) {
        if (tag.hasKey("NatDex")) {
            int[] array = tag.getIntArray("NatDex");
            for (int i = 0; i < array.length; i++) {
                short natDex = (short)(1 + i);
                if (array[i] > 0) {
                    DEXNAV.put(natDex, (short)array[i]);
                }
            }
        }
        /*
        for (String key : tag.getKeySet()) {
            short natDex = Short.parseShort(key);
            DEXNAV.put(natDex, tag.getShort(key));
        }*/
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }
}
