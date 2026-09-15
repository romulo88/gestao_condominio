package com.condominiogestao.pessoa;

import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Foto de perfil da pessoa - só uma por pessoa (item novo, pedido do Romulo pro cadastro
 * de funcionário), por isso fica em {@link Pessoa} (identidade compartilhada), não
 * duplicada por papel. Mesmo padrão de storage do anexo de demanda (item 4.9, ver
 * {@code DemandaDocumentoService}): bucket S3-compatível privado, chave gerada (nunca o
 * nome original), leitura sempre pelo caminho servido por {@code GET
 * /api/funcionarios/{id}/foto} (ver {@code FuncionarioController} + {@code
 * ArquivoStorageService}) - só funcionário usa essa foto até agora, por isso o caminho é
 * montado com esse prefixo mesmo aqui, que fala de {@link Pessoa} em geral.
 */
@Service
@Transactional(readOnly = true)
public class PessoaFotoService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final PessoaRepository repository;
    private final MinioClient minioClient;
    private final String bucket;

    public PessoaFotoService(PessoaRepository repository, MinioClient minioClient, @Value("${storage.bucket}") String bucket) {
        this.repository = repository;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /** Caminho relativo da foto atual - null se a pessoa não tiver foto cadastrada. */
    public String buscarUrl(Integer pessoaId) {
        Pessoa pessoa = buscarPessoa(pessoaId);
        return pessoa.getFotoUrl() != null ? caminhoFoto(pessoaId) : null;
    }

    /** Chave bruta da foto atual (sem assinar) - usada pelo endpoint que serve a imagem
     * direto (`GET /api/funcionarios/{id}/foto`, ver `FuncionarioController` +
     * `ArquivoStorageService`), depois que o link assinado do MinIO parou de resolver pra
     * quem acessa via túnel/celular (endereço do MinIO embutido no link não é alcançável
     * de fora do servidor - ver HANDOFF.md). */
    public String chave(Integer pessoaId) {
        Pessoa pessoa = buscarPessoa(pessoaId);
        if (pessoa.getFotoUrl() == null) {
            throw new ResourceNotFoundException("Pessoa não tem foto cadastrada: " + pessoaId);
        }
        return pessoa.getFotoUrl();
    }

    /** Mesma coisa que {@link #buscarUrl}, mas pra várias pessoas de uma vez (avatar de
     * responsável no card do Kanban) - uma consulta só em vez de N+1; só entram no mapa
     * as pessoas que realmente têm foto cadastrada. Montar o caminho em si não bate no
     * bucket (é só formatar string), só o {@code findAllById} é uma query de verdade. */
    public Map<Integer, String> buscarUrls(Collection<Integer> pessoaIds) {
        if (pessoaIds.isEmpty()) {
            return Map.of();
        }
        return repository.findAllById(pessoaIds).stream()
                .filter(pessoa -> pessoa.getFotoUrl() != null)
                .collect(Collectors.toMap(Pessoa::getId, pessoa -> caminhoFoto(pessoa.getId())));
    }

    /** Sobe a foto nova e só troca a referência se o upload funcionar - se der erro, a
     * foto antiga continua valendo. Substitui (nunca acumula: é sempre uma só por
     * pessoa) - a chave antiga é removida do bucket só depois que a nova já está salva. */
    @Transactional
    public String atualizar(Integer pessoaId, MultipartFile arquivo) {
        Pessoa pessoa = buscarPessoa(pessoaId);

        if (arquivo.isEmpty()) {
            throw new InvalidRequestException("Arquivo vazio");
        }
        String tipoMime = arquivo.getContentType();
        if (tipoMime == null || !TIPOS_PERMITIDOS.contains(tipoMime)) {
            throw new InvalidRequestException("Só imagem é aceita aqui (jpeg, png, webp ou gif) - recebido: " + tipoMime);
        }

        String chaveAntiga = pessoa.getFotoUrl();
        String chaveNova = "pessoas/%d/%s%s".formatted(pessoaId, UUID.randomUUID(), extensaoPara(tipoMime));
        try (var entrada = arquivo.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(chaveNova)
                    .stream(entrada, arquivo.getSize(), -1)
                    .contentType(tipoMime)
                    .build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao enviar a foto pro storage", ex);
        }

        pessoa.setFotoUrl(chaveNova);
        repository.save(pessoa);

        if (chaveAntiga != null) {
            removerDoBucketSilenciosamente(chaveAntiga);
        }

        return caminhoFoto(pessoaId);
    }

    @Transactional
    public void remover(Integer pessoaId) {
        Pessoa pessoa = buscarPessoa(pessoaId);
        if (pessoa.getFotoUrl() == null) {
            return;
        }
        String chave = pessoa.getFotoUrl();
        pessoa.setFotoUrl(null);
        repository.save(pessoa);
        removerDoBucketSilenciosamente(chave);
    }

    /** A referência já foi trocada/limpa no banco quando isso é chamado - não vale falhar
     * a operação inteira por causa de um objeto órfão no bucket (limpeza manual depois,
     * se precisar - diferente de {@code DemandaDocumentoService.remover}, onde o objeto
     * removido É o registro em si, então lá a falha tem que propagar). */
    private void removerDoBucketSilenciosamente(String chave) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(chave).build());
        } catch (Exception ignorado) {
            // ver javadoc do método
        }
    }

    private String caminhoFoto(Integer pessoaId) {
        return "/api/funcionarios/%d/foto".formatted(pessoaId);
    }

    private Pessoa buscarPessoa(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada: " + id));
    }

    private String extensaoPara(String tipoMime) {
        return switch (tipoMime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }
}
