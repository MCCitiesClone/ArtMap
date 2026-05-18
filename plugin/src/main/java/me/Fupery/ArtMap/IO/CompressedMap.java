package me.Fupery.ArtMap.IO;

import java.io.IOException;
import java.util.Arrays;

import org.bukkit.map.MapView;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Canvas.CanvasSize;
import me.Fupery.ArtMap.IO.ColourMap.ResolutionMapFormatter;
import me.Fupery.ArtMap.IO.Database.Map;

public class CompressedMap extends MapId {
    private byte[] compressedMap;
    private final int resolutionFactor;

    public CompressedMap(int id, int hash, byte[] compressedMap) {
        this(id, hash, compressedMap, CanvasSize.NORMAL.getResolutionFactor());
    }

    public CompressedMap(int id, int hash, byte[] compressedMap, int resolutionFactor) {
        super(id, hash);
        this.compressedMap = Arrays.copyOf(compressedMap, compressedMap.length);
        this.resolutionFactor = resolutionFactor;
    }

    public int getResolutionFactor() {
        return resolutionFactor;
    }

    public CanvasSize getCanvasSize() {
        return CanvasSize.fromResolutionFactor(resolutionFactor);
    }

    public static CompressedMap compress(MapView mapView) throws IOException {
        return compress(mapView.getId(), ArtMap.instance().getReflection().getMap(mapView), CanvasSize.NORMAL);
    }

    public static CompressedMap compress(int mapId, byte[] map) throws IOException {
        return compress(mapId, map, CanvasSize.NORMAL);
    }

    public static CompressedMap compress(int mapId, byte[] map, CanvasSize size) throws IOException {
        CanvasSize canvasSize = size == null ? CanvasSize.defaultSize() : size;
        byte[] compressed = new ResolutionMapFormatter(canvasSize).generateBLOB(map);
        return new CompressedMap(mapId, Arrays.hashCode(map), compressed, canvasSize.getResolutionFactor());
    }

    public static CompressedMap compress(int newId, MapView mapView) throws IOException {
        return compress(newId, mapView, CanvasSize.NORMAL);
    }

    public static CompressedMap compress(int newId, MapView mapView, CanvasSize size) throws IOException {
        byte[] mapBytes = ArtMap.instance().getReflection().getMap(mapView);
        return new CompressedMap(newId, Arrays.hashCode(mapBytes),
                new ResolutionMapFormatter(size).generateBLOB(mapBytes), size.getResolutionFactor());
    }

    public byte[] getCompressedMap() {
        return Arrays.copyOf(this.compressedMap, this.compressedMap.length);
    }

    public byte[] decompressMap() {
        return compressedMap == null ? new byte[Map.Size.MAX.value]
                : new ResolutionMapFormatter(resolutionFactor).readBLOB(compressedMap);
    }
}
