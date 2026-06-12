package com.englishstudy.backend.service;

import com.englishstudy.backend.request.ResourceRequest;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ResourceService {

    List<Map<String, Object>> listAll(String keyword);

    List<Map<String, Object>> listOwn();

    Map<String, Object> create(ResourceRequest request);

    Map<String, Object> update(Long id, ResourceRequest request);

    void delete(Long id);

    Map<String, Object> audit(Long id, String auditStatus);

    Map<String, Object> updateOnlineStatus(Long id, String onlineStatus);

    Map<String, Object> upload(MultipartFile file, String title, String description, String type);

    Map<String, Object> upload(MultipartFile file, String title, String description, String type, String visibility, String className);

    ResponseEntity<Resource> downloadFile(Long id);

    Resource loadFileResource(Long id);

    MediaType loadFileMediaType(Long id);

    long loadFileSize(Long id);
}
