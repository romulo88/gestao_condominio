package com.condominiogestao.storage;

import com.condominiogestao.common.ResourceNotFoundException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Serve o conteúdo de um objeto do bucket direto na resposta HTTP - usado pelos endpoints
 * {@code GET .../arquivo}, {@code .../foto} e {@code .../gif} (demanda documento, foto de
 * pessoa, GIF de condomínio). Substituiu o link assinado do MinIO que cada um desses três
 * serviços gerava antes: o link tinha o endereço do MinIO embutido (ex:
 * {@code http://localhost:9000/...}), que só o servidor consegue alcançar - de um
 * celular/túnel a imagem não carregava (ver HANDOFF.md). Servindo pelo próprio backend, o
 * navegador só precisa alcançar a mesma origem de sempre (a API), como qualquer outra
 * chamada - o proxy do Next.js (`rewrites()`) cuida do resto.
 */
@Service
public class ArquivoStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public ArquivoStorageService(MinioClient minioClient, @Value("${storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /** Baixa a chave do bucket e monta a resposta pronta pra servir como imagem/vídeo -
     * sem {@code tamanhoBytesConhecido} (ver overload). */
    public ResponseEntity<InputStreamResource> baixar(String chave, String tipoMimeConhecido) {
        return baixar(chave, tipoMimeConhecido, null);
    }

    /** {@code tipoMimeConhecido} é usado quando a entidade já guarda o mime-type (só
     * {@code DemandaDocumento} guarda - ver {@code DemandaDocumentoService}); nos outros
     * dois casos (foto de pessoa, GIF de condomínio, que não têm esse campo) passe
     * {@code null} pra inferir pela extensão da própria chave. {@code tamanhoBytesConhecido}
     * (também só {@code DemandaDocumento} guarda) manda o {@code Content-Length} certo na
     * resposta - sem isso o navegador recebe a resposta em chunked/tamanho desconhecido, o
     * que faz o player de vídeo não mostrar duração nem permitir arrastar a barra até
     * terminar de baixar tudo; pra imagem não faz diferença perceptível, por isso os outros
     * dois casos continuam passando {@code null} sem problema. Cache curto e privado (5min,
     * mais curto que os 15min do link assinado antigo - aqui não expira sozinho, então vale
     * ficar conservador) porque o conteúdo é de um bucket privado, não deve ficar em cache
     * de proxy compartilhado. */
    public ResponseEntity<InputStreamResource> baixar(
            String chave, String tipoMimeConhecido, Integer tamanhoBytesConhecido) {
        String tipoMime = tipoMimeConhecido != null ? tipoMimeConhecido : tipoMimePorExtensao(chave);
        InputStream conteudo;
        try {
            conteudo = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(chave).build());
        } catch (ErrorResponseException ex) {
            throw new ResourceNotFoundException("Arquivo não encontrado no storage: " + chave);
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao baixar arquivo do storage: " + chave, ex);
        }
        var resposta = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipoMime))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate());
        if (tamanhoBytesConhecido != null) {
            resposta = resposta.contentLength(tamanhoBytesConhecido);
        }
        return resposta.body(new InputStreamResource(conteudo));
    }

    private String tipoMimePorExtensao(String chave) {
        String minuscula = chave.toLowerCase();
        if (minuscula.endsWith(".png")) {
            return "image/png";
        }
        if (minuscula.endsWith(".webp")) {
            return "image/webp";
        }
        if (minuscula.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }
}
