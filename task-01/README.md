## Task 1 — Spring Boot + AWS S3

### Задача: Document Storage Service

Разработать небольшой backend-сервис для загрузки документов на Amazon S3.

Архитектура

                  ┌───────────────┐
                  │    Client     │
                  └───────┬───────┘
                          │
                     HTTP REST
                          │
                          ▼
                ┌──────────────────┐
                │   Spring Boot    │
                │ Document Service │
                └────────┬─────────┘
                         │
                AWS SDK for Java 2
                         │
                         ▼
                  ┌─────────────┐
                  │     S3      │
                  │   Bucket    │
                  └─────────────┘


### Переменные окружения
Для локальной разработки (credentials from  ~/.aws/credentials):

### Примеры запросов
#### Загрузить документ

``` shell
  curl -X POST http://localhost:8081/api/v1/documents  -F "file=@/home/alexei/dev/projects/aws/task-01/src/main/resources/banner.txt"

```
  
Пример ответа 201 Created:

``` json
  {
    "id": "d3b07384-d9a0-4f21-9f10-5c4f90d2b0aa",
    "fileName": "example.pdf",
    "contentType": "application/pdf",
    "size": 24576,
    "uploadedAt": "2026-10-19T11:30:00Z"
  }
```

#### Получить метаданные
``` shell
  curl http://localhost:8081/api/v1/documents/d3b07384-d9a0-4f21-9f10-5c4f90d2b0aa
```
#### Скачать документ
``` shell
  curl -OJ http://localhost:8081/api/v1/documents/d3b07384-d9a0-4f21-9f10-5c4f90d2b0aa/download
```

#### Удалить документ
``` shell
  curl -X DELETE http://localhost:8081/api/v1/documents/d3b07384-d9a0-4f21-9f10-5c4f90d2b0aa
```
Успешное удаление возвращает 204 No Content.
