package com.gmail.alexei28.aws.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.alexei28.aws.lambda.TaskLambda;
import com.gmail.alexei28.aws.lambda.domain.DocumentJob;
import com.gmail.alexei28.aws.lambda.processor.DocumentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/*
    Точка входа, которую вызывает ваша AWS Lambda при поступлении событий из AWS SQS.
    Ваша Lambda запускается автоматически при появлении сообщений в очереди SQS.
    Необходимо настроить в вашей Lambda функции handler:
    AWS console -> Lambda -> Event Functions -> select your lambda function -> Code -> Handler

        com.gmail.alexei28.aws.lambda.handler.DocumentSqsHandler::handleRequest

    Pipeline: контроллер -> AWS SQS -> AWS Lambda-обработчик (DocumentProcessor.process).
    Убедиться вручную в том, что AWS Lambda получила сообщение, вызвала обработчик и выполнила логику, можно с помощью AWS CloudWatch Logs.
    DocumentSqsHandler написан именно как обработчик AWS Lambda (реализует интерфейс RequestHandler<SQSEvent, SQSBatchResponse>),

    Важно!
    Чтобы протестировать работу метода handleRequest локально (не в облаке AWS Lambda), необходимо запустить тесты DocumentSqsHandlerTest.
*/
public class DocumentSqsHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
    private final DocumentProcessor documentProcessor;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(DocumentSqsHandler.class);

    /**
     * Дефолтный конструктор для AWS Lambda.
     * Автоматически достает готовые бины из контекста Spring Boot.
     */
    public DocumentSqsHandler() {
        ApplicationContext springContext = SpringApplication.run(TaskLambda.class);
        this.documentProcessor = springContext.getBean(DocumentProcessor.class);
        this.objectMapper = springContext.getBean(ObjectMapper.class);
    }

    /**
     * Конструктор с параметрами (удобен для модульного тестирования).
     */
    public DocumentSqsHandler(DocumentProcessor documentProcessor, ObjectMapper objectMapper) {
        this.documentProcessor = documentProcessor;
        this.objectMapper = objectMapper;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        logger.info("handleRequest, call");
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        if (sqsEvent == null || sqsEvent.getRecords() == null) {
            logger.info("handleRequest, not found records in SQSEvent");
            return new SQSBatchResponse(failures);
        }
        // Если Lambda получит SQS message, то SQS records size > 0
        logger.info("handleRequest, SQSEvent records size: {}", sqsEvent.getRecords().size());
        for (SQSEvent.SQSMessage sqsMessage : sqsEvent.getRecords()) {
            try {
                DocumentJob job = objectMapper.readValue(sqsMessage.getBody(), DocumentJob.class);
                logger.info("handleRequest, read job(id): {}", job.getId());
                documentProcessor.process(job);
                logger.info("handleRequest, successfully processed job(id): {} ", job.getId());
            } catch (Exception exception) {
                logger.error("handleRequest, error : {}, {}", exception, exception.getMessage());
                failures.add(new SQSBatchResponse.BatchItemFailure(sqsMessage.getMessageId()));
            }
        }
        return new SQSBatchResponse(failures);
    }
}