package dev.astatin.render;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;
import java.util.Properties;

/** Prepares the Android Skija native for Pojav's Java runtime before Skija loads. */
public final class PojavSkijaNative {
    private PojavSkijaNative() {
    }

    public static synchronized void prepare() throws IOException {
        prepare(System.getenv("POJAV_NATIVEDIR"), System.getProperty("os.arch"), System.getProperties(),
                Path.of(System.getProperty("java.io.tmpdir")), PojavSkijaNative.class.getClassLoader());
    }

    static void prepare(String pojavNativeDir, String architecture, Properties properties, Path tempRoot,
                        ClassLoader resources) throws IOException {
        if (pojavNativeDir == null || pojavNativeDir.isBlank()
                || properties.containsKey("skija.library.path")
                || "true".equals(properties.getProperty("skija.loadFromLibraryPath"))
                || "false".equals(properties.getProperty("skija.staticLoad"))) {
            return;
        }

        String abi = switch (architecture == null ? "" : architecture.toLowerCase(Locale.ROOT)) {
            case "aarch64", "arm64" -> "arm64";
            case "x86_64", "amd64", "x64" -> "x64";
            default -> throw new IOException("Unsupported Pojav architecture: " + architecture);
        };
        String resource = "io/github/humbleui/skija/android/" + abi + "/libskija.so";
        try (InputStream nativeLibrary = resources.getResourceAsStream(resource)) {
            if (nativeLibrary == null) {
                throw new IOException("Missing Android Skija native for " + abi);
            }

            // A new private directory per process avoids collisions and symlink replacement.
            Path directory = Files.createTempDirectory(tempRoot, "astatin-skija-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            Path library = directory.resolve("libskija.so");
            try {
                try (OutputStream output = Files.newOutputStream(library, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE)) {
                    nativeLibrary.transferTo(output);
                }
                properties.setProperty("skija.library.path", directory.toAbsolutePath().toString());
                directory.toFile().deleteOnExit();
                library.toFile().deleteOnExit();
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(library);
                Files.deleteIfExists(directory);
                throw e;
            }
        }
    }
}
