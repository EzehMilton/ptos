package com.ptos.integration.local;

// Replace with S3StorageService for production

import com.ptos.integration.FileStorageGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageGateway {

    private final Path rootDirectory;

    public LocalFileStorageService(@Value("${ptos.storage.local-dir:./uploads}") String localDir) {
        this.rootDirectory = Paths.get(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subdirectory) {
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        String normalizedSubdirectory = normalizeSubdirectory(subdirectory);
        Path targetDirectory = rootDirectory.resolve(normalizedSubdirectory).normalize();
        Path targetFile = targetDirectory.resolve(filename).normalize();

        ensureWithinRoot(targetDirectory);
        ensureWithinRoot(targetFile);

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store uploaded file", e);
        }

        return normalizedSubdirectory + "/" + filename;
    }

    @Override
    public Resource load(String key) {
        Path file = resolveKey(key);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file path", e);
        }
    }

    @Override
    public void delete(String key) {
        Path file = resolveKey(key);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete stored file", e);
        }
    }

    @Override
    public String getUrl(String key) {
        return "/uploads/" + key.replace("\\", "/");
    }

    private Path resolveKey(String key) {
        Path file = rootDirectory.resolve(key).normalize();
        ensureWithinRoot(file);
        return file;
    }

    private void ensureWithinRoot(Path path) {
        if (!path.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
    }

    private String normalizeSubdirectory(String subdirectory) {
        String normalized = subdirectory.replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(lastDot).toLowerCase();
    }
}
