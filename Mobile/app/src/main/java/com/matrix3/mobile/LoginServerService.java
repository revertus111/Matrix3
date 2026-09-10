package com.matrix3.mobile;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;

import com.rs.LoginLauncher;

public final class LoginServerService extends Service {

    private Thread launcherThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (launcherThread != null && launcherThread.isAlive()) {
            return START_NOT_STICKY;
        }

        launcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerRuntime.prepareServerProcess(LoginServerService.this);
                    LoginLauncher.main(new String[] { "true", "false" });
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    stopSelf();
                }
            }
        }, "Matrix3-LoginLauncher");
        launcherThread.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
