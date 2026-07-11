package com.cupk.service.impl;

import com.cupk.common.FileUsage;
import com.cupk.config.FileStorageProperties;
import com.cupk.exception.BusinessException;
import com.cupk.service.FileStorageService;
import com.cupk.util.ResourceFileValidator;
import com.cupk.vo.StoredFileVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {
    private final FileStorageProperties properties;
    private final ResourceFileValidator validator;

    public LocalFileStorageService(FileStorageProperties properties, ResourceFileValidator validator) {
        this.properties = properties;
        this.validator = validator;
    }

    @Override
    public StoredFileVO store(MultipartFile file, FileUsage usage, String resourceType) {
        validator.validate(file, usage, resourceType);
        String original = Path.of(file.getOriginalFilename()).getFileName().toString();
        String ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        LocalDate now = LocalDate.now();
        String relative = usage.directory() + "/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + UUID.randomUUID() + ext;
        Path target = safeResolve(relative);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return new StoredFileVO(relative, original, file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new BusinessException(500, "文件保存失败");
        }
    }

    @Override public Path resolve(String relativePath) {
        Path path = safeResolve(relativePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) throw new BusinessException(404, "文件不存在");
        return path;
    }

    @Override public void deleteIfExists(String relativePath) {
        if (relativePath == null) return;
        try { Files.deleteIfExists(safeResolve(relativePath)); }
        catch (Exception e) { throw new BusinessException(500, "文件清理失败"); }
    }

    private Path safeResolve(String relativePath) {
        if (relativePath == null || relativePath.contains("..") || relativePath.contains("\\") || Path.of(relativePath).isAbsolute()) {
            throw new BusinessException(400, "文件路径不合法");
        }
        Path root = properties.absoluteRoot();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) throw new BusinessException(400, "文件路径不合法");
        return resolved;
    }
}
