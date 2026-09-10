package com.matrix3.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class MatrixClientSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Object frameLock = new Object();

    private volatile boolean running;
    private volatile String status = "Preparing Matrix3 Mobile...";
    private Thread renderThread;
    private Bitmap frameBitmap;

    public MatrixClientSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void presentFrame(int[] argbPixels, int width, int height) {
        if (argbPixels == null || width <= 0 || height <= 0 || argbPixels.length < width * height) {
            return;
        }

        synchronized (frameLock) {
            if (frameBitmap == null || frameBitmap.getWidth() != width || frameBitmap.getHeight() != height) {
                if (frameBitmap != null) {
                    frameBitmap.recycle();
                }
                frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
            frameBitmap.setPixels(argbPixels, 0, width, 0, 0, width, height);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        renderThread = new Thread(this, "Matrix3-Mobile-Surface");
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (renderThread != null) {
            try {
                renderThread.join(1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas == null) {
                    continue;
                }

                canvas.drawColor(Color.BLACK);
                synchronized (frameLock) {
                    if (frameBitmap != null) {
                        canvas.drawBitmap(frameBitmap, null, canvas.getClipBounds(), paint);
                    } else {
                        paint.setColor(Color.WHITE);
                        paint.setTextSize(34.0f);
                        canvas.drawText(status, 40.0f, 70.0f, paint);
                    }
                }
            } finally {
                if (canvas != null) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }

            try {
                Thread.sleep(16L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        boolean down = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL;
        MobileInputState.updatePointer(event.getX(), event.getY(), down);
        requestFocus();
        return true;
    }
}
