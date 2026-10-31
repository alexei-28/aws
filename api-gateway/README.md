## Spring Boot + AWS API Gateway + Throttling (Rate Limiting)

### Учебный проект: aws-api-gateway-throttling
Цель
Научиться:
1. Создавать простой REST API на Spring Boot + Java 21.
2. Развернуть backend в AWS.
3. Создать AWS API Gateway перед backend.
4. Настроить Throttling.
5. Понять разницу между:
  - rate
  - burst
  - 429 Too Many Requests
6. Проверить throttling автоматически тестами.
7. Научиться отличать throttling API Gateway от throttling внутри Spring Boot.
8. Мое приложение должно принимать запросы только от API Gateway и было закрыто
   от прямых вызовов из интернета по публичному IP.

# Защита ваше Spring Boot приложение от прямых запросов из интернета через публичный IP вашего EC2 (например, http://16.171.27.181:8084
Чтобы защитить Spring Boot приложение на EC2, чтобы оно принимало запросы исключительно через AWS API Gateway и было закрыто 
от прямых вызовов из интернета по публичному IP, стандартным и самым надежным подходом в AWS является перевод приложения на приватный
режим + интеграция через VPC Link (если API Gateway — HTTP/REST API в приватной сети) либо использование 
секретного заголовка / mTLS (если API Gateway публичный, а доступ к EC2 ограничивается через Security Groups).

Ниже подробно рассмотрен лучший архитектурный паттерн с Private Integration (VPC Link), при котором EC2 вообще не имеет 
публичного IP (или порт 8084 закрыт для всего мира), а доступ разрешен только для API Gateway.

### Общая архитектура решения

    [ Клиент из интернета ]
          │
          ▼ (HTTPS / Публичный запрос)
    ┌────────────────────────────────────────┐
    │           AWS API Gateway              │
    │       (Публичная точка входа)          │
    └────────────────────────────────────────┘
          │
          ├────── (VPC Link / Private Integration)
          ▼
    ┌────────────────────────────────────────┐
    │          AWS VPC (Приватная сеть)      │
    │  ┌──────────────────────────────────┐  │
    │  │ Network Load Balancer (NLB)      │  │
    │  └──────────────────────────────────┘  │
    │                   │                    │
    │                   ▼ (Порт 8084)        │
    │  ┌──────────────────────────────────┐  │
    │  │ EC2 Instance (Spring Boot App)   │  │
    │  │ * Security Group: принимает      │  │
    │  │   трафик ТОЛЬКО от NLB           │  │
    │  └──────────────────────────────────┘  │
    └────────────────────────────────────────┘
  
API Gateway не нужно размещать после EC2. Он находится перед backend и принимает запросы первым.

### Как это работает:
1. API Gateway создается как публичный эндпоинт для клиентов.
2. Запросы из интернета идут только на API Gateway.
3. API Gateway отправляет запросы внутрь вашей VPC через VPC Link на Network Load Balancer (NLB).
4. NLB перенаправляет трафик на ваш EC2-инстанс на порт 8084.
5. Безопасность: Security Group вашего EC2 настраивается так, чтобы разрешать входящий трафик на порт 8084 только 
   от NLB (или из внутренней подсети), полностью блокируя любые прямые запросы по публичному IP.

### Что такое Throttling в Amazon API Gateway
Throttling — механизм управления потоком запросов, который помогает защитить backend от перегрузки.

В API Gateway нам понадобятся два параметра:
- Rate limit — количество запросов в секунду.
  Например, 5 requests/second. Это целевая средняя скорость обработки входящих запросов.
- Burst limit — размер кратковременного всплеска
  Например, 10. Позволяет принять короткий всплеск запросов сверх устойчивой скорости, в пределах доступного burst capacity.

### Как запускать:
#### Локально (в среде разработки):
Приложение запустится с дефолтным файлом application.yml, и environment будет равен Local.

```bash
  ./gradlew api-gateway:bootRun
``` 

Request to local to get info about server environment:

GET http://localhost:8084/api/v1/hello

Response:
```json
{
  "date": "2026-10-26T15:39:20.73470394",
  "hostname": "home-laptop",
  "javaVersion": "21.0.10",
  "os": "Linux",
  "environment": "Local"
}
```
#### На сервере EC2:
При запуске JAR-файла активируйте профиль ec2:

```bash
  java -Dspring.profiles.active=ec2 -jar api-gateway-0.0.1-SNAPSHOT.jar
``` 

Show active profile on EC2:

GET http://13.60.83.6:8084/api/v1/actuator/env

Request to EC2 to get info about server environment:

GET http://13.60.83.6:8084/api/v1/hello

Response:
```json
{
  "date": "2026-10-26T16:50:05.848795456",
  "hostname": "ip-172-31-47-191.eu-north-1.compute.internal",
  "javaVersion": "21.0.13",
  "os": "Linux",
  "environment": "EC2"
}
```
### Show current active profile

GET http://localhost:8084/api/v1/actuator/env

### Шаг 1. Создать EC2 в AWS
Для учебного приложения рекомендую начать с Linux-сервера без Docker: так ты поймёшь базовый процесс развёртывания Java-приложения.

- 1.1. Открой AWS Console
-
AWS EC2 Console - https://118697848787-sriuo4f6.eu-north-1.console.aws.amazon.com/ec2/home?utm_source=chatgpt.com&region=eu-north-1#Overview:

Перейди в AWS EC2 Console и выбери регион. Для твоих учебных AWS-проектов можно использовать Europe (Stockholm) — eu-north-1,
если остальные ресурсы уже находятся там.

Нажми Launch instance и заполни настройки:
- Name, например, api-gateway-server

- Application and OS Images (AMI)

Выбери Amazon Linux 2023, архитектура 64-bit x86.

#### Instance type
Выбери небольшой тип экземпляра, помеченный как подходящий для бесплатных преимуществ твоего аккаунта. Условия зависят от даты регистрации и оставшихся кредитов.
Amazon Elastic Compute Cloud

#### Key pair (login)
Нажми Create new key pair, назови ключ api-gateway-key, выбери .pem и скачай его. Не теряй файл и не публикуй его в Git.

#### Network settings
Настрой правила входящего трафика:
- SSH — TCP 22, источник My IP.
- Custom TCP — порт 8084, источник My IP для первоначального теста.

  Если приложение должно быть доступно всем через интернет, позднее можно изменить правило для 8084 на Anywhere, но для постоянной эксплуатации лучше использовать HTTPS через reverse proxy.

### Security Group
Открой EC2 Console.
1. Выбери свой экземпляр EC2.
2. Открой вкладку Security.
3. Нажми на связанную с ним Security group.
4. Выбери Edit inbound rules → Add rule.

Добавь правило:
- Type = Custom TCP
- Protocol = TCP
- Port range = 8084
- Source = My IP

#### Configure storage
Для начала оставь стандартный объём диска, убедившись, что его достаточно для Java и приложения.
Нажми Launch instance. Дождись состояния Running и успешных status checks. Официальная инструкция: создание и подключение к EC2.

### Шаг 2. Подключиться к серверу с Ubuntu
На странице EC2 выбери созданный сервер и открой Connect → SSH client. Там будет команда подключения.

1. На своём компьютере открой терминал. Перейди в каталог, где находится скачанный ключ:

  chmod 400 ec2-key.pem

2. Подключись к серверу, подставив его публичный IP (находясь в папке содержащую файл ec2-key.pem):

  ssh -i api-gateway-key.pem ec2-user@13.60.83.6

YOUR_PUBLIC_IP замени реальным IPv4-адресом экземпляра. Для Amazon Linux пользователь по умолчанию обычно ec2-user

После подключения ты работаешь уже внутри виртуальной машины AWS.

### Шаг 3. Установить Java 21 на EC2
В терминале подключённого сервера выполни:
sudo dnf update -y
sudo dnf install java-21-amazon-corretto-headless -y

Проверь установку:

java -version

Должна отображаться Java 21. Это соответствует твоему проекту, если он собран с Java 21.

Создай каталог приложения:

mkdir -p ~/apps/api-gateway

### Шаг 4. Собрать JAR на своём компьютере
Вернись в локальный терминал Ubuntu, в корень проекта aws

Если используешь Gradle Wrapper:
./gradlew clean api-gateway:bootJar

После успешной сборки проверь файл:
ls -lh api-gateway/build/libs/


Ожидаемый результат — файл вроде:

api-gateway-0.0.1-SNAPSHOT.jar

Если имя отличается, используй фактическое имя файла.

### Шаг 5. Скопировать JAR на EC2
Выполни команду на своём компьютере (находясь в корневой проектной папке "aws"), а не внутри SSH-сессии:
```bash
  scp -i api-gateway/secrets/api-gateway-key.pem \
  api-gateway/build/libs/api-gateway-0.0.1-SNAPSHOT.jar \
  ec2-user@13.60.83.6:~/apps/api-gateway/
```

Замени путь к ключу и IP-адрес на реальные значения. Если терминал открыт не в корне проекта, укажи полный путь к JAR.

### Шаг 6. Запустить Spring Boot
Теперь подключись к EC2 по SSH, если ещё не подключён, и выполни:
cd ~/apps/api-gateway

Запусти приложение:

java -jar -Dspring.profiles.active=ec2 api-gateway-0.0.1-SNAPSHOT.jar 

Проверь, запущено ли Java-приложение на порту 8084

 sudo ss -lntp | grep 8084

 Затем:

 pgrep -af java

- Если первая команда ничего не вывела — порт 8084 не прослушивается.
- Если вторая команда не показывает процесс приложения — вероятно, Spring Boot остановлен.

Spring Boot загрузит application-ec2.yml

Оставь терминал открытым. Когда увидишь в логах сообщение о запуске Spring Boot и встроенного сервера Tomcat, приложение должно быть запущено.

У тебя ранее был настроен порт 8084 и servlet context path /api/v1. Если эти настройки сохранены в профиле ec2, то обращаться к приложению нужно по адресу:
http://YOUR_PUBLIC_IP:8084/api/v1/...

Вместо ... укажи путь своего контроллера. Например, если у тебя есть GET /info, полный адрес будет

curl http://13.60.83.6:8084/api/v1/hello

### Шаг 7. Проверить работу
На самом EC2 можно выполнить:

curl http://localhost:8084/api/v1/actuator/health

Эта команда сработает, если Actuator установлен, endpoint health включён и доступен по указанному пути.
Если ты добавил другой endpoint, проверь его вместо этого адреса.

С локального компьютера проверь публичный адрес через браузер или curl.
Если локально всё работает, а снаружи нет, проверь Security Group, активный профиль и порт приложения.

Важно: сейчас запуск через java -jar работает только пока процесс не завершён. Позже можно настроить systemd, чтобы приложение автоматически запускалось после перезагрузки сервера.

## Пошаговая инструкция по настройке на AWS
Дано:
1. Instance ID: i-0f464128031def5fb (api-gateway-server)
2. Public IPv4 address: 13.60.83.6
3. Availability Zone: eu-north-1b
4. Security group ID: sg-05e5291bd3de56d95 (launch-wizard-4)
5. API Gateway: g5cxlwjn8f (my-api-gateweayi)
6. Load balancer: alb-api-gateway-throttling
   Tab "Security": Security groups -> Security Group ID: sg-05e5291bd3de56d95 (launch-wizard-4)
7. EC2 -> Target Groups -> My-TargetGroupName и откройте вкладку Targets.
   Registered targets -> Instance ID : i-0f464128031def5fb (api-gateway-server) -> Health status: Healthy
8. API Gateway -> выберите ваше API -> Stages -> $default -> Invoke URL
   https://g5cxlwjn8f.execute-api.eu-north-1.amazonaws.com

### Шаг 1. Изменение Security Group (Закрытие порта 8084 для 0.0.0.0/0)
   Security Group работает как виртуальный фарвол на уровне сетевых интерфейсов инстанса. 
   Нам нужно убрать правило, которое разрешает всему интернету (0.0.0.0/0) стучаться на порт 8084.
1. Откройте AWS Management Console и перейдите в сервис EC2.
2. В левом боковом меню прокрутите вниз до раздела Network & Security и выберите Security Groups.
3. Найдите и выберите Security Group, которая привязана к вашему EC2-инстансу с Spring Boot приложением
   Security group ID: sg-05e5291bd3de56d95
4. В нижней панели перейдите на вкладку Inbound rules (Входящие правила) и нажмите кнопку Edit inbound rules.
5. Найдите правило, где:
   Type: Custom TCP (или TCP)
   Port range: 8084
   Source: Anywhere-IPv4 (0.0.0.0/0)
6. Нажмите Delete (удалить) для этого правила.
7. Нажмите Save rules (Сохранить правила).

Результат: 
Теперь попытка выполнить curl http://13.60.83.6:8084/api/v1/hello

curl: (28) Failed to connect to 13.60.83.6:8084 after 136381 ms: Could not connect to server

из интернета завершится тайм-аутом (Connection timed out), так как порт физически закрыт фарволом AWS.
### Шаг 2. Настройка правильного Inbound Rule для NLB (или вашего IP)
### Шаг 3. Удаление или отключение Публичного IP-адреса (Public IPv4)
### Шаг 4. Проверка успешности настроек
1. Проверка из интернета (снаружи):
   Попробуйте выполнить запрос с вашего рабочего компьютера (или из внешней сети, не входящей в разрешенный список):
   curl http://13.60.83.6:8084/api/v1/hello

   Ожидаемый результат: Запрос зависает и выдает ошибку тайм-аута (Connection timed out) или Connection refused. Прямого доступа больше нет.
2. Проверка изнутри инфраструктуры (через API Gateway или NLB):
   Выполните запрос на публичный URL вашего API Gateway (или через настроенный VPC Link):

   curl https://ls5k9vv4qg.execute-api.eu-north-1.amazonaws.com/api/v1/hello

   Ожидаемый результат: API Gateway успешно принимает запрос, перенаправляет его внутри VPC на ваш EC2-инстанс (порт 8084), приложение обрабатывает запрос и возвращает JSON:
   ```json
    {"message":"Hello, AWS API Gateway! Current date: 2026-10-31T..."}
   ```
   
### Чек-лист для устранения ошибки 503 (Service Unavailable):
1. Проверьте, запущено ли приложение на самом EC2:
  Зайдите по SSH на ваш EC2-инстанс и убедитесь, что Spring Boot приложение действительно запущено и слушает порт 8084:

   sudo ss -tulpn | grep 8084

Ожидаемый результат: Вы должны увидеть процесс Java, слушающий порт 0.0.0.0:8084 или :::8084. Если приложение не запущенно или упало — поднимите его (java -jar ...).

2. Проверьте Target Group в AWS (Здоровье инстансов):
3. Проверьте взаимные Security Groups (Связка по ID):
4. Локальный тест с самого EC2:

   curl https://g5cxlwjn8f.execute-api.eu-north-1.amazonaws.com/default/hello