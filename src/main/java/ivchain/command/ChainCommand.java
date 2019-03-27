package ivchain.command;

import ivchain.capability.CapabilityChainTracker;
import ivchain.capability.IChainTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;

public class ChainCommand extends CommandBase {
    @Override
    public String getName() {
        return "ivchain";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "ivchain.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!sender.getEntityWorld().isRemote) {
            if (sender.getCommandSenderEntity() instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP)sender.getCommandSenderEntity();
                IChainTracker tracker = player.getCapability(CapabilityChainTracker.CHAIN_TRACKER, EnumFacing.UP);
                if (tracker != null) {
                    String entity = I18n.translateToLocal("ivchain.entity");
                    if (entity.equals("ivchain.entity")) {
                        sender.sendMessage(new TextComponentString("Chaining Current Pokemon: " + tracker.getChainName()));
                        sender.sendMessage(new TextComponentString("Current chain count: " + tracker.getChainValue()));
                    } else {
                        sender.sendMessage(new TextComponentTranslation("ivchain.entity", tracker.getChainName()));
                        sender.sendMessage(new TextComponentTranslation("ivchain.count", tracker.getChainValue()));
                    }
                }
                else {
                    sender.sendMessage(new TextComponentString("You don't have the chaining tracker?"));
                }
            }
        }
    }
}
