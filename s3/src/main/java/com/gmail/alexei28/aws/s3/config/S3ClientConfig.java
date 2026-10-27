
package com.gmail.alexei28.aws.s3.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/*-
    @EnableConfigurationProperties(DocumentProperties.class)
    Она сообщает Spring Boot:
        1. Зарегистрировать DocumentProperties как Spring Bean.
        2. Связать значения из application.yml с параметрами record.
        3. Сделать созданный объект доступным для dependency injection.
    DocumentProperties.class — явно указывает Spring, что нужно создать бин (bean) для класса DocumentProperties,
    обработать его аннотацию @ConfigurationProperties (которая задана внутри самого этого класса) и сделать доступным для внедрения
    (@Autowired или через конструктор) в другие компоненты приложения.
    @EnableConfigurationProperties - эту аннотацию вешают на класс конфигурации (или на главный класс приложения):
*/
@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
public class S3ClientConfig {

    /*
        S3Client — потокобезопасный клиент, который следует создавать один раз и переиспользовать как Spring bean.
        С помощью S3Client можно загружать, получать и удалять объекты, а также просматривать содержимое AWS S3 bucket.
        Для асинхронных операций существует S3AsyncClient.
    */
    @Bean
    public S3Client s3Client(DocumentProperties documentProperties) {
        String region = documentProperties.region();
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
