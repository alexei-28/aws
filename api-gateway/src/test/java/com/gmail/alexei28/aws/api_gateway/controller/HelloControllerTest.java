package com.gmail.alexei28.aws.api_gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/*
    @WebMvcTest делает следующее:
    1. Изолированный подъем контекста (Slice Testing): Аннотация говорит Spring Boot: «Не нужно поднимать всё приложение целиком,
       базы данных, кафку и сервер Томкат. Подними только веб-слой».
    2. Сканирование конкретного бина: Вы передали в аннотацию @WebMvcTest(HelloController.class).
        Spring Boot находит только этот конкретный контроллер, создаёт для него облегчённый контекст в памяти (ApplicationContext)
        и настраивает легковесный эмулятор веб-окружения (MockMvc).
    3. Эмуляция запросов: MockMvc не делает реальных HTTP-запросов по сети (порт 8084 не задействуется).
        Он напрямую вызывает методы вашего Java-класса HelloController через специальный виртуальный диспетчер Spring MVC (DispatcherServlet),
        имитируя работу веб-сервера.

    Поэтому тест отрабатывает за доли секунды и полностью изолирован от вашего main-класса(ApiGatewayMain) и внешних зависимостей.

    Run all tests only in HelloControllerTest
    ./gradlew :api-gateway:test --tests com.gmail.alexei28.aws.api_gateway.controller.HelloControllerTest
*/

@WebMvcTest(HelloController.class)
@RequestMapping("/api/v1")
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnHello() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", startsWith("Hello, AWS API Gateway")));
    }
}