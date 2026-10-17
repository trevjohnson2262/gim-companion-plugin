package com.gimcompanion;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Starts RuneLite with this plugin loaded, for testing in-game: ./gradlew run */
public class GimCompanionPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GimCompanionPlugin.class);
		RuneLite.main(args);
	}
}
