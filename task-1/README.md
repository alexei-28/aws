## Task 1 — Spring Boot + AWS S3

Сценарий

Пользователь загружает документы через REST API:

POST /api/v1/documents
Content-Type: multipart/form-data

Сейчас файлы сохраняются локально:

/tmp/uploads/{filename}

Нужно перенести хранение файлов в Amazon S3.

Требования:

Необходимо реализовать:

1. Создать S3 bucket.
2. Реализовать Java/Spring сервис:
3. Реализовать его через AWS S3.
4. REST API:
    - POST /api/v1/documents/{filename}
    - GET  /api/v1/documents/{filename}
    - DELETE /api/v1/documents/{filename}
5. Файлы должны храниться в S3 примерно так:
    - documents/
        - 2026/
            - report.pdf
            - invoice.pdf
6. Не хранить AWS credentials в application.yml.
