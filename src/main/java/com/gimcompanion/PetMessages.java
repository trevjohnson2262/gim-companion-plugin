package com.gimcompanion;

/** Spots the game messages that mean you got a pet (pets don't go through the loot tracker). */
public final class PetMessages
{
	private PetMessages()
	{
	}

	private static final String[] PET_MESSAGES = {
		"You have a funny feeling like you're being followed",
		"You feel something weird sneaking into your backpack",
		"You have a funny feeling like you would have been followed",
	};

	public static boolean isPetMessage(String message)
	{
		if (message == null)
		{
			return false;
		}
		for (String pet : PET_MESSAGES)
		{
			if (message.contains(pet))
			{
				return true;
			}
		}
		return false;
	}
}
