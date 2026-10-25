package com.gmail.alexei28.aws.lambda.service;

import com.gmail.alexei28.aws.lambda.domain.DocumentJob;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.util.UUID;

@Service
public class JobStorageService {
    private final DynamoDbTable<DocumentJob> documentJobsTable;
    private final String tableName;
    private static final Logger logger = LoggerFactory.getLogger(JobStorageService.class);

    public JobStorageService(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.aws.dynamodb.jobs-table}") String tableName
    ) {
        this.tableName = tableName;
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
}