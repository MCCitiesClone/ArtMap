package me.Fupery.ArtMap.Utils;

import java.io.File;

import org.bukkit.plugin.Plugin;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import me.Fupery.ArtMap.api.Utils.Version;
import me.Fupery.ArtMap.api.Utils.VersionHandler;
import me.Fupery.ArtMap.mocks.MockUtil;

public class VersionHandlerTest {

    private static MockUtil mocks;
    private static Plugin mockPlugin;

    @BeforeClass
    public static void setup() throws Exception {
        mocks = new MockUtil();
        mocks.mockServer("1.21.8-R0.1-SNAPSHOT").mockArtMap();
        mockPlugin = mocks.mockDataFolder(new File("target/plugins/Artmap/")).mockLogger()
                .getPluginMock();
    }

    @Test
    public void test_supportedVersion() {
        VersionHandler handler = new VersionHandler(mockPlugin);
        Assert.assertFalse(handler.getServerVersion().isLessThan(1, 20, 2));
    }

    @Test
    public void test_unsupportedVersion() {
        Version version = Version.getBukkitVersion("1.20.1-R0.1-SNAPSHOT");
        Assert.assertTrue(version.isLessThan(1, 20, 2));
    }
}
