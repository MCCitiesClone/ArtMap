package me.Fupery.ArtMap.IO.ColourMap;

import java.io.IOException;

import me.Fupery.ArtMap.Canvas.CanvasSize;
import me.Fupery.ArtMap.IO.Database.Map;

public class ResolutionMapFormatter implements MapFormatter {

    private final int magnitude;
    private final int gridSize;

    public ResolutionMapFormatter(CanvasSize size) {
        this.magnitude = size.getResolutionFactor();
        this.gridSize = size.getGridSize();
    }

    public ResolutionMapFormatter(int resolutionFactor) {
        this(CanvasSize.fromResolutionFactor(resolutionFactor));
    }

    private byte[] foldMap(byte[] mapData) {
        byte[] foldedData = new byte[gridSize * gridSize];
        for (int x = 0; x < 128; x += magnitude) {
            for (int y = 0; y < 128; y += magnitude) {
                foldedData[(x / magnitude) + ((y / magnitude) * gridSize)] = mapData[x + (y * 128)];
            }
        }
        return foldedData;
    }

    private byte[] unfoldMap(byte[] mapData) {
        byte[] unfoldedData = new byte[Map.Size.MAX.value];
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                int ix = x * magnitude;
                int iy = y * magnitude;
                for (int px = 0; px < magnitude; px++) {
                    for (int py = 0; py < magnitude; py++) {
                        unfoldedData[(px + ix) + ((py + iy) * 128)] = mapData[x + (y * gridSize)];
                    }
                }
            }
        }
        return unfoldedData;
    }

    @Override
    public byte[] generateBLOB(byte[] mapData) throws IOException {
        int logicalSize = gridSize * gridSize;
        if (mapData.length == logicalSize) {
            return Compressor.compress(mapData);
        }
        if (mapData.length == Map.Size.MAX.value) {
            return Compressor.compress(foldMap(mapData));
        }
        throw new IOException("Invalid MapData length " + mapData.length + " for resolution factor " + magnitude);
    }

    @Override
    public byte[] readBLOB(byte[] blobData) {
        byte[] decompressedData = Compressor.decompress(blobData);
        return unfoldMap(decompressedData);
    }
}
