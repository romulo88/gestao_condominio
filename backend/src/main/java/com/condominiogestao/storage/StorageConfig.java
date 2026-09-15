package com.condominiogestao.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client S3-compatível pro storage de anexos (item 4.9) - MinIO local / Cloudflare R2 em
 * produção falam o mesmo protocolo, só troca endpoint/credenciais (ver application.yml,
 * seção {@code storage}) - nenhum código daqui precisa saber em qual dos dois está rodando.
 */
@Configuration
public class StorageConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${storage.endpoint}") String endpoint,
            @Value("${storage.access-key}") String accessKey,
            @Value("${storage.secret-key}") String secretKey) {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }
}
