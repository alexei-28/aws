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


### How run

1. Аутентификация AWS: Убедитесь, что перед запуском приложения заданы переменные окружения или настроен aws configure:

   File  ~/.aws/credentials
   
2. Запуск приложения с использованием переменных окружения:

    AWS_ACCESS_KEY_ID=your_access_key \
	AWS_SECRET_ACCESS_KEY=your_secret_key/K7MDENG/bPxRfiCYEXAMPLEKEY \
	./gradlew bootRun

	, где AWS_ACCESS_KEY_ID,  AWS_SECRET_ACCESS_KEY в in ~/.aws/credentials

3. Отправка файла c помощью curl (e.g.):

   curl -X POST http://localhost:8081/api/v1/documents -F "file=@/home/alexei/dev/temp/temp.txt"


   С учетом префикса /api/v1 из настроек spring.mvc.servlet.path, эндпоинт доступен по адресу http://localhost:8081/api/v1/documents.
