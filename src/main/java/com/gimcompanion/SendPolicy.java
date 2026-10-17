package com.gimcompanion;

/**
 * When to send a batch. Checked every few seconds; no game access, so it's easy to test.
 * Keeps requests rare (the site runs on free plans) while making drops and logouts quick.
 */
public final class SendPolicy
{
	static final long XP_EVERY_MS = 60_000;
	static final long HEARTBEAT_EVERY_MS = 5 * 60_000;
	static final long RETRY_AFTER_FAILURE_MS = 60_000;

	private SendPolicy()
	{
	}

	/**
	 * @param hasEvents      logins, logouts, drops or pets are waiting
	 * @param loggedIn       currently in the game
	 * @param xpChanged      XP changed since the last send
	 * @param sinceLastSend  ms since the last successful send
	 * @param sinceFailure   ms since the last failed send (Long.MAX_VALUE if none)
	 */
	public static boolean shouldSend(boolean hasEvents, boolean loggedIn, boolean xpChanged, long sinceLastSend, long sinceFailure)
	{
		if (sinceFailure < RETRY_AFTER_FAILURE_MS)
		{
			return false;
		}
		if (hasEvents)
		{
			return true;
		}
		if (!loggedIn)
		{
			return false;
		}
		return (xpChanged && sinceLastSend >= XP_EVERY_MS) || sinceLastSend >= HEARTBEAT_EVERY_MS;
	}
}
