package com.condominiogestao.mensagemprivada;

import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.mensagemprivada.dto.MensagemPrivadaDocumentoResponse;
import com.condominiogestao.parametro.ParametroService;
import com.condominiogestao.security.ContextoAutenticado;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fotos anexadas a uma {@link MensagemPrivada} - mesmo espírito de {@code
 * DemandaDocumentoService} (bucket privado, chave gerada, serve via proxy assinado), só que
 * mais simples: só imagem (pedido do Romulo é "1 foto", nunca vídeo aqui) e o limite de
 * quantidade é por MENSAGEM, não por conversa (cada mensagem individual aceita até
 * {@code mensagemPrivadaMaximoFotos} fotos, parametrizável - ver {@link ParametroService}).
 * Reaproveita o parâmetro {@code tamanhoMaximoFotoMb} já existente pro teto de tamanho -
 * é a mesma restrição física (peso de uma foto), não faz sentido duplicar o parâmetro.
 *
 * <p>Só o próprio autor da mensagem pode anexar/remover suas fotos (diferente de anexo de
 * demanda, que aceita qualquer funcionário do condomínio) - a foto nasce junto com a
 * mensagem que a pessoa está escrevendo, não faz sentido outro participante anexar depois.
 */
@Service
@Transactional(readOnly = true)
public class MensagemPrivadaDocumentoService {

    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final int TAMANHO_MAXIMO_MB_PADRAO = 8;
    private static final int MAXIMO_FOTOS_PADRAO = 1;

    private final MensagemPrivadaDocumentoRepository repository;
    private final MensagemPrivadaRepository mensagemRepository;
    private final ConversaPrivadaService conversaPrivadaService;
    private final ParametroService parametroService;
    private final MinioClient minioClient;
    private final String bucket;

    public MensagemPrivadaDocumentoService(
            MensagemPrivadaDocumentoRepository repository,
            MensagemPrivadaRepository mensagemRepository,
            ConversaPrivadaService conversaPrivadaService,
            ParametroService parametroService,
            MinioClient minioClient,
            @Value("${storage.bucket}") String bucket) {
        this.repository = repository;
        this.mensagemRepository = mensagemRepository;
        this.conversaPrivadaService = conversaPrivadaService;
        this.parametroService = parametroService;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public MensagemPrivadaDocumento buscarParaBaixar(ContextoAutenticado contexto, Integer id) {
        MensagemPrivadaDocumento documento = buscarEntidade(id);
        conversaPrivadaService.buscarConversaParticipante(contexto, documento.getMensagem().getConversa().getId());
        return documento;
    }

    @Transactional
    public MensagemPrivadaDocumentoResponse upload(ContextoAutenticado contexto, Integer mensagemId, MultipartFile arquivo) {
        MensagemPrivada mensagem = buscarMensagem(mensagemId);
        conversaPrivadaService.buscarConversaParticipante(contexto, mensagem.getConversa().getId());
        exigirAutorDaMensagem(contexto, mensagem);

        if (arquivo.isEmpty()) {
            throw new InvalidRequestException("Arquivo vazio");
        }
        String tipoMime = arquivo.getContentType();
        if (tipoMime == null || !TIPOS_IMAGEM_PERMITIDOS.contains(tipoMime)) {
            throw new InvalidRequestException("Só imagem (jpeg, png, webp ou gif) é aceita aqui - recebido: " + tipoMime);
        }

        int tamanhoMaximoMb = parametroService.getInt("tamanhoMaximoFotoMb", TAMANHO_MAXIMO_MB_PADRAO);
        if (arquivo.getSize() > tamanhoMaximoMb * 1024L * 1024L) {
            throw new InvalidRequestException("Imagem não pode passar de " + tamanhoMaximoMb + "MB");
        }

        int maximoFotos = parametroService.getInt("mensagemPrivadaMaximoFotos", MAXIMO_FOTOS_PADRAO);
        long jaTem = repository.countByMensagemIdAndTipoMimeStartingWith(mensagemId, "image/");
        if (jaTem >= maximoFotos) {
            throw new ConflictException("Essa mensagem já tem o máximo de " + maximoFotos + " foto(s)");
        }

        String chave = "mensagens-privadas/%d/%s%s".formatted(mensagemId, UUID.randomUUID(), extensaoPara(tipoMime));
        try (var entrada = arquivo.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(chave)
                    .stream(entrada, arquivo.getSize(), -1)
                    .contentType(tipoMime)
                    .build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao enviar o arquivo pro storage", ex);
        }

        MensagemPrivadaDocumento documento = new MensagemPrivadaDocumento();
        documento.setMensagem(mensagem);
        documento.setNomeArquivo(arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "foto" + extensaoPara(tipoMime));
        documento.setUrl(chave);
        documento.setTipoMime(tipoMime);
        documento.setTamanhoBytes((int) arquivo.getSize());

        MensagemPrivadaDocumento salvo = repository.save(documento);
        return MensagemPrivadaDocumentoResponse.from(salvo, caminhoArquivo(salvo));
    }

    @Transactional
    public void remover(ContextoAutenticado contexto, Integer id) {
        MensagemPrivadaDocumento documento = buscarEntidade(id);
        conversaPrivadaService.buscarConversaParticipante(contexto, documento.getMensagem().getConversa().getId());
        exigirAutorDaMensagem(contexto, documento.getMensagem());

        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(documento.getUrl()).build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao remover o arquivo do storage", ex);
        }
        repository.delete(documento);
    }

    private String caminhoArquivo(MensagemPrivadaDocumento documento) {
        return "/api/mensagem-privada-documentos/%d/arquivo".formatted(documento.getId());
    }

    private void exigirAutorDaMensagem(ContextoAutenticado contexto, MensagemPrivada mensagem) {
        boolean ehAutor = "funcionario".equals(contexto.tipoPapel())
                ? mensagem.getFuncionarioAutor() != null && mensagem.getFuncionarioAutor().getId().equals(contexto.pessoaId())
                : mensagem.getMoradorAutor() != null && mensagem.getMoradorAutor().getId().equals(contexto.pessoaId());
        if (!ehAutor) {
            throw new ForbiddenException("Só quem escreveu a mensagem pode anexar ou remover suas fotos");
        }
    }

    private MensagemPrivadaDocumento buscarEntidade(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado: " + id));
    }

    private MensagemPrivada buscarMensagem(Integer id) {
        return mensagemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mensagem não encontrada: " + id));
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
