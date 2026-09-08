package com.eliteexiles.companion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(EliteExilesCompanionConfig.GROUP)
public interface EliteExilesCompanionConfig extends Config
{
    String GROUP = "eliteexilescompanion";
    String PRODUCTION_BRIDGE_URL = "https://elite-exiles-coach.tail0d6194.ts.net";

    @ConfigItem(
        keyName = "coachIntegration",
        name = "Enable Elite Exiles Sync",
        description = "Optional Elite Exiles connection. OFF by default. Linking sends your OSRS display name and one-time Discord link code. Once linked, Companion access does not depend on Discord roles, staff approval, or current clan membership. Qualifying native OSRS clan system broadcasts may be sent to build the shared clan activity feed; optional Live session sync sends skill levels/XP, total XP, session XP, and session start time for progression, SOTW, rank/emblem context, and clan features. Ordinary clan chat messages are never sent. No Elite Exiles network requests are made while this setting is disabled.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0
    )
    default boolean coachIntegration()
    {
        return false;
    }

    @ConfigItem(
        keyName = "bridgeUrl",
        name = "Elite Exiles bridge URL",
        description = "Elite Exiles production clan endpoint. Managed automatically for normal Plugin Hub users.",
        hidden = true,
        position = 1
    )
    default String bridgeUrl()
    {
        return PRODUCTION_BRIDGE_URL;
    }

    @ConfigItem(
        keyName = "autoSync",
        name = "Live session sync",
        description = "When Elite Exiles Sync is enabled and linked, send your current RSN, skill levels/XP, total XP, session XP and session start time for synced progression and clan competition. This is optional and does not award EE Points by itself.",
        position = 2
    )
    default boolean autoSync()
    {
        return false;
    }

    @ConfigItem(
        keyName = "refreshSeconds",
        name = "HQ refresh seconds",
        description = "How often optional Elite Exiles Sync refreshes HQ. Values below 60 seconds are treated as 60.",
        position = 3
    )
    default int refreshSeconds()
    {
        return 90;
    }
}
