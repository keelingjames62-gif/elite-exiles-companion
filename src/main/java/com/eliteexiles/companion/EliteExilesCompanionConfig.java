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
        description = "Optional Elite Exiles Discord coach connection. OFF by default. When enabled, linking sends your OSRS display name and one-time link code; optional Live session sync also sends skill levels/XP, total XP, session XP, and session start time. No Elite Exiles network requests are made while this setting is disabled.",
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
        description = "When Coach Integration is enabled and linked, send your current RSN, skill levels/XP, total XP and session XP to your Elite Exiles coach. This is optional and does not award EE Points by itself.",
        position = 2
    )
    default boolean autoSync()
    {
        return false;
    }

    @ConfigItem(
        keyName = "refreshSeconds",
        name = "Coach refresh seconds",
        description = "How often the optional Coach Integration refreshes its dashboard. Values below 30 seconds are treated as 30.",
        position = 3
    )
    default int refreshSeconds()
    {
        return 45;
    }
}
