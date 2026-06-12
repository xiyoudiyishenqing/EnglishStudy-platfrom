package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.entity.ResourceItem;
import com.englishstudy.backend.mapper.ResourceMapper;
import com.englishstudy.backend.request.ResourceRequest;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.LogService;
import com.englishstudy.backend.service.ResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResourceServiceImpl extends BaseService implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final LogService logService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public ResourceServiceImpl(ResourceMapper resourceMapper, LogService logService) {
        this.resourceMapper = resourceMapper;
        this.logService = logService;
    }

    @Override
    public List<Map<String, Object>> listAll(String keyword) {
        requireRole(RoleConstants.STUDENT, RoleConstants.ADMIN);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return resourceMapper.selectList(Wrappers.<ResourceItem>lambdaQuery()
                        .orderByDesc(ResourceItem::getCreatedAt))
                .stream()
                .filter(this::canViewResource)
                .filter(item -> safeKeyword.isEmpty()
                        || item.getTitle().toLowerCase().contains(safeKeyword)
                        || (item.getDescription() != null && item.getDescription().toLowerCase().contains(safeKeyword)))
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listOwn() {
        return resourceMapper.selectList(Wrappers.<ResourceItem>lambdaQuery()
                        .eq(ResourceItem::getCreatorId, currentUser().getUserId())
                        .orderByDesc(ResourceItem::getCreatedAt))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> create(ResourceRequest request) {
        requireRole(RoleConstants.TEACHER, RoleConstants.ADMIN);
        if (isBlank(request.getTitle()) || isBlank(request.getType()) || isBlank(request.getUrl())) {
            throw new BusinessException("资源标题、类型和地址不能为空");
        }
        ResourceItem resourceItem = new ResourceItem();
        fillResource(resourceItem, request);
        markForInsert(resourceItem);
        resourceMapper.insert(resourceItem);
        logService.save("资源管理", "新增资源", resourceItem.getTitle());
        return toView(resourceItem);
    }

    @Override
    public Map<String, Object> update(Long id, ResourceRequest request) {
        requireRole(RoleConstants.TEACHER, RoleConstants.ADMIN);
        ResourceItem resourceItem = requireResource(id);
        validateOwner(resourceItem);

        if (!isBlank(request.getTitle())) {
            resourceItem.setTitle(request.getTitle().trim());
        }
        if (!isBlank(request.getType())) {
            resourceItem.setType(request.getType().trim());
        }
        if (!isBlank(request.getUrl())) {
            resourceItem.setUrl(request.getUrl().trim());
        }
        if (request.getDescription() != null) {
            resourceItem.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getDownloadable() != null) {
            resourceItem.setDownloadable(request.getDownloadable());
        }
        if (request.getFileName() != null) {
            resourceItem.setFileName(trimToNull(request.getFileName()));
        }
        if (request.getContentType() != null) {
            resourceItem.setContentType(trimToNull(request.getContentType()));
        }
        if (request.getFileSize() != null) {
            resourceItem.setFileSize(request.getFileSize());
        }
        updatePublishScope(resourceItem, request);
        if (RoleConstants.ADMIN.equals(currentUser().getRole())) {
            if (!isBlank(request.getAuditStatus())) {
                resourceItem.setAuditStatus(normalizeAuditStatus(request.getAuditStatus()));
            }
            if (!isBlank(request.getOnlineStatus())) {
                resourceItem.setOnlineStatus(normalizeOnlineStatus(request.getOnlineStatus()));
            }
        } else {
            resourceItem.setAuditStatus(StatusConstants.PENDING);
        }
        markForUpdate(resourceItem);
        resourceMapper.updateById(resourceItem);
        logService.save("资源管理", "修改资源", resourceItem.getTitle());
        return toView(resourceItem);
    }

    @Override
    public void delete(Long id) {
        requireRole(RoleConstants.TEACHER, RoleConstants.ADMIN);
        ResourceItem resourceItem = requireResource(id);
        validateOwner(resourceItem);
        deletePhysicalFile(resourceItem.getStoredName());
        resourceMapper.deleteById(id);
        logService.save("资源管理", "删除资源", resourceItem.getTitle());
    }

    @Override
    public Map<String, Object> audit(Long id, String auditStatus) {
        requireRole(RoleConstants.ADMIN);
        ResourceItem resourceItem = requireResource(id);
        String normalizedStatus = normalizeAuditStatus(auditStatus);
        resourceItem.setAuditStatus(normalizedStatus);
        if (StatusConstants.REJECTED.equals(normalizedStatus)) {
            resourceItem.setOnlineStatus(StatusConstants.DRAFT);
        }
        markForUpdate(resourceItem);
        resourceMapper.updateById(resourceItem);
        logService.save("资源审核", normalizedStatus, resourceItem.getTitle());
        return toView(resourceItem);
    }

    @Override
    public Map<String, Object> updateOnlineStatus(Long id, String onlineStatus) {
        requireRole(RoleConstants.ADMIN);
        ResourceItem resourceItem = requireResource(id);
        String normalizedStatus = normalizeOnlineStatus(onlineStatus);
        if (StatusConstants.PUBLISHED.equals(normalizedStatus)
                && !StatusConstants.APPROVED.equals(resourceItem.getAuditStatus())) {
            throw new BusinessException("资源审核通过后才能上架");
        }
        resourceItem.setOnlineStatus(normalizedStatus);
        markForUpdate(resourceItem);
        resourceMapper.updateById(resourceItem);
        logService.save("资源上下架", normalizedStatus, resourceItem.getTitle());
        return toView(resourceItem);
    }

    @Override
    public Map<String, Object> upload(MultipartFile file, String title, String description, String type) {
        return upload(file, title, description, type, null, null);
    }

    @Override
    public Map<String, Object> upload(MultipartFile file,
                                      String title,
                                      String description,
                                      String type,
                                      String visibility,
                                      String className) {
        requireRole(RoleConstants.TEACHER, RoleConstants.ADMIN);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (isBlank(originalFilename)) {
            throw new BusinessException("文件名不能为空");
        }
        String normalizedType = normalizeType(type, originalFilename);
        validateFileType(normalizedType, originalFilename);

        try {
            Path uploadRoot = Paths.get(uploadDir);
            Files.createDirectories(uploadRoot);
            String extension = getExtension(originalFilename);
            String storedName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = uploadRoot.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            ResourceItem resourceItem = new ResourceItem();
            resourceItem.setTitle(isBlank(title) ? stripExtension(originalFilename) : title.trim());
            resourceItem.setDescription(trimToNull(description));
            resourceItem.setType(normalizedType);
            resourceItem.setFileName(originalFilename);
            resourceItem.setStoredName(storedName);
            resourceItem.setContentType(trimToNull(file.getContentType()));
            resourceItem.setFileSize(file.getSize());
            resourceItem.setUrl("/files/" + storedName);
            resourceItem.setDownloadable(true);
            resourceItem.setCreatorId(currentUser().getUserId());
            resourceItem.setCreatorName(currentUser().getRealName());
            resourceItem.setCreatorRole(currentUser().getRole());
            applyPublishDefaults(resourceItem, visibility, className);
            markForInsert(resourceItem);
            resourceMapper.insert(resourceItem);
            logService.save("资源管理", "上传资源", resourceItem.getTitle());
            return toView(resourceItem);
        } catch (IOException exception) {
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public ResponseEntity<Resource> downloadFile(Long id) {
        ResourceItem resourceItem = requireResource(id);
        requireViewPermission(resourceItem);
        Path filePath = resolveFilePath(resourceItem);
        FileSystemResource resource = new FileSystemResource(filePath.toFile());
        if (!resource.exists()) {
            throw new BusinessException("资源文件不存在");
        }

        String fileName = resourceItem.getFileName() == null ? filePath.getFileName().toString() : resourceItem.getFileName();
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        MediaType mediaType = parseMediaType(resourceItem.getContentType());

        long contentLength;
        try {
            contentLength = resource.contentLength();
        } catch (IOException exception) {
            throw new BusinessException("读取资源文件失败");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(mediaType)
                .contentLength(contentLength)
                .body(resource);
    }

    @Override
    public Resource loadFileResource(Long id) {
        ResourceItem resourceItem = requireResource(id);
        requireViewPermission(resourceItem);
        Path filePath = resolveFilePath(resourceItem);
        FileSystemResource resource = new FileSystemResource(filePath.toFile());
        if (!resource.exists()) {
            throw new BusinessException("资源文件不存在");
        }
        return resource;
    }

    @Override
    public MediaType loadFileMediaType(Long id) {
        ResourceItem resourceItem = requireResource(id);
        requireViewPermission(resourceItem);
        return parseMediaType(resourceItem.getContentType());
    }

    @Override
    public long loadFileSize(Long id) {
        ResourceItem resourceItem = requireResource(id);
        requireViewPermission(resourceItem);
        Path filePath = resolveFilePath(resourceItem);
        try {
            return Files.size(filePath);
        } catch (IOException exception) {
            throw new BusinessException("读取资源文件失败");
        }
    }

    private ResourceItem requireResource(Long id) {
        ResourceItem resourceItem = resourceMapper.selectById(id);
        if (resourceItem == null) {
            throw new BusinessException("资源不存在");
        }
        return resourceItem;
    }

    private void fillResource(ResourceItem resourceItem, ResourceRequest request) {
        resourceItem.setTitle(request.getTitle().trim());
        resourceItem.setDescription(trimToNull(request.getDescription()));
        resourceItem.setType(request.getType().trim());
        resourceItem.setUrl(request.getUrl().trim());
        resourceItem.setFileName(trimToNull(request.getFileName()));
        resourceItem.setStoredName(trimToNull(request.getStoredName()));
        resourceItem.setContentType(trimToNull(request.getContentType()));
        resourceItem.setFileSize(request.getFileSize());
        resourceItem.setDownloadable(request.getDownloadable() == null || request.getDownloadable());
        resourceItem.setCreatorId(currentUser().getUserId());
        resourceItem.setCreatorName(currentUser().getRealName());
        resourceItem.setCreatorRole(currentUser().getRole());
        applyPublishDefaults(resourceItem, request.getVisibility(), request.getClassName());
    }

    private void updatePublishScope(ResourceItem resourceItem, ResourceRequest request) {
        String visibility = RoleConstants.TEACHER.equals(currentUser().getRole())
                ? StatusConstants.CLASS
                : (isBlank(request.getVisibility()) ? resourceItem.getVisibility() : request.getVisibility());
        String className = RoleConstants.TEACHER.equals(currentUser().getRole())
                ? requireCurrentClassName()
                : (request.getClassName() == null ? resourceItem.getClassName() : request.getClassName());
        applyPublishDefaults(resourceItem, visibility, className);
    }

    private void applyPublishDefaults(ResourceItem resourceItem, String visibility, String className) {
        String normalizedVisibility = RoleConstants.TEACHER.equals(currentUser().getRole())
                ? StatusConstants.CLASS
                : normalizeVisibility(visibility);
        resourceItem.setVisibility(normalizedVisibility);
        if (StatusConstants.CLASS.equals(normalizedVisibility)) {
            String targetClass = RoleConstants.TEACHER.equals(currentUser().getRole())
                    ? requireCurrentClassName()
                    : trimToNull(className);
            if (targetClass == null) {
                throw new BusinessException("班级资源必须填写可见班级");
            }
            resourceItem.setClassName(targetClass);
        } else {
            resourceItem.setClassName(null);
        }
        if (RoleConstants.ADMIN.equals(currentUser().getRole())) {
            if (isBlank(resourceItem.getAuditStatus())) {
                resourceItem.setAuditStatus(StatusConstants.APPROVED);
            }
            if (isBlank(resourceItem.getOnlineStatus())) {
                resourceItem.setOnlineStatus(StatusConstants.PUBLISHED);
            }
        } else {
            resourceItem.setAuditStatus(StatusConstants.PENDING);
            resourceItem.setOnlineStatus(StatusConstants.PUBLISHED);
        }
    }

    private void validateOwner(ResourceItem resourceItem) {
        if (RoleConstants.TEACHER.equals(currentUser().getRole())
                && !resourceItem.getCreatorId().equals(currentUser().getUserId())) {
            throw new BusinessException("只能操作自己发布的资源");
        }
    }

    private boolean canViewResource(ResourceItem item) {
        if (RoleConstants.ADMIN.equals(currentUser().getRole())) {
            return true;
        }
        if (!StatusConstants.APPROVED.equals(item.getAuditStatus())
                || !StatusConstants.PUBLISHED.equals(item.getOnlineStatus())) {
            return false;
        }
        if (RoleConstants.TEACHER.equals(item.getCreatorRole())) {
            return sameClassName(currentUser().getClassName(), item.getClassName());
        }
        if (StatusConstants.CLASS.equals(item.getVisibility())) {
            return sameClassName(currentUser().getClassName(), item.getClassName());
        }
        return true;
    }

    private void requireViewPermission(ResourceItem item) {
        boolean owner = RoleConstants.TEACHER.equals(currentUser().getRole())
                && item.getCreatorId().equals(currentUser().getUserId());
        if (!owner && !canViewResource(item)) {
            throw new BusinessException("无权访问该资源");
        }
    }

    private void deletePhysicalFile(String storedName) {
        if (isBlank(storedName)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(storedName));
        } catch (IOException ignored) {
        }
    }

    private Map<String, Object> toView(ResourceItem item) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("title", item.getTitle());
        view.put("description", item.getDescription());
        view.put("type", item.getType());
        view.put("url", item.getUrl());
        view.put("fullUrl", buildFullUrl(item.getUrl()));
        view.put("fileName", item.getFileName());
        view.put("storedName", item.getStoredName());
        view.put("contentType", item.getContentType());
        view.put("fileSize", item.getFileSize());
        view.put("previewable", "视频".equals(item.getType()));
        view.put("downloadable", item.getDownloadable());
        view.put("creatorId", item.getCreatorId());
        view.put("creatorName", item.getCreatorName());
        view.put("creatorRole", item.getCreatorRole());
        view.put("visibility", item.getVisibility());
        view.put("className", item.getClassName());
        view.put("auditStatus", item.getAuditStatus());
        view.put("onlineStatus", item.getOnlineStatus());
        view.put("createdAt", item.getCreatedAt());
        return view;
    }

    private Path resolveFilePath(ResourceItem resourceItem) {
        if (isBlank(resourceItem.getStoredName())) {
            throw new BusinessException("当前资源不是本地上传文件");
        }
        return Paths.get(uploadDir).resolve(resourceItem.getStoredName());
    }

    private MediaType parseMediaType(String contentType) {
        if (isBlank(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String buildFullUrl(String url) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return "http://localhost:8080" + url;
    }

    private String normalizeType(String type, String filename) {
        if (!isBlank(type)) {
            return type.trim();
        }
        String extension = getExtension(filename).toLowerCase();
        if (".mp4".equals(extension) || ".avi".equals(extension) || ".mov".equals(extension) || ".wmv".equals(extension)) {
            return "视频";
        }
        return "文档";
    }

    private void validateFileType(String type, String filename) {
        String extension = getExtension(filename).toLowerCase();
        if ("视频".equals(type)) {
            if (!".mp4".equals(extension) && !".avi".equals(extension) && !".mov".equals(extension) && !".wmv".equals(extension)) {
                throw new BusinessException("视频资源只支持 mp4、avi、mov、wmv");
            }
            return;
        }
        if ("文档".equals(type)) {
            if (!".doc".equals(extension) && !".docx".equals(extension)) {
                throw new BusinessException("文档资源只支持 doc、docx");
            }
            return;
        }
        throw new BusinessException("资源类型仅支持 视频 或 文档");
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return index < 0 ? "" : filename.substring(index);
    }

    private String stripExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return index < 0 ? filename : filename.substring(0, index);
    }

    private String normalizeVisibility(String visibility) {
        if (isBlank(visibility)) {
            return StatusConstants.PUBLIC;
        }
        String normalized = visibility.trim().toUpperCase();
        if (!StatusConstants.PUBLIC.equals(normalized) && !StatusConstants.CLASS.equals(normalized)) {
            throw new BusinessException("资源可见范围仅支持 PUBLIC 或 CLASS");
        }
        return normalized;
    }

    private String normalizeAuditStatus(String auditStatus) {
        if (isBlank(auditStatus)) {
            throw new BusinessException("审核状态不能为空");
        }
        String normalized = auditStatus.trim().toUpperCase();
        if (!StatusConstants.APPROVED.equals(normalized)
                && !StatusConstants.REJECTED.equals(normalized)
                && !StatusConstants.PENDING.equals(normalized)) {
            throw new BusinessException("审核状态仅支持 APPROVED、REJECTED、PENDING");
        }
        return normalized;
    }

    private String normalizeOnlineStatus(String onlineStatus) {
        if (isBlank(onlineStatus)) {
            throw new BusinessException("上下架状态不能为空");
        }
        String normalized = onlineStatus.trim().toUpperCase();
        if (!StatusConstants.PUBLISHED.equals(normalized) && !StatusConstants.DRAFT.equals(normalized)) {
            throw new BusinessException("上下架状态仅支持 PUBLISHED 或 DRAFT");
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
