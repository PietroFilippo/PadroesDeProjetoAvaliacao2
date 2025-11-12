/**
 * QUESTÃO 3 - PADRÃO STATE
 * 
 * Justificativa da escolha do padrão:
 * O padrão State foi escolhido porque:
 * 1. Permite encapsular comportamentos específicos de cada estado da usina em classes separadas
 * 2. Facilita a adição de novos estados sem modificar estados existentes (Open/Closed Principle)
 * 3. Elimina condicionais complexos para gerenciar transições entre estados
 * 4. Cada estado conhece suas próprias regras de transição, respeitando Single Responsibility
 * 5. Permite validações complexas e específicas para cada transição de estado
 * 6. Facilita o controle de transições unidirecionais e prevenção de ciclos perigosos
 * 7. O contexto delega comportamento ao estado atual, simplificando a lógica
 */

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// Classe que representa as condições operacionais da usina
class CondicoesOperacionais {
    private double temperatura;
    private double pressao;
    private double nivelRadiacao;
    private boolean sistemaResfriamentoOperacional;
    private LocalDateTime inicioCondicaoAlerta;

    public CondicoesOperacionais() {
        this.temperatura = 25.0;
        this.pressao = 1.0;
        this.nivelRadiacao = 0.1;
        this.sistemaResfriamentoOperacional = true;
        this.inicioCondicaoAlerta = null;
    }

    public double getTemperatura() { return temperatura; }
    public double getPressao() { return pressao; }
    public double getNivelRadiacao() { return nivelRadiacao; }
    public boolean isSistemaResfriamentoOperacional() { return sistemaResfriamentoOperacional; }
    public LocalDateTime getInicioCondicaoAlerta() { return inicioCondicaoAlerta; }

    public void setTemperatura(double temperatura) { this.temperatura = temperatura; }
    public void setPressao(double pressao) { this.pressao = pressao; }
    public void setNivelRadiacao(double nivelRadiacao) { this.nivelRadiacao = nivelRadiacao; }
    public void setSistemaResfriamentoOperacional(boolean operacional) { 
        this.sistemaResfriamentoOperacional = operacional; 
    }
    public void setInicioCondicaoAlerta(LocalDateTime inicio) { 
        this.inicioCondicaoAlerta = inicio; 
    }

    // Calcula há quantos segundos a condição de alerta está ativa
    public long getSegundosEmAlerta() {
        if (inicioCondicaoAlerta == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(inicioCondicaoAlerta, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format(
            "Temperatura: %.1f°C, Pressão: %.2f bar, Radiação: %.2f mSv/h, Resfriamento: %s",
            temperatura, pressao, nivelRadiacao, 
            sistemaResfriamentoOperacional ? "OPERACIONAL" : "FALHA"
        );
    }
}

// Interface que define o contrato para todos os estados da usina
interface EstadoUsina {
    String getNome();
    String getDescricao();
    void entrar(UsinaNuclear contexto);
    void sair(UsinaNuclear contexto);
    void monitorar(UsinaNuclear contexto);
    boolean podeTransicionarPara(EstadoUsina novoEstado, UsinaNuclear contexto);
    List<String> getAcoesPermitidas();
}

// Estado abstrato base com comportamentos comuns
abstract class EstadoUsinaBase implements EstadoUsina {
    
    @Override
    public void entrar(UsinaNuclear contexto) {
        contexto.registrarLog("Entrando no estado: " + getNome());
    }
    
    @Override
    public void sair(UsinaNuclear contexto) {
        contexto.registrarLog("Saindo do estado: " + getNome());
    }
    
    @Override
    public List<String> getAcoesPermitidas() {
        return new ArrayList<>();
    }
    
    // Método helper para validar condições básicas
    protected boolean validarCondicoesSaida(UsinaNuclear contexto) {
        CondicoesOperacionais cond = contexto.getCondicoes();
        return cond.getTemperatura() < 100 
            && cond.getPressao() < 5 
            && cond.getNivelRadiacao() < 1.0;
    }
}

// ESTADO 1: DESLIGADA
class EstadoDesligada extends EstadoUsinaBase {
    
    @Override
    public String getNome() {
        return "DESLIGADA";
    }
    
    @Override
    public String getDescricao() {
        return "Usina completamente desligada. Todos os sistemas em modo standby.";
    }
    
    @Override
    public void monitorar(UsinaNuclear contexto) {
        // No estado desligado, não há monitoramento ativo
        contexto.registrarLog("[DESLIGADA] Sistemas em standby");
    }
    
    @Override
    public boolean podeTransicionarPara(EstadoUsina novoEstado, UsinaNuclear contexto) {
        // De DESLIGADA só pode ir para OPERACAO_NORMAL ou MANUTENCAO
        if (novoEstado instanceof EstadoOperacaoNormal) {
            return validarCondicoesSaida(contexto);
        }
        if (novoEstado instanceof EstadoManutencao) {
            return true;
        }
        contexto.registrarLog("[ERRO] Transição inválida de DESLIGADA para " + novoEstado.getNome());
        return false;
    }
    
    @Override
    public List<String> getAcoesPermitidas() {
        List<String> acoes = new ArrayList<>();
        acoes.add("Iniciar usina");
        acoes.add("Entrar em manutenção");
        return acoes;
    }
}

// Estado 2: OPERACAO_NORMAL
class EstadoOperacaoNormal extends EstadoUsinaBase {
    
    @Override
    public String getNome() {
        return "OPERACAO_NORMAL";
    }
    
    @Override
    public String getDescricao() {
        return "Usina operando dentro dos parâmetros normais.";
    }
    
    @Override
    public void entrar(UsinaNuclear contexto) {
        super.entrar(contexto);
        // Reseta o contador de alerta ao entrar em operação normal
        contexto.getCondicoes().setInicioCondicaoAlerta(null);
    }
    
    @Override
    public void monitorar(UsinaNuclear contexto) {
        CondicoesOperacionais cond = contexto.getCondicoes();
        contexto.registrarLog("[OPERACAO_NORMAL] Monitorando: " + cond);
        
        // REGRA: OPERACAO_NORMAL → ALERTA_AMARELO se temperatura > 300°C
        if (cond.getTemperatura() > 300) {
            contexto.registrarLog("[ALERTA] Temperatura acima de 300°C - Transicionando para ALERTA_AMARELO");
            contexto.transicionarPara(new EstadoAlertaAmarelo());
        }
    }
    
    @Override
    public boolean podeTransicionarPara(EstadoUsina novoEstado, UsinaNuclear contexto) {
        // De OPERACAO_NORMAL pode ir para DESLIGADA, ALERTA_AMARELO ou MANUTENCAO
        if (novoEstado instanceof EstadoDesligada) {
            return validarCondicoesSaida(contexto);
        }
        if (novoEstado instanceof EstadoAlertaAmarelo || novoEstado instanceof EstadoManutencao) {
            return true;
        }
        // Previne transição direta para ALERTA_VERMELHO ou EMERGENCIA
        contexto.registrarLog("[ERRO] Transição direta não permitida para " + novoEstado.getNome());
        return false;
    }
    
    @Override
    public List<String> getAcoesPermitidas() {
        List<String> acoes = new ArrayList<>();
        acoes.add("Monitorar condições");
        acoes.add("Desligar usina");
        acoes.add("Entrar em manutenção");
        return acoes;
    }
}

// Estado 3: ALERTA_AMARELO
class EstadoAlertaAmarelo extends EstadoUsinaBase {
    
    @Override
    public String getNome() {
        return "ALERTA_AMARELO";
    }
    
    @Override
    public String getDescricao() {
        return "Condições anormais detectadas. Atenção redobrada necessária.";
    }
    
    @Override
    public void entrar(UsinaNuclear contexto) {
        super.entrar(contexto);
        // Marca o início da condição de alerta
        contexto.getCondicoes().setInicioCondicaoAlerta(LocalDateTime.now());
    }
    
    @Override
    public void monitorar(UsinaNuclear contexto) {
        CondicoesOperacionais cond = contexto.getCondicoes();
        long segundosEmAlerta = cond.getSegundosEmAlerta();
        
        contexto.registrarLog(String.format(
            "[ALERTA_AMARELO] Monitorando (Alerta há %d segundos): %s",
            segundosEmAlerta, cond
        ));
        
        // Verifica se pode voltar para OPERACAO_NORMAL
        if (cond.getTemperatura() <= 300) {
            contexto.registrarLog("[INFO] Temperatura normalizada - Retornando para OPERACAO_NORMAL");
            contexto.transicionarPara(new EstadoOperacaoNormal());
            return;
        }
        
        // REGRA: ALERTA_AMARELO → ALERTA_VERMELHO se temperatura > 400°C por mais de 30 segundos
        if (cond.getTemperatura() > 400 && segundosEmAlerta >= 30) {
            contexto.registrarLog("[ALERTA] Temperatura > 400°C por mais de 30s - Transicionando para ALERTA_VERMELHO");
            contexto.transicionarPara(new EstadoAlertaVermelho());
        }
    }
    
    @Override
    public boolean podeTransicionarPara(EstadoUsina novoEstado, UsinaNuclear contexto) {
        // De ALERTA_AMARELO pode voltar para OPERACAO_NORMAL ou avançar para ALERTA_VERMELHO
        if (novoEstado instanceof EstadoOperacaoNormal) {
            return contexto.getCondicoes().getTemperatura() <= 300;
        }
        if (novoEstado instanceof EstadoAlertaVermelho) {
            CondicoesOperacionais cond = contexto.getCondicoes();
            return cond.getTemperatura() > 400 && cond.getSegundosEmAlerta() >= 30;
        }
        if (novoEstado instanceof EstadoManutencao) {
            return true;
        }
        // Previne saltos diretos para EMERGENCIA ou retorno para DESLIGADA
        contexto.registrarLog("[ERRO] Transição não permitida de ALERTA_AMARELO para " + novoEstado.getNome());
        return false;
    }
    
    @Override
    public List<String> getAcoesPermitidas() {
        List<String> acoes = new ArrayList<>();
        acoes.add("Ativar sistemas de resfriamento");
        acoes.add("Reduzir produção de energia");
        acoes.add("Notificar equipe técnica");
        return acoes;
    }
}

// Estado 4: ALERTA_VERMELHO
class EstadoAlertaVermelho extends EstadoUsinaBase {
    
    @Override
    public String getNome() {
        return "ALERTA_VERMELHO";
    }
    
    @Override
    public String getDescricao() {
        return "Situação crítica. Risco iminente. Protocolos de emergência preparados.";
    }
    
    @Override
    public void entrar(UsinaNuclear contexto) {
        super.entrar(contexto);
        contexto.registrarLog("[CRÍTICO] Usina em ALERTA_VERMELHO - Preparando protocolos de emergência");
    }
    
    @Override
    public void monitorar(UsinaNuclear contexto) {
        CondicoesOperacionais cond = contexto.getCondicoes();
        contexto.registrarLog("[ALERTA_VERMELHO] SITUAÇÃO CRÍTICA: " + cond);
        
        // REGRA: ALERTA_VERMELHO → EMERGENCIA se sistema de resfriamento falhar
        if (!cond.isSistemaResfriamentoOperacional()) {
            contexto.registrarLog("[EMERGÊNCIA] Sistema de resfriamento falhou - Ativando EMERGENCIA");
            contexto.transicionarPara(new EstadoEmergencia());
            return;
        }
        
        // Pode voltar para ALERTA_AMARELO se condições melhorarem
        if (cond.getTemperatura() <= 400) {
            contexto.registrarLog("[INFO] Temperatura reduzida - Retornando para ALERTA_AMARELO");
            contexto.transicionarPara(new EstadoAlertaAmarelo());
        }
    }
    
    @Override
    public boolean podeTransicionarPara(EstadoUsina novoEstado, UsinaNuclear contexto) {
        // De ALERTA_VERMELHO pode voltar para ALERTA_AMARELO ou avançar para EMERGENCIA
        if (novoEstado instanceof EstadoAlertaAmarelo) {
            return contexto.getCondicoes().getTemperatura() <= 400;
        }
        if (novoEstado instanceof EstadoEmergencia) {
            return !contexto.getCondicoes().isSistemaResfriamentoOperacional();
        }
        if (novoEstado instanceof EstadoManutencao) {
            // Manutenção pode ser ativada em emergência
            return true;
        }
        // Previne transições circulares perigosas
        contexto.registrarLog("[ERRO] Transição perigosa bloqueada de ALERTA_VERMELHO para " + novoEstado.getNome());
        return false;
    }
    
    @Override
    public List<String> getAcoesPermitidas() {
        List<String> acoes = new ArrayList<>();
        acoes.add("Ativar resfriamento de emergência");
        acoes.add("Evacuar pessoal não essencial");
        acoes.add("Acionar protocolos de segurança");
        return acoes;
    }
}

// Estado 5: EMERGENCIA
class EstadoEmergencia extends EstadoUsinaBase {
    
    @Override
    public String getNome() {
        return "EMERGENCIA";
    }
    
    @Override
    public String getDescricao() {
        return "Emergência Nuclear. Protocolos de segurança máxima ativados.";
    }
    
    @Override
    public void entrar(UsinaNuclear contexto) {
        super.entrar(contexto);
        contexto.registrarLog("Emergência Nuclear Ativada");
        contexto.registrarLog("Todos os Protocolos de Segurança Ativados");
    }
    
    @Override
    public void monitorar(UsinaNuclear contexto) {
        CondicoesOperacionais cond = contexto.getCondicoes();
        contexto.registrarLog("[EMERGENCIA] MÁXIMA PRIORIDADE: " + cond);
        
        // Em emergência, só sai com intervenção manual (modo manutenção)
        contexto.registrarLog("[EMERGENCIA] Aguardando intervenção manual para estabilização");
    }
    
    @Override
    public boolean podeTransicionarPara(EstadoUsina novoEstado, UsinaNuclear contexto) {
        // De EMERGENCIA só pode ir para MANUTENCAO (intervenção manual)
        // Isto previne ciclos perigosos e garante que a emergência seja tratada adequadamente
        if (novoEstado instanceof EstadoManutencao) {
            return true;
        }
        contexto.registrarLog("[ERRO] Em EMERGENCIA só é permitida transição para MANUTENCAO");
        return false;
    }
    
    @Override
    public List<String> getAcoesPermitidas() {
        List<String> acoes = new ArrayList<>();
        acoes.add("Desligamento de emergência");
        acoes.add("Evacuação total");
        acoes.add("Contenção de radiação");
        acoes.add("Solicitar manutenção de emergência");
        return acoes;
    }
}

// Estado Especial: MANUTENCAO
// Este estado sobreescreve o funcionamento normal e permite intervenção manual
class EstadoManutencao extends EstadoUsinaBase {
    
    private EstadoUsina estadoAnterior;
    
    public EstadoManutencao() {
        this.estadoAnterior = null;
    }
    
    public void setEstadoAnterior(EstadoUsina estado) {
        this.estadoAnterior = estado;
    }
    
    public EstadoUsina getEstadoAnterior() {
        return estadoAnterior;
    }
    
    @Override
    public String getNome() {
        return "MANUTENCAO";
    }
    
    @Override
    public String getDescricao() {
        return "Modo de manutenção ativo. Controle manual dos sistemas.";
    }
    
    @Override
    public void entrar(UsinaNuclear contexto) {
        super.entrar(contexto);
        contexto.registrarLog("[MANUTENCAO] Controles manuais ativados - Monitoramento automático suspenso");
        if (estadoAnterior != null) {
            contexto.registrarLog("[MANUTENCAO] Estado anterior salvo: " + estadoAnterior.getNome());
        }
    }
    
    @Override
    public void monitorar(UsinaNuclear contexto) {
        CondicoesOperacionais cond = contexto.getCondicoes();
        contexto.registrarLog("[MANUTENCAO] Modo manual - Condições: " + cond);
        // Em manutenção, o monitoramento não aciona transições automáticas
    }
    
    @Override
    public boolean podeTransicionarPara(EstadoUsina novoEstado, UsinaNuclear contexto) {
        // De MANUTENCAO pode ir para qualquer estado (controle manual)
        // Mas ainda valida condições de segurança básicas
        if (novoEstado instanceof EstadoManutencao) {
            return false; // Já está em manutenção
        }
        
        // Permite retorno para qualquer estado operacional
        if (novoEstado instanceof EstadoDesligada || 
            novoEstado instanceof EstadoOperacaoNormal ||
            novoEstado instanceof EstadoAlertaAmarelo) {
            return true;
        }
        
        // Estados críticos requerem confirmação especial
        if (novoEstado instanceof EstadoAlertaVermelho || 
            novoEstado instanceof EstadoEmergencia) {
            contexto.registrarLog("[AVISO] Transição para estado crítico requer validação extra");
            return true; // Permitido mas com aviso
        }
        
        return true;
    }
    
    @Override
    public List<String> getAcoesPermitidas() {
        List<String> acoes = new ArrayList<>();
        acoes.add("Inspeção de sistemas");
        acoes.add("Reparos e ajustes");
        acoes.add("Testes de segurança");
        acoes.add("Retornar ao estado operacional");
        return acoes;
    }
}

// Contexto da usina nuclear que gerencia os estados
class UsinaNuclear {
    
    private EstadoUsina estadoAtual;
    private CondicoesOperacionais condicoes;
    private List<String> log;
    private boolean emModoManutencao;
    
    public UsinaNuclear() {
        this.condicoes = new CondicoesOperacionais();
        this.log = new ArrayList<>();
        this.emModoManutencao = false;
        
        // Estado inicial: DESLIGADA
        this.estadoAtual = new EstadoDesligada();
        registrarLog("Usina inicializada em estado: " + estadoAtual.getNome());
    }
    
    // Transiciona para um novo estado com validação
    public boolean transicionarPara(EstadoUsina novoEstado) {
        if (novoEstado == null) {
            registrarLog("[ERRO] Tentativa de transição para estado nulo");
            return false;
        }
        
        // Valida se a transição é permitida
        if (!estadoAtual.podeTransicionarPara(novoEstado, this)) {
            registrarLog("[ERRO] Transição bloqueada: " + estadoAtual.getNome() + " -> " + novoEstado.getNome());
            return false;
        }
        
        // Se está entrando em manutenção, salva o estado anterior
        if (novoEstado instanceof EstadoManutencao) {
            ((EstadoManutencao) novoEstado).setEstadoAnterior(estadoAtual);
            emModoManutencao = true;
        }
        
        // Se está saindo de manutenção
        if (estadoAtual instanceof EstadoManutencao) {
            emModoManutencao = false;
        }
        
        // Realiza a transição
        String nomeEstadoAnterior = estadoAtual.getNome();
        estadoAtual.sair(this);
        estadoAtual = novoEstado;
        estadoAtual.entrar(this);
        
        registrarLog(">>> Transição realizada: " + nomeEstadoAnterior + " -> " + estadoAtual.getNome());
        return true;
    }
    
    // Ativa modo de manutenção (sobreescreve estado atual)
    public void ativarManutencao() {
        registrarLog("[AÇÃO] Solicitando modo de manutenção");
        transicionarPara(new EstadoManutencao());
    }
    
    // Sai do modo de manutenção e retorna ao estado anterior ou seguro
    public void desativarManutencao() {
        if (!(estadoAtual instanceof EstadoManutencao)) {
            registrarLog("[AVISO] Usina não está em modo de manutenção");
            return;
        }
        
        // Por segurança, sempre retorna para OPERACAO_NORMAL após manutenção
        registrarLog("[AÇÃO] Finalizando manutenção - Retornando para OPERACAO_NORMAL");
        transicionarPara(new EstadoOperacaoNormal());
    }
    
    // Executa monitoramento do estado atual
    public void monitorar() {
        estadoAtual.monitorar(this);
    }
    
    // Métodos de acesso
    public EstadoUsina getEstadoAtual() { return estadoAtual; }
    public CondicoesOperacionais getCondicoes() { return condicoes; }
    public boolean isEmModoManutencao() { return emModoManutencao; }
    
    public void registrarLog(String mensagem) {
        String timestamp = LocalDateTime.now().toString().substring(11, 19);
        String logEntry = "[" + timestamp + "] " + mensagem;
        log.add(logEntry);
        System.out.println(logEntry);
    }
    
    public List<String> getLog() {
        return new ArrayList<>(log);
    }
    
    public void exibirStatus() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Status da Usina Nuclear");
        System.out.println("=".repeat(80));
        System.out.println("Estado atual: " + estadoAtual.getNome());
        System.out.println("Descrição: " + estadoAtual.getDescricao());
        System.out.println("Modo manutenção: " + (emModoManutencao ? "ATIVO" : "INATIVO"));
        System.out.println("\nCondições Operacionais:");
        System.out.println("  " + condicoes);
        System.out.println("\nAções permitidas:");
        for (String acao : estadoAtual.getAcoesPermitidas()) {
            System.out.println("  - " + acao);
        }
        System.out.println("=".repeat(80));
    }
}

// Classe principal que demonstra o uso do padrão State
public class Questao3State {
    
    // Demonstra o funcionamento do sistema de controle da usina
    public static void demonstrarStatePattern() {
        String separator = "=".repeat(80);
        
        System.out.println("\n" + separator);
        System.out.println("Sistema de Controle de Usina Nuclear - Padrão State");
        System.out.println(separator);
        
        UsinaNuclear usina = new UsinaNuclear();
        CondicoesOperacionais cond = usina.getCondicoes();
        
        // Cenário 1: Ligar a usina
        System.out.println("\n" + separator);
        System.out.println("Cenário 1: Inicializando usina");
        System.out.println(separator);
        usina.exibirStatus();
        usina.transicionarPara(new EstadoOperacaoNormal());
        aguardar(1);
        
        // Cenário 2: Temperatura subindo para alerta amarelo
        System.out.println("\n" + separator);
        System.out.println("Cenário 2: Temperatura aumentando");
        System.out.println(separator);
        cond.setTemperatura(350);
        usina.monitorar();
        aguardar(1);
        usina.exibirStatus();
        
        // Cenário 3: Temperatura crítica mantida (simulando 30+ segundos)
        System.out.println("\n" + separator);
        System.out.println("Cenário 3: Temperatura crítica por período prolongado");
        System.out.println(separator);
        cond.setTemperatura(420);
        cond.setInicioCondicaoAlerta(LocalDateTime.now().minusSeconds(35)); // Simula 35 segundos
        usina.monitorar();
        aguardar(1);
        usina.exibirStatus();
        
        // Cenário 4: Falha no sistema de resfriamento -> EMERGENCIA
        System.out.println("\n" + separator);
        System.out.println("Cenário 4: Falha no sistema de resfriamento");
        System.out.println(separator);
        cond.setSistemaResfriamentoOperacional(false);
        usina.monitorar();
        aguardar(1);
        usina.exibirStatus();
        
        // Cenário 5: Ativando modo de manutenção de emergência
        System.out.println("\n" + separator);
        System.out.println("Cenário 5: Ativando modo de manutenção de emergência");
        System.out.println(separator);
        usina.ativarManutencao();
        aguardar(1);
        
        // Simulando reparos em manutenção
        System.out.println("\n[MANUTENCAO] Realizando reparos no sistema de resfriamento...");
        aguardar(2);
        cond.setSistemaResfriamentoOperacional(true);
        cond.setTemperatura(280);
        System.out.println("[MANUTENCAO] Sistema de resfriamento restaurado");
        System.out.println("[MANUTENCAO] Temperatura reduzida para níveis seguros");
        usina.exibirStatus();
        
        // Cenário 6: Saindo de manutenção
        System.out.println("\n" + separator);
        System.out.println("Cenário 6: Finalizando manutenção e retornando à operação");
        System.out.println(separator);
        usina.desativarManutencao();
        aguardar(1);
        usina.exibirStatus();
        
        // Cenário 7: Tentativa de transição proibida (ciclo perigoso)
        System.out.println("\n" + separator);
        System.out.println("Cenário 7: Testando prevenção de transições perigosas");
        System.out.println(separator);
        System.out.println("\nTentando transição direta de OPERACAO_NORMAL para EMERGENCIA (proibida):");
        usina.transicionarPara(new EstadoEmergencia());
        
        // Cenário 8: Desligamento controlado
        System.out.println("\n" + separator);
        System.out.println("Cenário 8: Desligamento controlado da usina");
        System.out.println(separator);
        cond.setTemperatura(50);
        cond.setPressao(1.5);
        usina.transicionarPara(new EstadoDesligada());
        aguardar(1);
        usina.exibirStatus();
    }
    
    // Método auxiliar para simular passagem de tempo
    private static void aguardar(int segundos) {
        try {
            Thread.sleep(segundos * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Método main para executar a demonstração
    public static void main(String[] args) {
        demonstrarStatePattern();
    }
}

