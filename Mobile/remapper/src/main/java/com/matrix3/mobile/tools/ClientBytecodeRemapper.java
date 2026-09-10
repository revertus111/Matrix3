package com.matrix3.mobile.tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public final class ClientBytecodeRemapper {

    private ClientBytecodeRemapper() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected: <client classes dir> <output jar>");
        }

        final Path inputRoot = Paths.get(args[0]).toAbsolutePath().normalize();
        final Path outputJar = Paths.get(args[1]).toAbsolutePath().normalize();
        if (!Files.isDirectory(inputRoot)) {
            throw new IOException("Client classes directory does not exist: " + inputRoot);
        }

        Files.createDirectories(outputJar.getParent());
        final Set<String> mappedDesktopTypes = new TreeSet<String>();
        final Remapper remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                String mapped = mapDesktopType(internalName);
                if (!internalName.equals(mapped)) {
                    mappedDesktopTypes.add(internalName + " -> " + mapped);
                }
                return mapped;
            }
        };

        try (JarOutputStream out = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(outputJar)))) {
            Files.walk(inputRoot)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .sorted()
                    .forEach(path -> writeClass(inputRoot, path, out, remapper));
        }

        System.out.println("Matrix3 mobile client desktop API remap:");
        for (String type : mappedDesktopTypes) {
            System.out.println("  " + type);
        }
        System.out.println("Remapped client jar: " + outputJar);
    }

    private static void writeClass(Path inputRoot, Path classFile, JarOutputStream out, Remapper remapper) {
        try (InputStream in = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            ClassWriter writer = new ClassWriter(0);
            ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
            reader.accept(classRemapper, 0);

            String entryName = inputRoot.relativize(classFile).toString().replace('\\', '/');
            JarEntry entry = new JarEntry(entryName);
            entry.setTime(0L);
            out.putNextEntry(entry);
            out.write(writer.toByteArray());
            out.closeEntry();
        } catch (IOException exception) {
            throw new RuntimeException("Unable to remap " + classFile, exception);
        }
    }

    private static String mapDesktopType(String internalName) {
        if (internalName.startsWith("java/applet/")) {
            return "com/matrix3/mobile/compat/applet/" + internalName.substring("java/applet/".length());
        }
        if (internalName.startsWith("java/awt/")) {
            return "com/matrix3/mobile/compat/awt/" + internalName.substring("java/awt/".length());
        }
        if (internalName.startsWith("javax/swing/")) {
            return "com/matrix3/mobile/compat/swing/" + internalName.substring("javax/swing/".length());
        }
        if (internalName.startsWith("javax/imageio/")) {
            return "com/matrix3/mobile/compat/imageio/" + internalName.substring("javax/imageio/".length());
        }
        return internalName;
    }
}
