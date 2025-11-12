# Avaliação de Padrões de Projeto - 2º Bimestre

Implementação de quatro questões utilizando padrões de projeto clássicos do GoF (Gang of Four) em Java, com foco em princípios SOLID e boas práticas de design.

## Questões Implementadas

### Questão 1 - Padrão Strategy

**Problema**: Sistema de análise de risco financeiro com múltiplos algoritmos intercambiáveis.

**Padrão Escolhido**: Strategy

**Justificativa**:
- Encapsula diferentes algoritmos de análise (VaR, Expected Shortfall, Stress Testing)
- Permite troca dinâmica de algoritmos em tempo de execução
- Elimina condicionais complexos para seleção de algoritmos
- Facilita adição de novos algoritmos sem modificar código existente

**Componentes Principais**:
- `RiskAnalysisStrategy`: Interface para estratégias
- `ValueAtRiskStrategy`, `ExpectedShortfallStrategy`, `StressTestingStrategy`: Implementações concretas
- `RiskAnalysisContext`: Contexto que utiliza as estratégias
- `FinancialContext`: Dados de entrada (portfólio, volatilidade, etc.)

---

### Questão 2 - Padrão Adapter

**Problema**: Integração de sistema moderno com legado bancário incompatível.

**Padrão Escolhido**: Adapter

**Justificativa**:
- Permite integração sem modificar sistemas existentes (Open/Closed Principle)
- Resolve incompatibilidade de assinaturas e tipos de dados
- Encapsula complexidade de conversão
- Possibilita comunicação bidirecional

**Componentes Principais**:
- `ProcessadorTransacoes`: Interface moderna (String, double, String)
- `SistemaBancarioLegado`: Sistema legado (HashMap<String, Object>)
- `AdaptadorTransacoesBancarias`: Adaptador que converte entre os dois formatos
- Conversões: String ↔ Código numérico de moedas (USD=1, EUR=2, BRL=3)
- Campos obrigatórios legados: `codigoFilial`, `timestampLegado`

---

### Questão 3 - Padrão State

**Problema**: Sistema de controle de usina nuclear com estados operacionais complexos.

**Padrão Escolhido**: State

**Justificativa**:
- Encapsula comportamentos específicos de cada estado
- Facilita adição de novos estados sem modificar existentes
- Elimina condicionais complexos de transição
- Permite validações complexas por estado

**Componentes Principais**:
- `EstadoUsina`: Interface para estados
- Estados: `DESLIGADA`, `OPERACAO_NORMAL`, `ALERTA_AMARELO`, `ALERTA_VERMELHO`, `EMERGENCIA`, `MANUTENCAO`
- `UsinaNuclear`: Contexto que gerencia transições
- `CondicoesOperacionais`: Temperatura, pressão, radiação, sistema de resfriamento

**Regras Implementadas**:
- OPERACAO_NORMAL → ALERTA_AMARELO: temperatura > 300°C
- ALERTA_AMARELO → ALERTA_VERMELHO: temperatura > 400°C por mais de 30 segundos
- ALERTA_VERMELHO → EMERGENCIA: falha no sistema de resfriamento
- EMERGENCIA só acessível via ALERTA_VERMELHO
- Modo MANUTENCAO sobreescreve estados normais

---

### Questão 4 - Padrão Chain of Responsibility

**Problema**: Sistema de validação de documentos fiscais eletrônicos (NF-e) com múltiplas regras em cadeia.

**Padrão Escolhido**: Chain of Responsibility

**Justificativa**:
- Desacopla remetentes de destinatários de requisições
- Permite múltiplos validadores processarem em sequência
- Facilita adicionar/remover validadores
- Cada validador decide se processa e/ou passa adiante

**Componentes Principais**:
- `ValidadorDocumento`: Interface para validadores (Handler)
- `ValidadorBase`: Implementação base com timeout e circuit breaker
- Validadores:
  1. `ValidadorSchemaXML` (2s timeout) - Valida estrutura XML
  2. `ValidadorCertificadoDigital` (3s timeout) - Verifica expiração/revogação
  3. `ValidadorRegrasFiscais` (5s timeout) - Valida cálculo de impostos
  4. `ValidadorBancoDados` (4s timeout) - Verifica duplicidade (suporta rollback)
  5. `ValidadorServicoSEFAZ` (10s timeout) - Consulta online

**Funcionalidades Especiais**:
- **Circuit Breaker**: Interrompe após 3 falhas consecutivas
- **Validações Condicionais**: Validadores 3 e 5 só executam se anteriores passarem
- **Rollback**: Validador 4 desfaz inserção se validações subsequentes falharem
- **Timeout Individual**: Cada validador executa com timeout específico usando ExecutorService

