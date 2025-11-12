/**
 * QUESTÃO 1 - PADRÃO STRATEGY
 * 
 * Justificativa da escolha do padrão:
 * O padrão Strategy foi escolhido porque:
 * 1. Permite encapsular diferentes algoritmos de análise de risco em classes separadas
 * 2. Possibilita a troca dinâmica de algoritmos em tempo de execução
 * 3. Elimina condicionais complexos (if/else ou switch) para selecionar algoritmos
 * 4. Facilita a adição de novos algoritmos sem modificar código existente (Open/Closed Principle)
 * 5. O cliente não precisa conhecer detalhes de implementação dos algoritmos (Dependency Inversion)
 */

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

// Interface abstrata que define o contrato para todas as estratégias de análise de risco
interface RiskAnalysisStrategy {
    AnalysisResult calculate(FinancialContext financialContext);
    String getName();
}

// Classe para representar o contexto financeiro
class FinancialContext {
    private double portfolioValue;
    private double volatility;
    private double confidenceLevel;
    private int timeHorizon;
    private String assetClass;

    public FinancialContext(double portfolioValue, double volatility, double confidenceLevel, 
                           int timeHorizon, String assetClass) {
        this.portfolioValue = portfolioValue;
        this.volatility = volatility;
        this.confidenceLevel = confidenceLevel;
        this.timeHorizon = timeHorizon;
        this.assetClass = assetClass;
    }

    public double getPortfolioValue() { return portfolioValue; }
    public double getVolatility() { return volatility; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public int getTimeHorizon() { return timeHorizon; }
    public String getAssetClass() { return assetClass; }

    public void setPortfolioValue(double portfolioValue) { this.portfolioValue = portfolioValue; }
    public void setVolatility(double volatility) { this.volatility = volatility; }
    public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public void setTimeHorizon(int timeHorizon) { this.timeHorizon = timeHorizon; }
    public void setAssetClass(String assetClass) { this.assetClass = assetClass; }
}

// Classe para representar os detalhes da análise
class AnalysisDetails {
    private Map<String, String> details;

    public AnalysisDetails() {
        this.details = new HashMap<>();
    }

    public void addDetail(String key, String value) {
        details.put(key, value);
    }

    public Map<String, String> getDetails() {
        return new HashMap<>(details);
    }
}

// Classe para representar cenários de stress
class StressScenario {
    private String description;
    private String potentialLoss;
    private String impact;

    public StressScenario(String description, String potentialLoss, String impact) {
        this.description = description;
        this.potentialLoss = potentialLoss;
        this.impact = impact;
    }

    public String getDescription() { return description; }
    public String getPotentialLoss() { return potentialLoss; }
    public String getImpact() { return impact; }
}

// Classe para representar o resultado da análise
class AnalysisResult {
    private String strategy;
    private String metric;
    private String result;
    private String interpretation;
    private AnalysisDetails details;
    private Map<String, StressScenario> scenarios;
    private String recommendation;
    private String timestamp;

    public AnalysisResult(String strategy, String metric, String result, String interpretation) {
        this.strategy = strategy;
        this.metric = metric;
        this.result = result;
        this.interpretation = interpretation;
        this.details = new AnalysisDetails();
        this.scenarios = new HashMap<>();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public String getStrategy() { return strategy; }
    public String getMetric() { return metric; }
    public String getResult() { return result; }
    public String getInterpretation() { return interpretation; }
    public AnalysisDetails getDetails() { return details; }
    public Map<String, StressScenario> getScenarios() { return scenarios; }
    public String getRecommendation() { return recommendation; }
    public String getTimestamp() { return timestamp; }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public void addDetail(String key, String value) {
        details.addDetail(key, value);
    }

    public void addScenario(String key, StressScenario scenario) {
        scenarios.put(key, scenario);
    }
}

// Implementações específicas de cada algoritmo

// ESTRATÉGIA 1: Value at Risk (VaR)
class ValueAtRiskStrategy implements RiskAnalysisStrategy {
    @Override
    public AnalysisResult calculate(FinancialContext financialContext) {
        double portfolioValue = financialContext.getPortfolioValue();
        double volatility = financialContext.getVolatility();
        double confidenceLevel = financialContext.getConfidenceLevel();
        int timeHorizon = financialContext.getTimeHorizon();
        String assetClass = financialContext.getAssetClass();

        // Cálculo dummy
        double varValue = portfolioValue * volatility * Math.sqrt(timeHorizon) * confidenceLevel;

        AnalysisResult result = new AnalysisResult(
            getName(),
            "Value at Risk (VaR)",
            String.format("VaR calculado: $%.2f", varValue),
            String.format("Com %.0f%% de confiança, a perda máxima esperada em %d dias não deve exceder $%.2f",
                confidenceLevel * 100, timeHorizon, varValue)
        );

        result.addDetail("portfolioValue", String.format("$%.0f", portfolioValue));
        result.addDetail("volatility", String.format("%.2f%%", volatility * 100));
        result.addDetail("confidenceLevel", String.format("%.0f%%", confidenceLevel * 100));
        result.addDetail("timeHorizon", String.format("%d dias", timeHorizon));
        result.addDetail("assetClass", assetClass);

        return result;
    }

    @Override
    public String getName() {
        return "Value at Risk (VaR)";
    }
}

// ESTRATÉGIA 2: Expected Shortfall (ES / CVaR)
class ExpectedShortfallStrategy implements RiskAnalysisStrategy {
    @Override
    public AnalysisResult calculate(FinancialContext financialContext) {
        double portfolioValue = financialContext.getPortfolioValue();
        double volatility = financialContext.getVolatility();
        double confidenceLevel = financialContext.getConfidenceLevel();
        int timeHorizon = financialContext.getTimeHorizon();
        String assetClass = financialContext.getAssetClass();

        // Cálculo dummy
        double esValue = portfolioValue * volatility * Math.sqrt(timeHorizon) * confidenceLevel * 1.3;

        AnalysisResult result = new AnalysisResult(
            getName(),
            "Expected Shortfall (CVaR)",
            String.format("ES calculado: $%.2f", esValue),
            String.format("Nos piores %.0f%% dos cenários, a perda média esperada seria de $%.2f",
                (1 - confidenceLevel) * 100, esValue)
        );

        result.addDetail("portfolioValue", String.format("$%.0f", portfolioValue));
        result.addDetail("volatility", String.format("%.2f%%", volatility * 100));
        result.addDetail("confidenceLevel", String.format("%.0f%%", confidenceLevel * 100));
        result.addDetail("timeHorizon", String.format("%d dias", timeHorizon));
        result.addDetail("assetClass", assetClass);
        result.addDetail("comparison", "Expected Shortfall é tipicamente 30% maior que VaR para melhor capturar tail risk");

        return result;
    }

    @Override
    public String getName() {
        return "Expected Shortfall (CVaR)";
    }
}

// ESTRATÉGIA 3: Stress Testing
class StressTestingStrategy implements RiskAnalysisStrategy {
    @Override
    public AnalysisResult calculate(FinancialContext financialContext) {
        double portfolioValue = financialContext.getPortfolioValue();
        double volatility = financialContext.getVolatility();
        int timeHorizon = financialContext.getTimeHorizon();
        String assetClass = financialContext.getAssetClass();

        // Cálculo dummy
        double marketCrashScenario = portfolioValue * (volatility * 3) * Math.sqrt(timeHorizon);
        double extremeVolatilityScenario = portfolioValue * (volatility * 5);
        double liquidityCrisisScenario = portfolioValue * 0.25; // 25% de perda

        AnalysisResult result = new AnalysisResult(
            getName(),
            "Stress Testing",
            "Análise de cenários extremos concluída",
            "Múltiplos cenários de stress foram avaliados para identificar vulnerabilidades"
        );

        result.addScenario("marketCrash", new StressScenario(
            "Crash de mercado (3x volatilidade normal)",
            String.format("$%.2f", marketCrashScenario),
            "ALTO"
        ));

        result.addScenario("extremeVolatility", new StressScenario(
            "Volatilidade extrema (5x normal)",
            String.format("$%.2f", extremeVolatilityScenario),
            "CRÍTICO"
        ));

        result.addScenario("liquidityCrisis", new StressScenario(
            "Crise de liquidez (perda de 25% do portfólio)",
            String.format("$%.2f", liquidityCrisisScenario),
            "SEVERO"
        ));

        result.addDetail("portfolioValue", String.format("$%.0f", portfolioValue));
        result.addDetail("baseVolatility", String.format("%.2f%%", volatility * 100));
        result.addDetail("timeHorizon", String.format("%d dias", timeHorizon));
        result.addDetail("assetClass", assetClass);

        result.setRecommendation("Considere diversificação adicional e hedging para cenários extremos");

        return result;
    }

    @Override
    public String getName() {
        return "Stress Testing";
    }
}

// Contexto que utiliza as estratégias de análise de risco

// Esta classe encapsula o contexto financeiro complexo e permite trocar algoritmos dinamicamente sem que o cliente precise conhecer os detalhes.
class RiskAnalysisContext {
    private RiskAnalysisStrategy strategy;
    private FinancialContext financialContext;

    public RiskAnalysisContext(RiskAnalysisStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy não pode ser nula");
        }
        this.strategy = strategy;
        this.financialContext = new FinancialContext(0, 0, 0.95, 1, "Mixed");
    }

    // Define o contexto financeiro com múltiplos parâmetros
    public void setFinancialContext(double portfolioValue, double volatility, 
                                   double confidenceLevel, int timeHorizon, String assetClass) {
        this.financialContext = new FinancialContext(
            portfolioValue, 
            volatility, 
            confidenceLevel, 
            timeHorizon, 
            assetClass
        );
    }

    // Troca a estratégia em tempo de execução
    public void setStrategy(RiskAnalysisStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy não pode ser nula");
        }
        System.out.println("\nEstratégia alterada para: " + strategy.getName());
        this.strategy = strategy;
    }

    // Executa a análise de risco usando a estratégia atual
    public AnalysisResult performAnalysis() {
        System.out.println("\nExecutando análise com: " + strategy.getName());
        return strategy.calculate(financialContext);
    }

    // Retorna o contexto financeiro atual
    public FinancialContext getFinancialContext() {
        return financialContext;
    }

    // Retorna a estratégia atual
    public String getCurrentStrategy() {
        return strategy.getName();
    }
}

// Demonstração de uso
public class Questao1Strategy {
    
    // Função helper para exibir resultados de forma formatada
    private static void displayResult(AnalysisResult result) {
        String separator = "=".repeat(80);
        
        System.out.println("\n" + separator);
        System.out.println("Análise de Risco: " + result.getStrategy());
        System.out.println(separator);
        System.out.println("\nResultado: " + result.getResult());
        System.out.println("\nInterpretação: " + result.getInterpretation());
        
        if (result.getDetails() != null && !result.getDetails().getDetails().isEmpty()) {
            System.out.println("\nDetalhes:");
            result.getDetails().getDetails().forEach((key, value) -> {
                System.out.println("   - " + key + ": " + value);
            });
        }
        
        if (result.getScenarios() != null && !result.getScenarios().isEmpty()) {
            System.out.println("\nCenários de Stress:");
            result.getScenarios().forEach((key, scenario) -> {
                System.out.println("\n   " + scenario.getDescription());
                System.out.println("   Perda Potencial: " + scenario.getPotentialLoss());
                System.out.println("   Impacto: " + scenario.getImpact());
            });
            if (result.getRecommendation() != null) {
                System.out.println("\n   Recomendação: " + result.getRecommendation());
            }
        }
        
        System.out.println("\nTimestamp: " + result.getTimestamp());
        System.out.println(separator);
    }

    // Função principal que demonstra o uso do padrão Strategy
    public static void demonstrateStrategyPattern() {
        System.out.println("\n");
        System.out.println("Sistema de Análise de Risco - Padrão Strategy");
        
        // Criação das estratégias concretas
        RiskAnalysisStrategy varStrategy = new ValueAtRiskStrategy();
        RiskAnalysisStrategy esStrategy = new ExpectedShortfallStrategy();
        RiskAnalysisStrategy stressStrategy = new StressTestingStrategy();
        
        // Criação do contexto com estratégia inicial
        RiskAnalysisContext riskAnalyzer = new RiskAnalysisContext(varStrategy);
        
        // Definição do contexto financeiro complexo
        riskAnalyzer.setFinancialContext(
            1000000,        // $1 milhão
            0.02,          // 2% de volatilidade
            0.95,          // 95% de confiança
            10,            // 10 dias
            "Ações e Bonds"
        );
        
        System.out.println("Valor do Portfólio: $1,000,000");
        System.out.println("Volatilidade: 2%");
        System.out.println("Nível de Confiança: 95%");
        System.out.println("Horizonte de Tempo: 10 dias");
        System.out.println("Classe de Ativo: Ações e Bonds");
        
        // Demonstração 1: Value at Risk
        AnalysisResult result = riskAnalyzer.performAnalysis();
        displayResult(result);
        
        // Demonstração 2: Expected Shortfall
        // Troca de estratégia em tempo de execução
        riskAnalyzer.setStrategy(esStrategy);
        result = riskAnalyzer.performAnalysis();
        displayResult(result);
        
        // Demonstração 3: Stress Testing
        // Nova troca de estratégia
        riskAnalyzer.setStrategy(stressStrategy);
        result = riskAnalyzer.performAnalysis();
        displayResult(result);
        
        // Demonstração 4: Mudança de contexto e nova análise        
        riskAnalyzer.setFinancialContext(
            5000000,        // $5 milhões
            0.035,         // 3.5% de volatilidade (mais arriscado)
            0.99,          // 99% de confiança (mais conservador)
            21,            // 21 dias (1 mês)
            "Derivativos"
        );
        
        System.out.println("Novo contexto:");
        System.out.println("Valor do Portfólio: $5,000,000");
        System.out.println("Volatilidade: 3.5%");
        System.out.println("Nível de Confiança: 99%");
        System.out.println("Horizonte de Tempo: 21 dias");
        System.out.println("Classe de Ativo: Derivativos");
        
        // Volta para VaR com novo contexto
        riskAnalyzer.setStrategy(varStrategy);
        result = riskAnalyzer.performAnalysis();
        displayResult(result);
        
        System.out.println("\nDemonstração concluída");
    }

    // Método main para executar demonstração
    public static void main(String[] args) {
        demonstrateStrategyPattern();
    }
}

