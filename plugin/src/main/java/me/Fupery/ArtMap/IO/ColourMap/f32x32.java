package me.Fupery.ArtMap.IO.ColourMap;

import java.io.IOException;

import me.Fupery.ArtMap.Canvas.CanvasSize;

/**
 * @deprecated Use {@link ResolutionMapFormatter} instead.
 */
@Deprecated
public class f32x32 implements MapFormatter {

    private final ResolutionMapFormatter delegate = new ResolutionMapFormatter(CanvasSize.NORMAL);

    @Override
    public byte[] generateBLOB(byte[] mapData) throws IOException {
        return delegate.generateBLOB(mapData);
    }

    @Override
    public byte[] readBLOB(byte[] blobData) {
        return delegate.readBLOB(blobData);
    }
}
