package com.gmail.alexei28.aws.lambda.service;

import com.gmail.alexei28.aws.lambda.domain.DocumentJob;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
    Amazon DynamoDB — это полностью управляемая облачная NoSQL база данных (типа "ключ-значение" и документная БД),
    предоставляемая Amazon Web Services (AWS).
    Она спроектирована для работы с любыми нагрузками при стабильной
    и предсказуемой задержке чтения и записи на уровне единиц миллисекунд, обеспечивая автоматическое горизонтальное масштабирование.
    DynamoDB полностью бессерверная база данных. Вы платите только за реальные операции чтения и записи (режим On-Demand)
    или за выделенную емкость (Provisioned). Если ваша Lambda не работает и запросов нет, вы не платите ничего.
*/
@Service
public class JobStorageService {
    private final DynamoDbTable<DocumentJob> documentJobsTable;
    private final String tableName;
    private final DynamoDbClient dynamoDbClient;
    private static final Logger logger = LoggerFactory.getLogger(JobStorageService.class);

    public JobStorageService(
            @Value("${app.aws.dynamodb.jobs-table}") String tableName,
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbClient dynamoDbClient
    ) {
        this.tableName = tableName;
        this.dynamoDbClient = dynamoDbClient;
        this.documentJobsTable = enhancedClient.table(tableName, TableSchema.fromBean(DocumentJob.class));
    }

    // Чтобы приложение само создавало таблицу, если она отсутствует.
    // Убедитесь, что IAM-роль, назначенная вашей AWS Lambda функции, имеет разрешение dynamodb:CreateTable
    // (если вы используете автоматическое создание)
    @PostConstruct
    public void initTable() {
        try {
            documentJobsTable.createTable(builder -> builder
                    .provisionedThroughput(b -> b
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
            );
            logger.info("initTable, AWS DynamoDB table '{}' created successfully.", tableName);
        } catch (ResourceInUseException e) {
            logger.info("initTable, AWS DynamoDB table '{}' already exists.", tableName);
        } catch (Exception e) {
            logger.warn("initTable, Could not create AWS DynamoDB table automatically: {}", e.getMessage());
        }
    }

    public void save(DocumentJob job) {
        documentJobsTable.putItem(job);
    }

    public DocumentJob findById(UUID jobId) {
        return documentJobsTable.getItem(Key.builder()
                .partitionValue(jobId.toString())
                .build());
    }

    public List<DocumentJob> findAllJobs() {
        SdkIterable<DocumentJob> sdkIterable = documentJobsTable
                .scan()// выполняет DynamoDB Scan — читает все элементы таблицы.
                .items(); // получает DocumentJob из результатов scan.
        return sdkIterable.stream().toList();
    }

    /*
        выполняет операцию Scan всей таблицы DynamoDB оптимизированным способом:
        он вычитывает из базы данных исключительно поле с идентификатором (id), автоматически обрабатывает постраничную выдачу (пагинацию)
        и преобразует результат в список Java UUID.
    */
    public List<String> findAllJobIds() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                // Проекция указывает AWS вернуть только запрошенное поле (idKey), а не всю тяжелую сущность DocumentJob со всеми её данными.
                // Это радикально снижает сетевой трафик и потребление оперативной памяти (RAM) в Lambda.
                .projectionExpression("#idKey")
                .expressionAttributeNames(Map.of("#idKey", "id")) // Псевдоним #idKey маппится на реальный атрибут таблицы "id"
                .build();

        /*
            dynamoDbClient.scanPaginator(...) — низкоуровневый метод AWS SDK v2.
            По умолчанию DynamoDB возвращает результаты страницами размером максимум 1 МБ.
            Использование scanPaginator избавляет вас от необходимости вручную писать код с циклами и LastEvaluatedKey
            для дозагрузки следующих страниц: пагинатор автоматически под капотом вытягивает все страницы данных до конца
            таблицы по мере необходимости.
            .items() — извлекает элементы из пагинатора. Каждый элемент представляет собой карту Map<String, AttributeValue>,
            где ключ — имя атрибута, а значение — обертка AttributeValue, содержащая тип и данные.
            .stream() — открывает Java Stream для декларативной обработки элементов.
            .map(item -> ...) — трансформирует каждый элемент карты в объект UUID:
            item.get("id") — достает из карты атрибут по имени "id".
            .s() — извлекает из объекта AttributeValue строковое значение (метод .s() означает тип String).
         */
        return dynamoDbClient
                .scanPaginator(scanRequest)
                .items()
                .stream()
                .map(item -> item.get("id").s())
                .toList();
    }

}