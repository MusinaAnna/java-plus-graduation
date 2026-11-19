# Explore With Me

Это дипломный проект на Java 21 и Spring Boot. На втором этапе я разделила основное приложение на несколько микросервисов.

## Какие сервисы есть в проекте

| Модуль | За что отвечает |
|---|---|
| `core/user-service` | Пользователи |
| `core/event-service` | Создание, изменение и поиск мероприятий |
| `core/request-service` | Заявки на участие в мероприятиях |
| `core/additional-service` | Категории, подборки и комментарии |
| `statistics-service/statistics-service-server` | Сохранение и получение статистики просмотров |
| `infra/discovery-server` | Eureka: помогает сервисам находить друг друга |
| `infra/config-server` | Хранит общие настройки сервисов |
| `infra/gateway-server` | Принимает внешние запросы на порту `8080` |
| `common` | Общие DTO и классы, которые нужны нескольким сервисам |

У четырёх основных сервисов свои базы PostgreSQL. Например, сервис мероприятий хранит идентификатор автора, а информацию о пользователе получает из `user-service`.

Сервисы обращаются друг к другу через OpenFeign. Eureka помогает найти нужный сервис. В списках данные о пользователях и категориях запрашиваются сразу для нескольких мероприятий, чтобы не отправлять запрос для каждого по отдельности.

## API

Внешние запросы нужно отправлять на `http://localhost:8080`. Gateway направляет их в нужный сервис:

- `/admin/users/**` — пользователи;
- `/events/**`, `/admin/events/**`, `/users/{userId}/events/**` — мероприятия;
- `/users/{userId}/requests/**` и `/users/{userId}/events/{eventId}/requests/**` — заявки;
- `/categories/**`, `/compilations/**`, `/admin/categories/**`, `/admin/compilations/**` и запросы комментариев — дополнительный сервис;
- `/hit`, `/stats` — статистика.

Спецификации внешнего API находятся в файлах [основного сервиса](ewm-main-service-spec.json) и [статистики](ewm-stats-service-spec.json).

### Внутренние запросы между сервисами

Эти адреса используются внутри Docker-сети через OpenFeign. Снаружи через Gateway они не открываются.

| Сервис | Внутренний запрос | Для чего нужен |
|---|---|---|
| `user-service` | `GET /internal/users/{userId}` | Получить пользователя |
| `user-service` | `GET /internal/users?ids=...` | Получить нескольких пользователей |
| `event-service` | `GET /internal/events/{eventId}` | Получить данные мероприятия |
| `event-service` | `GET /internal/events/short?ids=...` | Получить несколько мероприятий |
| `event-service` | `GET /internal/events/exists-by-category/{categoryId}` | Проверить, используется ли категория |
| `request-service` | `GET /internal/requests/confirmed-count?eventId=...` | Узнать число подтверждённых заявок |
| `request-service` | `GET /internal/requests/confirmed-counts?eventIds=...` | Получить количества для нескольких мероприятий |
| `additional-service` | `GET /internal/categories/{categoryId}` | Получить категорию |
| `additional-service` | `GET /internal/categories?ids=...` | Получить несколько категорий |

## Настройки

Конфигурация сервисов находится в `infra/config-server/src/main/resources/config/`. Маршруты Gateway указаны в `gateway-server.properties`.

В папке каждого сервиса есть `src/main/resources/application.properties`: там настроено подключение к Eureka и Config Server.

Для межсервисных вызовов используется Circuit Breaker. Если сервис заявок недоступен, количество подтверждённых заявок временно будет `0`. Если не работает статистика, просмотры также будут `0`. Для временных ошибок `5xx` при внутренних GET-запросах настроены повторные попытки. Если недоступны данные пользователя или категории, мероприятие нельзя показать полностью, поэтому возвращается ошибка `503`.

## Как запустить

Нужны JDK 21, Maven и Docker Compose. Собрать проект можно через встроенный Maven в IntelliJ IDEA с профилем `check`.

Сборка из терминала в корневой папке проекта:

```bash
mvn clean install -Pcheck
```

После успешной сборки можно запустить контейнеры:

```bash
docker compose up -d --build
docker compose ps
```

Dockerfile использует JAR-файлы из `target`, поэтому Maven нужно запустить до Docker Compose. Данные PostgreSQL сохраняются в Docker volumes.

## Как проверить

Исходные коллекции Postman лежат в папке `postman/`:

- `ewm-main-service.json` — основной API;
- `feature.json` — комментарии;
- `ewm-stat-service.json` — статистика.

Коллекции не менялись. Основные запросы проходят через Gateway на порту `8080`. В исходной коллекции статистики часть запросов отправляется на `9090`, поэтому для её запуска с Gateway нужен временный перевод порта `9090` на `8080`.

Для проверки отказоустойчивости можно по очереди отключать микросервисы и проверять функции, не зависящие от них. После тестирования сервисы нужно снова запустить.

Работа над вторым этапом ведётся в ветке `microservices`.
