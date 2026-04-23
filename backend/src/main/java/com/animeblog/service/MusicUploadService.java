package com.animeblog.service;

import com.animeblog.dto.MusicUploadResponse;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MusicUploadService {

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "flac", "ogg", "m4a", "aac");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final String UPLOAD_URL_PREFIX = "/uploads/";

    @Value("${app.upload.dir:./storage/uploads}")
    private String uploadDir;

    @Value("${app.upload.music.max-audio-file-size-bytes:104857600}")
    private long maxAudioFileSizeBytes;

    @Value("${app.upload.music.max-cover-file-size-bytes:10485760}")
    private long maxCoverFileSizeBytes;

    public MusicUploadResponse upload(MultipartFile audioFile, MultipartFile coverFile) throws IOException {
        if ((audioFile == null || audioFile.isEmpty()) && (coverFile == null || coverFile.isEmpty())) {
            throw new IllegalArgumentException("请至少上传一个音频或封面文件");
        }

        MusicUploadResponse response = new MusicUploadResponse();

        if (audioFile != null && !audioFile.isEmpty()) {
            validateFileSize(audioFile, maxAudioFileSizeBytes, "音频");
            String extension = validateExtension(audioFile.getOriginalFilename(), AUDIO_EXTENSIONS, "音频");
            Path storedAudio = storeFile(audioFile, "music/audio", extension);
            response.setUrl(toPublicUrl(storedAudio));
            response.setOriginalFilename(audioFile.getOriginalFilename());
            fillAudioMetadata(storedAudio, audioFile.getOriginalFilename(), response);
        }

        if (coverFile != null && !coverFile.isEmpty()) {
            validateFileSize(coverFile, maxCoverFileSizeBytes, "封面");
            String extension = validateExtension(coverFile.getOriginalFilename(), IMAGE_EXTENSIONS, "封面");
            Path storedCover = storeFile(coverFile, "music/cover", extension);
            response.setCoverUrl(toPublicUrl(storedCover));
        }

        return response;
    }

    public void deleteManagedFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith(UPLOAD_URL_PREFIX)) {
            return;
        }

        Path root = getUploadRoot();
        String relativePath = fileUrl.substring(UPLOAD_URL_PREFIX.length()).replace("/", java.io.File.separator);
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private Path storeFile(MultipartFile file, String categoryPath, String extension) throws IOException {
        Path targetDir = getUploadRoot().resolve(categoryPath).normalize();
        Files.createDirectories(targetDir);

        String filename = UUID.randomUUID() + "." + extension;
        Path target = targetDir.resolve(filename).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return target;
    }

    private void fillAudioMetadata(Path audioPath, String originalFilename, MusicUploadResponse response) {
        String fallbackTitle = StringUtils.hasText(originalFilename)
                ? stripExtension(originalFilename)
                : stripExtension(audioPath.getFileName().toString());
        response.setTitle(fallbackTitle);

        try {
            AudioFile audioFile = AudioFileIO.read(audioPath.toFile());
            Tag tag = audioFile.getTag();
            if (tag != null) {
                response.setTitle(firstNonBlank(tag.getFirst(FieldKey.TITLE), fallbackTitle));
                response.setArtist(emptyToNull(tag.getFirst(FieldKey.ARTIST)));
                response.setAlbum(emptyToNull(tag.getFirst(FieldKey.ALBUM)));
            }

            AudioHeader audioHeader = audioFile.getAudioHeader();
            if (audioHeader != null && audioHeader.getTrackLength() > 0) {
                response.setDuration(audioHeader.getTrackLength());
            } else if (audioHeader instanceof GenericAudioHeader genericAudioHeader) {
                float preciseLength = genericAudioHeader.getPreciseLength();
                if (Float.isFinite(preciseLength) && preciseLength > 0) {
                    response.setDuration(Math.round(preciseLength));
                }
            }
        } catch (Exception ignored) {
        }

        if (response.getDuration() == null || response.getDuration() <= 0) {
            response.setDuration(extractDurationWithJavaSound(audioPath));
        }
    }

    private Integer extractDurationWithJavaSound(Path audioPath) {
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(audioPath.toFile());
            Map<String, Object> properties = fileFormat.properties();
            Object duration = properties.get("duration");
            if (duration instanceof Long) {
                return (int) (((Long) duration) / 1_000_000L);
            }
        } catch (Exception ignored) {
        }

        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioPath.toFile())) {
            long frames = audioInputStream.getFrameLength();
            float frameRate = audioInputStream.getFormat().getFrameRate();
            if (frames > 0 && frameRate > 0) {
                return (int) Math.round(frames / frameRate);
            }
        } catch (Exception ignored) {
        }

        return 0;
    }

    private String validateExtension(String originalFilename, Set<String> allowedExtensions, String fileLabel) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException(fileLabel + "文件缺少扩展名");
        }

        String normalized = extension.toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(normalized)) {
            throw new IllegalArgumentException(fileLabel + "文件格式不支持");
        }
        return normalized;
    }

    private void validateFileSize(MultipartFile file, long maxSizeBytes, String fileLabel) {
        if (maxSizeBytes > 0 && file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(fileLabel + "文件大小不能超过 " + formatFileSize(maxSizeBytes));
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes >= 1024L * 1024L) {
            double sizeInMb = bytes / (1024d * 1024d);
            return String.format(Locale.ROOT, "%.0fMB", sizeInMb);
        }
        if (bytes >= 1024L) {
            double sizeInKb = bytes / 1024d;
            return String.format(Locale.ROOT, "%.0fKB", sizeInKb);
        }
        return bytes + "B";
    }

    private String toPublicUrl(Path storedPath) {
        Path relative = getUploadRoot().relativize(storedPath);
        return UPLOAD_URL_PREFIX + relative.toString().replace("\\", "/");
    }

    private Path getUploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String stripExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
