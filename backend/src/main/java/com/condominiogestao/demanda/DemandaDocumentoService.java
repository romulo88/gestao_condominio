package com.condominiogestao.demanda;

import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.demanda.dto.DemandaDocumentoResponse;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioPerfil;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.morador.Morador;
import com.condominiogestao.morador.MoradorRepository;
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
 * Anexos de imagem/vídeo numa demanda (item 4.9). Arquivo em si fica no storage
 * S3-compatível (MinIO local / Cloudflare R2 em produção - ver {@code StorageConfig});
 * aqui só a referência ({@link DemandaDocumento}) e a orquestração do upload/remoção.
 * Bucket é privado de propósito - toda leitura gera um link assinado com validade curta
 * em vez de um endereço público permanente, então quem não tem direito de ver a demanda
 * (ver {@link #listar} pra leitura e {@link #exigirPodeMexerAnexos} pra anexar/remover)
 * também não consegue adivinhar/acessar o arquivo direto.
 *
 * <p>Pedido do Romulo (vídeo): cada demanda aceita um máximo de fotos e de vídeo, e vídeo
 * tem um teto de tamanho próprio, maior que o de foto - ver {@link #upload}. Os 4 números
 * (quantidade de foto, quantidade de vídeo, tamanho de foto, tamanho de vídeo) NÃO são
 * mais constante fixa (pedido do Romulo, v147: "criar uma tabela parametros... para que o
 * administrador possa controlar esses valores gerais, sem precisar subir versão") - vêm
 * de {@link ParametroService#getInt}, que lê a tabela {@code parametros} (nomes
 * {@code maximoFotos}/{@code maximoVideos}/{@code tamanhoMaximoFotoMb}/
 * {@code tamanhoMaximoVideoMb}). As constantes `_PADRAO` abaixo só valem se o parâmetro
 * correspondente não existir na tabela (fallback, nunca deveria acontecer de verdade já
 * que a migration V19 semeia os 4).
 */
@Service
@Transactional(readOnly = true)
public class DemandaDocumentoService {

    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    // video/quicktime = .mov, o formato padrão de vídeo gravado num iPhone - inclui de
    // propósito, já que o app é usado como PWA no celular (ver HANDOFF.md).
    private static final Set<String> TIPOS_VIDEO_PERMITIDOS = Set.of("video/mp4", "video/webm", "video/quicktime");

    private static final int IMAGEM_TAMANHO_MAXIMO_BYTES_MB_PADRAO = 8;
    private static final int VIDEO_TAMANHO_MAXIMO_BYTES_MB_PADRAO = 15;
    private static final int MAX_FOTOS_PADRAO = 3;
    private static final int MAX_VIDEOS_PADRAO = 1;

    private final DemandaDocumentoRepository repository;
    private final DemandaRepository demandaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final MoradorRepository moradorRepository;
    private final DemandaService demandaService;
    private final ParametroService parametroService;
    private final MinioClient minioClient;
    private final String bucket;

    public DemandaDocumentoService(
            DemandaDocumentoRepository repository,
            DemandaRepository demandaRepository,
            FuncionarioRepository funcionarioRepository,
            MoradorRepository moradorRepository,
            DemandaService demandaService,
            ParametroService parametroService,
            MinioClient minioClient,
            @Value("${storage.bucket}") String bucket) {
        this.repository = repository;
        this.demandaRepository = demandaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.moradorRepository = moradorRepository;
        this.demandaService = demandaService;
        this.parametroService = parametroService;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /** Ver os anexos segue a mesma visibilidade do quadro Kanban (qualquer funcionário ou
     * morador do condomínio, respeitando sigilo - ver {@link DemandaService#podeVer}):
     * pedido do Romulo pra todo morador poder acompanhar as demandas, não só quem abriu.
     * Anexar/remover continua restrito a funcionário do condomínio ou ao solicitante
     * (ver {@link #exigirPodeMexerAnexos}). */
    public List<DemandaDocumentoResponse> listar(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = buscarDemanda(demandaId);
        if (!demandaService.podeVer(contexto, demanda)) {
            throw new ForbiddenException("Você não tem acesso a essa demanda");
        }
        return repository.findByDemandaId(demandaId).stream()
                .map(documento -> DemandaDocumentoResponse.from(documento, caminhoArquivo(documento)))
                .toList();
    }

    /** Busca o anexo com a mesma checagem de visibilidade de {@link #listar} - usada por
     * `GET /api/demanda-documentos/{id}/arquivo`, que serve a imagem direto (ver
     * `DemandaDocumentoController` + `ArquivoStorageService`) em vez de devolver um link
     * assinado do MinIO. */
    public DemandaDocumento buscarParaBaixar(ContextoAutenticado contexto, Integer id) {
        DemandaDocumento documento =
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado: " + id));
        if (!demandaService.podeVer(contexto, documento.getDemanda())) {
            throw new ForbiddenException("Você não tem acesso a essa demanda");
        }
        return documento;
    }

    /** Recebe o arquivo, valida que é imagem OU vídeo (tipo, tamanho e o limite de
     * quantidade por demanda), sobe pro bucket com uma chave gerada (nunca o nome
     * original - evita colisão e path traversal) e só então grava a referência - se o
     * upload falhar, não sobra linha nenhuma no banco. */
    @Transactional
    public DemandaDocumentoResponse upload(ContextoAutenticado contexto, Integer demandaId, MultipartFile arquivo) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirPodeMexerAnexos(contexto, demanda);

        if (arquivo.isEmpty()) {
            throw new InvalidRequestException("Arquivo vazio");
        }
        String tipoMime = arquivo.getContentType();
        boolean ehImagem = tipoMime != null && TIPOS_IMAGEM_PERMITIDOS.contains(tipoMime);
        boolean ehVideo = tipoMime != null && TIPOS_VIDEO_PERMITIDOS.contains(tipoMime);
        if (!ehImagem && !ehVideo) {
            throw new InvalidRequestException(
                    "Só imagem (jpeg, png, webp ou gif) ou vídeo (mp4, webm ou mov) é aceito aqui - recebido: " + tipoMime);
        }

        int fotoTamanhoMaximoMb = parametroService.getInt("tamanhoMaximoFotoMb", IMAGEM_TAMANHO_MAXIMO_BYTES_MB_PADRAO);
        int videoTamanhoMaximoMb = parametroService.getInt("tamanhoMaximoVideoMb", VIDEO_TAMANHO_MAXIMO_BYTES_MB_PADRAO);
        if (ehImagem && arquivo.getSize() > fotoTamanhoMaximoMb * 1024L * 1024L) {
            throw new InvalidRequestException("Imagem não pode passar de " + fotoTamanhoMaximoMb + "MB");
        }
        if (ehVideo && arquivo.getSize() > videoTamanhoMaximoMb * 1024L * 1024L) {
            throw new InvalidRequestException("Vídeo não pode passar de " + videoTamanhoMaximoMb + "MB");
        }

        int maximoFotos = parametroService.getInt("maximoFotos", MAX_FOTOS_PADRAO);
        int maximoVideos = parametroService.getInt("maximoVideos", MAX_VIDEOS_PADRAO);
        String prefixoTipo = ehImagem ? "image/" : "video/";
        long jaTem = repository.countByDemandaIdAndTipoMimeStartingWith(demandaId, prefixoTipo);
        if (ehImagem && jaTem >= maximoFotos) {
            throw new ConflictException("Essa demanda já tem o máximo de " + maximoFotos + " fotos");
        }
        if (ehVideo && jaTem >= maximoVideos) {
            throw new ConflictException("Essa demanda já tem o máximo de " + maximoVideos + " vídeo");
        }

        String chave = "demandas/%d/%s%s".formatted(demandaId, UUID.randomUUID(), extensaoPara(tipoMime));
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

        DemandaDocumento documento = new DemandaDocumento();
        documento.setDemanda(demanda);
        documento.setNomeArquivo(
                arquivo.getOriginalFilename() != null
                        ? arquivo.getOriginalFilename()
                        : (ehVideo ? "video" : "imagem") + extensaoPara(tipoMime));
        // Guarda a CHAVE do objeto no bucket, não uma URL pública - a coluna se chama
        // `url` (schema antigo) mas o que sai daqui é assinado na hora da leitura, ver
        // gerarUrlAssinada.
        documento.setUrl(chave);
        documento.setTipoMime(tipoMime);
        documento.setTamanhoBytes((int) arquivo.getSize());

        if (TipoPessoa.morador.name().equals(contexto.tipoPapel())) {
            documento.setMoradorUpload(buscarMorador(contexto.pessoaId()));
        } else {
            documento.setFuncionarioUpload(buscarFuncionario(contexto.pessoaId()));
        }

        DemandaDocumento salvo = repository.save(documento);
        return DemandaDocumentoResponse.from(salvo, caminhoArquivo(salvo));
    }

    /** Remoção física de verdade (não soft-delete) - diferente das entidades de negócio
     * do resto do sistema: aqui é só um arquivo anexado, não um registro de auditoria.
     * Remove do bucket primeiro; só apaga a linha do banco se isso funcionar. */
    @Transactional
    public void remover(ContextoAutenticado contexto, Integer id) {
        DemandaDocumento documento =
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado: " + id));
        exigirPodeMexerAnexos(contexto, documento.getDemanda());

        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(documento.getUrl()).build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao remover o arquivo do storage", ex);
        }
        repository.delete(documento);
    }

    /** Caminho relativo (mesma origem da API, não é mais link assinado do MinIO - ver
     * HANDOFF.md) que o front busca pra exibir a imagem, servido por {@code GET
     * /api/demanda-documentos/{id}/arquivo} (ver {@link DemandaDocumentoController}). */
    private String caminhoArquivo(DemandaDocumento documento) {
        return "/api/demanda-documentos/%d/arquivo".formatted(documento.getId());
    }

    /** Quem pode anexar/remover imagem numa demanda: funcionário do condomínio dela, ou
     * o próprio solicitante (morador ou funcionário) - mesma checagem "é dono ou é
     * funcionário deste condomínio" usada nas outras ações de Demanda (aprovar, etc.).
     * Mais restrito que a leitura ({@link #listar}), que segue a visibilidade do quadro
     * Kanban pra todo morador poder acompanhar.
     * ⚠️ Não repete o filtro de sigilo completo de {@link DemandaService#listar} - mesma
     * lacuna já documentada lá (ações em cima de uma demanda específica ainda não sabem
     * se ela é sigilosa, só isso aqui). */
    private void exigirPodeMexerAnexos(ContextoAutenticado contexto, Demanda demanda) {
        // Perfil de acesso restrito (rondista/agente de convívio, pedido do Romulo) não
        // vale "qualquer funcionário do condomínio" - só libera quando `podeVer` já libera
        // (mesma regra de visibilidade: a própria demanda, ou aprovada em que é
        // responsável). Demais perfis continuam com o comportamento de sempre.
        boolean ehFuncionarioDoCondominio = TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && demanda.getCondominio().getId().equals(contexto.condominioId())
                && (!FuncionarioPerfil.acessoRestrito(contexto.perfil()) || demandaService.podeVer(contexto, demanda));
        boolean ehSolicitante = TipoPessoa.morador.name().equals(contexto.tipoPapel())
                ? demanda.getMoradorSolicitante() != null
                        && demanda.getMoradorSolicitante().getId().equals(contexto.pessoaId())
                : demanda.getFuncionarioSolicitante() != null
                        && demanda.getFuncionarioSolicitante().getId().equals(contexto.pessoaId());
        if (!ehFuncionarioDoCondominio && !ehSolicitante) {
            throw new ForbiddenException("Só funcionário deste condomínio ou quem abriu a demanda pode fazer isso");
        }
    }

    private Demanda buscarDemanda(Integer id) {
        return demandaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + id));
    }

    private Funcionario buscarFuncionario(Integer id) {
        return funcionarioRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }

    private Morador buscarMorador(Integer id) {
        return moradorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + id));
    }

    private String extensaoPara(String tipoMime) {
        return switch (tipoMime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            default -> "";
        };
    }
}
