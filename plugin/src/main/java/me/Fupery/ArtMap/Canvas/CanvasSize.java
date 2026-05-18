package me.Fupery.ArtMap.Canvas;

import me.Fupery.ArtMap.IO.Database.Map;
import me.Fupery.ArtMap.Recipe.ArtMaterial;

public enum CanvasSize {
    NORMAL(32, 4),
    MEDIUM(64, 2),
    LARGE(128, 1);

    private final int gridSize;
    private final int resolutionFactor;

    CanvasSize(int gridSize, int resolutionFactor) {
        this.gridSize = gridSize;
        this.resolutionFactor = resolutionFactor;
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getResolutionFactor() {
        return resolutionFactor;
    }

    public Map.Size getMapSize() {
        switch (this) {
            case MEDIUM:
                return Map.Size.MEDIUM;
            case LARGE:
                return Map.Size.MAX;
            case NORMAL:
            default:
                return Map.Size.STANDARD;
        }
    }

    public static CanvasSize defaultSize() {
        return NORMAL;
    }

    public static CanvasSize fromResolutionFactor(int factor) {
        for (CanvasSize size : values()) {
            if (size.resolutionFactor == factor) {
                return size;
            }
        }
        return NORMAL;
    }

    public static CanvasSize fromArtMaterial(ArtMaterial material) {
        if (material == null) {
            return NORMAL;
        }
        switch (material) {
            case CANVAS_MEDIUM:
                return MEDIUM;
            case CANVAS_LARGE:
                return LARGE;
            case CANVAS:
            default:
                return NORMAL;
        }
    }

    public static boolean isCanvasMaterial(ArtMaterial material) {
        return material == ArtMaterial.CANVAS
                || material == ArtMaterial.CANVAS_MEDIUM
                || material == ArtMaterial.CANVAS_LARGE;
    }
}
