package com.matrix3.mobile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class ClientRuntime {

    private ClientRuntime() {
    }

    static void start(MatrixClientSurfaceView surface) throws Exception {
        MobileAwtBridge.attach(surface);
        MobileAwtBridge.setStatus("Starting Matrix3 revision-830 client...");

        Class<?> appletClass = Class.forName("game.RS3Applet");
        Object applet = appletClass.getDeclaredConstructor().newInstance();
        Method init = appletClass.getMethod("init");
        try {
            init.invoke(applet);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw exception;
        }
    }
}
