package com.gimcompanion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntToLongFunction;
import net.runelite.client.game.ItemStack;

/** Picks the items worth logging out of a loot drop. No game access, so it's easy to test. */
public final class Drops
{
	private Drops()
	{
	}

	/**
	 * @param priceOf  Grand Exchange price of one of this item (0 if untradeable)
	 * @param nameOf   the item's name
	 * @param minValue log stacks worth at least this much in total
	 */
	public static List<DropItem> worthLogging(Collection<ItemStack> loot, IntToLongFunction priceOf, IntFunction<String> nameOf, long minValue)
	{
		List<DropItem> items = new ArrayList<>();
		for (ItemStack stack : loot)
		{
			long value = priceOf.applyAsLong(stack.getId()) * stack.getQuantity();
			if (value > 0 && value >= minValue && stack.getQuantity() > 0)
			{
				items.add(new DropItem(stack.getId(), nameOf.apply(stack.getId()), stack.getQuantity(), value));
			}
		}
		return items;
	}
}
