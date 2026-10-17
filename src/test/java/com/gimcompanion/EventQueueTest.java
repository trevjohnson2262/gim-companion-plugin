package com.gimcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class EventQueueTest
{
	@Test
	public void onlySendsEventsForTheCharacterYoureOn()
	{
		EventQueue queue = new EventQueue();
		queue.add(PluginEvent.login("HD Blade", 1));
		queue.add(PluginEvent.login("hdspade", 2));
		queue.add(PluginEvent.logout("HD Blade", 3));

		List<PluginEvent> batch = queue.nextBatch("HD Blade");
		assertEquals(2, batch.size());
		assertTrue(batch.stream().allMatch(e -> e.getRsn().equals("HD Blade")));
		assertTrue(queue.hasEventsFor("hdspade"));
		assertFalse(queue.hasEventsFor("Zezima"));
	}

	@Test
	public void forgetsOnlyWhatTheSiteConfirmed()
	{
		EventQueue queue = new EventQueue();
		PluginEvent a = PluginEvent.login("HD Blade", 1);
		PluginEvent b = PluginEvent.logout("HD Blade", 2);
		queue.add(a);
		queue.add(b);

		assertTrue(queue.removeIds(Collections.singleton(a.getId())));
		assertEquals(Collections.singletonList(b), queue.all());
		assertFalse(queue.removeIds(Collections.singleton("unknown")));
	}

	@Test
	public void dropsTheOldestWhenFull()
	{
		EventQueue queue = new EventQueue();
		PluginEvent first = PluginEvent.login("HD Blade", 0);
		queue.add(first);
		for (int i = 1; i <= EventQueue.MAX_EVENTS; i++)
		{
			queue.add(PluginEvent.login("HD Blade", i));
		}
		assertEquals(EventQueue.MAX_EVENTS, queue.size());
		assertFalse(queue.all().contains(first));
		assertEquals(EventQueue.MAX_PER_BATCH, queue.nextBatch("HD Blade").size());
	}

	@Test
	public void survivesBeingSavedAndLoaded()
	{
		Gson gson = new Gson();
		Type type = new TypeToken<List<PluginEvent>>()
		{
		}.getType();
		EventQueue queue = new EventQueue();
		queue.add(PluginEvent.drop("HD Blade", 5, "Vorkath", Collections.singletonList(new DropItem(11286, "Draconic visage", 1, 3_000_000))));
		queue.add(PluginEvent.pet("HD Blade", 6, "You have a funny feeling like you're being followed."));

		EventQueue loaded = new EventQueue();
		loaded.replaceAll(gson.fromJson(gson.toJson(queue.all(), type), type));
		assertEquals(queue.all(), loaded.all());
	}

	@Test
	public void everyEventGetsItsOwnId()
	{
		Set<String> ids = java.util.stream.IntStream.range(0, 100)
			.mapToObj(i -> PluginEvent.login("HD Blade", i).getId())
			.collect(Collectors.toSet());
		assertEquals(100, ids.size());
	}
}
