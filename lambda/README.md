## Task — Spring Boot + AWS Lambda + AWS DynamoDB

### Задача: 
В качестве учебного проекта возьмём сервис обработки документов: 
 Spring Boot предоставляет REST API, а AWS Lambda асинхронно обрабатывает задания(jobs).

#### Шаги выполнения
1. Отправляем (POST запрос) сообщение содержащее objectKey(e.g documents/77a40eaf-5765-40b9-b60f-e7a060c17918) в твой контроллер com.gmail.alexei28.aws.lambda.controller.DocumentController
   - objectKey должен существовать в твоем AWS S3 Bucket.
2. Из входящего сообщения создаем job (com.gmail.alexei28.aws.lambda.domain.DocumentJob). Job id генерируется(UUID). Необходимо указать твой AWS S3 bucket.
   - Example:
   ```json
    {
      "id" : "68691be1-74aa-40cc-ac43-b287302c7127",
      "bucket" : "my-bucket-118697848787-eu-north-1-an",
      "objectKey" : "documents/77a40eaf-5765-40b9-b60f-e7a060c17918",
      "submittedAt" : 1792847485.046601467
    }
   ```
3. Сохраняем созданный job в AWS DynamoDB   
4. Отправляем job в мою очередь AWS SQS (создана заранее)
5. Моя Lambda функция (создана заранее) настроена (Runtime settings -> Handler) на вызов com.gmail.alexei28.aws.lambda.handler.DocumentSqsHandler::handleRequest
6. Моя Lambda функция запускается автоматически (вызов метода handleRequest) при появлении сообщений в очереди AWS SQS.

### Разделение ответственности:
- Spring Boot принимает REST-запросы и помещает задания в очередь.
- SQS хранит задания, пока они не будут обработаны.
- Lambda запускает Java-код при появлении сообщений в очереди.
- S3 хранит документы, а DynamoDB — статус обработки.
- DLQ получает сообщения, которые не удалось обработать после допустимого количества попыток.

### Пример запроса:
Upload file to the AWS SQS.
Значение objectKey — это имя твоего объекта в твоем S3-бакете.
Именв можно посмотреть: AWS Console -> S3 -> select your S3 bucket -> section Objects -> select specific object -> key

```bash
POST http://localhost:8082/api/v1/documents
Content-Type: application/json

{
  "objectKey": "documents/2f96cbcf-8f24-4279-8b97-d0d1ff32eb5a"
}
```

Ответ 202 Accepted:

```json
{
  "jobId": "4caa64a3-f7a9-4bd0-8a1d-1e1e2b48d514",
  "status": "ACCEPTED"
}
```
Job с этим jobId отправляется в твою очередь AWS SQS.


# Просмотр сообщений в AWS SQS
  AWS Console -> SQS -> Send and receive messages -> Press Poll for messages

# Просмотр логов в AWS Lambda
  AWS Console -> Lambda -> Event Functions -> Select your Lambda -> Monitor -> View CloudWatch logs -> Log streams

### Что нужно сделать, чтобы Lambda заработала?
Поскольку ваш класс DocumentSqsHandler написан именно как обработчик AWS Lambda (реализует интерфейс RequestHandler<SQSEvent, SQSBatchResponse>),
вам нужно развернуть его в облаке:
1. Создайте Lambda-функцию:
Нажмите кнопку «Create function» (Создать функцию) на экране, который у вас сейчас открыт, выберите среду выполнения (например, Java)
и создайте функцию.
2. Загрузите код:
Соберите ваш проект в скомпилированный архив (JAR или ZIP) и загрузите его в созданную функцию. При настройке функции укажите 
правильный класс-обработчик (Handler):com.gmail.alexei28.aws.lambda.handler.DocumentSqsHandler.

3. Настройте триггер SQS:
В панели управления созданной Lambda-функции добавьте триггер (Add trigger) и выберите вашу очередь document-jobs.

4. Чтобы Lambda могла автоматически забирать сообщения, ей необходимы права на действия:
  - sqs:ReceiveMessage
  - sqs:DeleteMessage
  - sqs:GetQueueAttributes
  - sqs:GetQueueUrl

5. Как только триггер будет настроен и функция связана с очередью, AWS Lambda начнет автоматически вызывать ваш код при появлении
   новых сообщений, и тогда вы сможете отслеживать их выполнение через CloudWatch Logs!

### Настройка прав IAM для  работы триггера 
Откройте настройки прав Lambda:
- Перейдите в вашу Lambda-функцию в консоли AWS.
- Выберите вкладку Configuration (Конфигурация) -> подвкладка Permissions (Права).

#### Перейдите к IAM-роли:
- В блоке Execution role (Роль выполнения) найдите название вашей роли и нажмите на ссылку с ее именем (она откроется в новой вкладке консоли IAM).

#### Добавьте политику разрешений:
- На странице роли в IAM нажмите кнопку Add permissions (Добавить разрешения) -> Attach policies (Прикрепить политики).
- В строке поиска введите: AWSLambdaSQSQueueExecution
- Найдите и выберите галочкой готовую системную политику AWSLambdaSQSQueueExecutionRole (она создана специально для таких связок Lambda + SQS).
- Нажмите кнопку Add permissions внизу.

- AWSLambdaBasicExecutionRole — отвечает только за отправку логов в CloudWatch.
- AWSLambdaSQSQueueExecutionRole — отвечает за чтение сообщений из очереди SQS.

В них нет никаких разрешений для работы с S3, поэтому при попытке обратиться к бакету Lambda получает ошибку 403 Forbidden (s3:ListBucket).

### Как исправить прямо в консоли AWS:
1. В верхней правой части блока с политиками (на скриншоте) нажмите кнопку Add permissions и выберите Create inline policy (Создать встроенную политику).
2. Переключитесь на вкладку JSON и вставьте следующий код (он разрешает чтение и просмотр содержимого именно вашего бакета):

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket",
                "s3:GetObject"
            ],
            "Resource": [
                "arn:aws:s3:::my-bucket-118697848787-eu-north-1-an",
                "arn:aws:s3:::my-bucket-118697848787-eu-north-1-an/*"
            ]
        }
    ]
}
```
### Убедитесь, что IAM-роль, назначенная вашей AWS Lambda функции, имеет разрешение dynamodb:CreateTable
 1. Откройте консоль AWS Lambda и выберите вашу функцию.
 2. Перейдите на вкладку Configuration (Конфигурация) -> Permissions (Разрешения).
 3. В блоке Execution role (Роль выполнения) найдите название роли и нажмите на него (вы перейдете в сервис IAM).
 4. На странице роли в IAM нажмите кнопку Add permissions (Добавить разрешения) -> Create inline policy (Создать встроенную политику).
 5. Перейдите на вкладку JSON и вставьте следующий JSON-код:
 
 ```json
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Action": [
                  "dynamodb:CreateTable",
                  "dynamodb:DescribeTable",
                  "dynamodb:PutItem",
                  "dynamodb:GetItem",
                  "dynamodb:UpdateItem",
                  "dynamodb:DeleteItem",
                  "dynamodb:Scan",
                  "dynamodb:Query"
              ],
              "Resource": "*"
          }
      ]
  } 
```
 6. Нажмите Next, введите имя политики (например, LambdaDynamoDbTablePolicy) и нажмите Create policy.

#### Повторите попытку:
- Вернитесь в консоль Lambda, обновите страницу и попробуйте снова добавить триггер SQS. Ошибка должна исчезнуть, 
  так как у роли появились все необходимые права.

