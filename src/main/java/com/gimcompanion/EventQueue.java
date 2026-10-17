package com.gimcompanion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Events waiting to be sent. The plugin saves this list in RuneLite's settings whenever it
 * changes, so events made while the site was unreachable survive a RuneLite restart.
 * Thread-safe: the game thread adds events, the network thread removes sent ones.
 */
public class EventQueue
{
	/** Kept small: if the site is down for a long time, the oldest events are dropped first. */
	static final int MAX_EVENTS = 300;
	/** The site accepts at most this many per request. */
	static final int MAX_PER_BATCH = 200;

	private final List<PluginEvent> events = new ArrayList<>();

	public synchronized void add(PluginEvent event)
	{
		events.add(event);
		while (events.size() > MAX_EVENTS)
		{
			events.remove(0);
		}
	}

	/** The oldest events for this character, up to one batch's worth. */
	public synchronized List<PluginEvent> nextBatch(String rsn)
	{
		return events.stream()
			.filter(e -> rsn.equals(e.getRsn()))
			.limit(MAX_PER_BATCH)
			.collect(Collectors.toList());
	}

	public synchronized boolean hasEventsFor(String rsn)
	{
		return events.stream().anyMatch(e -> rsn.equals(e.getRsn()));
	}

	/** Forgets events the site has confirmed. Returns whether anything changed. */
	public synchronized boolean removeIds(Set<String> ids)
	{
		return events.removeIf(e -> ids.contains(e.getId()));
	}

	public synchronized List<PluginEvent> all()
	{
		return new ArrayList<>(events);
	}

	public synchronized void replaceAll(List<PluginEvent> saved)
	{
		events.clear();
		if (saved != null)
		{
			saved.forEach(this::add);
		}
	}

	public synchronized int size()
	{
		return events.size();
	}
}
