package dev.astatin.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PojavSkijaNativeTest {
    @TempDir Path temp;

    private final ClassLoader resources = new ClassLoader() {
        @Override
        public InputStream getResourceAsStream(String name) {
            if (name.equals("io/github/humbleui/skija/android/arm64/libskija.so")) {
                return new ByteArrayInputStream("arm native".getBytes(StandardCharsets.UTF_8));
            }
            if (name.equals("io/github/humbleui/skija/android/x64/libskija.so")) {
                return new ByteArrayInputStream("x64 native".getBytes(StandardCharsets.UTF_8));
            }
            return null;
        }
    };

    @Test
    void desktopAndExplicitOverridesAreUntouched() throws Exception {
        Properties desktop = new Properties();
        PojavSkijaNative.prepare(null, "unsupported", desktop, temp, resources);
        assertFalse(desktop.containsKey("skija.library.path"));
        for (String override : List.of("skija.library.path", "skija.loadFromLibraryPath", "skija.staticLoad")) {
            Properties properties = new Properties();
            String value = switch (override) {
                case "skija.staticLoad" -> "false";
                case "skija.loadFromLibraryPath" -> "true";
                default -> "user-value";
            };
            properties.setProperty(override, value);
            PojavSkijaNative.prepare("/launcher", "unsupported", properties, temp, resources);
            assertEquals(value, properties.getProperty(override));
            assertEquals(1, properties.size());
        }
        try (var paths = Files.list(temp)) {
            assertEquals(0, paths.count());
        }
    }

    @Test
    void enabledStaticLoadDoesNotBlockAndroidNativeSelection() throws Exception {
        assumePosix();
        Properties properties = new Properties();
        properties.setProperty("skija.staticLoad", "true");
        properties.setProperty("skija.loadFromLibraryPath", "false");
        PojavSkijaNative.prepare("/launcher", "arm64", properties, temp, resources);
        assertEquals("arm native", Files.readString(Path.of(properties.getProperty("skija.library.path"))
                .resolve("libskija.so")));
    }

    @Test
    void selectsCorrectNativeForEachAbiAlias() throws Exception {
        assumePosix();
        for (String arch : List.of("aarch64", "arm64", "amd64", "x86_64", "x64")) {
            Properties properties = new Properties();
            PojavSkijaNative.prepare("/launcher", arch, properties, temp, resources);
            Path dir = Path.of(properties.getProperty("skija.library.path"));
            String expected = arch.equals("aarch64") || arch.equals("arm64") ? "arm native" : "x64 native";
            assertEquals(expected, Files.readString(dir.resolve("libskija.so")));
            assertEquals(temp, dir.getParent());
            assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(dir));
        }
    }

    @Test
    void rejectsUnsupportedArchitectureAndMissingNativeWithoutSettingPath() throws Exception {
        assumePosix();
        Properties properties = new Properties();
        assertThrows(java.io.IOException.class,
                () -> PojavSkijaNative.prepare("/launcher", "../../arm64", properties, temp, resources));
        ClassLoader empty = new ClassLoader(null) {};
        assertThrows(java.io.IOException.class,
                () -> PojavSkijaNative.prepare("/launcher", "arm64", properties, temp, empty));
        assertThrows(java.io.IOException.class,
                () -> PojavSkijaNative.prepare("/launcher", "arm64", properties, temp.resolve("missing"), resources));
        assertFalse(properties.containsKey("skija.library.path"));
    }

    @Test
    void failedExtractionRemovesPartialFileAndDirectory() throws Exception {
        assumePosix();
        Properties properties = new Properties();
        ClassLoader broken = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return new InputStream() {
                    @Override
                    public int read() throws java.io.IOException {
                        throw new java.io.IOException("interrupted download");
                    }
                };
            }
        };
        assertThrows(java.io.IOException.class,
                () -> PojavSkijaNative.prepare("/launcher", "arm64", properties, temp, broken));
        assertFalse(properties.containsKey("skija.library.path"));
        try (var paths = Files.list(temp)) {
            assertEquals(0, paths.count());
        }
    }

    @Test
    void concurrentSessionsUseDifferentDirectories() throws Exception {
        assumePosix();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> extract());
            var second = executor.submit(() -> extract());
            assertNotEquals(first.get(), second.get());
        }
    }

    private Path extract() throws Exception {
        Properties properties = new Properties();
        PojavSkijaNative.prepare("/launcher", "arm64", properties, temp, resources);
        Path directory = Path.of(properties.getProperty("skija.library.path"));
        assertEquals("arm native", Files.readString(directory.resolve("libskija.so")));
        return directory;
    }

    private void assumePosix() throws Exception {
        assumeTrue(Files.getFileStore(temp).supportsFileAttributeView("posix"));
    }
}
