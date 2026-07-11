package com.cupk.service;

import com.cupk.common.FileUsage;
import com.cupk.vo.StoredFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {
    StoredFileVO store(MultipartFile file, FileUsage usage, String resourceType);
    Path resolve(String relativePath);
    void deleteIfExists(String relativePath);
}
