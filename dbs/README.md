# Spring Boot + AWS RDS, AWS DynamoDB, AWS DocumentDB, AWS S3

# AWS Document Processing & Notification Hub

## Проверить работу приложения на AWS
- Проверить работу приложение запущенное на Amazon EC2, от создания документа до получения результата анализа.
- Не надо проверять сервисы(AWS RDS, S3, DynamoDB, DocumentDB), они все настроены работают.
- Запросы делать только через curl.

### Предполагаемый сценарий тестирования:
1. Локальный компьютер отправляет HTTP-запрос на EC2.
2. Spring Boot принимает документ.
3. Файл сохраняется в S3.
4. Метаданные сохраняются в PostgreSQL (RDS).
5. Состояние обработки записывается в DynamoDB.
6. Приложение выполняет анализ документа.
7. Результат сохраняется в DocumentDB.
8. Статус обработки обновляется в DynamoDB.
9. Клиент получает результат через REST API.


- Если первая команда ничего не вывела — порт 8084 не прослушивается.
- Если вторая команда не показывает процесс приложения — вероятно, Spring Boot остановлен.


## Checklist
1. Прооверка работы приложения в local environment 
2. Прооверка работы приложения в настоящем AWS-окружении, используя Amazon RDS + DynamoDB + Amazon DocumentDB + S3

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

#### Распределение ответственности оставляем таким:
- RDS PostgreSQL - Создание и чтение документов
- DynamoDB - Создание и обновление статуса обработки
- DocumentDB - Сохранение и чтение JSON-результата анализа

Важное решение: RDS остаётся источником истины для бизнес-данных. Не нужно дублировать одну и ту же информацию во всех трёх базах.

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
  "id": "4c12e8d7-5de9-49a0-864b-54209baaec00",
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
docker exec -it aws_dbs_postgres \
  psql -U postgres -d aws_dbs_document_hub \
  -c "SELECT id, user_id, filename, content_type, s3_key, created_at FROM documents;"
```

### Получить все документы пользователя:
```bash
curl -i http://localhost:8084/api/v1/documents
```

### Получение документа по ID
```bash
curl -i http://localhost:8084/api/v1/documents/4c12e8d7-5de9-49a0-864b-54209baaec00
```
response
```json
{
  "id":"4c12e8d7-5de9-49a0-864b-54209baaec00",
  "filename":"invoice-001.pdf",
  "contentType":"application/pdf",
  "s3Key":"documents/550e8400-e29b-41d4-a716-446655440000/4c12e8d7-5de9-49a0-864b-54209baaec00",
  "createdAt":"2026-11-05T13:14:55.794384Z"
}
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

# Deploy на AWS
# AWS Dev Environment Setup Guide (`dev`)

Для твоего проекта я рекомендую сделать отдельное AWS-окружение `dev`, в котором Spring Boot приложение будет работать на новом EC2, а три базы данных будут размещены в AWS:

- **EC2** — Spring Boot приложение `dbs`
- **RDS PostgreSQL** — бизнес-данные документов
- **DynamoDB** — состояние и прогресс обработки
- **DocumentDB** — результаты анализа документов в JSON
- **S3** — исходные файлы документов
- **SQS** — очередь задач обработки

Будем переносить проект постепенно: сначала подключим PostgreSQL на RDS, затем DynamoDB, затем DocumentDB. После каждого этапа проверим работу через HTTP-запросы и AWS Console.

---

## 1. Целевая архитектура AWS

```
[ Твой компьютер (curl / Postman) ]
               │
               ▼
   [ EC2 — Spring Boot `dbs` ]
   (Java 21 · REST API · Port 8084)
               │
       ┌───────┼──────────────┐
       ▼       ▼              ▼
    [ RDS ] [ DynamoDB ] [ DocumentDB ]
 (PostgreSQL) (Processing) (Analysis JSON)
```

Все три базы доступны приложению по приватной сети VPC, а не напрямую из интернета.

> **Важный принцип:** EC2 и базы должны находиться в одной VPC. RDS и DocumentDB не должны иметь публичный доступ. Для DynamoDB будем использовать IAM Role и AWS SDK, а не access keys в `application.yml`.

---

## 2. Порядок реализации

| Этап | Что делаем | Проверка |
| :--- | :--- | :--- |
| **1** | VPC, Security Groups, IAM Role | Сетевая доступность |
| **2** | Создаём RDS PostgreSQL | Подключение и Flyway |
| **3** | Создаём DynamoDB | Запись и чтение статуса |
| **4** | Создаём DocumentDB | Запись и чтение JSON |
| **5** | Создаём EC2 | Запуск Spring Boot |
| **6** | Подключаем все сервисы | Проверка REST API |
| **7** | Проверяем IAM и CloudWatch | Логи и права доступа |

Я бы делал именно в таком порядке: сначала базы и доступ к ним, затем EC2. Так проще локализовать ошибки подключения.

---

## 3. Подготовка AWS Account и Region

Для проекта предлагаю использовать:

- **Region:** `eu-north-1`
- **Location:** Europe (Stockholm)

Ты уже использовал `eu-north-1` для SQS. Кроме того, Amazon DocumentDB поддерживает этот регион. Поэтому все компоненты проекта лучше держать в одной Region.

1. Открой [AWS Management Console](https://console.aws.amazon.com/).
2. В правом верхнем углу выбери **Europe (Stockholm) — eu-north-1**.
3. Проверь, что все создаваемые ресурсы будут в этой Region.
4. Открой **Billing → Budgets** и настрой уведомление о расходах.

> **Важно по стоимости:**
> RDS, EC2 и особенно DocumentDB могут создавать расходы, даже если ты не отправляешь запросы приложению. DynamoDB в режиме On-Demand удобнее для учебного проекта с небольшим количеством запросов, но хранение данных также тарифицируется. Не рассчитывай, что все эти ресурсы автоматически бесплатны.

---

## 4. Сеть: VPC и Security Groups

Сначала подготовим сетевую инфраструктуру.

### 4.1. VPC

Открой **VPC → Your VPCs**.

Для учебного окружения можно использовать существующую **Default VPC**, если она есть в `eu-north-1`.

Проверь:
- VPC существует.
- В ней есть Subnets минимум в двух Availability Zones.
- Включены **DNS resolution** и **DNS hostnames**.
- Для EC2 будет доступна публичная подсеть с маршрутом через Internet Gateway.

Для RDS и DocumentDB создадим отдельные DB Subnet Groups с подсетями из двух Availability Zones. Это необходимо для корректной конфигурации этих сервисов.

> Для учебного окружения допустимо использовать Default VPC. Для более строгой production-архитектуры лучше отдельная VPC с двумя публичными и двумя приватными подсетями.

### 4.2. Security Groups

Открой **EC2 → Network & Security → Security Groups**.

Создай три Security Groups:

#### 1. `sg-dbs-ec2` (Для Spring Boot приложения)
| Type | Port | Source |
| :--- | :--- | :--- |
| SSH | 22 | My IP |
| Custom TCP | 8084 | My IP |

> Порт 8084 нужен только для первоначального тестирования API. В production не следует оставлять его открытым напрямую в интернет: лучше использовать ALB и HTTPS.

#### 2. `sg-dbs-rds` (Для PostgreSQL)
| Type | Port | Source |
| :--- | :--- | :--- |
| PostgreSQL | 5432 | `sg-dbs-ec2` |

#### 3. `sg-dbs-docdb` (Для DocumentDB)
| Type | Port | Source |
| :--- | :--- | :--- |
| Custom TCP | 27017 | `sg-dbs-ec2` |

Для всех трёх групп оставь стандартный outbound traffic, если пока не настраиваешь более строгую egress-политику.

> **Принцип безопасности:** в качестве Source указывай Security Group EC2, а не `0.0.0.0/0`. Так базы смогут принимать соединения от приложения, но не от произвольных адресов интернета.

---

## 5. Создание AWS RDS PostgreSQL

Начнём с PostgreSQL, поскольку он уже работает локально и его интеграция с приложением у тебя реализована.

Открой [Amazon RDS Console](https://console.aws.amazon.com/rds/).

### 5.1. Создание базы

1. Выбери **Databases → Create database**.
2. Выбери **Standard create**.
3. **Engine type:** `PostgreSQL`.
4. **Engine version:** PostgreSQL 17, если доступна в выбранной конфигурации.
5. **Template:** `Dev/Test`.

Рекомендуемые параметры для учебного окружения:

| Параметр | Значение |
| :--- | :--- |
| **DB instance identifier** | `document-hub-postgres-dev` |
| **Master username** | `dbadmin` |
| **Initial database name** | `document_hub` |
| **Deployment** | Single-AZ |
| **Instance class** | Минимальный доступный подходящий класс |
| **Storage** | 20 GiB gp3 |
| **Storage autoscaling** | Пока отключить |
| **Public access** | No |
| **VPC** | Твоя VPC |
| **VPC security group** | `sg-dbs-rds` |
| **Backup retention** | 1 day для учебного окружения |

Пароль сохрани в AWS Secrets Manager, если доступна соответствующая опция управления паролем.

> Для RDS используй приватное подключение. AWS рекомендует не выставлять базу в интернет, а разрешать подключение от EC2 через Security Group.

### 5.2. После создания RDS

Дождись статуса `Available`.

Открой базу и скопируй:
- Endpoint
- Port
- Database name
- Username

Endpoint будет примерно такого вида:
```text
document-hub-postgres-dev.xxxxx.eu-north-1.rds.amazonaws.com
```

> Не используй этот пример как реальный адрес — скопируй endpoint из своей консоли.

### 5.3. Проверка подключения

На своём компьютере открой терминал. Перейди в каталог, где находится скачанный ключ:
```bash
chmod 400 dbs-key-ec2.pem
```

Подключись к серверу, подставив его публичный IP (находясь в папке содержащую файл dbs-key-ec2.pem):
```bash
ssh -i dbs-key-ec2.pem ec2-user@51.20.8.221
````
Позже, когда создадим EC2, выполни на EC2:

```bash
sudo dnf install -y postgresql17
```

*Если пакет с таким именем отсутствует в выбранном Amazon Linux AMI, установи совместимый PostgreSQL client.*

Проверка:

```bash
psql \
  -h aws-dbs-document-hub.chwkoqm0uc0i.eu-north-1.rds.amazonaws.com \
  -p 5432 \
  -U dbadmin \
  -d postgres
```

password = postgres

Введи пароль. Если подключение успешно, выполни:

```sql
SELECT version();
```

Затем закройте сессию:
```sql
\q
```

`RDS connectivity verified` — *только после фактического успешного подключения.*

---

## 6. Создание DynamoDB

Открой [Amazon DynamoDB Console](https://console.aws.amazon.com/dynamodb/).

### 6.1. Создание таблицы

Выбери **Tables → Create table**.

Настройки:

| Параметр | Значение |
| :--- | :--- |
| **Table name** | `dbs-document-processing-state-dev` |
| **Partition key** | `documentId` |
| **Partition key type** | String |
| **Sort key** | Не нужен |
| **Capacity mode** | On-demand |

> **Почему On-demand:** для нашего учебного проекта не нужно заранее рассчитывать Read/Write Capacity Units. Оплата идёт за фактические запросы, а не за зарезервированную пропускную способность.

### 6.2. Модель данных

Предлагаю хранить состояние обработки примерно так:

```json
{
  "documentId": "4d0afc55-d987-444e-8379-ed062e0f0306",
  "status": "PROCESSING",
  "currentStep": "TEXT_EXTRACTION",
  "attempts": 1,
  "createdAt": "2026-11-04T10:00:00Z",
  "updatedAt": "2026-11-04T10:01:00Z",
  "expiresAt": 1794000000
}
```

Назначение полей:

| Поле | Назначение |
| :--- | :--- |
| `documentId` | Partition Key |
| `status` | Текущий статус |
| `currentStep` | Этап обработки |
| `attempts` | Количество попыток |
| `createdAt` | Время создания |
| `updatedAt` | Последнее обновление |
| `expiresAt` | TTL, Unix timestamp в секундах |

Для `expiresAt` впоследствии включим TTL в настройках таблицы. Пока TTL можно не активировать, чтобы записи не удалялись во время тестирования.

### 6.3. Проверка DynamoDB

После создания таблицы:
1. Открой её.
2. Перейди в **Explore table items**.
3. Добавь тестовый Item.
4. Укажи `documentId` и остальные поля.
5. Сохрани.
6. Проверь, что запись появилась.

Это проверяет саму таблицу. Подключение Java-приложения проверим после настройки AWS SDK и IAM Role.

---

## 7. Создание Amazon DocumentDB

Это наиболее специфичная часть конфигурации.

Открой [Amazon DocumentDB Console](https://console.aws.amazon.com/docdb/).

### 7.1. Создание Cluster

Выбери **Clusters → Create**.

Настройки:

| Параметр | Значение |
| :--- | :--- |
| **Cluster type** | Instance-based |
| **Cluster identifier** | `document-hub-docdb-dev` |
| **Engine version** | Версия по умолчанию |
| **Instance count** | 1 |
| **Instance class** | Минимальный доступный подходящий класс |
| **VPC** | Та же VPC |
| **Subnet group** | Подсети минимум в двух AZ |
| **Security group** | `sg-dbs-docdb` |
| **Port** | 27017 |
| **Encryption** | Enabled |
| **Backup retention** | 1 day |
| **Deletion protection** | Disabled (для временного dev-окружения) |

Для DocumentDB нужен кластер и как минимум одна primary instance. Создание кластера может занять несколько минут.

> **Следи за расходами:** для учебного окружения достаточно одной instance без replica. Не создавай дополнительные реплики, пока не проверишь базовую интеграцию. DocumentDB тарифицирует вычислительные ресурсы и хранение данных.

### 7.2. Credentials

Создай отдельные credentials для DocumentDB. **Не используй PostgreSQL username/password.**

Сохрани:
- Username
- Password
- Cluster endpoint
- Port

### 7.3. TLS

DocumentDB требует TLS при стандартной конфигурации.

Скачай официальный CA bundle на EC2:

```bash
sudo mkdir -p /etc/ssl/documentdb

sudo curl -o /etc/ssl/documentdb/global-bundle.pem \
  https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
```

AWS описывает подключение Java-приложения через MongoDB Java Driver и CA bundle.

---

## 8. IAM Role для EC2

Это важный этап. Spring Boot должен получать доступ к AWS через IAM Role, а не через сохранённые `AWS_ACCESS_KEY_ID` и `AWS_SECRET_ACCESS_KEY`.

### 8.1. Создание IAM Role

Открой [AWS IAM Console](https://console.aws.amazon.com/iam/).

1. Выбери **Roles → Create role**.
2. **Trusted entity type:** `AWS service`.
3. **Use case:** `EC2`.
4. **Название:** `dbs-ec2-role`
5. Создай роль.

Добавь разрешения для DynamoDB. Для начала можно создать собственную IAM Policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DocumentProcessingStateAccess",
      "Effect": "Allow",
      "Action": [
        "dynamodb:DescribeTable",
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem"
      ],
      "Resource": "arn:aws:dynamodb:eu-north-1:YOUR_ACCOUNT_ID:table/document-processing-state-dev"
    }
  ]
}
```

*Замени `YOUR_ACCOUNT_ID` на ID своего AWS Account.*

Назови policy: `dbs-dynamodb-access-policy`

При необходимости добавь:
- `AmazonSSMManagedInstanceCore` — для подключения через Systems Manager.
- Разрешения на чтение конкретных секретов из Secrets Manager, если приложение будет получать пароли от баз оттуда.
- S3 permissions позже, когда подключим реальный S3.

> **Важно:** Не используй `AdministratorAccess` для приложения. IAM Role через Instance Profile позволяет AWS SDK автоматически получать временные credentials.

---

## 9. Создание EC2

Теперь создаём виртуальный сервер, на котором будет работать Spring Boot.

Открой [Amazon EC2 Console](https://console.aws.amazon.com/ec2/).

### 9.1. Launch instance

Выбери **Instances → Launch instances**.

Параметры:

| Параметр | Значение |
| :--- | :--- |
| **Name** | `document-hub-ec2-dev` |
| **AMI** | Amazon Linux 2023 |
| **Architecture** | x86_64 |
| **Instance type** | `t3.small` (для начала) |
| **Key pair** | Создать новую |
| **VPC** | Та же VPC |
| **Subnet** | Public subnet |
| **Auto-assign public IP** | Enable |
| **Security Group** | `sg-dbs-ec2` |
| **IAM Instance Profile** | `dbs-ec2-role` |
| **Storage** | 10–20 GiB gp3 |

> `t3.small` — практичнее для запуска Java-приложения, чем `t3.micro` с ограниченной памятью. Это не утверждение о бесплатности: проверь цену в выбранной Region.

### 9.2. Подключение к EC2

После запуска дождись статуса: `Running`

Скопируй Public IPv4 address.

На своём Ubuntu компьютере:

```bash
chmod 400 ~/Downloads/dbs-key.pem
```

Подключись:

```bash
ssh -i ~/Downloads/dbs-key.pem \
  ec2-user@YOUR_EC2_PUBLIC_IP
```

Если SSH не работает, проверь:
- правильный Key Pair;
- Security Group — порт 22 от твоего IP;
- Public IPv4;
- маршрут Public Subnet через Internet Gateway.

*Альтернативный вариант — Session Manager без открытия SSH-порта, если настроены SSM Agent и IAM permissions.*

### 9.3. Установка Java 21

На EC2:

```bash
sudo dnf update -y
sudo dnf install -y java-21-amazon-corretto-headless
java -version
```

Ожидаемый результат — Java 21.

---

## 10. Настройка Spring Boot для AWS

Теперь нужно подготовить проект к работе с AWS.

У тебя уже есть локальный `application.yml`, но нельзя просто перенести его на EC2: там используются `localhost` и локальный PostgreSQL.

Сделаем отдельные профили:

```text
dbs/
└── src/main/resources/
    ├── application.yml
    ├── application-local.yml
    └── application-aws.yml
```

### 10.1. application.yml

Общие настройки приложения:

```yaml
server:
  port: 8084

spring:
  application:
    name: dbs

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 10.2. application-aws.yml

Пример конфигурации:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME:document_hub}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  data:
    mongodb:
      uri: ${DOCDB_URI}

  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: false

  flyway:
    enabled: true

aws:
  region: ${AWS_REGION:eu-north-1}
  dynamodb:
    table-name: document-processing-state-dev
```

Здесь:
- `DB_HOST` — endpoint RDS.
- `DB_USERNAME` и `DB_PASSWORD` — credentials PostgreSQL.
- `DOCDB_URI` — URI DocumentDB.
- `AWS_REGION` — AWS Region.
- `table-name` — имя таблицы DynamoDB.

Для DocumentDB URI будет примерно таким:

```text
mongodb://USERNAME:PASSWORD@DOCDB_ENDPOINT:27017/document_analysis?tls=true&tlsCAFile=/etc/ssl/documentdb/global-bundle.pem&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false
```

Подставь реальные значения. Если в пароле есть специальные символы, их нужно URL-encode в URI.

> **Важно:** `spring.data.mongodb.uri` заработает только после подключения MongoDB Driver / Spring Data MongoDB и настройки совместимого TLS trust store.

### 10.3. Настройка AWS SDK

Для DynamoDB используй AWS SDK for Java 2.x.

Gradle Kotlin DSL:

```kotlin
dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.40.0"))
    implementation("software.amazon.awssdk:dynamodb")
}
```

*Версию BOM перед использованием проверь на совместимость с текущим AWS SDK release.*

Пример создания клиента:

```java
@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(
            @Value("${aws.region}") String region
    ) {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .build();
    }
}
```

Обрати внимание: мы намеренно не указываем `credentialsProvider`. SDK использует Default Credentials Provider Chain и на EC2 сможет получить временные credentials через IAM Role.


## 11. Подготовка приложения к запуску на EC2

### 11.1. Сборка JAR

На локальном компьютере:

```bash
./gradlew :dbs:clean :dbs:bootJar
```

Проверь результат:

```bash
ls -lh dbs/build/libs/
```

### 11.2. Передача JAR на EC2

```bash
scp -i ~/dev/projects/aws/dbs/secrets/dbs-key-ec2.pem \
  dbs/build/libs/dbs-0.0.1-SNAPSHOT.jar \
  ec2-user@51.20.8.221:/home/ec2-user/dbs.jar
```
### Connect to EC2

```bash
ssh -i dbs-key-ec2.pem ec2-user@51.20.8.221
```

### Проверь, запущено ли Java-приложение на порту 8084
```bash
sudo ss -lntp | grep 8084
````
Затем:
```bash
pgrep -af java

kill -9 PID
```

### Run app on EC2

```bash
java -jar dbs.jar --spring.profiles.active=ec2
```

# Полная проверка AWS-приложения `com.gmail.alexei28.aws.dbs`

Предлагаю провести полноценное End-to-End (E2E) тестирование приложения в реальном AWS-окружении, используя все четыре AWS-сервиса:
1. **Amazon RDS (PostgreSQL)**
2. **Amazon DynamoDB**
3. **Amazon DocumentDB**
4. **Amazon S3**

Проверять будем не только доступность AWS-сервисов, но и то, что Spring Boot действительно записывает и читает данные из каждой базы, загружает файлы в S3 и корректно обрабатывает документы.

> **Важный принцип:** успешный запуск Spring Boot ещё не означает, что все четыре AWS-сервиса работают правильно. Каждый сервис должен быть проверен отдельно, а затем в составе общего бизнес-сценария.

---

# E2E-проверка жизненного цикла документа в AWS

Проверим приложение `com.gmail.alexei28.aws.dbs`, запущенное на Amazon EC2, по полному сценарию:
$$\text{Создание документа} \longrightarrow \text{Загрузка в S3} \longrightarrow \text{Запуск обработки} \longrightarrow \text{Изменение статуса в DynamoDB} \longrightarrow \text{Сохранение результата} \longrightarrow \text{Получение через REST API}$$

Используем **исключительно `curl`** для HTTP-запросов к приложению и S3.

### Ограничения (не делаем)
*   Не проверяем доступность AWS-сервисов отдельно.
*   Не используем AWS CLI, Postman или Swagger.
*   Не проверяем инфраструктуру и конфигурацию EC2.
*   Не изменяем данные в базах вручную.

> Проверяем только реальный жизненный цикл одного документа.

---

## 1. Подготовка переменных

На своём компьютере открой терминал и выполни:

```bash
export BASE_URL="http://<EC2_PUBLIC_IP>:8080/api/v1"
export USER_ID="550e8400-e29b-41d4-a716-446655440000"
export TEST_FILE="/tmp/dbs-e2e-test.txt"
export CONTENT_TYPE="text/plain"
```

Замени `<EC2_PUBLIC_IP>` публичным IP своего EC2.

Создай тестовый документ:

```bash
cat > "$TEST_FILE" <<'EOF'
Document ID: E2E-TEST-001

This is an AWS Document Processing Hub end-to-end test.

The document contains information about software engineering,
Java development, cloud computing and distributed systems.

Expected analysis:
- Document type: text
- Language: English
- Main topic: Software Engineering
EOF
```

Проверим размер файла:

```bash
wc -c "$TEST_FILE"
```

Здесь мы подготовили небольшой текстовый файл, который будет проходить весь жизненный цикл.

---

## 2. Создание документа

В контроллере:
```java
@PostMapping
public ResponseEntity<DocumentCreatedResponse> create(...)
```

**Endpoint:** `POST /api/v1/documents`

### Выполнение запроса (Единый вызов):

Создание документа нужно выполнить **только один раз**. Выполняем запрос с записью ответа и извлечением параметров:

```bash
curl -sS -X POST \
  "$BASE_URL/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "filename": "dbs-e2e-test.txt",
    "contentType": "text/plain"
  }' | tee /tmp/dbs-create-response.json
```

> **Важно:** названия JSON-полей должны соответствовать фактическому `CreateDocumentRequest`. Если DTO отличается, скорректируй JSON.

### Извлечение параметров:

```bash
export DOCUMENT_ID=$(jq -r '.id' /tmp/dbs-create-response.json)
export UPLOAD_URL=$(jq -r '.uploadUrl' /tmp/dbs-create-response.json)

test "$DOCUMENT_ID" != "null" && test "$UPLOAD_URL" != "null" \
  && echo "Document created: $DOCUMENT_ID"
```

### Ожидаемый ответ:
```json
{
  "id": "DOCUMENT_UUID",
  "filename": "dbs-e2e-test.txt",
  "uploadUrl": "https://my-bucket-....s3.eu-north-1.amazonaws.com/...",
  "createdAt": "2026-11-06T..."
}
```

**Успех:** Документ создан, приложение вернуло UUID и pre-signed URL.

---

## 3. Загрузка документа в Amazon S3

Теперь загружаем настоящий файл по полученному pre-signed URL:

```bash
curl -i -X PUT \
  "$UPLOAD_URL" \
  -H "Content-Type: $CONTENT_TYPE" \
  --data-binary @"$TEST_FILE"
```

### Ожидаемый результат:
```http
HTTP/1.1 200 OK
ETag: "..."
```

**Успех:** Файл загружен непосредственно в S3, минуя сервер приложения.

> **Обрати внимание:**
> *   Используем именно `PUT`.
> *   `Content-Type` должен совпадать с тем, который использовался при генерации pre-signed URL.
> *   Используем `--data-binary`, а не `-d`, чтобы передать файл без изменения содержимого.
> *   Не добавляем JSON-заголовок.

---

## 4. Проверка статуса обработки документа

**Endpoint:** `GET /api/v1/documents/{id}/status`

Выполни:

```bash
curl -i -X GET \
  "$BASE_URL/documents/$DOCUMENT_ID/status" \
  -H "Accept: application/json"
```

### Ожидаемый ответ (пример):
```json
{
  "documentId": "DOCUMENT_UUID",
  "status": "PROCESSING",
  "progress": 25,
  "currentStep": "DOCUMENT_UPLOADED",
  "attempts": 1,
  "updatedAt": "2026-11-06T13:00:00Z"
}
```
*(Точные названия статусов и этапов зависят от реализации твоего processing worker).*

### Что проверяем:

| Поле | Ожидаемое поведение |
| :--- | :--- |
| `documentId` | Совпадает с созданным UUID |
| `status` | Отражает состояние обработки |
| `progress` | Изменяется по мере обработки |
| `currentStep` | Показывает текущий этап |
| `attempts` | Отражает количество попыток |
| `updatedAt` | Обновляется при изменении состояния |

---

## 5. Ожидание завершения обработки

Обработка асинхронная, поэтому не нужно ожидать завершения сразу после загрузки файла. Используем `curl` в цикле для периодического получения статуса:

```bash
for i in $(seq 1 30); do
  echo "========== CHECK $i =========="

  RESPONSE=$(curl -sS \
    "$BASE_URL/documents/$DOCUMENT_ID/status")

  echo "$RESPONSE" | jq .

  STATUS=$(echo "$RESPONSE" | jq -r '.status')

  if [ "$STATUS" = "COMPLETED" ]; then
    echo "Document processing completed!"
    break
  fi

  if [ "$STATUS" = "FAILED" ]; then
    echo "Document processing failed!"
    break
  fi

  sleep 5
done
```

*Максимальное время ожидания — 150 секунд.*

### Ожидаемый финальный статус:
```json
{
  "documentId": "DOCUMENT_UUID",
  "status": "COMPLETED",
  "progress": 100,
  "currentStep": "COMPLETED",
  "attempts": 1,
  "updatedAt": "2026-11-06T13:02:00Z"
}
```

> Если статус не меняется, это ещё не доказывает неисправность БД: нужно проверить, запустился ли механизм обработки после загрузки объекта.

---

## 6. Получение результата анализа

Когда статус стал `COMPLETED`, запрашиваем результат.

**Endpoint:** `GET /api/v1/documents/{id}/analysis`

Выполни:

```bash
curl -i -X GET \
  "$BASE_URL/documents/$DOCUMENT_ID/analysis" \
  -H "Accept: application/json" \
  | tee /tmp/dbs-analysis-response.json
```

### Ожидаемый результат (пример):
```json
{
  "documentId": "DOCUMENT_UUID",
  "analysis": {
    "documentType": "TEXT",
    "language": "English",
    "summary": "Document about software engineering...",
    "keywords": [
      "Java",
      "Spring Boot",
      "AWS",
      "Distributed Systems"
    ]
  },
  "createdAt": "2026-11-06T13:02:00Z"
}
```

### Проверяем:
*   Результат содержит UUID нашего документа.
*   Анализ не пустой.
*   Содержание соответствует загруженному тексту.
*   Результат действительно получен из приложения, а не сформирован тестовым запросом.

---

## 7. Получение полной информации о документе

Дополнительно проверяем: `GET /api/v1/documents/{id}`

```bash
curl -i -X GET \
  "$BASE_URL/documents/$DOCUMENT_ID" \
  -H "Accept: application/json"
```

### Проверяем, что:
*   Документ существует.
*   UUID совпадает.
*   Filename соответствует загруженному файлу.
*   Информация о документе согласуется с результатом анализа.

---

## 8. Повторная проверка результата

Выполним повторный запрос анализа:

```bash
curl -sS \
  "$BASE_URL/documents/$DOCUMENT_ID/analysis" \
  -H "Accept: application/json" \
  | jq .
```

Результат должен быть доступен повторно без повторной загрузки файла и без повторного запуска обработки.

---

## 9. Итоговый сценарий E2E

### Схема жизненного цикла документа:

1. **Create document**
   * `POST /documents` $\rightarrow$ `201 Created`
   * **PostgreSQL:** запись документа.
2. **Upload document**
   * `PUT S3 pre-signed URL` $\rightarrow$ `200 OK`
   * **S3:** сохранение исходного файла.
3. **Start processing**
   * Асинхронный механизм обработки $\rightarrow$ Передача документа обработчику.
4. **Track processing**
   * `GET /documents/{id}/status`
   * **DynamoDB:** статус, прогресс, этапы обработки.
5. **Complete analysis**
   * Обработчик сохраняет результат.
   * **DocumentDB / MongoDB:** результат анализа (если именно так реализовано в приложении).
6. **Get analysis**
   * `GET /documents/{id}/analysis` $\rightarrow$ `200 OK`
   * Получение результата через REST API.

---

## Важное замечание по коду

В предоставленной реализации есть ключевой момент, который необходимо учитывать при проведении E2E-тестов.

**В наличии:**
*   `S3StorageServiceImpl.generateUploadUrl()` — генерирует URL для загрузки.
*   `ProcessingServiceAwsImpl.getStatus()` — получает состояние из DynamoDB.
*   `ProcessingServiceAwsImpl.requestRetry()` — отправляет сообщение в SQS для повторной обработки.

**Не достаёт для полной картины:**
*   Не описан явный механизм, который запускает первоначальную обработку после загрузки документа в S3.
*   В контроллере отсутствует endpoint подтверждения загрузки (например, `POST /documents/{id}/complete`).

Поэтому нельзя гарантировать, что после шага 3 автоматически начнётся обработка. Это зависит от реализации `DocumentService.create()` и механизма триггеров (например, **S3 Event Notification**, **SQS** или внутренние фоновые процессы).

### Требуемые классы для уточнения:
1. `DocumentServiceImpl` — чтобы установить, что происходит при создании документа и как запускается обработка.
2. `ProcessingService` — интерфейс и реализация, которая непосредственно выполняет анализ документа.

> **Цель проверки:** один документ, один UUID, одна загрузка, одна обработка и один успешно полученный результат анализа.