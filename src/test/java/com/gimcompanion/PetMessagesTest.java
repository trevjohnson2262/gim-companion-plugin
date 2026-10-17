package com.gimcompanion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PetMessagesTest
{
	@Test
	public void spotsEveryPetMessage()
	{
		assertTrue(PetMessages.isPetMessage("You have a funny feeling like you're being followed."));
		assertTrue(PetMessages.isPetMessage("You feel something weird sneaking into your backpack."));
		assertTrue(PetMessages.isPetMessage("You have a funny feeling like you would have been followed..."));
	}

	@Test
	public void ignoresEverythingElse()
	{
		assertFalse(PetMessages.isPetMessage("Your Vorkath kill count is: 50."));
		assertFalse(PetMessages.isPetMessage(""));
		assertFalse(PetMessages.isPetMessage(null));
	}
}
