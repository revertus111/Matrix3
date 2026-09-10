package com.matrix3.mobile;

import android.graphics.Bitmap;

import java.lang.ref.WeakReference;

/**
 * Shared main-process bridge between the remapped Matrix3 desktop UI layer
 * and the real Android SurfaceView. The obfuscated client does not depend on
 * Android classes directly; compatibility classes call this bridge instead.
 */
public final class MobileAwtBridge {

    private static volatile WeakReference<MatrixClientSurfaceView> surfaceRef =
            new WeakReference<MatrixClientSurfaceView>(null);

    private MobileAwtBridge() {
    }

    public static void attach(MatrixClientSurfaceView surface) {
        surfaceRef = new WeakReference<MatrixClientSurfaceView>(surface);
    }

    public static int getWidth() {
        MatrixClientSurfaceView surface = surfaceRef.get();
        return surface == null ? 1024 : Math.max(1, surface.getWidth());
    }

    public static int getHeight() {
        MatrixClientSurfaceView surface = surfaceRef.get();
        return surface == null ? 768 : Math.max(1, surface.getHeight());
    }

    public static void present(int[] argbPixels, int width, int height) {
        MatrixClientSurfaceView surface = surfaceRef.get();
        if (surface != null) {
            surface.presentFrame(argbPixels, width, height);
        }
    }

    public static void setStatus(String status) {
        MatrixClientSurfaceView surface = surfaceRef.get();
        if (surface != null) {
            surface.setStatus(status);
        }
    }
}
