# Parking — Stapar Senior Java Developer Assessment

Sistema de gerenciamento de estacionamento com precificação dinâmica por ocupação, processamento idempotente de webhooks e integração resiliente com simulador externo.

---

## Stack

| Camada | Tecnologia |
|--------|------------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5 |
| Persistência | Spring Data JPA + Hibernate + MySQL 8.4 |
| Migrações | Flyway |
| Cache | Caffeine (TTL 30s, max 10 000 entradas) |
| Resiliência | Resilience4j (circuit breaker, retry, time limiter) |
| Métricas | Spring Actuator + Micrometer Prometheus |
| Build | Maven |
| Infraestrutura | Docker Compose |

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose
- Simulador de garagem rodando em `http://localhost:3000`

---

## Subindo o ambiente

```bash
# 1. Sobe o MySQL
docker compose up -d

# 2. Compila e inicia a aplicação
./mvnw spring-boot:run
```

A aplicação sobe na porta **3003**. As migrações Flyway são executadas automaticamente. Na inicialização, o `GarageBootstrapService` consulta o simulador (`GET /garage`) e sincroniza setores e vagas no banco.

---

## API

### POST /webhook

Recebe eventos do simulador de garagem.

```json
{
  "license_plate": "ABC-1234",
  "entry_time":    "2024-01-15T10:00:00Z",
  "exit_time":     null,
  "lat":           -23.5505,
  "lng":           -46.6333,
  "event_type":    "ENTRY"
}
```

Tipos de evento: `ENTRY` · `PARKED` · `EXIT`

O endpoint é idempotente — eventos duplicados são descartados via chave SHA-256 (license_plate + event_type + timestamps + coordenadas).

**Resposta:** `200 OK`

---

### GET /revenue

Retorna a receita de um setor em uma data específica.

**Body:**

```json
{
  "date":   "2024-01-15",
  "sector": "A"
}
```

**Resposta:**

```json
{
  "amount":    150.00,
  "currency":  "BRL",
  "timestamp": "2024-01-15T21:00:00Z"
}
```

Resultado cacheado por 30 segundos. Cache é invalidado a cada evento processado.

---

## Domínio

```
GarageSector  1 ──< ParkingSpot
                         │
                         └──< ParkingTicket
WebhookEvent ──triggers──> lifecycle do ParkingTicket
```

### Ciclo de vida de um ticket

| Evento | Ação |
|--------|------|
| `ENTRY` | Localiza vaga disponível, ocupa, cria ticket `OPEN` com multiplicador dinâmico |
| `PARKED` | Atualiza ticket com horário e ID da vaga (busca por coordenadas) → `PARKED` |
| `EXIT` | Calcula cobrança, fecha ticket, libera vaga → `CLOSED` |

---

## Precificação dinâmica

Implementada via Strategy Pattern (`OccupancyBasedPricingStrategy`):

| Taxa de ocupação | Multiplicador |
|------------------|---------------|
| < 25% | 0,90× (desconto 10%) |
| 25 – 50% | 1,00× (preço base) |
| 50 – 75% | 1,10× (acréscimo 10%) |
| 75 – 100% | 1,25× (acréscimo 25%) |

**Regras de cálculo:**

- Primeiros 30 minutos: gratuito
- Frações de hora são arredondadas para cima
- `valor = horas × preço_base × multiplicador`

---

## Concorrência

- **Optimistic locking** (`@Version`) em `GarageSector` e `ParkingSpot`
- **Pessimistic locking** no setor durante entry/exit para evitar race conditions em `occupiedCount`
- **Processamento assíncrono** de webhooks via `@Async`

---

## Resiliência (Resilience4j) — chamada ao simulador

| Padrão | Configuração |
|--------|--------------|
| Retry | 3 tentativas, backoff exponencial (500ms × 2) |
| Circuit Breaker | Janela 10 chamadas, threshold 50%, espera 10s |
| Time Limiter | Timeout 3s |

Caso o simulador esteja indisponível na inicialização, o sistema sobe em modo degradado via fallback.

---

## Testes

```bash
./mvnw test
```

Cobertura de testes unitários e de integração para:

- `PricingServiceTest` — regras de cobrança (gratuidade, arredondamento, multiplicador)
- `WebhookServiceTest` — idempotência, duplicatas, processamento assíncrono
- `RevenueServiceTest` — agregação de receita com fronteiras UTC
- `ParkingEventProcessorTest` — processamento de eventos ENTRY / PARKED / EXIT
- `GarageBootstrapServiceTest` — sincronização de garagem na inicialização
- `WebhookControllerTest` / `RevenueControllerTest` — camada HTTP

---

## Estrutura do projeto

```
src/main/java/com/marcel/parking/
├── controller/        # WebhookController, RevenueController
├── service/           # WebhookService, ParkingEventProcessor, RevenueService,
│                      # PricingService, GarageBootstrapService
├── strategy/          # DynamicPricingStrategy, OccupancyBasedPricingStrategy
├── entity/            # GarageSector, ParkingSpot, ParkingTicket, WebhookEvent
├── repository/        # Spring Data JPA repositories
├── dto/               # Request/Response DTOs
├── client/            # GarageSimulatorClient
├── config/            # ObjectMapper (snake_case), AsyncConfig, ResilienceConfig
├── enumtype/          # WebhookEventType, ParkingSpotStatus, ParkingTicketStatus
└── exception/         # BusinessException, ResourceNotFoundException,
                       # SectorFullException, GlobalExceptionHandler

src/main/resources/
├── application.properties
└── db/migration/      # V1__initial.sql, V2 (version GarageSector),
                       # V3 (version ParkingSpot)
```

---

## Monitoramento

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Health check |
| `GET /actuator/metrics` | Métricas da JVM e aplicação |
| `GET /actuator/prometheus` | Scrape Prometheus |
