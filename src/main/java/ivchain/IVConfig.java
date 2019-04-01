package ivchain;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class IVConfig {
    private static Configuration config;
    public static boolean easyMode = false;
    public static boolean chainHA = true;

    public static void init(File file) {
        config = new Configuration(file);
        easyMode = config.getBoolean("EasyMode", "General", easyMode, "If enabled, the chaining handler skips IVs that were already rolled to be perfect.");
        chainHA = config.getBoolean("ChainHA", "General", chainHA, "If enabled, IV chaining will increase chances of a Pokemon having its Hidden Ability.");
        if (config.hasChanged())
            config.save();
    }
}
