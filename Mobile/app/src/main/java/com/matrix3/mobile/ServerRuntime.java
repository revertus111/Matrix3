package com.matrix3.mobile;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.system.Os;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class ServerRuntime {

    private static final String SERVER_ASSET_ROOT = "server";
    private static final String INSTALL_MARKER = ".bundled_data_installed_v1";
    private static final String CACHE_SENTINEL = "main_file_cache.dat2";

    private ServerRuntime() {
    }

    static File getServerHome(Context context) {
        return new File(context.getFilesDir(), "matrix3/server");
    }

    static File getCacheHome(Context context) {
        return new File(getServerHome(context), "data/cache");
    }

    static boolean hasCache(Context context) {
        return new File(getCacheHome(context), CACHE_SENTINEL).isFile();
    }

    static synchronized void ensureBundledData(Context context) throws IOException {
        File serverHome = getServerHome(context);
        File marker = new File(serverHome, INSTALL_MARKER);
        if (marker.isFile()) {
            return;
        }

        if (!serverHome.exists() && !serverHome.mkdirs()) {
            throw new IOException("Unable to create Matrix3 server home: " + serverHome);
        }

        copyAssetTree(context.getAssets(), SERVER_ASSET_ROOT, serverHome);
        if (!marker.createNewFile() && !marker.isFile()) {
            throw new IOException("Unable to write Matrix3 mobile install marker.");
        }
    }

    static void prepareServerProcess(Context context) throws Exception {
        ensureBundledData(context);
        File serverHome = getServerHome(context);
        System.setProperty("user.dir", serverHome.getAbsolutePath());
        Os.chdir(serverHome.getAbsolutePath());
    }

    static boolean importCache(Context context, Uri treeUri) throws IOException {
        DocumentFile selected = DocumentFile.fromTreeUri(context, treeUri);
        if (selected == null || !selected.isDirectory()) {
            return false;
        }

        DocumentFile source = resolveCacheDirectory(selected);
        if (source == null) {
            return false;
        }

        File cacheHome = getCacheHome(context);
        if (!cacheHome.exists() && !cacheHome.mkdirs()) {
            throw new IOException("Unable to create cache directory: " + cacheHome);
        }

        copyDocumentDirectory(context, source, cacheHome);
        return hasCache(context);
    }

    private static DocumentFile resolveCacheDirectory(DocumentFile selected) {
        DocumentFile sentinel = selected.findFile(CACHE_SENTINEL);
        if (sentinel != null && sentinel.isFile()) {
            return selected;
        }

        DocumentFile nestedCache = selected.findFile("cache");
        if (nestedCache != null && nestedCache.isDirectory()) {
            sentinel = nestedCache.findFile(CACHE_SENTINEL);
            if (sentinel != null && sentinel.isFile()) {
                return nestedCache;
            }
        }
        return null;
    }

    private static void copyDocumentDirectory(Context context, DocumentFile source, File destination)
            throws IOException {
        for (DocumentFile child : source.listFiles()) {
            File output = new File(destination, child.getName());
            if (child.isDirectory()) {
                if (!output.exists() && !output.mkdirs()) {
                    throw new IOException("Unable to create directory: " + output);
                }
                copyDocumentDirectory(context, child, output);
            } else if (child.isFile()) {
                try (InputStream in = context.getContentResolver().openInputStream(child.getUri())) {
                    if (in == null) {
                        throw new IOException("Unable to open cache file: " + child.getUri());
                    }
                    copyStream(in, output);
                }
            }
        }
    }

    private static void copyAssetTree(AssetManager assets, String assetPath, File destination)
            throws IOException {
        String[] children = assets.list(assetPath);
        if (children == null) {
            throw new IOException("Unable to list bundled asset path: " + assetPath);
        }

        if (children.length == 0) {
            File parent = destination.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Unable to create asset parent: " + parent);
            }
            try (InputStream in = assets.open(assetPath)) {
                copyStream(in, destination);
            }
            return;
        }

        if (!destination.exists() && !destination.mkdirs()) {
            throw new IOException("Unable to create asset directory: " + destination);
        }

        for (String child : children) {
            copyAssetTree(assets, assetPath + "/" + child, new File(destination, child));
        }
    }

    private static void copyStream(InputStream in, File output) throws IOException {
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create output parent: " + parent);
        }

        try (OutputStream out = new FileOutputStream(output)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
