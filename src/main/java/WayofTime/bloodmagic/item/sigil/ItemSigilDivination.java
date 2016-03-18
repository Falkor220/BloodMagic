package WayofTime.bloodmagic.item.sigil;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import WayofTime.bloodmagic.api.Constants;
import WayofTime.bloodmagic.api.altar.IBloodAltar;
import WayofTime.bloodmagic.api.iface.IAltarReader;
import WayofTime.bloodmagic.api.util.helper.NetworkHelper;
import WayofTime.bloodmagic.api.util.helper.PlayerHelper;
import WayofTime.bloodmagic.tile.TileIncenseAltar;
import WayofTime.bloodmagic.util.ChatUtil;
import WayofTime.bloodmagic.util.helper.TextHelper;

public class ItemSigilDivination extends ItemSigilBase implements IAltarReader
{
    public ItemSigilDivination()
    {
        super("divination");
        setRegistryName(Constants.BloodMagicItem.SIGIL_DIVINATION.getRegName());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
    {
        if (!world.isRemote)
        {
            RayTraceResult position = getMovingObjectPositionFromPlayer(world, player, false);

            if (position == null)
            {
                int currentEssence = NetworkHelper.getSoulNetwork(getOwnerUUID(stack)).getCurrentEssence();

                List<ITextComponent> toSend = new ArrayList<ITextComponent>();
                if (!getOwnerName(stack).equals(PlayerHelper.getUsernameFromPlayer(player)))
                    toSend.add(new TextComponentString(TextHelper.localize(tooltipBase + "otherNetwork", getOwnerName(stack))));
                toSend.add(new TextComponentString(TextHelper.localize(tooltipBase + "currentEssence", currentEssence)));
                ChatUtil.sendNoSpam(player, toSend.toArray(new ITextComponent[toSend.size()]));
            } else
            {
                if (position.typeOfHit == RayTraceResult.Type.BLOCK)
                {
                    TileEntity tile = world.getTileEntity(position.getBlockPos());

                    if (tile != null && tile instanceof IBloodAltar)
                    {
                        IBloodAltar altar = (IBloodAltar) tile;
                        int tier = altar.getTier().ordinal() + 1;
                        int currentEssence = altar.getCurrentBlood();
                        int capacity = altar.getCapacity();
                        altar.checkTier();
                        ChatUtil.sendNoSpam(player, TextHelper.localize(tooltipBase + "currentAltarTier", tier), TextHelper.localize(tooltipBase + "currentEssence", currentEssence), TextHelper.localize(tooltipBase + "currentAltarCapacity", capacity));
                    } else if (tile != null && tile instanceof TileIncenseAltar)
                    {
                        TileIncenseAltar altar = (TileIncenseAltar) tile;
                        altar.recheckConstruction();
                        double tranquility = altar.tranquility;
                        ChatUtil.sendNoSpam(player, TextHelper.localize(tooltipBase + "currentTranquility", (int) ((100D * (int) (100 * tranquility)) / 100d)), TextHelper.localize(tooltipBase + "currentBonus", (int) (100 * altar.incenseAddition)));
                    } else
                    {
                        int currentEssence = NetworkHelper.getSoulNetwork(getOwnerUUID(stack)).getCurrentEssence();
                        ChatUtil.sendNoSpam(player, TextHelper.localize(tooltipBase + "currentEssence", currentEssence));
                    }
                }
            }
        }

        return super.onItemRightClick(stack, world, player, hand);
    }
}
