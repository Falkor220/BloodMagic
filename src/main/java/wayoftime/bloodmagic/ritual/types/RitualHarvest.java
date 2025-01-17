package wayoftime.bloodmagic.ritual.types;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import net.minecraft.block.BlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.ritual.AreaDescriptor;
import wayoftime.bloodmagic.ritual.EnumRuneType;
import wayoftime.bloodmagic.ritual.IMasterRitualStone;
import wayoftime.bloodmagic.ritual.Ritual;
import wayoftime.bloodmagic.ritual.RitualComponent;
import wayoftime.bloodmagic.ritual.RitualRegister;
import wayoftime.bloodmagic.ritual.harvest.HarvestRegistry;
import wayoftime.bloodmagic.ritual.harvest.IHarvestHandler;

/**
 * This ritual uses registered {@link IHarvestHandler}'s to harvest blocks.
 * <p>
 * To register a new Handler for this ritual use
 * {@link HarvestRegistry#registerHandler(IHarvestHandler)}
 * <p>
 * This ritual includes a way to change the range based on what block is above
 * the MasterRitualStone. You can use
 * {@link HarvestRegistry#registerRangeAmplifier(BlockState, int)} to register a
 * new amplifier.
 */
@RitualRegister("harvest")
public class RitualHarvest extends Ritual
{
	public static final String HARVEST_RANGE = "harvestRange";

	public RitualHarvest()
	{
		super("ritualHarvest", 0, 20000, "ritual." + BloodMagic.MODID + ".harvestRitual");
		addBlockRange(HARVEST_RANGE, new AreaDescriptor.Rectangle(new BlockPos(-4, 1, -4), 9, 5, 9));
		setMaximumVolumeAndDistanceOfRange(HARVEST_RANGE, 0, 15, 15);
	}

	@Override
	public void performRitual(IMasterRitualStone masterRitualStone)
	{
		World world = masterRitualStone.getWorldObj();
		BlockPos pos = masterRitualStone.getMasterBlockPos();

		if (masterRitualStone.getOwnerNetwork().getCurrentEssence() < getRefreshCost())
		{
			masterRitualStone.getOwnerNetwork().causeNausea();
			return;
		}

		int harvested = 0;

		AreaDescriptor harvestArea = masterRitualStone.getBlockRange(HARVEST_RANGE);

		harvestArea.resetIterator();
		while (harvestArea.hasNext())
		{
			BlockPos nextPos = harvestArea.next().add(pos);
			if (harvestBlock(world, nextPos, masterRitualStone.getMasterBlockPos()))
			{
				harvested++;
			}
		}

		masterRitualStone.getOwnerNetwork().syphon(masterRitualStone.ticket(getRefreshCost() * harvested));
	}

	@Override
	public int getRefreshCost()
	{
		return 20;
	}

	@Override
	public int getRefreshTime()
	{
		return 5;
	}

	@Override
	public void gatherComponents(Consumer<RitualComponent> components)
	{
		addCornerRunes(components, 1, 0, EnumRuneType.DUSK);
		addParallelRunes(components, 2, 0, EnumRuneType.EARTH);
		addOffsetRunes(components, 3, 1, 0, EnumRuneType.EARTH);
		addOffsetRunes(components, 3, 2, 0, EnumRuneType.WATER);
	}

	@Override
	public Ritual getNewCopy()
	{
		return new RitualHarvest();
	}

	public static boolean harvestBlock(World world, BlockPos cropPos, BlockPos controllerPos)
	{
		BlockState harvestState = world.getBlockState(cropPos);
		TileEntity potentialInventory = world.getTileEntity(controllerPos.up());
		IItemHandler itemHandler = null;
		if (potentialInventory != null && potentialInventory.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).isPresent())
			itemHandler = potentialInventory.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).resolve().get();

		for (IHarvestHandler handler : HarvestRegistry.getHarvestHandlers())
		{
			if (handler.test(world, cropPos, harvestState))
			{
				List<ItemStack> drops = Lists.newArrayList();
				if (handler.harvest(world, cropPos, harvestState, drops))
				{
					for (ItemStack stack : drops)
					{
						if (stack.isEmpty())
							continue;

						// TODO I wrote this, but didn't actually think about whether it should be a
						// thing. Remove the true if we want to keep it
						if (itemHandler == null || true)
							InventoryHelper.spawnItemStack(world, cropPos.getX(), cropPos.getY(), cropPos.getZ(), stack);
						else
						{
							ItemStack remainder = ItemHandlerHelper.insertItemStacked(itemHandler, stack, false);
							if (!remainder.isEmpty())
								InventoryHelper.spawnItemStack(world, cropPos.getX(), cropPos.getY(), cropPos.getZ(), remainder);
						}
					}
					return true;
				}
			}
		}

		return false;
	}
}
