package com.eliteexiles.companion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(EliteExilesCompanionConfig.GROUP)
public interface EliteExilesCompanionConfig extends Config
{
    String GROUP = "eliteexilescompanion";

    @ConfigItem(
        keyName = "coachIntegration",
        name = "Enable Coach Integration",
        description = "Optional Elite Exiles Discord coach connection. OFF by default. No Elite Exiles network requests are made while this setting is disabled.",
        warning = "This optional feature connects to a third-party Elite Exiles server not controlled or verified by the RuneLite developers. When you link, it sends your OSRS display name and one-time link code. If Live session sync is enabled, it also sends skill levels, skill XP, total XP, session XP, and session start time. Your IP address is visible to the server as part of the network connection. No Elite Exiles data is sent while this setting is off.",
        position = 0
    )
    default boolean coachIntegration()
    {
        return false;
    }

    @ConfigItem(
        keyName = "bridgeUrl",
        name = "Coach bridge URL",
        description = "Address used only when Coach Integration is enabled. Remote addresses must use HTTPS. Localhost HTTP is allowed for same-PC testing.",
        position = 1
    )
    default String bridgeUrl()
    {
        return "http://127.0.0.1:47621";
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
