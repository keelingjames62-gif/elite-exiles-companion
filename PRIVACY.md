# Privacy

## Local Mode

Elite Exiles Companion runs in Local Mode by default. While **Enable Coach Integration** is disabled, the plugin does not make requests to the Elite Exiles bridge and does not send Elite Exiles player/session data to that service.

## Optional Coach Integration

Coach Integration is an opt-in feature. RuneLite displays a third-party-server warning on the setting that enables it.

The production Elite Exiles HTTPS bridge is preconfigured for normal Plugin Hub users so members do not have to paste a server address.

When enabled and used:

- Linking sends the OSRS display name and one-time link code.
- If Live session sync is enabled, the plugin can send skill levels, skill XP, total XP, session XP, and session start time.
- A remote server necessarily receives the connecting IP address as part of the network connection.
- The plugin requires HTTPS for Elite Exiles bridge connections and permits only standard HTTPS port 443.
- A randomly generated Elite Exiles bridge bearer token is stored in RuneLite configuration after linking so later bridge requests can be authenticated. It is not a Jagex credential.

Disabling Coach Integration stops new Elite Exiles bridge requests from being created by the plugin.

## Discord join button

The unlinked coach card includes a user-clicked **JOIN ELITE EXILES DISCORD** button. It opens this fixed invite in the user's normal browser:

`https://discord.gg/FTJhv48K2`

The plugin does not append an RSN, link code, bridge token, query string, fragment, or tracking parameter to that invite. The button does not auto-open.

## Safe diagnostics

The **RUN SAFE DIAGNOSTICS** action is available only after the user has opted into Coach Integration and linked the Companion. It sends the existing bearer token to the configured HTTPS Elite Exiles bridge and performs read-only health/authentication and JSON echo checks. The diagnostics do not award points, complete goals/missions, alter registrations, change Discord settings, or write live RuneLite progression/session data.

## RuneScape credentials

The plugin does not request, read, transmit, or store Jagex/RuneScape passwords, Jagex Account credentials, bank PINs, or authenticator secrets.
