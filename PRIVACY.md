# Privacy

## Local Mode

Elite Exiles Companion runs in Local Mode by default. While **Enable Coach Integration** is disabled, the plugin does not make requests to the Elite Exiles bridge and does not send Elite Exiles player/session data to that service.

## Optional Coach Integration

Coach Integration is an opt-in feature. RuneLite displays a warning on the setting that enables it.

When enabled and used:

- Linking sends the OSRS display name and one-time link code.
- If Live session sync is enabled, the plugin can send skill levels, skill XP, total XP, session XP, and session start time.
- A remote server necessarily receives the connecting IP address as part of the network connection.
- Remote bridge URLs must use HTTPS. Plain HTTP is accepted only for localhost testing.

Disabling Coach Integration stops new Elite Exiles bridge requests from being created by the plugin.

## RuneScape credentials

The plugin does not request, read, transmit, or store Jagex/RuneScape passwords or Jagex Account credentials.
