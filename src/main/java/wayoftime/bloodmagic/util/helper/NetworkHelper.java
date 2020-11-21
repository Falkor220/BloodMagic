package wayoftime.bloodmagic.util.helper;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import wayoftime.bloodmagic.api.item.IBindable;
import wayoftime.bloodmagic.core.data.BMWorldSavedData;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.core.data.SoulNetwork;
import wayoftime.bloodmagic.core.data.SoulTicket;
import wayoftime.bloodmagic.core.registry.OrbRegistry;
import wayoftime.bloodmagic.event.SoulNetworkEvent;
import wayoftime.bloodmagic.orb.BloodOrb;
import wayoftime.bloodmagic.orb.IBloodOrb;

public class NetworkHelper
{
	@Nullable
	private static BMWorldSavedData dataHandler;

	/**
	 * Gets the SoulNetwork for the player.
	 *
	 * @param uuid - The UUID of the SoulNetwork owner - this is UUID.toString().
	 * @return - The SoulNetwork for the given name.
	 */
	public static SoulNetwork getSoulNetwork(String uuid)
	{
		if (dataHandler == null)
		{
			if (ServerLifecycleHooks.getCurrentServer() == null)
				return null;

			DimensionSavedDataManager savedData = ServerLifecycleHooks.getCurrentServer().func_241755_D_().getSavedData();
			dataHandler = savedData.getOrCreate(() -> new BMWorldSavedData(), BMWorldSavedData.ID);
		}

		return dataHandler.getNetwork(UUID.fromString(uuid));
	}

	/**
	 * @param uuid - The Player's Mojang UUID
	 * @see NetworkHelper#getSoulNetwork(String)
	 */
	public static SoulNetwork getSoulNetwork(UUID uuid)
	{
		return getSoulNetwork(uuid.toString());
	}

	/**
	 * @param player - The Player
	 * @see NetworkHelper#getSoulNetwork(String)
	 */
	public static SoulNetwork getSoulNetwork(PlayerEntity player)
	{
		return getSoulNetwork(PlayerHelper.getUUIDFromPlayer(player));
	}

	public static SoulNetwork getSoulNetwork(Binding binding)
	{
		return getSoulNetwork(binding.getOwnerId());
	}

	/**
	 * Gets the current orb tier of the SoulNetwork.
	 *
	 * @param soulNetwork - SoulNetwork to get the tier of.
	 * @return - The Orb tier of the given SoulNetwork
	 */
	public static int getCurrentMaxOrb(SoulNetwork soulNetwork)
	{
		return soulNetwork.getOrbTier();
	}

	public static int getMaximumForTier(int tier)
	{
		int ret = 0;

		if (tier > OrbRegistry.getTierMap().size() || tier < 0)
			return ret;

		for (ItemStack orbStack : OrbRegistry.getOrbsForTier(tier))
		{
			BloodOrb orb = ((IBloodOrb) orbStack.getItem()).getOrb(orbStack);
			if (orb.getCapacity() > ret)
				ret = orb.getCapacity();
		}

		return ret;
	}

	// Syphon

	/**
	 * Syphons from the player and damages them if there was not enough stored LP.
	 * <p>
	 * Handles null-checking the player for you.
	 *
	 * @param soulNetwork - SoulNetwork to syphon from
	 * @param user        - User of the item.
	 * @param toSyphon    - Amount of LP to syphon
	 * @return - Whether the action should be performed.
	 * @deprecated Use {@link #getSoulNetwork(PlayerEntity)} and
	 *             {@link SoulNetwork#syphonAndDamage$(PlayerEntity, SoulTicket)}
	 */
	@Deprecated
	public static boolean syphonAndDamage(SoulNetwork soulNetwork, PlayerEntity user, int toSyphon)
	{

//        if (soulNetwork.getNewOwner() == null)
//        {
//            soulNetwork.syphon(toSyphon);
//            return true;
//        }

		return soulNetwork.syphonAndDamage(user, toSyphon);
	}

	/**
	 * Syphons a player from within a container.
	 *
	 * @param stack  - ItemStack in the Container.
	 * @param ticket - SoulTicket to syphon
	 * @return - If the syphon was successful.
	 */
	public static boolean syphonFromContainer(ItemStack stack, SoulTicket ticket)
	{
		if (!(stack.getItem() instanceof IBindable))
			return false;

		Binding binding = ((IBindable) stack.getItem()).getBinding(stack);
		if (binding == null)
			return false;

		SoulNetwork network = getSoulNetwork(binding);
		SoulNetworkEvent.Syphon.Item event = new SoulNetworkEvent.Syphon.Item(network, ticket, stack);

		return !MinecraftForge.EVENT_BUS.post(event) && network.syphon(event.getTicket(), true) >= ticket.getAmount();
	}

	/**
	 * Checks if the ItemStack has a user to be syphoned from.
	 *
	 * @param stack    - ItemStack to check
	 * @param toSyphon - Amount of LP to syphon
	 * @return - If syphoning is possible
	 */
	public static boolean canSyphonFromContainer(ItemStack stack, int toSyphon)
	{
		if (!(stack.getItem() instanceof IBindable))
			return false;

		Binding binding = ((IBindable) stack.getItem()).getBinding(stack);
		if (binding == null)
			return false;

		SoulNetwork network = getSoulNetwork(binding);
		if (network == null)
			return false;

		return network.getCurrentEssence() >= toSyphon;
	}

	// Set

	/**
	 * Sets the orb tier of the SoulNetwork to the given orb. Will not set if the
	 * given tier is lower than the current tier.
	 *
	 * @param soulNetwork - SoulNetwork to set the orb tier of
	 * @param maxOrb      - Tier of orb to set to
	 */
	public static void setMaxOrb(SoulNetwork soulNetwork, int maxOrb)
	{
		soulNetwork.setOrbTier(Math.max(maxOrb, soulNetwork.getOrbTier()));
	}
}