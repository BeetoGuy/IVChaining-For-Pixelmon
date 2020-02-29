package ivchain;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class IVConfig {
    private static Configuration config;
    public static boolean easyMode = false;
    public static boolean chainHA = true;
    public static boolean grassEncounterShinyIncrease = true;
    public static boolean legendaryIVs = false;
    public static int tier1HAChain = 10;


    public static void init(File file) {
        config = new Configuration(file);
        easyMode = config.getBoolean("EasyMode", "General", easyMode, "If enabled, the chaining handler skips IVs that were already rolled to be perfect.");
        chainHA = config.getBoolean("ChainHA", "General", chainHA, "If enabled, IV chaining will increase chances of a Pokemon having its Hidden Ability.");
        grassEncounterShinyIncrease = config.getBoolean("GrassShinyIncrase", "General", grassEncounterShinyIncrease, "If enabled, encounters from tall grass have a higher shiny chance if Pokemon is part of chain.");
        legendaryIVs = config.getBoolean("LegendaryIVChaining", "General", legendaryIVs, "If enabled, legendaries generate extra perfect IVs if chained. (WARNING: Will take a long time to chain up)");
        if (config.hasChanged())
            config.save();
    }
}
