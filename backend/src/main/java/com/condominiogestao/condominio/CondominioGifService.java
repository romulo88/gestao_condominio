package com.condominiogestao.condominio;

import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * GIF opcional do condomínio (pedido do Romulo) - exibido no lugar do título da página no
 * Kanban, quando cadastrado. Mesmo padrão de storage do resto do sistema (ver
 * {@code PessoaFotoService}/{@code DemandaDocumentoService}): bucket S3-compatível
 * privado, chave gerada (nunca o nome original), leitura sempre pelo caminho servido por
 * {@code GET /api/condominios/{id}/gif} (ver {@code CondominioController} + {@code
 * ArquivoStorageService}). Só GIF é aceito aqui - diferente da foto de pessoa, que aceita
 * jpeg/png/webp/gif, esse campo é especificamente pra GIF.
 */
@Service
@Transactional(readOnly = true)
public class CondominioGifService {

    private static final String TIPO_PERMITIDO = "image/gif";

    private final CondominioRepository repository;
    private final MinioClient minioClient;
    private final String bucket;

    public CondominioGifService(
            CondominioRepository repository, MinioClient minioClient, @Value("${storage.bucket}") String bucket) {
        this.repository = repository;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /** Caminho relativo do GIF atual - null se o condomínio não tiver um cadastrado. */
    public String buscarUrl(Integer condominioId) {
        Condominio condominio = buscarCondominio(condominioId);
        return condominio.getGifUrl() != null ? caminhoGif(condominioId) : null;
    }

    /** Chave bruta do GIF atual (sem assinar) - usada pelo endpoint que serve a imagem
     * direto (`GET /api/condominios/{id}/gif`, ver `CondominioController` +
     * `ArquivoStorageService`), depois que o link assinado do MinIO parou de resolver pra
     * quem acessa via túnel/celular (endereço do MinIO embutido no link não é alcançável
     * de fora do servidor - ver HANDOFF.md). */
    public String chave(Integer condominioId) {
        Condominio condominio = buscarCondominio(condominioId);
        if (condominio.getGifUrl() == null) {
            throw new ResourceNotFoundException("Condomínio não tem GIF cadastrado: " + condominioId);
        }
        return condominio.getGifUrl();
    }

    /** Sobe o GIF novo e só troca a referência se o upload funcionar - se der erro, o GIF
     * antigo continua valendo. Substitui (nunca acumula: é sempre um só por condomínio) -
     * a chave antiga é removida do bucket só depois que a nova já está salva. */
    @Transactional
    public String atualizar(Integer condominioId, MultipartFile arquivo) {
        Condominio condominio = buscarCondominio(condominioId);

        if (arquivo.isEmpty()) {
            throw new InvalidRequestException("Arquivo vazio");
        }
        String tipoMime = arquivo.getContentType();
        if (!TIPO_PERMITIDO.equals(tipoMime)) {
            throw new InvalidRequestException("Só GIF é aceito aqui - recebido: " + tipoMime);
        }

        String chaveAntiga = condominio.getGifUrl();
        String chaveNova = "condominios/%d/%s.gif".formatted(condominioId, UUID.randomUUID());
        try (var entrada = arquivo.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(chaveNova)
                    .stream(entrada, arquivo.getSize(), -1)
                    .contentType(tipoMime)
                    .build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao enviar o GIF pro storage", ex);
        }

        condominio.setGifUrl(chaveNova);
        repository.save(condominio);

        if (chaveAntiga != null) {
            removerDoBucketSilenciosamente(chaveAntiga);
        }

        return caminhoGif(condominioId);
    }

    @Transactional
    public void remover(Integer condominioId) {
        Condominio condominio = buscarCondominio(condominioId);
        if (condominio.getGifUrl() == null) {
            return;
        }
        String chave = condominio.getGifUrl();
        condominio.setGifUrl(null);
        repository.save(condominio);
        removerDoBucketSilenciosamente(chave);
    }

    /** A referência já foi trocada/limpa no banco quando isso é chamado - não vale falhar
     * a operação inteira por causa de um objeto órfão no bucket (mesmo raciocínio de
     * {@code PessoaFotoService.removerDoBucketSilenciosamente}). */
    private void removerDoBucketSilenciosamente(String chave) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(chave).build());
        } catch (Exception ignorado) {
            // ver javadoc do método
        }
    }

    private String caminhoGif(Integer condominioId) {
        return "/api/condominios/%d/gif".formatted(condominioId);
    }

    private Condominio buscarCondominio(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + id));
    }
}
