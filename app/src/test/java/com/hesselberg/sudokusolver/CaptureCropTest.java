package com.hesselberg.sudokusolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CaptureCropTest {

    @Test
    public void normalizeViewFrame_centersSquareOnPortraitPreview() {
        float[] normalized = CaptureCrop.normalizeViewFrame(20f, 120f, 380f, 480f, 400, 800);

        assertEquals(0.05f, normalized[0], 0.0001f);
        assertEquals(0.15f, normalized[1], 0.0001f);
        assertEquals(0.95f, normalized[2], 0.0001f);
        assertEquals(0.60f, normalized[3], 0.0001f);
    }

    @Test
    public void normalizedFrameToPixelRect_mapsFractionsToPixels() {
        int[] crop = CaptureCrop.normalizedFrameToPixelRect(1000, 2000, 0.1f, 0.2f, 0.9f, 0.8f);

        assertNotNull(crop);
        assertEquals(100, crop[0]);
        assertEquals(400, crop[1]);
        assertEquals(800, crop[2]);
        assertEquals(1200, crop[3]);
    }

    @Test
    public void normalizedFrameToPixelRect_rejectsInvertedFrame() {
        assertNull(CaptureCrop.normalizedFrameToPixelRect(100, 100, 0.8f, 0.1f, 0.2f, 0.9f));
    }

    @Test
    public void insetNormalizedFrame_shrinksTowardCenter() {
        float[] inset = CaptureCrop.insetNormalizedFrame(0.1f, 0.2f, 0.9f, 0.8f, 0.05f);

        assertEquals(0.14f, inset[0], 0.0001f);
        assertEquals(0.23f, inset[1], 0.0001f);
        assertEquals(0.86f, inset[2], 0.0001f);
        assertEquals(0.77f, inset[3], 0.0001f);
    }
}
