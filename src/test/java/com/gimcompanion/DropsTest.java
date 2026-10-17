package com.gimcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.game.ItemStack;
import org.junit.Test;

public class DropsTest
{
	private static final Map<Integer, Integer> PRICES = new HashMap<>();
	private static final Map<Integer, String> NAMES = new HashMap<>();

	static
	{
		item(11286, "Draconic visage", 3_000_000);
		item(536, "Dragon bones", 3_000);
		item(995, "Coins", 1);
		item(12921, "Pet snakeling", 0); // untradeable: no price
	}

	private static void item(int id, String name, int price)
	{
		PRICES.put(id, price);
		NAMES.put(id, name);
	}

	private static List<DropItem> worthLogging(long min, ItemStack... loot)
	{
		return Drops.worthLogging(Arrays.asList(loot), id -> PRICES.get(id), NAMES::get, min);
	}

	@Test
	public void keepsStacksWorthAtLeastTheMinimum()
	{
		List<DropItem> items = worthLogging(100_000,
			new ItemStack(11286, 1),
			new ItemStack(536, 2),
			new ItemStack(995, 250_000));
		assertEquals(Arrays.asList(
			new DropItem(11286, "Draconic visage", 1, 3_000_000),
			new DropItem(995, "Coins", 250_000, 250_000)), items);
	}

	@Test
	public void countsTheWholeStack()
	{
		// 40 dragon bones at 3K each = 120K: worth logging even though one isn't.
		assertEquals(1, worthLogging(100_000, new ItemStack(536, 40)).size());
	}

	@Test
	public void skipsUntradeablesWithNoPrice()
	{
		assertTrue(worthLogging(0, new ItemStack(12921, 1)).isEmpty());
	}

	@Test
	public void doesNotOverflowOnHugeStacks()
	{
		List<DropItem> items = worthLogging(100_000, new ItemStack(11286, 1_000_000));
		assertEquals(3_000_000L * 1_000_000L, items.get(0).getValue());
	}
}
