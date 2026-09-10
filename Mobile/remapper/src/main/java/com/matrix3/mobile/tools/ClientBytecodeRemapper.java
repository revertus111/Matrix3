package com.matrix3.mobile.tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
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
        final Set<String> desktopMethods = new TreeSet<String>();
        final Set<String> desktopFields = new TreeSet<String>();
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
                    .forEach(path -> writeClass(inputRoot, path, out, remapper, desktopMethods, desktopFields));
        }

        System.out.println("Matrix3 mobile client desktop API remap:");
        for (String type : mappedDesktopTypes) {
            System.out.println("  " + type);
        }
        System.out.println("Matrix3 mobile desktop method contract:");
        for (String method : desktopMethods) {
            System.out.println("  " + method);
        }
        System.out.println("Matrix3 mobile desktop field contract:");
        for (String field : desktopFields) {
            System.out.println("  " + field);
        }
        System.out.println("Remapped client jar: " + outputJar);
    }

    private static void writeClass(Path inputRoot, Path classFile, JarOutputStream out, Remapper remapper,
            Set<String> desktopMethods, Set<String> desktopFields) {
        try (InputStream in = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            reader.accept(new DesktopApiUsageCollector(desktopMethods, desktopFields),
                    ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

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

    private static final class DesktopApiUsageCollector extends ClassVisitor {

        private final Set<String> methods;
        private final Set<String> fields;

        DesktopApiUsageCollector(Set<String> methods, Set<String> fields) {
            super(Opcodes.ASM9);
            this.methods = methods;
            this.fields = fields;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor,
                        boolean isInterface) {
                    if (isDesktopType(owner)) {
                        methods.add(owner + "." + methodName + methodDescriptor);
                    }
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                    if (isDesktopType(owner)) {
                        fields.add(owner + "." + fieldName + ":" + fieldDescriptor);
                    }
                }
            };
        }
    }

    private static boolean isDesktopType(String internalName) {
        return internalName.startsWith("java/applet/")
                || internalName.startsWith("java/awt/")
                || internalName.startsWith("javax/swing/")
                || internalName.startsWith("javax/imageio/");
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
