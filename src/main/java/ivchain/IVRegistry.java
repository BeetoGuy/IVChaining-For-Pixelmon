package ivchain;

import com.google.common.collect.Lists;
import ivchain.item.ItemDexNav;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = "ivchain")
public class IVRegistry {
    public static List<Item> ITEMS = Lists.newArrayList();

    public static Item dexNav = new ItemDexNav().setRegistryName(new ResourceLocation("ivchain", "dexnav")).setTranslationKey("ivchain:dexnav");

    public static void init() {
        addItem(dexNav);
    }

    private static void addItem(Item item) {
        ITEMS.add(item);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> evt) {
        ITEMS.forEach(evt.getRegistry()::register);
    }
}
