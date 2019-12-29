package ivchain.item;

import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import ivchain.IVConfig;
import ivchain.IVRegistry;
import ivchain.capability.CapabilityChainTracker;
import ivchain.capability.IChainTracker;
import ivchain.event.IVChainEventHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemDexNav extends Item {
    public ItemDexNav() {
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack itemstack, EntityPlayer player, EntityLivingBase entity, EnumHand hand) {
        if (player != null && !toggleMode(player, itemstack, false) && !player.world.isRemote && IVConfig.dexNav && entity instanceof EntityPixelmon) {
            EntityPixelmon pixelmon = (EntityPixelmon)entity;
            if (pixelmon.getOwner() == null) {
                player.sendStatusMessage(IVChainEventHandler.PixelHandler.getPokemonInformation(player, pixelmon.getPokemonData()), true);
                return true;
            }
        }
        return false;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && stack.getItem() == IVRegistry.dexNav) {
            IChainTracker tracker = player.getCapability(CapabilityChainTracker.CHAIN_TRACKER, EnumFacing.UP);
            boolean tracking;
            if (player.isSneaking()) {
                tracking = toggleMode(player, stack, true);
            } else {
                tracking = toggleMode(player, stack, false);
            }
            if (tracking && tracker != null && tracker.getChainValue() > 0) {
                player.sendStatusMessage(new TextComponentTranslation("ivchain.dexnav.chaincheck", tracker.getChainName(), tracker.getChainValue()), true);
            }
        }
        return new ActionResult<>(EnumActionResult.FAIL, stack);
    }

    private boolean toggleMode(EntityPlayer player, ItemStack stack, boolean toggle) {
        NBTTagCompound tag;
        boolean trackerMode = false;
        if (stack.hasTagCompound()) {
            tag = stack.getTagCompound();
            trackerMode = tag.getBoolean("IsTracking");
        } else {
            tag = new NBTTagCompound();
            tag.setBoolean("IsTracking", trackerMode);
            stack.setTagCompound(tag);
        }

        if (toggle) {
            tag.setBoolean("IsTracking", !trackerMode);
            trackerMode = !trackerMode;
            String message;
            if (!trackerMode) {
                message = "ivchain.dexnav.trackeroff";
            } else {
                message = "ivchain.dexnav.trackeron";
            }
            player.sendStatusMessage(new TextComponentTranslation(message), false);
        }
        return trackerMode;
    }
}
