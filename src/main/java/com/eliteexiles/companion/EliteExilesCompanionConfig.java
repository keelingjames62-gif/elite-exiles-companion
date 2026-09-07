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
        name = "Enable Coach Integration",
        description = "Optional Elite Exiles Discord Coach connection. OFF by default. Linking sends your OSRS display name and one-time link code; membership verification sends the names of your current normal and guest clan channels so the server can verify Elite Exiles access; Coach Chat sends questions you type and may include the resolved OSRS subject for follow-up continuity; optional Live session sync also sends skill levels/XP, total XP, session XP, and session start time. No Elite Exiles network requests are made while this setting is disabled.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0
    )
    default boolean coachIntegration()
    {
        return false;
    }

    @ConfigItem(
        keyName = "bridgeUrl",
        name = "Coach bridge URL",
        description = "Elite Exiles production coach endpoint. Managed automatically for normal Plugin Hub users.",
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
        description = "When Coach Integration is enabled and linked, send your current RSN, skill levels/XP, total XP, session XP and session start time to your Elite Exiles coach. This is optional and does not award EE Points by itself.",
        position = 2
    )
    default boolean autoSync()
    {
        return false;
    }

    @ConfigItem(
        keyName = "refreshSeconds",
        name = "Coach refresh seconds",
        description = "How often the optional Coach Integration refreshes Exile HQ. Values below 30 seconds are treated as 30.",
        position = 3
    )
    default int refreshSeconds()
    {
        return 45;
    }
}
