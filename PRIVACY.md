# Privacy

## Local Mode

Elite Exiles Companion runs in Local Mode by default. While **Enable Elite Exiles Sync** is disabled, the plugin does not make requests to the Elite Exiles bridge and does not send Elite Exiles player/session data to that service.

## Optional Elite Exiles Sync

Elite Exiles Sync is opt-in. RuneLite displays a third-party-server warning on the setting that enables it. The production Elite Exiles HTTPS bridge is preconfigured for normal Plugin Hub users so members do not have to paste a server address.

When Elite Exiles Sync is enabled and used:

- Linking sends the OSRS display name and one-time link code.
- After that one-time Discord link succeeds, Companion access is tied to the stored bridge token. Discord roles, staff approval, and current clan membership are not used as ongoing access gates; the user can revoke the link with the plugin's unlink action.
- GROUP hosting can send the chosen activity and an optional OSRS world. GROUP join/start/leave/completion-confirmation actions send the chosen group identifier. The server also keeps the group participant list and lifecycle state needed to coordinate the shared Discord/RuneLite group.
- Qualifying native OSRS **clan system broadcasts** (`CLAN_MESSAGE`) may be sent to the Elite Exiles service to build the shared clan activity feed. The plugin strips game formatting, normalizes whitespace, caps the transmitted message length, and only considers broadcasts that look like clan activity such as loot, pets, Collection Log, Combat Achievements, personal bests, levels, quests or PvP notices. Ordinary member clan-chat messages (`CLAN_CHAT`), public chat, private messages and guest-clan conversation are not uploaded by this feature.
- Dashboard/progression responses can include the linked account's access state, rank/EE progression, goals/missions, progression suggestions, GROUP listings, SOTW standings, recent normalized clan activity, a read-only equipped Elite Exiles cosmetic identity, community/event summary and a read-only Mystery Pet Hunt summary.
- An explicit progression refresh causes the Elite Exiles backend to refresh the linked account from its existing Jagex-data path and update SOTW scoring state.
- If Live session sync is enabled, the plugin can send the current RSN, skill levels, skill XP, total XP, session XP, and session start time.
- The remote server necessarily receives the connecting IP address as part of the network connection.
- The plugin requires HTTPS for Elite Exiles bridge connections and permits only standard HTTPS port 443.
- A randomly generated Elite Exiles bridge bearer token is stored in RuneLite configuration after linking so later bridge requests can be authenticated. It is not a Jagex credential.

The v2.1 RuneLite client does **not** provide a general-purpose Coach/Wiki/AI question interface and does not send question text to the Elite Exiles service.

Disabling Elite Exiles Sync stops new Elite Exiles bridge requests from being created by the plugin.

## Discord join buttons

Exile HQ includes user-clicked **JOIN ELITE EXILES DISCORD** / **OPEN ELITE EXILES DISCORD** buttons. They open this fixed invite in the user's normal browser:

`https://discord.gg/FTJhv48K2`

The plugin does not append an RSN, link code, bridge token, query string, fragment, or tracking parameter to that invite. The button never auto-opens.

## Clan activity feed and cosmetic identity

The server stores only normalized qualifying clan-broadcast activity needed for the shared feed and keeps that feed bounded. Raw broadcast events do not directly award EE Points. Elite Exiles cosmetic ownership/equipping is managed server-side through Discord `/eeshop`; RuneLite receives a read-only identity/theme snapshot for local presentation. RuneLite does not upload ordinary clan-chat conversation to determine or render those cosmetics.

## GROUP data

GROUP is a clan coordination feature. The server can retain an open group for a limited period so a small clan does not require everyone to be online at the same instant. A group may include the host's linked member identity, chosen activity, optional world, participant list, timestamps and lifecycle state. RuneLite and Discord use the same single group record. The host must explicitly START before completion confirmations are accepted.

## SOTW

SOTW stores the selected weekly skill plus each participating member's server-refreshed baseline/current XP and division label. Client-reported Live session XP does not directly award EE Points or permanent ranks.

## Mystery Pet Hunt

The RuneLite sidebar only receives and displays a read-only summary for the linked Elite Exiles member. Pet rolls, claims, rewards and collection changes remain Discord/server-side actions and are not performed by RuneLite.

## Safe diagnostics

The **DIAGNOSTICS** action is available only after the user has opted into Elite Exiles Sync and linked the Companion. It sends the existing bearer token to the configured HTTPS Elite Exiles bridge and performs read-only health/authentication and JSON echo checks. The diagnostics do not award points, complete goals/missions, alter registrations, change Discord settings, or write live RuneLite progression/session data.

## RuneScape credentials

The plugin does not request, read, transmit, or store Jagex/RuneScape passwords, Jagex Account credentials, bank PINs, or authenticator secrets.
