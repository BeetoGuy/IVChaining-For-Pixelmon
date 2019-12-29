package ivchain.util;

import ivchain.capability.CapabilityChainTracker;
import ivchain.capability.IChainTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.DimensionManager;

import java.io.*;

public class DexNavSaveManager {
    private EntityPlayerMP player;

    public DexNavSaveManager(EntityPlayerMP player) {
        this.player = player;
    }

    public IChainTracker getTracker() {
        return player.getCapability(CapabilityChainTracker.CHAIN_TRACKER, EnumFacing.UP);
    }

    public void writeToFile() {
        if (getTracker().isDirty()) {
            File file = getFile();
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try {
                CompressedStreamTools.safeWrite(getTracker().getPokedexMap(), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void readFromFile() {
        if (getFile().exists()) {
            try {
                DataInputStream dataStream = new DataInputStream(new FileInputStream(getFile()));
                Throwable th = null;
                try {
                    NBTTagCompound tag = CompressedStreamTools.read(dataStream);
                    getTracker().readPokedexMap(tag);
                } catch (Throwable thro) {
                    th = thro;
                } finally {
                    if (dataStream != null) {
                        if (th != null) {
                            try {
                                dataStream.close();
                            } catch (Throwable thro) {
                                th.addSuppressed(thro);
                            }
                        } else {
                            dataStream.close();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public File getFile() {
        return new File(DimensionManager.getCurrentSaveRootDirectory(), "dexnav/" + player.getUniqueID().toString() + ".dat");
    }
}
