package com.gimcompanion;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.task.Schedule;
import net.runelite.client.util.Text;

/**
 * Sends your logins, world, XP and big drops to your group's GIM Companion site, which shows
 * who's playing and posts drops to everyone's dashboard. Off until you tick
 * "Send data to GIM Companion" and paste your key from the site's Group page.
 */
@Slf4j
@PluginDescriptor(
	name = "GIM Companion",
	description = "Shares your logins, XP and big drops with your Group Ironman's GIM Companion site",
	tags = {"group", "ironman", "gim", "drops", "tracker"}
)
public class GimCompanionPlugin extends Plugin
{
	private static final String QUEUE_KEY = "pendingEvents";
	private static final Type EVENT_LIST = new TypeToken<List<PluginEvent>>()
	{
	}.getType();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private GimCompanionConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ApiClient api;

	@Inject
	private Gson gson;

	private final EventQueue queue = new EventQueue();
	private final Map<String, Integer> skillXp = new ConcurrentHashMap<>();
	private final AtomicBoolean sending = new AtomicBoolean();

	// Written on the game thread, read on the sending thread.
	private volatile boolean loggedIn;
	private volatile boolean loginPending; // logged in, but the character's name isn't known yet
	private volatile String rsn;
	private volatile int world;
	private volatile boolean xpChanged;

	private volatile long lastSendAt;
	private volatile long lastFailureAt;
	private volatile boolean keyRejected; // stop sending until the settings change
	private volatile boolean announced; // said "connected" in chat this session

	@Override
	protected void startUp()
	{
		loadQueue();
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				loggedIn = true;
				loginPending = true;
				world = client.getWorld();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		saveQueue();
		loggedIn = false;
		loginPending = false;
		rsn = null;
		skillXp.clear();
		xpChanged = false;
		keyRejected = false;
		announced = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			world = client.getWorld(); // also catches world hops
			if (!loggedIn)
			{
				loggedIn = true;
				loginPending = true;
			}
		}
		else if (state == GameState.LOGIN_SCREEN && loggedIn)
		{
			loggedIn = false;
			if (rsn != null)
			{
				addEvent(PluginEvent.logout(rsn, System.currentTimeMillis()));
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (!loginPending)
		{
			return;
		}
		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null)
		{
			return;
		}
		loginPending = false;
		if (!player.getName().equals(rsn))
		{
			skillXp.clear(); // a different character than last time
		}
		rsn = player.getName();
		announced = false;
		addEvent(PluginEvent.login(rsn, System.currentTimeMillis()));
		readAllSkills();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Integer before = skillXp.put(SkillNames.of(event.getSkill()), event.getXp());
		if (before == null || before != event.getXp())
		{
			xpChanged = true;
		}
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (!config.enabled() || rsn == null)
		{
			return;
		}
		List<DropItem> items = Drops.worthLogging(
			event.getItems(),
			itemManager::getItemPrice,
			id -> itemManager.getItemComposition(id).getName(),
			config.minDropValue());
		if (!items.isEmpty())
		{
			addEvent(PluginEvent.drop(rsn, System.currentTimeMillis(), event.getName(), items));
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE || !config.enabled() || !config.logPets() || rsn == null)
		{
			return;
		}
		String message = Text.removeTags(event.getMessage());
		if (PetMessages.isPetMessage(message))
		{
			addEvent(PluginEvent.pet(rsn, System.currentTimeMillis(), message));
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!GimCompanionConfig.GROUP.equals(event.getGroup()) || QUEUE_KEY.equals(event.getKey()))
		{
			return;
		}
		keyRejected = false;
		lastFailureAt = 0;
		if (config.enabled() && !config.key().trim().isEmpty())
		{
			checkKey();
		}
	}

	/** Every few seconds, off the game thread: send a batch if one is due. */
	@Schedule(period = 5, unit = ChronoUnit.SECONDS, asynchronous = true)
	public void sendIfDue()
	{
		String key = config.key().trim();
		String who = rsn;
		if (!config.enabled() || key.isEmpty() || keyRejected || who == null)
		{
			return;
		}

		long now = System.currentTimeMillis();
		long sinceFailure = lastFailureAt == 0 ? Long.MAX_VALUE : now - lastFailureAt;
		if (!SendPolicy.shouldSend(queue.hasEventsFor(who), loggedIn, xpChanged, now - lastSendAt, sinceFailure))
		{
			return;
		}
		if (!sending.compareAndSet(false, true))
		{
			return; // the previous request is still going
		}

		List<PluginEvent> events = queue.nextBatch(who);
		boolean withXp = xpChanged && !skillXp.isEmpty();
		if (withXp)
		{
			xpChanged = false;
		}

		Map<String, Object> batch = new LinkedHashMap<>();
		batch.put("rsn", who);
		batch.put("world", world > 0 ? world : null);
		batch.put("skills", withXp ? new HashMap<>(skillXp) : null);
		batch.put("events", events);

		api.sendBatch(config.serverUrl(), key, batch, (code, body) ->
		{
			try
			{
				handleSendResult(code, body, who, events, withXp);
			}
			finally
			{
				sending.set(false);
			}
		});
	}

	private void handleSendResult(int code, String body, String who, List<PluginEvent> events, boolean withXp)
	{
		long now = System.currentTimeMillis();
		if (code == 200)
		{
			lastSendAt = now;
			lastFailureAt = 0;
			Accepted accepted = parseAccepted(body);
			if (accepted.accepted != null && queue.removeIds(new HashSet<>(accepted.accepted)))
			{
				saveQueue();
			}
			if (!announced)
			{
				announced = true;
				chat("Connected as " + who + ".");
			}
			return;
		}

		lastFailureAt = now;
		if (withXp)
		{
			xpChanged = true; // try the XP again next time
		}
		if (code == 401 || code == 409)
		{
			// Wrong key, or a key for another character: waiting won't fix it, so stop until
			// the settings change, and say why.
			keyRejected = true;
			String error = api.errorMessage(body);
			chat(error != null ? error : "Your key wasn't accepted. Make a new one on the site's Group page.");
		}
		else if (code == 400)
		{
			// The site says the batch itself is broken. Resending it would fail forever.
			log.debug("GIM Companion batch rejected: {}", body);
			Set<String> ids = events.stream().map(PluginEvent::getId).collect(Collectors.toSet());
			if (queue.removeIds(ids))
			{
				saveQueue();
			}
		}
		// Anything else (site down, no internet): keep the events and retry in a minute.
	}

	private void checkKey()
	{
		api.whoAmI(config.serverUrl(), config.key().trim(), (code, body) ->
		{
			if (code == 200)
			{
				Me me = parseMe(body);
				announced = true;
				chat("Connected as " + me.rsn + " (" + me.group + ").");
			}
			else if (code == 401)
			{
				keyRejected = true;
				chat("That key wasn't recognised. Copy it again from the site's Group page.");
			}
			else
			{
				chat("Couldn't reach the GIM Companion site. Will keep trying.");
			}
		});
	}

	private void readAllSkills()
	{
		for (Skill skill : Skill.values())
		{
			if (skill.getName().equalsIgnoreCase("Overall"))
			{
				continue; // not a real skill (older RuneLite versions list it)
			}
			int xp = client.getSkillExperience(skill);
			if (xp >= 0)
			{
				skillXp.put(SkillNames.of(skill), xp);
			}
		}
		xpChanged = true;
	}

	private void addEvent(PluginEvent event)
	{
		queue.add(event);
		saveQueue();
	}

	private void loadQueue()
	{
		String saved = configManager.getConfiguration(GimCompanionConfig.GROUP, QUEUE_KEY);
		if (saved == null || saved.isEmpty())
		{
			return;
		}
		try
		{
			queue.replaceAll(gson.fromJson(saved, EVENT_LIST));
		}
		catch (JsonParseException e)
		{
			log.debug("Couldn't read saved GIM Companion events, starting fresh", e);
		}
	}

	private void saveQueue()
	{
		configManager.setConfiguration(GimCompanionConfig.GROUP, QUEUE_KEY, gson.toJson(queue.all(), EVENT_LIST));
	}

	private void chat(String text)
	{
		String message = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("GIM Companion: ")
			.append(ChatColorType.NORMAL)
			.append(text)
			.build();
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
	}

	private Accepted parseAccepted(String body)
	{
		try
		{
			Accepted accepted = gson.fromJson(body, Accepted.class);
			return accepted != null ? accepted : new Accepted();
		}
		catch (JsonParseException e)
		{
			return new Accepted();
		}
	}

	private Me parseMe(String body)
	{
		try
		{
			Me me = gson.fromJson(body, Me.class);
			return me != null && me.rsn != null ? me : new Me();
		}
		catch (JsonParseException e)
		{
			return new Me();
		}
	}

	/** The site's answer to a batch. */
	private static class Accepted
	{
		List<String> accepted;
	}

	/** The site's answer to "who is this key for?". */
	private static class Me
	{
		String rsn = "your character";
		String group = "your group";
	}

	@Provides
	GimCompanionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GimCompanionConfig.class);
	}
}
