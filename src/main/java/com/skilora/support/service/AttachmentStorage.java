package com.skilora.support.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AttachmentStorage {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentStorage.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
        "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg", "gif", "bmp", "zip", "rar"
    );
    private static final java.util.Set<String> ALLOWED_MIME_PREFIXES = java.util.Set.of(
        "image/", "text/", "application/pdf", "application/msword",
        "application/vnd.openxmlformats-officedocument", "application/zip", "application/x-rar"
    );

    private AttachmentStorage() {}

    public static StoredAttachment store(File source, int ticketId) throws IOException {
        if (source == null) throw new IOException("Source file is null");
        if (!source.exists() || !source.isFile()) throw new IOException("Source file does not exist: " + source);

        // Validate file size
        if (source.length() > MAX_FILE_SIZE) {
            throw new IOException("File exceeds maximum size of 10 MB: " + source.getName());
        }

        // Validate extension
        String ext = getExtension(source.getName()).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IOException("File type not allowed: ." + ext);
        }

        String home = System.getProperty("user.home");
        Path baseDir = Path.of(home, ".skilora", "attachments", String.valueOf(ticketId));
        Files.createDirectories(baseDir);

        String safeName = sanitizeFileName(source.getName());
        String fileName = TS.format(LocalDateTime.now()) + "_" + safeName;
        Path target = baseDir.resolve(fileName);

        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

        String mime = null;
        try {
            mime = Files.probeContentType(target);
        } catch (IOException e) {
            logger.debug("Could not detect mime type for {}", target, e);
        }

        // Validate MIME type — delete file if not allowed
        if (mime != null && ALLOWED_MIME_PREFIXES.stream().noneMatch(mime::startsWith)) {
            Files.deleteIfExists(target);
            throw new IOException("File MIME type not allowed: " + mime);
        }

        long size = Files.size(target);

        return new StoredAttachment(source.getName(), mime, target.toAbsolutePath().toString(), size);
    }

    private static String getExtension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static final class StoredAttachment {
        private final String originalName;
        private final String mimeType;
        private final String storedPath;
        private final long size;

        StoredAttachment(String originalName, String mimeType, String storedPath, long size) {
            this.originalName = originalName;
            this.mimeType = mimeType;
            this.storedPath = storedPath;
            this.size = size;
        }

        public String getOriginalName() { return originalName; }
        public String getMimeType() { return mimeType; }
        public String getStoredPath() { return storedPath; }
        public long getSize() { return size; }
    }
}

