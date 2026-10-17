package com.gimcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Collections;
import org.junit.Test;

/**
 * The site (src/lib/plugin/events.ts in the gim-companion repo) expects these exact field
 * names. If this test needs changing, the site's parseBatch probably does too.
 */
public class BatchJsonTest
{
	private final Gson gson = new Gson();

	@Test
	public void dropEventsHaveTheFieldsTheSiteReads()
	{
		PluginEvent drop = PluginEvent.drop("HD Blade", 1_760_000_000_000L, "Vorkath",
			Collections.singletonList(new DropItem(11286, "Draconic visage", 1, 3_000_000L)));
		JsonObject json = gson.toJsonTree(drop).getAsJsonObject();

		assertEquals("drop", json.get("type").getAsString());
		assertEquals(drop.getId(), json.get("id").getAsString());
		assertEquals(1_760_000_000_000L, json.get("at").getAsLong());
		assertEquals("Vorkath", json.get("source").getAsString());
		JsonObject item = json.getAsJsonArray("items").get(0).getAsJsonObject();
		assertEquals(11286, item.get("itemId").getAsInt());
		assertEquals("Draconic visage", item.get("name").getAsString());
		assertEquals(1, item.get("qty").getAsInt());
		assertEquals(3_000_000L, item.get("value").getAsLong());
	}

	@Test
	public void loginEventsLeaveOutUnusedFields()
	{
		JsonObject json = gson.toJsonTree(PluginEvent.login("HD Blade", 5)).getAsJsonObject();
		assertEquals("login", json.get("type").getAsString());
		assertFalse(json.has("items"));
		assertFalse(json.has("message"));
	}
}
