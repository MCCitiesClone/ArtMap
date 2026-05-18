package me.Fupery.ArtMap.IO;

import me.Fupery.ArtMap.Canvas.CanvasSize;
import me.Fupery.DataTables.DataTables;
import me.Fupery.DataTables.PixelTable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;

public class PixelTableManager {
    private int resolutionFactor;
    private float[] yawBounds;
    private Object[] pitchBounds;

    private PixelTableManager(int resolutionFactor, float[] yawBounds, Object[] pitchBounds) {
        this.resolutionFactor = resolutionFactor;
        this.yawBounds = yawBounds;
        this.pitchBounds = pitchBounds;
    }

    public static PixelTableManager buildTables(JavaPlugin plugin) {
        return buildTables(plugin, CanvasSize.NORMAL.getResolutionFactor());
    }

    public static PixelTableManager buildTables(JavaPlugin plugin, int resolutionFactor) {
        PixelTable table;
        try {
            table = DataTables.loadTable(resolutionFactor);
            if (table == null) {
                return null;
            }
            return new PixelTableManager(resolutionFactor, table.getYawBounds(), table.getPitchBounds());
        } catch (Exception | NoClassDefFoundError | DataTables.InvalidResolutionFactorException e) {
            return null;
        }
    }

    public static Map<CanvasSize, PixelTableManager> buildAllTables(JavaPlugin plugin) {
        Map<CanvasSize, PixelTableManager> tables = new EnumMap<>(CanvasSize.class);
        for (CanvasSize size : CanvasSize.values()) {
            PixelTableManager manager = buildTables(plugin, size.getResolutionFactor());
            if (manager == null) {
                return null;
            }
            tables.put(size, manager);
        }
        return Collections.unmodifiableMap(tables);
    }

    public float[] getYawBounds() {
        return Arrays.copyOf(this.yawBounds, this.yawBounds.length);
    }

    public Object[] getPitchBounds() {
        return Arrays.copyOf(this.pitchBounds, this.pitchBounds.length);
    }

    public int getResolutionFactor() {
        return resolutionFactor;
    }
}
