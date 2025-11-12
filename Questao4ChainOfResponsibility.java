/**
 * QUESTÃO 4 - PADRÃO CHAIN OF RESPONSIBILITY
 * 
 * Justificativa da escolha do padrão:
 * O padrão Chain of Responsibility foi escolhido porque:
 * 1. Permite desacoplar remetentes de destinatários de uma requisição de validação
 * 2. Facilita adicionar ou remover validadores sem afetar outros elementos da cadeia
 * 3. Permite que múltiplos validadores processem a requisição em sequência
 * 4. Cada validador decide se processa e/ou passa para o próximo (validações condicionais)
 * 5. Respeita o Single Responsibility Principle - cada validador tem uma responsabilidade específica
 * 6. Respeita o Open/Closed Principle - novos validadores podem ser adicionados sem modificar existentes
 * 7. Facilita implementação de circuit breaker e rollback de forma centralizada
 * 8. Permite controle fino sobre o fluxo de validação (ordem, condições, interrupções)
 */

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

// Classe que representa um documento fiscal eletrônico (NF-e)
class DocumentoFiscal {
    private String numeroNFe;
    private String xmlConteudo;
    private String certificadoDigital;
    private LocalDate dataEmissao;
    private double valorTotal;
    private double valorImpostos;
    private boolean inseridoBancoDados;
    private String idBancoDados;

    public DocumentoFiscal(String numeroNFe, String xmlConteudo, String certificadoDigital,
                          LocalDate dataEmissao, double valorTotal, double valorImpostos) {
        this.numeroNFe = numeroNFe;
        this.xmlConteudo = xmlConteudo;
        this.certificadoDigital = certificadoDigital;
        this.dataEmissao = dataEmissao;
        this.valorTotal = valorTotal;
        this.valorImpostos = valorImpostos;
        this.inseridoBancoDados = false;
        this.idBancoDados = null;
    }

    public String getNumeroNFe() { return numeroNFe; }
    public String getXmlConteudo() { return xmlConteudo; }
    public String getCertificadoDigital() { return certificadoDigital; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public double getValorTotal() { return valorTotal; }
    public double getValorImpostos() { return valorImpostos; }
    public boolean isInseridoBancoDados() { return inseridoBancoDados; }
    public String getIdBancoDados() { return idBancoDados; }

    public void setInseridoBancoDados(boolean inserido) { this.inseridoBancoDados = inserido; }
    public void setIdBancoDados(String id) { this.idBancoDados = id; }
    public void setValorImpostos(double valor) { this.valorImpostos = valor; }

    @Override
    public String toString() {
        return String.format("NF-e %s - Valor: R$%.2f - Data: %s", 
            numeroNFe, valorTotal, dataEmissao.format(DateTimeFormatter.ISO_DATE));
    }
}

// Enumeração para resultado de validação
enum ResultadoValidacao {
    SUCESSO,
    FALHA,
    TIMEOUT,
    IGNORADO
}

// Classe que representa o resultado de uma validação
class RespostaValidacao {
    private ResultadoValidacao resultado;
    private String nomeValidador;
    private String mensagem;
    private long tempoExecucaoMs;
    private boolean requerRollback;

    public RespostaValidacao(ResultadoValidacao resultado, String nomeValidador, 
                            String mensagem, long tempoExecucaoMs) {
        this.resultado = resultado;
        this.nomeValidador = nomeValidador;
        this.mensagem = mensagem;
        this.tempoExecucaoMs = tempoExecucaoMs;
        this.requerRollback = false;
    }

    public ResultadoValidacao getResultado() { return resultado; }
    public String getNomeValidador() { return nomeValidador; }
    public String getMensagem() { return mensagem; }
    public long getTempoExecucaoMs() { return tempoExecucaoMs; }
    public boolean isRequerRollback() { return requerRollback; }
    public void setRequerRollback(boolean requer) { this.requerRollback = requer; }

    public boolean isSucesso() {
        return resultado == ResultadoValidacao.SUCESSO;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (%dms)", 
            resultado, nomeValidador, mensagem, tempoExecucaoMs);
    }
}

// Contexto de validação que mantém estado da cadeia
class ContextoValidacao {
    private DocumentoFiscal documento;
    private List<RespostaValidacao> respostas;
    private int falhasConsecutivas;
    private boolean circuitBreakerAtivado;
    private List<ValidadorDocumento> validadoresComRollback;
    private static final int MAX_FALHAS_CIRCUIT_BREAKER = 3;

    public ContextoValidacao(DocumentoFiscal documento) {
        this.documento = documento;
        this.respostas = new ArrayList<>();
        this.falhasConsecutivas = 0;
        this.circuitBreakerAtivado = false;
        this.validadoresComRollback = new ArrayList<>();
    }

    public DocumentoFiscal getDocumento() { return documento; }
    public List<RespostaValidacao> getRespostas() { return new ArrayList<>(respostas); }
    public int getFalhasConsecutivas() { return falhasConsecutivas; }
    public boolean isCircuitBreakerAtivado() { return circuitBreakerAtivado; }

    public void adicionarResposta(RespostaValidacao resposta) {
        respostas.add(resposta);
        
        if (!resposta.isSucesso() && resposta.getResultado() != ResultadoValidacao.IGNORADO) {
            falhasConsecutivas++;
            
            // Circuit breaker: após 3 falhas, interrompe a cadeia
            if (falhasConsecutivas >= MAX_FALHAS_CIRCUIT_BREAKER) {
                circuitBreakerAtivado = true;
                registrarLog("[CIRCUIT BREAKER] Ativado após " + MAX_FALHAS_CIRCUIT_BREAKER + " falhas");
            }
        } else if (resposta.isSucesso()) {
            falhasConsecutivas = 0; // Reset em caso de sucesso
        }
    }

    public void registrarValidadorComRollback(ValidadorDocumento validador) {
        validadoresComRollback.add(validador);
    }

    public void executarRollback() {
        if (validadoresComRollback.isEmpty()) {
            registrarLog("[ROLLBACK] Nenhum validador requer rollback");
            return;
        }

        registrarLog("[ROLLBACK] Iniciando rollback de " + validadoresComRollback.size() + " validador(es)");
        
        // Executa rollback em ordem reversa
        for (int i = validadoresComRollback.size() - 1; i >= 0; i--) {
            ValidadorDocumento validador = validadoresComRollback.get(i);
            validador.rollback(documento);
        }
        
        registrarLog("[ROLLBACK] Concluído");
    }

    public boolean deveInterromperCadeia() {
        return circuitBreakerAtivado;
    }

    public boolean todosSucessoAteAgora() {
        for (RespostaValidacao resposta : respostas) {
            if (resposta.getResultado() == ResultadoValidacao.FALHA || 
                resposta.getResultado() == ResultadoValidacao.TIMEOUT) {
                return false;
            }
        }
        return true;
    }

    public boolean temValidadoresComRollback() {
        return !validadoresComRollback.isEmpty();
    }

    private void registrarLog(String mensagem) {
        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + mensagem);
    }

    public void exibirResumo() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Resumo da Validação");
        System.out.println("=".repeat(80));
        System.out.println("Documento: " + documento);
        System.out.println("Total de validações: " + respostas.size());
        
        long sucessos = respostas.stream().filter(RespostaValidacao::isSucesso).count();
        long falhas = respostas.stream()
            .filter(r -> r.getResultado() == ResultadoValidacao.FALHA || r.getResultado() == ResultadoValidacao.TIMEOUT)
            .count();
        
        System.out.println("Sucessos: " + sucessos);
        System.out.println("Falhas: " + falhas);
        System.out.println("Circuit Breaker: " + (circuitBreakerAtivado ? "ATIVADO" : "INATIVO"));
        
        System.out.println("\nDetalhamento:");
        for (RespostaValidacao resposta : respostas) {
            System.out.println("  " + resposta);
        }
        System.out.println("=".repeat(80));
    }
}

// Interface que define o contrato para validadores (Handler do Chain)
interface ValidadorDocumento {
    RespostaValidacao validar(ContextoValidacao contexto);
    void setProximo(ValidadorDocumento proximo);
    ValidadorDocumento getProximo();
    String getNome();
    int getTimeoutSegundos();
    boolean suportaRollback();
    void rollback(DocumentoFiscal documento);
}

// Classe abstrata base para validadores com comportamento comum
abstract class ValidadorBase implements ValidadorDocumento {
    
    protected ValidadorDocumento proximo;
    protected int timeoutSegundos;

    public ValidadorBase(int timeoutSegundos) {
        this.timeoutSegundos = timeoutSegundos;
    }

    @Override
    public void setProximo(ValidadorDocumento proximo) {
        this.proximo = proximo;
    }

    @Override
    public ValidadorDocumento getProximo() {
        return proximo;
    }

    @Override
    public int getTimeoutSegundos() {
        return timeoutSegundos;
    }

    @Override
    public boolean suportaRollback() {
        return false; // Por padrão, validadores não requerem rollback
    }

    @Override
    public void rollback(DocumentoFiscal documento) {
        // Implementação padrão vazia
    }

    // Método template que executa a validação com timeout
    @Override
    public RespostaValidacao validar(ContextoValidacao contexto) {
        registrarLog("[" + getNome() + "] Iniciando validação");
        
        // Verifica circuit breaker
        if (contexto.isCircuitBreakerAtivado()) {
            registrarLog("[" + getNome() + "] Ignorado - Circuit breaker ativado");
            RespostaValidacao resposta = new RespostaValidacao(
                ResultadoValidacao.IGNORADO, getNome(), 
                "Ignorado devido ao circuit breaker", 0
            );
            contexto.adicionarResposta(resposta);
            return resposta;
        }

        // Verifica se deve executar baseado em condições
        if (!deveExecutar(contexto)) {
            registrarLog("[" + getNome() + "] Ignorado - Condições não atendidas");
            RespostaValidacao resposta = new RespostaValidacao(
                ResultadoValidacao.IGNORADO, getNome(), 
                "Ignorado devido a falhas em validações anteriores", 0
            );
            contexto.adicionarResposta(resposta);
            
            // Passa para o próximo mesmo quando ignorado
            if (proximo != null) {
                return proximo.validar(contexto);
            }
            return resposta;
        }

        // Executa validação com timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<RespostaValidacao> future = executor.submit(() -> executarValidacao(contexto.getDocumento()));
        
        RespostaValidacao resposta;
        long inicio = System.currentTimeMillis();
        
        try {
            resposta = future.get(timeoutSegundos, TimeUnit.SECONDS);
            long duracao = System.currentTimeMillis() - inicio;
            
            // Atualiza tempo real de execução
            resposta = new RespostaValidacao(
                resposta.getResultado(), getNome(), 
                resposta.getMensagem(), duracao
            );
            
            registrarLog("[" + getNome() + "] " + resposta.getMensagem() + " (" + duracao + "ms)");
            
        } catch (TimeoutException e) {
            long duracao = System.currentTimeMillis() - inicio;
            resposta = new RespostaValidacao(
                ResultadoValidacao.TIMEOUT, getNome(),
                "Timeout após " + timeoutSegundos + " segundos", duracao
            );
            registrarLog("[" + getNome() + "] TIMEOUT");
            future.cancel(true);
            
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            resposta = new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                "Erro: " + e.getMessage(), duracao
            );
            registrarLog("[" + getNome() + "] ERRO: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        // Adiciona resposta ao contexto
        contexto.adicionarResposta(resposta);

        // Se validador suporta rollback e foi bem-sucedido, registra para possível rollback
        if (suportaRollback() && resposta.isSucesso()) {
            contexto.registrarValidadorComRollback(this);
        }

        // Se falhou e validações subsequentes devem fazer rollback, marca
        if (!resposta.isSucesso() && contexto.temValidadoresComRollback()) {
            resposta.setRequerRollback(true);
        }

        // Passa para o próximo validador na cadeia
        if (proximo != null && !contexto.isCircuitBreakerAtivado()) {
            return proximo.validar(contexto);
        }

        return resposta;
    }

    // Método abstrato que subclasses devem implementar com a lógica específica
    protected abstract RespostaValidacao executarValidacao(DocumentoFiscal documento) throws Exception;

    // Método que subclasses podem sobrescrever para definir condições de execução
    protected boolean deveExecutar(ContextoValidacao contexto) {
        return true; // Por padrão, sempre executa
    }

    protected void registrarLog(String mensagem) {
        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + mensagem);
    }
}

// Validador 1: Schema XML contra XSD
class ValidadorSchemaXML extends ValidadorBase {

    public ValidadorSchemaXML() {
        super(2); // Timeout de 2 segundos
    }

    @Override
    public String getNome() {
        return "Validador de Schema XML";
    }

    @Override
    protected RespostaValidacao executarValidacao(DocumentoFiscal documento) throws Exception {
        // Simula validação de schema XML
        Thread.sleep(300); // Simula processamento
        
        String xml = documento.getXmlConteudo();
        
        if (xml == null || xml.isEmpty()) {
            return new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                "XML vazio ou inválido", 0
            );
        }

        if (!xml.contains("<?xml") || !xml.contains("NFe")) {
            return new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                "XML não está em conformidade com schema XSD da NF-e", 0
            );
        }

        return new RespostaValidacao(
            ResultadoValidacao.SUCESSO, getNome(),
            "Schema XML validado com sucesso", 0
        );
    }
}

// Validador 2: Certificado Digital
class ValidadorCertificadoDigital extends ValidadorBase {

    public ValidadorCertificadoDigital() {
        super(3); // Timeout de 3 segundos
    }

    @Override
    public String getNome() {
        return "Validador de Certificado Digital";
    }

    @Override
    protected RespostaValidacao executarValidacao(DocumentoFiscal documento) throws Exception {
        // Simula validação de certificado
        Thread.sleep(400);
        
        String certificado = documento.getCertificadoDigital();
        
        if (certificado == null || certificado.isEmpty()) {
            return new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                "Certificado digital não encontrado", 0
            );
        }

        // Simula verificação de expiração
        if (certificado.contains("EXPIRADO")) {
            return new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                "Certificado digital expirado", 0
            );
        }

        // Simula verificação de revogação
        if (certificado.contains("REVOGADO")) {
            return new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                "Certificado digital revogado", 0
            );
        }

        return new RespostaValidacao(
            ResultadoValidacao.SUCESSO, getNome(),
            "Certificado digital válido (expiração e revogação OK)", 0
        );
    }
}

// Validador 3: Regras Fiscais
// Este validador só executa se os anteriores passarem
class ValidadorRegrasFiscais extends ValidadorBase {

    public ValidadorRegrasFiscais() {
        super(5); // Timeout de 5 segundos
    }

    @Override
    public String getNome() {
        return "Validador de Regras Fiscais";
    }

    @Override
    protected boolean deveExecutar(ContextoValidacao contexto) {
        // Só executa se todos os validadores anteriores passaram
        return contexto.todosSucessoAteAgora();
    }

    @Override
    protected RespostaValidacao executarValidacao(DocumentoFiscal documento) throws Exception {
        // Simula validação de regras fiscais e cálculo de impostos
        Thread.sleep(600);
        
        double valorTotal = documento.getValorTotal();
        double valorImpostos = documento.getValorImpostos();
        
        // Simula cálculo de impostos (35% do valor total)
        double impostosCalculados = valorTotal * 0.35;
        double diferenca = Math.abs(impostosCalculados - valorImpostos);
        
        if (diferenca > 0.01) { // Tolerância de 1 centavo
            return new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                String.format("Impostos incorretos. Esperado: R$%.2f, Informado: R$%.2f", 
                    impostosCalculados, valorImpostos), 0
            );
        }

        return new RespostaValidacao(
            ResultadoValidacao.SUCESSO, getNome(),
            "Cálculo de impostos correto", 0
        );
    }
}

// Validador 4: Banco de Dados (verifica duplicidade)
// Este validador suporta rollback
class ValidadorBancoDados extends ValidadorBase {

    private static List<String> numerosInseridos = new ArrayList<>();

    public ValidadorBancoDados() {
        super(4); // Timeout de 4 segundos
    }

    @Override
    public String getNome() {
        return "Validador de Banco de Dados";
    }

    @Override
    public boolean suportaRollback() {
        return true; // Este validador suporta rollback
    }

    @Override
    protected RespostaValidacao executarValidacao(DocumentoFiscal documento) throws Exception {
        // Simula consulta ao banco de dados
        Thread.sleep(500);
        
        String numeroNFe = documento.getNumeroNFe();
        
        // Verifica duplicidade
        if (numerosInseridos.contains(numeroNFe)) {
            return new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                "NF-e duplicada - Número já existe no banco de dados", 0
            );
        }

        // Simula inserção no banco
        numerosInseridos.add(numeroNFe);
        documento.setInseridoBancoDados(true);
        documento.setIdBancoDados("DB_" + numeroNFe + "_" + System.currentTimeMillis());
        
        registrarLog("[" + getNome() + "] NF-e inserida no banco: " + documento.getIdBancoDados());

        return new RespostaValidacao(
            ResultadoValidacao.SUCESSO, getNome(),
            "NF-e registrada no banco de dados (ID: " + documento.getIdBancoDados() + ")", 0
        );
    }

    @Override
    public void rollback(DocumentoFiscal documento) {
        if (documento.isInseridoBancoDados()) {
            registrarLog("[ROLLBACK - " + getNome() + "] Removendo NF-e do banco: " + documento.getIdBancoDados());
            numerosInseridos.remove(documento.getNumeroNFe());
            documento.setInseridoBancoDados(false);
            documento.setIdBancoDados(null);
        }
    }
}

// Validador 5: Serviço SEFAZ
// Este validador só executa se os anteriores passarem
class ValidadorServicoSEFAZ extends ValidadorBase {

    public ValidadorServicoSEFAZ() {
        super(10); // Timeout de 10 segundos (serviço externo pode ser lento)
    }

    @Override
    public String getNome() {
        return "Validador de Serviço SEFAZ";
    }

    @Override
    protected boolean deveExecutar(ContextoValidacao contexto) {
        // Só executa se todos os validadores anteriores passaram
        return contexto.todosSucessoAteAgora();
    }

    @Override
    protected RespostaValidacao executarValidacao(DocumentoFiscal documento) throws Exception {
        // Simula consulta ao serviço SEFAZ (pode ser lento)
        Thread.sleep(800);
        
        String numeroNFe = documento.getNumeroNFe();
        
        // Simula diferentes respostas do SEFAZ
        if (numeroNFe.endsWith("999")) {
            return new RespostaValidacao(
                ResultadoValidacao.FALHA, getNome(),
                "SEFAZ rejeitou a NF-e - Inconsistência nos dados", 0
            );
        }

        return new RespostaValidacao(
            ResultadoValidacao.SUCESSO, getNome(),
            "NF-e autorizada pela SEFAZ", 0
        );
    }
}

// Construtor da cadeia de validação
class CadeiaValidacaoNFe {
    
    private ValidadorDocumento primeiroValidador;

    public CadeiaValidacaoNFe() {
        construirCadeia();
    }

    private void construirCadeia() {
        // Cria os validadores
        ValidadorDocumento validador1 = new ValidadorSchemaXML();
        ValidadorDocumento validador2 = new ValidadorCertificadoDigital();
        ValidadorDocumento validador3 = new ValidadorRegrasFiscais();
        ValidadorDocumento validador4 = new ValidadorBancoDados();
        ValidadorDocumento validador5 = new ValidadorServicoSEFAZ();

        // Monta a cadeia
        validador1.setProximo(validador2);
        validador2.setProximo(validador3);
        validador3.setProximo(validador4);
        validador4.setProximo(validador5);

        primeiroValidador = validador1;
    }

    public ContextoValidacao validar(DocumentoFiscal documento) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Iniciando validação de NF-e: " + documento.getNumeroNFe());
        System.out.println("=".repeat(80));

        ContextoValidacao contexto = new ContextoValidacao(documento);
        
        // Inicia a cadeia de validação
        primeiroValidador.validar(contexto);

        // Se houve falha em validadores que requerem rollback, executa rollback
        boolean houveRollback = contexto.getRespostas().stream()
            .anyMatch(RespostaValidacao::isRequerRollback);
        
        if (houveRollback) {
            contexto.executarRollback();
        }

        return contexto;
    }
}

// Classe principal que demonstra o uso do padrão Chain of Responsibility
public class Questao4ChainOfResponsibility {
    
    // Demonstra o funcionamento do sistema de validação
    public static void demonstrarChainPattern() {
        String separator = "=".repeat(80);
        
        System.out.println("\n" + separator);
        System.out.println("Sistema de Validação de NF-e - Padrão Chain of Responsibility");
        System.out.println(separator);

        CadeiaValidacaoNFe cadeia = new CadeiaValidacaoNFe();

        // Cenário 1: NF-e válida - todas as validações passam
        System.out.println("\n" + separator);
        System.out.println("Cenário 1: NF-e completamente válida");
        System.out.println(separator);
        
        DocumentoFiscal nfe1 = new DocumentoFiscal(
            "NFE00001",
            "<?xml version='1.0'?><NFe><dados>conteudo</dados></NFe>",
            "CERT_VALIDO_2025",
            LocalDate.now(),
            10000.00,
            3500.00 // 35% de impostos
        );
        
        ContextoValidacao contexto1 = cadeia.validar(nfe1);
        contexto1.exibirResumo();

        // Cenário 2: XML inválido - primeira validação falha
        System.out.println("\n" + separator);
        System.out.println("Cenário 2: XML inválido - primeira validação falha");
        System.out.println(separator);
        
        DocumentoFiscal nfe2 = new DocumentoFiscal(
            "NFE00002",
            "xml invalido sem tags",
            "CERT_VALIDO_2025",
            LocalDate.now(),
            5000.00,
            1750.00
        );
        
        ContextoValidacao contexto2 = cadeia.validar(nfe2);
        contexto2.exibirResumo();

        // Cenário 3: Certificado expirado - validações condicionais são ignoradas
        System.out.println("\n" + separator);
        System.out.println("Cenário 3: Certificado expirado");
        System.out.println(separator);
        
        DocumentoFiscal nfe3 = new DocumentoFiscal(
            "NFE00003",
            "<?xml version='1.0'?><NFe><dados>conteudo</dados></NFe>",
            "CERT_EXPIRADO",
            LocalDate.now(),
            8000.00,
            2800.00
        );
        
        ContextoValidacao contexto3 = cadeia.validar(nfe3);
        contexto3.exibirResumo();

        // Cenário 4: Impostos incorretos - Validador 3 falha
        System.out.println("\n" + separator);
        System.out.println("Cenário 4: Cálculo de impostos incorreto");
        System.out.println(separator);
        
        DocumentoFiscal nfe4 = new DocumentoFiscal(
            "NFE00004",
            "<?xml version='1.0'?><NFe><dados>conteudo</dados></NFe>",
            "CERT_VALIDO_2025",
            LocalDate.now(),
            10000.00,
            2000.00 // Valor incorreto (deveria ser 3500)
        );
        
        ContextoValidacao contexto4 = cadeia.validar(nfe4);
        contexto4.exibirResumo();

        // Cenário 5: NF-e duplicada - Validador 4 falha
        System.out.println("\n" + separator);
        System.out.println("Cenário 5: NF-e duplicada no banco de dados");
        System.out.println(separator);
        
        // Tenta inserir a mesma NF-e do cenário 1 novamente
        DocumentoFiscal nfe5 = new DocumentoFiscal(
            "NFE00001", // Mesmo número do cenário 1
            "<?xml version='1.0'?><NFe><dados>conteudo</dados></NFe>",
            "CERT_VALIDO_2025",
            LocalDate.now(),
            10000.00,
            3500.00
        );
        
        ContextoValidacao contexto5 = cadeia.validar(nfe5);
        contexto5.exibirResumo();

        // Cenário 6: SEFAZ rejeita - demonstra rollback do banco de dados
        System.out.println("\n" + separator);
        System.out.println("Cenário 6: SEFAZ rejeita (demonstra rollback)");
        System.out.println(separator);
        
        DocumentoFiscal nfe6 = new DocumentoFiscal(
            "NFE00999", // Termina em 999 - será rejeitado pela SEFAZ
            "<?xml version='1.0'?><NFe><dados>conteudo</dados></NFe>",
            "CERT_VALIDO_2025",
            LocalDate.now(),
            15000.00,
            5250.00
        );
        
        ContextoValidacao contexto6 = cadeia.validar(nfe6);
        contexto6.exibirResumo();

        // Cenário 7: Circuit Breaker - simula múltiplas falhas
        System.out.println("\n" + separator);
        System.out.println("Cenário 7: Circuit Breaker após 3 falhas");
        System.out.println(separator);
        
        DocumentoFiscal nfe7 = new DocumentoFiscal(
            "NFE00007",
            "xml invalido", // Falhará na validação 1
            "CERT_EXPIRADO", // Falhará na validação 2 (mas não chegará lá)
            LocalDate.now(),
            1000.00,
            100.00 // Impostos incorretos (falhará validação 3)
        );
        
        // Cria contexto e força 3 falhas para ativar circuit breaker
        ContextoValidacao contexto7 = new ContextoValidacao(nfe7);
        
        // Adiciona 3 falhas manualmente para demonstrar circuit breaker
        contexto7.adicionarResposta(new RespostaValidacao(ResultadoValidacao.FALHA, "Teste 1", "Falha", 100));
        contexto7.adicionarResposta(new RespostaValidacao(ResultadoValidacao.FALHA, "Teste 2", "Falha", 100));
        contexto7.adicionarResposta(new RespostaValidacao(ResultadoValidacao.FALHA, "Teste 3", "Falha", 100));
        
        // Agora tenta validar - os validadores serão ignorados devido ao circuit breaker
        new ValidadorSchemaXML().validar(contexto7);
        
        contexto7.exibirResumo();
    }
    
    // Método main para executar a demonstração
    public static void main(String[] args) {
        demonstrarChainPattern();
    }
}

