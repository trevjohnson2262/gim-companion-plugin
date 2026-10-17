package com.gimcompanion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(GimCompanionConfig.GROUP)
public interface GimCompanionConfig extends Config
{
	String GROUP = "gimcompanion";
	String DEFAULT_SERVER = "https://gim-companion.vercel.app";

	@ConfigSection(
		name = "Advanced",
		description = "Only change these if you run your own copy of the GIM Companion site",
		position = 10,
		closedByDefault = true
	)
	String advanced = "advanced";

	@ConfigItem(
		keyName = "enabled",
		name = "Send data to GIM Companion",
		description = "Send your logins, world, XP and big drops to your group's GIM Companion site",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 0
	)
	default boolean enabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "key",
		name = "Key",
		description = "Your personal key from the Group page of the GIM Companion site",
		secret = true,
		position = 1
	)
	default String key()
	{
		return "";
	}

	@Range(min = 0)
	@ConfigItem(
		keyName = "minDropValue",
		name = "Minimum drop value",
		description = "Drops worth at least this many coins (Grand Exchange price) are logged automatically",
		position = 2
	)
	default int minDropValue()
	{
		return 100_000;
	}

	@ConfigItem(
		keyName = "logPets",
		name = "Log pets",
		description = "Log a drop when you get a pet",
		position = 3
	)
	default boolean logPets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "serverUrl",
		name = "Server",
		description = "Address of the GIM Companion site",
		position = 0,
		section = advanced
	)
	default String serverUrl()
	{
		return DEFAULT_SERVER;
	}
}
