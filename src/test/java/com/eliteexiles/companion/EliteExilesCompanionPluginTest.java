package com.eliteexiles.companion;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class EliteExilesCompanionPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(EliteExilesCompanionPlugin.class);
        RuneLite.main(args);
    }
}
