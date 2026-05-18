package me.Fupery.ArtMap.io;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import me.Fupery.ArtMap.Canvas.CanvasSize;
import me.Fupery.ArtMap.IO.ColourMap.ResolutionMapFormatter;
import me.Fupery.ArtMap.IO.Database.Map;

public class ResolutionMapFormatterTest {

    @Test
    public void roundTripNormalLogicalBuffer() throws Exception {
        byte[] logical = new byte[Map.Size.STANDARD.value];
        Arrays.fill(logical, (byte) 1);
        ResolutionMapFormatter formatter = new ResolutionMapFormatter(CanvasSize.NORMAL);
        byte[] blob = formatter.generateBLOB(logical);
        byte[] restored = formatter.readBLOB(blob);
        Assert.assertEquals(Map.Size.MAX.value, restored.length);
        Assert.assertEquals((byte) 1, restored[0]);
    }

    @Test
    public void roundTripMediumLogicalBuffer() throws Exception {
        byte[] logical = new byte[Map.Size.MEDIUM.value];
        Arrays.fill(logical, (byte) 2);
        ResolutionMapFormatter formatter = new ResolutionMapFormatter(CanvasSize.MEDIUM);
        byte[] blob = formatter.generateBLOB(logical);
        byte[] restored = formatter.readBLOB(blob);
        Assert.assertEquals(Map.Size.MAX.value, restored.length);
        Assert.assertEquals((byte) 2, restored[64 + 2]);
    }

    @Test
    public void canvasSizeFromFactor() {
        Assert.assertEquals(CanvasSize.NORMAL, CanvasSize.fromResolutionFactor(4));
        Assert.assertEquals(CanvasSize.MEDIUM, CanvasSize.fromResolutionFactor(2));
        Assert.assertEquals(CanvasSize.LARGE, CanvasSize.fromResolutionFactor(1));
    }
}
