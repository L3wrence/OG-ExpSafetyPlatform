package com.cupk.util;

import com.cupk.common.FileUsage;
import com.cupk.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ResourceFileValidator {
    private static final Map<String, Set<String>> EXTENSIONS = Map.of(
            "VIDEO", Set.of("mp4", "webm"), "DOCUMENT", Set.of("pdf", "txt", "md"),
            "IMAGE", Set.of("jpg", "jpeg", "png", "webp", "gif"), "AUDIO", Set.of("mp3", "wav", "ogg"));
    private static final Map<String, Long> MAX_BYTES = Map.of(
            "VIDEO", 500L * 1024 * 1024, "DOCUMENT", 50L * 1024 * 1024,
            "IMAGE", 10L * 1024 * 1024, "AUDIO", 100L * 1024 * 1024);

    public void validate(MultipartFile file, FileUsage usage, String resourceType) {
        if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
            throw new BusinessException(400, "请选择本地文件");
        }
        if (usage == FileUsage.COURSE_COVER) {
            validateCover(file);
            return;
        }
        String type = resourceType == null ? "" : resourceType.toUpperCase(Locale.ROOT);
        Set<String> allowed = EXTENSIONS.get(type);
        if (allowed == null) throw new BusinessException(400, "资源类型只能是视频、文档、图片或音频");
        String ext = extension(file.getOriginalFilename());
        if (!allowed.contains(ext) || !mimeMatches(type, file.getContentType())) {
            throw new BusinessException(400, typeMessage(type));
        }
        if (file.getSize() > MAX_BYTES.get(type)) throw new BusinessException(400, typeMessage(type));
        if ("IMAGE".equals(type)) verifyImage(file);
        if ("DOCUMENT".equals(type) && "pdf".equals(ext)) verifyPdf(file);
    }

    private void validateCover(MultipartFile file) {
        String ext = extension(file.getOriginalFilename());
        if (!Set.of("jpg", "jpeg", "png", "webp").contains(ext) || file.getSize() > 5L * 1024 * 1024
                || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BusinessException(400, "课程封面只支持 JPEG/PNG/WebP，且不能超过 5MB");
        }
        verifyImage(file);
    }

    private boolean mimeMatches(String type, String mime) {
        if (!StringUtils.hasText(mime)) return false;
        return switch (type) {
            case "VIDEO" -> Set.of("video/mp4", "video/webm").contains(mime);
            case "DOCUMENT" -> Set.of("application/pdf", "text/plain", "text/markdown").contains(mime);
            case "IMAGE" -> Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(mime);
            case "AUDIO" -> Set.of("audio/mpeg", "audio/wav", "audio/x-wav", "audio/ogg").contains(mime);
            default -> false;
        };
    }

    private void verifyImage(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            if (ImageIO.read(in) == null) throw new BusinessException(400, "图片文件无法解析");
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(400, "图片文件无法解析"); }
    }

    private void verifyPdf(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            if (!new String(in.readNBytes(5)).equals("%PDF-")) throw new BusinessException(400, "PDF 文件格式不正确");
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(400, "PDF 文件格式不正确"); }
    }

    private String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private String typeMessage(String type) {
        return switch (type) {
            case "VIDEO" -> "视频只支持 MP4/WebM，且不能超过 500MB";
            case "DOCUMENT" -> "文档只支持 PDF/TXT/MD，且不能超过 50MB";
            case "IMAGE" -> "图片只支持 JPG/PNG/WebP/GIF，且不能超过 10MB";
            case "AUDIO" -> "音频只支持 MP3/WAV/OGG，且不能超过 100MB";
            default -> "不支持的资源文件";
        };
    }
}
