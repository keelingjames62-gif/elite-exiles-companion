package com.eliteexiles.companion;

import org.junit.Assert;
import org.junit.Test;

public class BridgeUrlMigrationTest
{
    @Test
    public void preservesValidHttpsOrigins()
    {
        Assert.assertFalse(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl(
            EliteExilesCompanionConfig.PRODUCTION_BRIDGE_URL));
        Assert.assertFalse(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl(
            "https://coach.example.com"));
        Assert.assertFalse(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl(
            "https://coach.example.com/"));
        Assert.assertFalse(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl(
            "https://coach.example.com:443"));
    }

    @Test
    public void migratesBlankLegacyAndUnsafeValues()
    {
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl(null));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl(""));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl(" "));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("http://127.0.0.1:47621"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("http://localhost:47621"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("E9XU7ANY"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("http://coach.example.com"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("ftp://coach.example.com"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("https://user:pass@coach.example.com"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("https://coach.example.com/path"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("https://coach.example.com/?q=1"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("https://coach.example.com/#fragment"));
        Assert.assertTrue(EliteExilesCompanionPlugin.shouldMigrateBridgeUrl("https://coach.example.com:8443"));
    }
}
