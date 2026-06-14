package me.Fupery.ArtMap.api.Utils;

import org.bukkit.plugin.java.JavaPlugin;

public class VersionHandler {
    private final Version serverVersion;

    public VersionHandler(JavaPlugin plugin) {
        serverVersion = Version.getBukkitVersion();
        if (serverVersion.isLessThan(1, 20, 2)) {
            throw new IllegalStateException(
                    "ArtMap requires Minecraft 1.20.2 or newer (detected: " + serverVersion + ")");
        }
    }

    public Version getServerVersion() {
        return serverVersion;
    }

    @Override
    public String toString() {
        return serverVersion.toString();
    }
}
