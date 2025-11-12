/**
 * QUESTÃO 2 - PADRÃO ADAPTER
 * 
 * Justificativa da escolha do padrão:
 * O padrão Adapter foi escolhido porque:
 * 1. Permite integrar uma interface moderna com um sistema legado sem modificar nenhum dos dois
 * 2. Resolve incompatibilidade de assinaturas de métodos e tipos de dados
 * 3. Encapsula a complexidade de conversão de dados entre formatos diferentes
 * 4. Possibilita comunicação bidirecional através de adaptação nos dois sentidos
 * 5. Respeita o Open/Closed Principle - o legado permanece fechado para modificação
 * 6. Respeita o Single Responsibility Principle - a conversão é responsabilidade única do adapter
 * 7. Facilita testes isolados tanto do sistema moderno quanto do legado
 */

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;



// Interface moderna para processamento de transações
// Utiliza tipos de dados atualizados e assinaturas de métodos simplificadas
interface ProcessadorTransacoes {
    RespostaTransacao autorizar(String cartao, double valor, String moeda);
    String obterStatusTransacao(String idTransacao);
}

// Classe que representa a resposta de uma transação no formato moderno
class RespostaTransacao {
    private String idTransacao;
    private boolean aprovada;
    private String mensagem;
    private String codigoResposta;
    private LocalDateTime dataHora;
    private double valorProcessado;
    private String moeda;

    public RespostaTransacao(String idTransacao, boolean aprovada, String mensagem, 
                           String codigoResposta, double valorProcessado, String moeda) {
        this.idTransacao = idTransacao;
        this.aprovada = aprovada;
        this.mensagem = mensagem;
        this.codigoResposta = codigoResposta;
        this.dataHora = LocalDateTime.now();
        this.valorProcessado = valorProcessado;
        this.moeda = moeda;
    }

    public String getIdTransacao() { return idTransacao; }
    public boolean isAprovada() { return aprovada; }
    public String getMensagem() { return mensagem; }
    public String getCodigoResposta() { return codigoResposta; }
    public LocalDateTime getDataHora() { return dataHora; }
    public double getValorProcessado() { return valorProcessado; }
    public String getMoeda() { return moeda; }

    @Override
    public String toString() {
        return String.format(
            "RespostaTransacao{id='%s', aprovada=%s, mensagem='%s', codigo='%s', valor=%.2f %s, data=%s}",
            idTransacao, aprovada, mensagem, codigoResposta, valorProcessado, moeda,
            dataHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}

// Classe que representa o sistema bancário legado
// Utiliza HashMap com tipos Object e estrutura de dados obsoleta
// Esta classe simula um sistema legado real com todas suas peculiaridades:
// - Usa HashMap<String, Object> em vez de tipos específicos
// - Requer campos que não fazem sentido no sistema moderno
// - Usa codificação numérica para valores que poderiam ser strings
class SistemaBancarioLegado {
    
    public HashMap<String, Object> processarTransacao(HashMap<String, Object> parametros) {
        HashMap<String, Object> resultado = new HashMap<>();
        
        try {
            // Validação de campos obrigatórios do sistema legado
            validarCamposObrigatorios(parametros);
            
            String numeroCartao = (String) parametros.get("numeroCartao");
            Integer valorCentavos = (Integer) parametros.get("valorCentavos");
            Integer codigoMoeda = (Integer) parametros.get("codigoMoeda");
            String codigoFilial = (String) parametros.get("codigoFilial");
            
            // Simulação de lógica de processamento do legado
            boolean aprovado = simularValidacaoLegado(numeroCartao, valorCentavos);
            
            String idTransacao = gerarIdTransacaoLegado();
            
            resultado.put("idTransacao", idTransacao);
            resultado.put("statusCode", aprovado ? 0 : 9999);
            resultado.put("descricaoStatus", aprovado ? "TRANSACAO APROVADA" : "TRANSACAO NEGADA");
            resultado.put("valorProcessadoCentavos", valorCentavos);
            resultado.put("codigoMoeda", codigoMoeda);
            resultado.put("timestampProcessamento", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            ));
            resultado.put("codigoFilialProcessadora", codigoFilial);
            
            System.out.println("[LEGADO] Transação processada: " + idTransacao);
            
        } catch (Exception e) {
            resultado.put("idTransacao", "ERROR");
            resultado.put("statusCode", -1);
            resultado.put("descricaoStatus", "ERRO: " + e.getMessage());
            resultado.put("valorProcessadoCentavos", 0);
            resultado.put("codigoMoeda", 0);
            resultado.put("timestampProcessamento", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            ));
        }
        
        return resultado;
    }
    
    // Valida campos obrigatórios do sistema legado.
    private void validarCamposObrigatorios(HashMap<String, Object> parametros) {
        String[] camposObrigatorios = {
            "numeroCartao", "valorCentavos", "codigoMoeda", "codigoFilial", "timestampLegado"
        };
        
        for (String campo : camposObrigatorios) {
            if (!parametros.containsKey(campo) || parametros.get(campo) == null) {
                throw new IllegalArgumentException("Campo obrigatório ausente: " + campo);
            }
        }
    }
    
    // Simula a lógica de validação do sistema legado
    // Regras dummy: aprova se valor < 500000 centavos e cartão tem mais de 10 dígitos
    private boolean simularValidacaoLegado(String numeroCartao, Integer valorCentavos) {
        return numeroCartao != null 
            && numeroCartao.length() >= 10 
            && valorCentavos <= 500000;
    }
    
    // Gera ID de transação no formato legado
    private String gerarIdTransacaoLegado() {
        return "LEG" + System.currentTimeMillis() + ((int)(Math.random() * 1000));
    }
    
    // Consulta status de uma transação no sistema legado
    public HashMap<String, Object> consultarStatusTransacao(String idTransacao) {
        HashMap<String, Object> status = new HashMap<>();
        status.put("idTransacao", idTransacao);
        status.put("statusCode", 0);
        status.put("descricaoStatus", "PROCESSADA");
        status.put("timestampConsulta", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        ));
        return status;
    }
}

// Adapter que converte entre a interface moderna e o sistema legado
// Converte chamadas modernas (autorizar) para o formato legado (processarTransacao)
// Converte respostas do legado de volta para o formato moderno
// Gerencia campos obrigatórios do legado que não existem na interface moderna
// Realiza conversão de tipos de dados (String/double -> HashMap<String,Object>)
// Traduz códigos de moeda entre string (USD, EUR, BRL) e inteiros (1, 2, 3)
class AdaptadorTransacoesBancarias implements ProcessadorTransacoes {
    
    private SistemaBancarioLegado sistemaLegado;
    private String codigoFilialPadrao;
    
    // Mapeamento de códigos de moeda conforme especificação
    private static final Map<String, Integer> CODIGO_MOEDA = Map.of(
        "USD", 1,
        "EUR", 2,
        "BRL", 3
    );
    
    private static final Map<Integer, String> MOEDA_CODIGO = Map.of(
        1, "USD",
        2, "EUR",
        3, "BRL"
    );
    
    // Construtor do adapter
    public AdaptadorTransacoesBancarias(SistemaBancarioLegado sistemaLegado, String codigoFilialPadrao) {
        if (sistemaLegado == null) {
            throw new IllegalArgumentException("Sistema legado não pode ser nulo");
        }
        if (codigoFilialPadrao == null || codigoFilialPadrao.trim().isEmpty()) {
            throw new IllegalArgumentException("Código da filial não pode ser vazio");
        }
        
        this.sistemaLegado = sistemaLegado;
        this.codigoFilialPadrao = codigoFilialPadrao;
        
        System.out.println("[ADAPTER] Inicializado com filial: " + codigoFilialPadrao);
    }
    
    // Implementação do método da interface moderna
    // Converte chamada moderna para formato legado (Moderno -> Legado)
    @Override
    public RespostaTransacao autorizar(String cartao, double valor, String moeda) {
        System.out.println("\n[ADAPTER] Convertendo chamada moderna para formato legado");
        
        // Validação de entrada
        validarEntradaModerna(cartao, valor, moeda);
        
        // CONVERSÃO MODERNO -> LEGADO
        HashMap<String, Object> parametrosLegado = converterParaFormatoLegado(cartao, valor, moeda);
        
        System.out.println("[ADAPTER] Parâmetros legado: " + parametrosLegado);
        
        // Chama o sistema legado
        HashMap<String, Object> respostaLegado = sistemaLegado.processarTransacao(parametrosLegado);
        
        System.out.println("[ADAPTER] Resposta legado: " + respostaLegado);
        
        // CONVERSÃO LEGADO -> MODERNO
        RespostaTransacao respostaModerna = converterParaFormatoModerno(respostaLegado);
        
        System.out.println("[ADAPTER] Resposta moderna: " + respostaModerna);
        
        return respostaModerna;
    }
    
    // Implementação do método de consulta de status
    // Converte resposta do legado para formato moderno
    @Override
    public String obterStatusTransacao(String idTransacao) {
        System.out.println("\n[ADAPTER] Consultando status da transação: " + idTransacao);
        
        HashMap<String, Object> statusLegado = sistemaLegado.consultarStatusTransacao(idTransacao);
        
        Integer statusCode = (Integer) statusLegado.get("statusCode");
        String descricao = (String) statusLegado.get("descricaoStatus");
        
        return String.format("Status: %s (Código: %d)", descricao, statusCode);
    }
    
    // Valida entrada da interface moderna
    private void validarEntradaModerna(String cartao, double valor, String moeda) {
        if (cartao == null || cartao.trim().isEmpty()) {
            throw new IllegalArgumentException("Número do cartão não pode ser vazio");
        }
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
        if (!CODIGO_MOEDA.containsKey(moeda)) {
            throw new IllegalArgumentException(
                "Moeda inválida. Moedas aceitas: " + CODIGO_MOEDA.keySet()
            );
        }
    }
    
    // CONVERSÃO BIDIRECIONAL PARTE 1: Moderno -> Legado
    // Converte parâmetros modernos para o formato HashMap esperado pelo legado
    // Conversões realizadas:
    // 1. String cartao -> "numeroCartao" (String)
    // 2. double valor -> "valorCentavos" (Integer) - multiplica por 100
    // 3. String moeda -> "codigoMoeda" (Integer) - usa mapeamento
    // 4. Adiciona "codigoFilial" - campo obrigatório do legado não presente na interface moderna
    // 5. Adiciona "timestampLegado" - timestamp no formato legado
    private HashMap<String, Object> converterParaFormatoLegado(String cartao, double valor, String moeda) {
        HashMap<String, Object> parametros = new HashMap<>();
        
        // Conversão de tipos e formatos
        parametros.put("numeroCartao", cartao);
        parametros.put("valorCentavos", (int) Math.round(valor * 100)); // Converte para centavos
        parametros.put("codigoMoeda", CODIGO_MOEDA.get(moeda)); // Converte string para código
        
        // Campos obrigatórios do legado que não existem na interface moderna
        parametros.put("codigoFilial", codigoFilialPadrao);
        parametros.put("timestampLegado", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        ));
        
        return parametros;
    }
    
    // CONVERSÃO BIDIRECIONAL PARTE 2: Legado -> Moderno
    // Converte resposta do sistema legado para o formato moderno RespostaTransacao
    // Conversões realizadas:
    // 1. "idTransacao" (String) -> idTransacao (String)
    // 2. "statusCode" (Integer) -> aprovada (boolean) - 0 = true, outros = false
    // 3. "descricaoStatus" (String) -> mensagem (String)
    // 4. "statusCode" (Integer) -> codigoResposta (String) - converte para string
    // 5. "valorProcessadoCentavos" (Integer) -> valorProcessado (double) - divide por 100
    // 6. "codigoMoeda" (Integer) -> moeda (String) - usa mapeamento reverso
    private RespostaTransacao converterParaFormatoModerno(HashMap<String, Object> respostaLegado) {
        String idTransacao = (String) respostaLegado.get("idTransacao");
        Integer statusCode = (Integer) respostaLegado.get("statusCode");
        String descricaoStatus = (String) respostaLegado.get("descricaoStatus");
        Integer valorCentavos = (Integer) respostaLegado.get("valorProcessadoCentavos");
        Integer codigoMoeda = (Integer) respostaLegado.get("codigoMoeda");
        
        // Conversões de formato legado para moderno
        boolean aprovada = (statusCode == 0);
        double valorProcessado = valorCentavos / 100.0; // Converte centavos para unidades
        String moeda = MOEDA_CODIGO.getOrDefault(codigoMoeda, "UNKNOWN");
        String codigoResposta = String.valueOf(statusCode);
        
        return new RespostaTransacao(
            idTransacao,
            aprovada,
            descricaoStatus,
            codigoResposta,
            valorProcessado,
            moeda
        );
    }
    
    // Método utilitário para converter código de moeda para string
    // Útil para conversões adicionais se necessário
    public static String converterCodigoMoedaParaString(int codigo) {
        return MOEDA_CODIGO.getOrDefault(codigo, "UNKNOWN");
    }
    
    // Método utilitário para converter string de moeda para código
    // Útil para conversões adicionais se necessário
    public static Integer converterStringParaCodigoMoeda(String moeda) {
        return CODIGO_MOEDA.get(moeda);
    }
}

// Classe principal que demonstra o uso do padrão Adapter
public class Questao2Adapter {
    
    // Demonstra o funcionamento bidirecional do Adapter
    // Cenários demonstrados:
    // 1. Transação aprovada em USD
    // 2. Transação aprovada em EUR
    // 3. Transação aprovada em BRL
    // 4. Transação negada (valor alto)
    // 5. Consulta de status
    public static void demonstrarAdapterPattern() {
        String separator = "=".repeat(80);
        
        System.out.println("\n" + separator);
        System.out.println("Sistema de Integração Bancária - Padrão Adapter");
        System.out.println(separator);
        
        // Criação do sistema legado
        SistemaBancarioLegado sistemaLegado = new SistemaBancarioLegado();
        
        // Criação do adapter com código de filial padrão
        // O código da filial é um campo obrigatório do legado que não existe na interface moderna
        ProcessadorTransacoes processador = new AdaptadorTransacoesBancarias(
            sistemaLegado, 
            "FIL001" // Código da filial padrão
        );
                
        // Cenário 1: Transação em USD
        System.out.println("\n" + separator);
        System.out.println("Cenário 1: Transação de $250.75 USD");
        System.out.println(separator);
        
        RespostaTransacao resposta1 = processador.autorizar(
            "4532123456789012", 
            250.75, 
            "USD"
        );
        
        exibirResultado(resposta1);
        
        // Cenário 2: Transação em EUR
        System.out.println("\n" + separator);
        System.out.println("Cenário 2: Transação de €1,500.00 EUR");
        System.out.println(separator);
        
        RespostaTransacao resposta2 = processador.autorizar(
            "5412345678901234", 
            1500.00, 
            "EUR"
        );
        
        exibirResultado(resposta2);
        
        // Cenário 3: Transação em BRL
        System.out.println("\n" + separator);
        System.out.println("Cenário 3: Transação de R$99.99 BRL");
        System.out.println(separator);
        
        RespostaTransacao resposta3 = processador.autorizar(
            "6011123456789012", 
            99.99, 
            "BRL"
        );
        
        exibirResultado(resposta3);
        
        // Cenário 4: Transação NEGADA (valor muito alto)
        System.out.println("\n" + separator);
        System.out.println("Cenário 4: Transação de $10,000.00 USD (Deve ser negada)");
        System.out.println(separator);
        
        RespostaTransacao resposta4 = processador.autorizar(
            "4532123456789012", 
            10000.00, 
            "USD"
        );
        
        exibirResultado(resposta4);
        
        // Cenário 5: Consulta de status
        System.out.println("\n" + separator);
        System.out.println("Cenário 5: Consulta de status de transação");
        System.out.println(separator);
        
        String status = processador.obterStatusTransacao(resposta1.getIdTransacao());
        System.out.println("\n" + status);
    }
    
    // Exibe o resultado de uma transação de forma formatada
    private static void exibirResultado(RespostaTransacao resposta) {
        System.out.println("\nResultado da Transação");
        System.out.println("ID: " + resposta.getIdTransacao());
        System.out.println("Status: " + (resposta.isAprovada() ? "APROVADA" : "NEGADA"));
        System.out.println("Mensagem: " + resposta.getMensagem());
        System.out.println("Código: " + resposta.getCodigoResposta());
        System.out.println("Valor: " + String.format("%.2f %s", 
            resposta.getValorProcessado(), resposta.getMoeda()));
        System.out.println("Data/Hora: " + resposta.getDataHora().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }
    
    // Método main para executar a demonstração
    public static void main(String[] args) {
        demonstrarAdapterPattern();
    }
}

