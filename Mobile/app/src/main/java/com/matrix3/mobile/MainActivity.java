package com.matrix3.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class MainActivity extends Activity {

    private static final int CACHE_PICKER_REQUEST = 830;
    private static final int LOGIN_PORT = 7777;
    private static final int GAME_PORT = 43593;

    private MatrixClientSurfaceView clientSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        clientSurface = new MatrixClientSurfaceView(this);
        setContentView(clientSurface);
        bootstrap();
    }

    private void bootstrap() {
        clientSurface.setStatus("Preparing Matrix3 data...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerRuntime.ensureBundledData(MainActivity.this);
                    if (ServerRuntime.hasCache(MainActivity.this)) {
                        startMatrixStack();
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                requestCacheFolder();
                            }
                        });
                    }
                } catch (Throwable throwable) {
                    showFailure("Bootstrap failed: " + throwable.getMessage());
                }
            }
        }, "Matrix3-Mobile-Bootstrap").start();
    }

    private void requestCacheFolder() {
        clientSurface.setStatus("Select the revision-830 cache folder...");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, CACHE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CACHE_PICKER_REQUEST) {
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            clientSurface.setStatus("Revision-830 cache is required to start Matrix3.");
            return;
        }

        final Uri treeUri = data.getData();
        int takeFlags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
        } catch (SecurityException ignored) {
        }

        clientSurface.setStatus("Importing revision-830 cache...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!ServerRuntime.importCache(MainActivity.this, treeUri)) {
                        showFailure("That folder does not contain main_file_cache.dat2.");
                        return;
                    }
                    startMatrixStack();
                } catch (IOException exception) {
                    showFailure("Cache import failed: " + exception.getMessage());
                }
            }
        }, "Matrix3-Mobile-CacheImport").start();
    }

    private void startMatrixStack() {
        clientSurface.setStatus("Starting Matrix3 login server...");
        startService(new Intent(this, LoginServerService.class));

        if (!waitForPort(LOGIN_PORT, 30000L)) {
            showFailure("Login server did not open port " + LOGIN_PORT + ".");
            return;
        }

        clientSurface.setStatus("Starting Matrix3 game server...");
        startService(new Intent(this, GameServerService.class));

        if (!waitForPort(GAME_PORT, 60000L)) {
            showFailure("Game server did not open port " + GAME_PORT + ".");
            return;
        }

        clientSurface.setStatus("Matrix3 servers online. Preparing 830 Android client host...");
    }

    private boolean waitForPort(int port, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
                return true;
            } catch (IOException ignored) {
                try {
                    Thread.sleep(250L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void showFailure(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                clientSurface.setStatus(message);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        MobileInputState.updateKey(keyCode, true);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        MobileInputState.updateKey(keyCode, false);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            stopService(new Intent(this, GameServerService.class));
            stopService(new Intent(this, LoginServerService.class));
        }
        super.onDestroy();
    }
}
