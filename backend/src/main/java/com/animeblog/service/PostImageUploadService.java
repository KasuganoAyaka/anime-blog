package com.animeblog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PostImageUploadService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final String UPLOAD_URL_PREFIX = "/uploads/";
    private static final int SIGNATURE_READ_LIMIT = 16;
    private static final float WEBP_QUALITY = 0.84f;

    static {
        ImageIO.scanForPlugins();
    }

    @Value("${app.upload.dir:./storage/uploads}")
    private String uploadDir;

    @Value("${app.upload.post-image.max-files:10}")
    private int maxFiles;

    @Value("${app.upload.post-image.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Value("${app.upload.post-image.max-total-size-bytes:20971520}")
    private long maxTotalSizeBytes;

    @Value("${app.upload.avatar.max-file-size-bytes:5242880}")
    private long maxAvatarFileSizeBytes;

    @Value("${app.upload.avatar.max-dimension:512}")
    private int maxAvatarDimension;

    public List<String> upload(List<MultipartFile> imageFiles) throws IOException {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("请至少上传一张图片");
        }
        if (imageFiles.size() > maxFiles) {
            throw new IllegalArgumentException("单次最多上传 " + maxFiles + " 张图片");
        }

        List<String> urls = new ArrayList<>();
        List<Path> storedPaths = new ArrayList<>();
        long totalSize = 0L;

        try {
            for (MultipartFile imageFile : imageFiles) {
                if (imageFile == null || imageFile.isEmpty()) {
                    continue;
                }

                validateFileSize(imageFile, maxFileSizeBytes);
                totalSize += imageFile.getSize();
                if (totalSize > maxTotalSizeBytes) {
                    throw new IllegalArgumentException("图片总大小不能超过 " + formatSize(maxTotalSizeBytes));
                }

                String detectedExtension = validateAndResolveExtension(imageFile);
                Path storedImage = storeOptimizedImage(imageFile, "post/images", detectedExtension, 0);
                storedPaths.add(storedImage);
                urls.add(toPublicUrl(storedImage));
            }
        } catch (IOException | RuntimeException ex) {
            rollbackStoredFiles(storedPaths);
            throw ex;
        }

        if (urls.isEmpty()) {
            throw new IllegalArgumentException("请至少上传一张有效图片");
        }
        return urls;
    }

    public String uploadAvatar(MultipartFile avatarFile) throws IOException {
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("请选择头像图片");
        }

        validateFileSize(avatarFile, maxAvatarFileSizeBytes);
        String detectedExtension = validateAndResolveExtension(avatarFile);
        Path storedAvatar = storeOptimizedImage(avatarFile, "avatars", detectedExtension, maxAvatarDimension);
        return toPublicUrl(storedAvatar);
    }

    public void deleteManagedFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith(UPLOAD_URL_PREFIX)) {
            return;
        }

        Path uploadRoot = getUploadRoot();
        Path relativePath = Paths.get(fileUrl.substring(UPLOAD_URL_PREFIX.length())).normalize();
        Path targetPath = uploadRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ignored) {
            // Ignore cleanup failures for managed assets.
        }
    }

    private void validateFileSize(MultipartFile file, long limitBytes) {
        if (file.getSize() <= 0) {
            throw new IllegalArgumentException("请上传有效图片");
        }
        if (file.getSize() > limitBytes) {
            throw new IllegalArgumentException("图片大小不能超过 " + formatSize(limitBytes));
        }
    }

    private String validateAndResolveExtension(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            throw new IllegalArgumentException("图片文件缺少扩展名");
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if (!IMAGE_EXTENSIONS.contains(normalizedExtension)) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、WebP、GIF 图片");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("上传文件必须是图片");
        }

        String detectedExtension = detectImageExtension(file);
        if (detectedExtension == null || !IMAGE_EXTENSIONS.contains(detectedExtension)) {
            throw new IllegalArgumentException("图片内容无效或格式不受支持");
        }
        if (!isExtensionCompatible(normalizedExtension, detectedExtension)) {
            throw new IllegalArgumentException("图片扩展名与实际内容不匹配");
        }

        return "jpeg".equals(detectedExtension) ? "jpg" : detectedExtension;
    }

    private Path storeOptimizedImage(MultipartFile file, String categoryPath, String detectedExtension, int maxDimension) throws IOException {
        Path targetDir = getUploadRoot().resolve(categoryPath).normalize();
        Files.createDirectories(targetDir);

        DecodedImage decodedImage = decodeImage(file, detectedExtension);
        if (decodedImage.animatedGif()) {
            return copyOriginalFile(file, targetDir, "gif");
        }

        BufferedImage normalizedImage = normalizeImage(decodedImage.bufferedImage());
        if (maxDimension > 0) {
            normalizedImage = resizeIfNeeded(normalizedImage, maxDimension);
        }

        Path target = targetDir.resolve(UUID.randomUUID() + ".webp").normalize();
        writeWebp(normalizedImage, target);
        return target;
    }

    private Path copyOriginalFile(MultipartFile file, Path targetDir, String extension) throws IOException {
        Path target = targetDir.resolve(UUID.randomUUID() + "." + extension).normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private DecodedImage decodeImage(MultipartFile file, String detectedExtension) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageInputStream == null) {
                throw new IllegalArgumentException("无法读取图片内容");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("当前图片格式暂不支持处理");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                boolean animatedGif = "gif".equals(detectedExtension) && isAnimatedGif(reader);
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("无法解析图片内容");
                }
                return new DecodedImage(image, animatedGif);
            } finally {
                reader.dispose();
            }
        }
    }

    private boolean isAnimatedGif(ImageReader reader) {
        try {
            return reader.getNumImages(true) > 1;
        } catch (IOException ignored) {
            return false;
        }
    }

    private BufferedImage normalizeImage(BufferedImage sourceImage) {
        BufferedImage normalized = new BufferedImage(
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return normalized;
    }

    private BufferedImage resizeIfNeeded(BufferedImage sourceImage, int maxDimension) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxDimension) {
            return sourceImage;
        }

        double scale = maxDimension / (double) longestSide;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private void writeWebp(BufferedImage image, Path target) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IllegalStateException("WebP writer is not available");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(Files.newOutputStream(target))) {
            writer.setOutput(outputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compressionTypes = writeParam.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    writeParam.setCompressionType(compressionTypes[0]);
                }
                writeParam.setCompressionQuality(WEBP_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private String toPublicUrl(Path storedPath) {
        Path relative = getUploadRoot().relativize(storedPath);
        return UPLOAD_URL_PREFIX + relative.toString().replace("\\", "/");
    }

    private Path getUploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String detectImageExtension(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(SIGNATURE_READ_LIMIT);
            return detectImageExtension(header);
        }
    }

    private String detectImageExtension(byte[] header) {
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 'P'
                && header[2] == 'N'
                && header[3] == 'G'
                && (header[4] & 0xFF) == 0x0D
                && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A
                && (header[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (header.length >= 6) {
            String signature = new String(header, 0, 6, StandardCharsets.US_ASCII);
            if ("GIF87a".equals(signature) || "GIF89a".equals(signature)) {
                return "gif";
            }
        }
        if (header.length >= 12) {
            String riff = new String(header, 0, 4, StandardCharsets.US_ASCII);
            String webp = new String(header, 8, 4, StandardCharsets.US_ASCII);
            if ("RIFF".equals(riff) && "WEBP".equals(webp)) {
                return "webp";
            }
        }
        return null;
    }

    private boolean isExtensionCompatible(String declaredExtension, String detectedExtension) {
        if ("jpeg".equals(declaredExtension)) {
            declaredExtension = "jpg";
        }
        if ("jpeg".equals(detectedExtension)) {
            detectedExtension = "jpg";
        }
        return declaredExtension.equals(detectedExtension);
    }

    private void rollbackStoredFiles(List<Path> storedPaths) {
        for (int i = storedPaths.size() - 1; i >= 0; i--) {
            Path path = storedPaths.get(i);
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Ignore rollback cleanup failures.
            }
        }
    }

    private String formatSize(long sizeInBytes) {
        long megaBytes = 1024L * 1024L;
        if (sizeInBytes % megaBytes == 0) {
            return (sizeInBytes / megaBytes) + "MB";
        }
        return String.format(Locale.ROOT, "%.1fMB", sizeInBytes / (double) megaBytes);
    }

    private record DecodedImage(BufferedImage bufferedImage, boolean animatedGif) {
    }
}
