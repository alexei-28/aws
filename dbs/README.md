# Spring Boot + AWS RDS, AWS DynamoDB, AWS DocumentDB

# AWS Document Processing & Notification Hub

Главная цель проекта — практически продемонстрировать понимание трёх разных типов AWS-баз данных:

1. **AWS RDS (PostgreSQL)** — реляционные транзакционные данные.
2. **AWS DynamoDB** — key-value / NoSQL для оперативного состояния.
3. **AWS DocumentDB** — документные данные с гибкой JSON-структурой.

Spring Boot приложение разворачивается на **AWS EC2**.

Ключевой архитектурный принцип: **каждая база данных отвечает за свою бизнес-задачу и не дублирует функциональность остальных**.

---

## 1. Концепция проекта

**Document Processing & Notification Hub** — небольшая система обработки документов.

Пользователь:

1. Загружает документ.
2. Система регистрирует документ.
3. Создаётся задача на обработку.
4. Система отслеживает состояние обработки.
5. После обработки сохраняется результат анализа.
6. Пользователь получает возможность просмотреть результат.

Обработку документа можно имитировать без настоящего OCR или AI.

---

## 2. Распределение ответственности между базами данных

### 2.1 AWS RDS — PostgreSQL

**Назначение:** бизнес-данные и источник достоверной информации (Source of Truth).

RDS хранит:

- пользователей;
- документы;
- права доступа;
- историю обработки;
- настройки уведомлений.

Пример таблиц:

```text
users
documents
document_permissions
processing_history
notification_preferences
```

### Почему RDS?

- ACID-транзакции;
- Foreign Keys;
- Referential Integrity;
- JOIN;
- Unique Constraints;
- надёжное хранение бизнес-данных.

Пример:

Нельзя создать документ для пользователя, которого не существует.

---

### 2.2 AWS DynamoDB

**Назначение:** оперативное состояние обработки документов.

DynamoDB хранит только текущее состояние выполняющейся задачи.

Пример:

```text
PK = DOCUMENT#123
SK = PROCESSING
```

Атрибуты:

```text
status
progress
currentStep
attempts
startedAt
updatedAt
expiresAt
```

Пример:

```json
{
  "PK": "DOCUMENT#123",
  "SK": "PROCESSING",
  "status": "PROCESSING",
  "progress": 65,
  "currentStep": "EXTRACTING",
  "attempts": 2,
  "startedAt": "2026-11-03T14:00:00Z",
  "updatedAt": "2026-11-03T14:03:00Z",
  "expiresAt": 1793800980
}
```

### Почему DynamoDB?

- быстрый доступ по ключу;
- высокая производительность;
- горизонтальное масштабирование;
- Conditional Writes;
- TTL;
- отсутствие необходимости в JOIN.

DynamoDB **не хранит бизнес-историю обработки**. История остаётся в RDS.

---

### 2.3 AWS DocumentDB

**Назначение:** хранение гибких результатов анализа документов.

Результаты анализа разных типов документов могут иметь разные структуры.

Например, Invoice:

```json
{
  "_id": "123",
  "documentId": "DOC-123",
  "type": "INVOICE",
  "analysis": {
    "invoiceNumber": "INV-2026-001",
    "total": 1250.50,
    "currency": "EUR",
    "supplier": {
      "name": "Company A",
      "vatNumber": "MD123456"
    }
  },
  "metadata": {
    "confidence": 0.98,
    "processorVersion": "1.0"
  }
}
```

Contract может иметь совершенно другую структуру:

```json
{
  "_id": "456",
  "documentId": "DOC-456",
  "type": "CONTRACT",
  "analysis": {
    "parties": [
      "Company A",
      "Company B"
    ],
    "effectiveDate": "2026-11-01",
    "clauses": [
      {
        "name": "Termination",
        "risk": "HIGH"
      }
    ]
  },
  "metadata": {
    "confidence": 0.95
  }
}
```

### Почему DocumentDB?

- гибкая JSON-подобная структура;
- вложенные объекты;
- массивы;
- разные схемы документов;
- возможность развивать структуру без постоянных SQL migrations.

DocumentDB **не дублирует RDS**:

- RDS → бизнес-метаданные документа;
- DocumentDB → результат анализа документа.

> DocumentDB совместима с MongoDB API, но не является полной копией MongoDB. Поддерживаемые возможности необходимо проверять для конкретной версии DocumentDB.

---

# 3. Общая архитектура

```text
                         ┌─────────────────┐
                         │     Client      │
                         └────────┬────────┘
                                  │ HTTPS
                                  ▼
                         ┌─────────────────┐
                         │    AWS EC2      │
                         │                 │
                         │  Spring Boot    │
                         │   Application   │
                         └───────┬─────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
              ▼                  ▼                  ▼
      ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
      │ RDS          │   │ DynamoDB     │   │ DocumentDB   │
      │ PostgreSQL   │   │              │   │              │
      │              │   │ Job State    │   │ JSON Results │
      │ Business     │   │              │   │              │
      │ Data         │   │              │   │              │
      └──────────────┘   └──────────────┘   └──────────────┘

              ┌──────────────────┴──────────────────┐
              │                                     │
              ▼                                     ▼
      ┌──────────────┐                     ┌──────────────┐
      │     S3       │                     │     SQS      │
      │              │                     │              │
      │ Original     │                     │ Processing   │
      │ Files        │                     │ Queue        │
      └──────────────┘                     └──────────────┘
```

---

# 4. AWS-компоненты

| AWS Service | Ответственность |
|---|---|
| EC2 | Размещение Spring Boot |
| RDS PostgreSQL | Транзакционные бизнес-данные |
| DynamoDB | Оперативное состояние задач |
| DocumentDB | Гибкие результаты анализа |
| S3 | Хранение исходных файлов |
| SQS | Асинхронная обработка |
| IAM | Контроль доступа |
| Secrets Manager | Хранение секретов |
| CloudWatch | Логи и мониторинг |

S3 и SQS являются дополнительными сервисами и не заменяют три основные базы данных.

---

# 5. Бизнес-сценарий

## 5.1 Регистрация документа

Клиент вызывает:

```http
POST /api/v1/documents
```

Spring Boot создаёт запись в RDS:

```text
documents
├── id
├── user_id
├── filename
├── content_type
├── s3_key
└── created_at
```

---

## 5.2 Загрузка файла

Приложение создаёт pre-signed URL.

Клиент загружает файл непосредственно в S3.

```text
Client
  │
  │ PUT
  ▼
S3
```

Это позволяет не передавать большие файлы через EC2.

---

## 5.3 Создание processing job

Создаётся запись в DynamoDB:

```text
status = PENDING
progress = 0
```

После этого публикуется сообщение в SQS:

```json
{
  "documentId": "DOC-123",
  "eventType": "DOCUMENT_UPLOADED"
}
```

---

## 5.4 Обработка документа

Worker получает сообщение из SQS.

Он:

1. получает документ из S3;
2. обновляет DynamoDB;
3. выполняет обработку;
4. сохраняет результат в DocumentDB;
5. обновляет статус в DynamoDB;
6. записывает историю в RDS.

Например:

```text
PENDING
   ↓
PROCESSING
   ↓
EXTRACTING
   ↓
ANALYZING
   ↓
COMPLETED
```

---

# 6. Роль каждой базы данных

Очень важно уметь объяснить это на собеседовании.

| Вопрос | База |
|---|---|
| Какие бизнес-данные существуют? | RDS |
| Какой пользователь владеет документом? | RDS |
| Какие права доступа у пользователя? | RDS |
| Какой сейчас статус обработки? | DynamoDB |
| Какой progress текущей задачи? | DynamoDB |
| Сколько попыток обработки было? | DynamoDB |
| Каков результат анализа документа? | DocumentDB |
| Какие поля были извлечены из Invoice? | DocumentDB |
| Какие clauses найдены в Contract? | DocumentDB |

Главный принцип:

```text
RDS       → Business Truth
DynamoDB  → Operational State
DocumentDB → Flexible Document Result
```

---

# 7. REST API

Для первой версии достаточно следующих endpoints:

| Method | Endpoint | Storage | Назначение |
|---|---|---|---|
| POST | `/api/v1/documents` | RDS | Регистрация документа |
| GET | `/api/v1/documents` | RDS | Список документов |
| GET | `/api/v1/documents/{id}` | RDS | Информация о документе |
| GET | `/api/v1/documents/{id}/status` | DynamoDB | Текущий статус |
| GET | `/api/v1/documents/{id}/analysis` | DocumentDB | Результат анализа |
| POST | `/api/v1/documents/{id}/retry` | DynamoDB + SQS | Повторная обработка |

Разделение API подчёркивает ответственность каждого хранилища.

---

# 8. Spring Boot Architecture

Рекомендуется использовать **Modular Monolith**.

Микросервисы для этого учебного проекта не нужны.

```text
document-hub/
│
├── src/main/java/com/example/documenthub/
│
├── document/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── entity/
│
├── processing/
│   ├── service/
│   ├── worker/
│   ├── model/
│   └── repository/
│
├── analysis/
│   ├── service/
│   ├── model/
│   └── repository/
│
├── storage/
│   └── S3StorageService.java
│
├── messaging/
│   ├── SqsPublisher.java
│   └── SqsConsumer.java
│
└── config/
    ├── RdsConfig.java
    ├── DynamoDbConfig.java
    └── DocumentDbConfig.java
```

---

# 9. Технологический стек

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| Build | Gradle Kotlin DSL |
| RDS | PostgreSQL + Spring Data JPA |
| DynamoDB | AWS SDK for Java 2.x |
| DocumentDB | Spring Data MongoDB |
| Migration | Flyway |
| AWS | AWS SDK v2 |
| API | REST |
| Documentation | SpringDoc OpenAPI |
| Testing | JUnit 5 + Mockito |
| Integration tests | Testcontainers |
| Monitoring | Spring Actuator + CloudWatch |

Для DynamoDB рекомендуется использовать AWS SDK напрямую, чтобы продемонстрировать понимание DynamoDB API и модели доступа.

---

# 10. Production deployment

Рекомендуемая архитектура:

```text
Internet
   │
   ▼
HTTPS
   │
   ▼
┌──────────────────────────────┐
│            EC2               │
│                              │
│     Spring Boot Application  │
└──────────────┬───────────────┘
               │
       ┌───────┼────────┐
       │       │        │
       ▼       ▼        ▼
      RDS  DocumentDB DynamoDB
       │       │        │
       │       │        │
       └───────┴────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
      S3               SQS
```

## Network

Рекомендуется:

- EC2 в public subnet;
- RDS в private subnet;
- DocumentDB в private subnet;
- DynamoDB через VPC Gateway Endpoint;
- S3 через VPC Endpoint;
- SQS через IAM.

---

# 11. Security

EC2 не должен использовать AWS Access Key / Secret Key в конфигурации приложения.

Использовать:

```text
EC2
 │
 ▼
IAM Instance Profile
 │
 ├── S3 permissions
 ├── DynamoDB permissions
 ├── SQS permissions
 └── Secrets Manager permissions
```

Secrets Manager:

```text
RDS credentials
DocumentDB credentials
Application secrets
```

Security Groups:

```text
Internet
   │
   ▼
EC2 : 443
   │
   ├──► RDS : 5432
   │
   └──► DocumentDB : 27017
```

RDS и DocumentDB не должны быть доступны непосредственно из Internet.

---

# 12. Transactional Outbox

Для Senior-level проекта рекомендуется добавить Transactional Outbox.

Проблема:

```text
DB transaction
     +
SQS publish
```

Если RDS commit успешен, но SQS publish завершился ошибкой, данные становятся несогласованными.

Решение:

```text
┌─────────────────────────────┐
│ PostgreSQL transaction      │
│                             │
│ documents                   │
│ outbox_events               │
└──────────────┬──────────────┘
               │ commit
               ▼
        Outbox Publisher
               │
               ▼
              SQS
```

`documents` и `outbox_events` записываются в одной ACID-транзакции.

---

# 13. Idempotency

SQS может доставить одно сообщение больше одного раза.

Поэтому обработка должна быть идемпотентной.

Например:

```text
documentId = DOC-123
processingAttempt = 5
```

Worker перед обработкой проверяет состояние.

Если задача уже завершена:

```text
COMPLETED
```

повторное сообщение не должно повторно выполнять бизнес-операцию.

Для защиты можно использовать Conditional Writes в DynamoDB.

---

# 14. Retry и Dead Letter Queue

Для SQS рекомендуется:

```text
SQS
 │
 ├── retry
 ├── retry
 ├── retry
 │
 ▼
DLQ
```

Если документ невозможно обработать после нескольких попыток, сообщение отправляется в Dead Letter Queue.

Это позволяет продемонстрировать:

- retry;
- exponential backoff;
- DLQ;
- error handling;
- observability.

---

# 15. Сценарии отказа

Для Senior interview особенно полезно реализовать следующие сценарии.

### Scenario 1 — Duplicate SQS message

SQS повторно доставляет сообщение.

Ожидаемый результат:

```text
Document processed only once
```

---

### Scenario 2 — DocumentDB unavailable

DocumentDB временно недоступна.

Ожидаемый результат:

```text
Processing does not silently disappear
        ↓
Retry
        ↓
Success
```

или:

```text
Retry exhausted
        ↓
DLQ
```

---

### Scenario 3 — EC2 restart

EC2 перезапускается во время обработки.

После восстановления приложение должно обнаружить незавершённые задачи и корректно продолжить обработку.

---

# 16. Testing

Минимальный набор:

### Unit tests

- DocumentService;
- ProcessingService;
- AnalysisService;
- retry logic;
- idempotency logic.

### Integration tests

RDS:

```text
PostgreSQL Testcontainer
```

DynamoDB:

```text
DynamoDB Local
```

DocumentDB:

```text
MongoDB Testcontainer
```

При этом необходимо помнить, что MongoDB container не гарантирует полное поведение AWS DocumentDB.

---

# 17. Observability

Spring Boot:

```text
Spring Actuator
```

Метрики:

```text
documents.created
documents.processed
documents.failed
processing.duration
processing.retries
```

CloudWatch:

```text
Application logs
Error logs
Processing metrics
EC2 metrics
```

Полезно использовать correlation ID:

```text
X-Correlation-Id
```

и передавать его через весь processing flow.

---

# 18. Этапы реализации

## Stage 1 — RDS

Реализовать:

- Spring Boot;
- PostgreSQL;
- JPA;
- Flyway;
- User;
- Document;
- REST API.

Результат:

```text
Spring Boot → RDS
```

---

## Stage 2 — DynamoDB

Добавить:

- ProcessingState;
- status;
- progress;
- attempts;
- TTL;
- Conditional Writes.

Результат:

```text
Spring Boot → RDS
             → DynamoDB
```

---

## Stage 3 — DocumentDB

Добавить:

- AnalysisResult;
- Invoice analysis;
- Contract analysis;
- nested JSON;
- queries.

Результат:

```text
Spring Boot → RDS
             → DynamoDB
             → DocumentDB
```

---

## Stage 4 — S3 + SQS

Добавить:

- S3;
- pre-signed URLs;
- SQS;
- Worker;
- asynchronous processing.

---

## Stage 5 — AWS EC2

Развернуть:

```text
Spring Boot
     ↓
AWS EC2
     ↓
RDS
DynamoDB
DocumentDB
S3
SQS
```

---

## Stage 6 — Production readiness

Добавить:

- IAM;
- Secrets Manager;
- Security Groups;
- CloudWatch;
- retry;
- DLQ;
- idempotency;
- Transactional Outbox;
- Actuator;
- integration tests.

---

# 19. Что проект демонстрирует на Senior Interview

Проект позволяет обсуждать:

### Databases

- SQL vs NoSQL;
- relational vs key-value vs document database;
- ACID;
- eventual consistency;
- indexing;
- partition key;
- access patterns;
- schema flexibility.

### AWS

- EC2;
- RDS;
- DynamoDB;
- DocumentDB;
- S3;
- SQS;
- IAM;
- VPC;
- Security Groups;
- Secrets Manager;
- CloudWatch.

### Spring Boot

- Spring Data JPA;
- Spring Data MongoDB;
- AWS SDK;
- REST;
- transactions;
- asynchronous processing;
- background workers.

### Distributed Systems

- idempotency;
- retry;
- DLQ;
- eventual consistency;
- Transactional Outbox;
- failure recovery.

---

# 20. Главное архитектурное решение

В проекте **не нужно искусственно дублировать данные**.

Правильное разделение:

```text
                 DOCUMENT HUB
                      │
       ┌──────────────┼──────────────┐
       │              │              │
       ▼              ▼              ▼
      RDS         DynamoDB       DocumentDB
       │              │              │
       │              │              │
       ▼              ▼              ▼
 Business Data   Operational     Flexible
   / Source        State          Analysis
   of Truth
```

### RDS

Отвечает:

> Какие бизнес-данные существуют?

### DynamoDB

Отвечает:

> В каком состоянии сейчас находится processing job?

### DocumentDB

Отвечает:

> Каков гибкий результат анализа документа?

Именно такое разделение ответственности является главным архитектурным преимуществом проекта.

---

# 21. Итоговый AWS Stack

```text
Java 21
Spring Boot
Gradle
     │
     ▼
AWS EC2
     │
     ├──────────────► RDS PostgreSQL
     │                  Business Data
     │
     ├──────────────► DynamoDB
     │                  Processing State
     │
     ├──────────────► DocumentDB
     │                  Analysis Results
     │
     ├──────────────► S3
     │                  Original Documents
     │
     └──────────────► SQS
                        Processing Queue

Supporting Services:

IAM
VPC
Security Groups
Secrets Manager
CloudWatch
```

## Итог

Это небольшой, но достаточно содержательный проект для подготовки к **Senior Java Developer interview**.

Главное не количество AWS-сервисов, а возможность аргументированно объяснить:

> **Почему именно RDS используется для этой задачи, DynamoDB — для другой, а DocumentDB — для третьей?**

При этом каждая база имеет собственную ответственность:

```text
RDS        → transactional business data
DynamoDB   → operational state
DocumentDB → flexible document results
```

А **EC2** является runtime-платформой для Spring Boot приложения.


### Как проверить, что локально работает
Чтобы проверить, что твой dbs локально работает, рекомендую пройти несколько проверок: 

PostgreSQL → Spring Boot → REST API → Docker.

Важно: сейчас ProcessingServiceImpl и S3StorageServiceImpl — заглушки.
Поэтому приложение может запуститься, но функциональность DynamoDB, SQS и S3 ещё не работает.

### Download docker images (if needed) and create container
```bash
docker compose -f dbs-docker-compose.yml down -v
docker compose -f dbs-docker-compose.yml up -d
```

### Проверить PostgreSQL в Docker
```bash
docker compose -f dbs-docker-compose.yml ps
```
### Проверить подключение
```bash
docker exec -it aws_dbs_postgres psql -U postgres -d aws_dbs_document_hub
```
Внутри PostgreSQL:

```sql
SELECT version();

SELECT current_database();

\dt
```
### Проверить Spring Boot
   Запусти приложение:
```bash
   ./gradlew bootRun
```

Или через IntelliJ IDEA.

Ожидаемый результат в логах:
```log
Started DbsMain
```

Проверь, что приложение действительно подключилось к PostgreSQL. В логах не должно быть ошибок подключения или миграций Flyway.
### Проверить Actuator
   Если ещё не добавил Actuator, добавь зависимость:

   implementation("org.springframework.boot:spring-boot-starter-actuator")


В application.yml:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

Проверь:
curl http://localhost:8084/api/v1/actuator/health


Ожидаемый ответ:
{
"status": "UP"
}

## Проверить REST API
Предположим, что у тебя пока нет настроенной Spring Security и JWT-аутентификации. Тогда можно временно тестировать endpoints без авторизации.

Если Security уже настроен, используй действительный JWT.

Проверка регистрации документа

### Проверка создание документа - POST /documents
```bash
curl -i -X POST http://localhost:8084/api/v1/documents \
  -H "Content-Type: application/json" \
  -d '{
    "filename": "invoice-001.pdf",
    "contentType": "application/pdf"
  }'
  ```

Ответ:
```json
{
  "id": "4d0afc55-d987-444e-8379-ed062e0f0306",
  "filename": "invoice-001.pdf",
  "uploadUrl": "http://localhost:9000/mock-upload/documents/550e8400-e29b-41d4-a716-446655440000/4d0afc55-d987-444e-8379-ed062e0f0306",
  "createdAt": "2026-11-03T18:45:25.499931201Z"
}
```

- id — UUID зарегистрированного документа.
- filename — имя файла.
- uploadUrl — временный mock URL вместо настоящего S3 pre-signed URL.
- createdAt — время создания документа в UTC.

Вызов происходит по цепочке:
   
      DocumentController.create()
         |
         v
      DocumentService.create()
         |
         v
      S3StorageServiceImpl.generateUploadUrl()
         |
         v
      UnsupportedOperationException
         |
         v
      HTTP 500


#### На сервере EC2:
При запуске JAR-файла активируйте профиль ec2:

### Проверить PostgreSQL
```bash
docker exec -it document-hub-postgres \
  psql -U postgres -d document_hub \
  -c "SELECT id, user_id, filename, content_type, s3_key, created_at FROM documents;"
```

### Получить все документы пользователя:
```bash
curl -i http://localhost:8084/api/v1/documents
```

### Получение документа по ID
```bash
curl -i http://localhost:8084/api/v1/documents/4d0afc55-d987-444e-8379-ed062e0f0306
```

### Следующая проверка — 404 Not Found
```bash
curl -i http://localhost:8084/api/v1/documents/00000000-0000-0000-0000-000000000000
```

response
```json
{
   "timestamp": "2026-11-03T19:06:13.278+00:00",
   "status": 404,
   "error": "Not Found",
   "path": "/api/v1/documents/00000000-0000-0000-0000-000000000000"
}
```
Обрати внимание: сейчас Spring Boot возвращает стандартный JSON с полями timestamp, status, error, path

### Запуск на EC2
```bash
  java -Dspring.profiles.active=ec2 -jar dbs-0.0.1-SNAPSHOT.jar
``` 
