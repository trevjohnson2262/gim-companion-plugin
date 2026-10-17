# GIM Companion

A RuneLite plugin for a Group Ironman team using the GIM Companion website. It shares, for
your own character only:

- when you log in and out, and which world you're on
- your skill XP (at most once a minute)
- drops worth 100,000 coins or more (you can change the amount), and pets

Your group sees these on the site's dashboard: who's playing now, and a "Recent drops" feed.

## Setup

1. Install **GIM Companion** from the Plugin Hub.
2. On your group's GIM Companion site, open the **Group** page and press **Make my RuneLite key**.
3. In the plugin's settings, tick **Send data to GIM Companion** and paste the key into **Key**.
4. The game chat shows "GIM Companion: Connected as …" once it works.

The key is tied to your character: it can't post as anyone else. Making a new key turns the
old one off.

## Privacy

Nothing is sent until you tick **Send data to GIM Companion**. When on, the plugin sends only
the data listed above, for the character you're logged in as, to the server in its settings
(by default `https://gim-companion.vercel.app`). That server sees your IP address, like any
website you visit. It does not send your bank, inventory, location, or anything about other
players.

Drops come from RuneLite's built-in Loot Tracker, so keep that plugin turned on.

## Developing

Requires Java 11 or newer.

```
./gradlew test   # unit tests
./gradlew run    # starts RuneLite with this plugin loaded
```

The site side lives in the gim-companion repo (`src/lib/plugin/`).
