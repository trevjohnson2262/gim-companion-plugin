package com.gimcompanion;

import static com.gimcompanion.SendPolicy.HEARTBEAT_EVERY_MS;
import static com.gimcompanion.SendPolicy.XP_EVERY_MS;
import static com.gimcompanion.SendPolicy.shouldSend;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SendPolicyTest
{
	private static final long NO_FAILURE = Long.MAX_VALUE;

	@Test
	public void sendsDropsLoginsAndLogoutsRightAway()
	{
		assertTrue(shouldSend(true, true, false, 1_000, NO_FAILURE));
		assertTrue(shouldSend(true, false, false, 1_000, NO_FAILURE)); // a logout, after logging out
	}

	@Test
	public void sendsXpAtMostOnceAMinute()
	{
		assertFalse(shouldSend(false, true, true, XP_EVERY_MS - 1, NO_FAILURE));
		assertTrue(shouldSend(false, true, true, XP_EVERY_MS, NO_FAILURE));
	}

	@Test
	public void checksInEveryFewMinutesWhilePlaying()
	{
		assertFalse(shouldSend(false, true, false, HEARTBEAT_EVERY_MS - 1, NO_FAILURE));
		assertTrue(shouldSend(false, true, false, HEARTBEAT_EVERY_MS, NO_FAILURE));
		assertFalse(shouldSend(false, false, false, HEARTBEAT_EVERY_MS * 10, NO_FAILURE)); // logged out: quiet
	}

	@Test
	public void waitsAMinuteAfterAFailure()
	{
		assertFalse(shouldSend(true, true, true, HEARTBEAT_EVERY_MS, 10_000));
		assertTrue(shouldSend(true, true, true, HEARTBEAT_EVERY_MS, SendPolicy.RETRY_AFTER_FAILURE_MS));
	}
}
