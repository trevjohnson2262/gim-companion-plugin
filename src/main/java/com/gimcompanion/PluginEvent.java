package com.gimcompanion;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Something that happened in-game, waiting to be sent to the site. Each event gets a random
 * id when it's created, so if a batch is sent twice (say, after a timeout) the site saves it once.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginEvent
{
	static final String LOGIN = "login";
	static final String LOGOUT = "logout";
	static final String DROP = "drop";
	static final String PET = "pet";

	private String id;
	private String type;
	private long at;
	// Which character this happened to. Only events for the character you're logged in as are
	// sent, so events queued on one account never get posted under another.
	private String rsn;
	private String source; // drops: what it dropped from
	private List<DropItem> items; // drops only
	private String message; // pets: the game message

	static PluginEvent login(String rsn, long at)
	{
		return new PluginEvent(newId(), LOGIN, at, rsn, null, null, null);
	}

	static PluginEvent logout(String rsn, long at)
	{
		return new PluginEvent(newId(), LOGOUT, at, rsn, null, null, null);
	}

	static PluginEvent drop(String rsn, long at, String source, List<DropItem> items)
	{
		return new PluginEvent(newId(), DROP, at, rsn, source, items, null);
	}

	static PluginEvent pet(String rsn, long at, String message)
	{
		return new PluginEvent(newId(), PET, at, rsn, null, null, message);
	}

	private static String newId()
	{
		return UUID.randomUUID().toString();
	}
}
