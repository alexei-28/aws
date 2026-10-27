## Spring Boot + AWS EC2

EC2 - Amazon Elastic Compute Cloud (Amazon EC2).

Это веб-сервис от Amazon Web Services (AWS), который предоставляет масштабируемые вычислительные мощности 
(виртуальные серверы — инстансы) в облаке.

### Задача: Document Storage Service
Разработать небольшой backend приложение для тестирования EC2.

### Общая схема

        1. Твой компьютер (Ubuntu)
    Собираешь приложение и получаешь JAR-файл
                |
                V
            2. AWS EC2
    Создаёшь виртуальную машину Linux
                |
                V
        3. Запуск Spring Boot
    Устанавливаешь Java, копируешь JAR и запускаешь с профилем ec2
                |
                V
        4. Проверка через браузер
    Обращаешься к публичному IP сервера по HTTP

### Как запускать:
#### Локально (в среде разработки):
Приложение запустится с дефолтным файлом application.yml, и environment будет равен Local.

```bash
  ./gradlew ec2:bootRun
``` 

Request to local to get info about server environment:

GET http://localhost:8083/api/v1/info

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
  java -Dspring.profiles.active=ec2 -jar ec2-0.0.1-SNAPSHOT.jar
``` 

Show active profile on EC2:

GET http://51.20.65.62:8083/api/v1/actuator/env

Request to EC2 to get info about server environment:

GET http://51.20.65.62:8083/api/v1/info

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

GET http://localhost:8083/api/v1/actuator/env

### Шаг 1. Создать EC2 в AWS
Для учебного приложения рекомендую начать с Linux-сервера без Docker: так ты поймёшь базовый процесс развёртывания Java-приложения.

- 1.1. Открой AWS Console
- 
  AWS EC2 Console - https://118697848787-sriuo4f6.eu-north-1.console.aws.amazon.com/ec2/home?utm_source=chatgpt.com&region=eu-north-1#Overview:

Перейди в AWS EC2 Console и выбери регион. Для твоих учебных AWS-проектов можно использовать Europe (Stockholm) — eu-north-1, 
если остальные ресурсы уже находятся там.

Нажми Launch instance и заполни настройки:
- Name, например, ec2-server

- Application and OS Images (AMI)

Выбери Amazon Linux 2023, архитектура 64-bit x86.

#### Instance type
Выбери небольшой тип экземпляра, помеченный как подходящий для бесплатных преимуществ твоего аккаунта. Условия зависят от даты регистрации и оставшихся кредитов.
Amazon Elastic Compute Cloud

#### Key pair (login)
Нажми Create new key pair, назови ключ ec2-key, выбери .pem и скачай его. Не теряй файл и не публикуй его в Git.

#### Network settings
Настрой правила входящего трафика:
- SSH — TCP 22, источник My IP.
- Custom TCP — порт 8083, источник My IP для первоначального теста.

  Если приложение должно быть доступно всем через интернет, позднее можно изменить правило для 8083 на Anywhere, но для постоянной эксплуатации лучше использовать HTTPS через reverse proxy.

### Security Group
Открой EC2 Console.
1. Выбери свой экземпляр EC2.
2. Открой вкладку Security.
3. Нажми на связанную с ним Security group.
4. Выбери Edit inbound rules → Add rule.

Добавь правило:
- Type = Custom TCP
- Protocol = TCP
- Port range = 8083
- Source = My IP

#### Configure storage
Для начала оставь стандартный объём диска, убедившись, что его достаточно для Java и приложения.
Нажми Launch instance. Дождись состояния Running и успешных status checks. Официальная инструкция: создание и подключение к EC2.

### Шаг 2. Подключиться к серверу с Ubuntu
На странице EC2 выбери созданный сервер и открой Connect → SSH client. Там будет команда подключения.

На своём компьютере открой терминал. Перейди в каталог, где находится скачанный ключ:
chmod 400 ec2-key.pem


Подключись к серверу, подставив его публичный IP (находясь в папке содержащую файл ec2-key.pem):
ssh -i ec2-key.pem ec2-user@51.20.65.62

YOUR_PUBLIC_IP замени реальным IPv4-адресом экземпляра. Для Amazon Linux пользователь по умолчанию обычно ec2-user.

После подключения ты работаешь уже внутри виртуальной машины AWS.

### Шаг 3. Установить Java 21 на EC2
В терминале подключённого сервера выполни:
sudo dnf update -y
sudo dnf install java-21-amazon-corretto-headless -y


Проверь установку:
java -version

Должна отображаться Java 21. Это соответствует твоему проекту, если он собран с Java 21.

Создай каталог приложения:
mkdir -p ~/apps/ec2

### Шаг 4. Собрать JAR на своём компьютере
Вернись в локальный терминал Ubuntu, в корень проекта ec2.

Если используешь Gradle Wrapper:
./gradlew clean bootJar


После успешной сборки проверь файл:
ls -lh build/libs/


Ожидаемый результат — файл вроде:

ec2-0.0.1-SNAPSHOT.jar

Если имя отличается, используй фактическое имя файла.

### Шаг 5. Скопировать JAR на EC2
Выполни команду на своём компьютере (находясь в корневой проектной папке "aws"), а не внутри SSH-сессии:
scp -i ec2/secrets/ec2-key.pem \
   ec2/build/libs/ec2-0.0.1-SNAPSHOT.jar \
   ec2-user@51.20.65.62:~/apps/ec2/

Замени путь к ключу и IP-адрес на реальные значения. Если терминал открыт не в корне проекта, укажи полный путь к JAR.

### Шаг 6. Запустить Spring Boot
Теперь подключись к EC2 по SSH, если ещё не подключён, и выполни:
cd ~/apps/ec2


Запусти приложение:
java -Dspring.profiles.active=ec2 \
-jar ec2-0.0.1-SNAPSHOT.jar


Оставь терминал открытым. Когда увидишь в логах сообщение о запуске Spring Boot и встроенного сервера Tomcat, приложение должно быть запущено.

У тебя ранее был настроен порт 8083 и servlet context path /api/v1. Если эти настройки сохранены в профиле ec2, то обращаться к приложению нужно по адресу:
http://YOUR_PUBLIC_IP:8083/api/v1/...


Вместо ... укажи путь своего контроллера. Например, если у тебя есть GET /info, полный адрес будет

http://51.20.65.62:8083/api/v1/info

### Шаг 7. Проверить работу
На самом EC2 можно выполнить:
curl http://localhost:8083/actuator/health

Эта команда сработает, если Actuator установлен, endpoint health включён и доступен по указанному пути. 
Если ты добавил другой endpoint, проверь его вместо этого адреса.

С локального компьютера проверь публичный адрес через браузер или curl. 
Если локально всё работает, а снаружи нет, проверь Security Group, активный профиль и порт приложения.

Важно: сейчас запуск через java -jar работает только пока процесс не завершён. Позже можно настроить systemd, чтобы приложение автоматически запускалось после перезагрузки сервера.


