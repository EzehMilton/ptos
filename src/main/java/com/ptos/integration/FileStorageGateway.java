package com.ptos.integration;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageGateway {

    String store(MultipartFile file, String subdirectory);

    Resource load(String key);

    void delete(String key);

    String getUrl(String key);
}
