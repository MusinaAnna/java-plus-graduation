# Explore With Me — диплом, этап 1: Spring Cloud

## Архитектура

Многомодульный Maven-проект использует Java 21, Spring Boot 3.3.13 и Spring Cloud 2023.0.6.

- `infra/discovery-server` — Eureka, порт `8761`.
- `infra/config-server` — Spring Cloud Config Server, порт выбирается автоматически; конфигурация хранится в `src/main/resources/config/`.
- `infra/gateway-server` — API Gateway, порт `8080`, маршруты к `main-service` через `lb://main-service`.
- `core/main-service` — основное приложение, порт выбирается автоматически.
- `statistics-service/statistics-service-server` — сервис статистики, порт выбирается автоматически.
- `statistics-service/statistics-service-client` — клиент статистики, находит `stats-server` через Eureka.
- `common` и `statistics-service/statistics-service-dto` — общие модели и DTO.

Gateway обслуживает публичные API основного приложения по путям `/admin/**`, `/users/**`, `/events/**`, `/categories/**`, `/compilations/**`. Автоматическая генерация маршрутов к другим сервисам Eureka отключена.

## Сборка

Для IntelliJ IDEA: Project SDK и Maven Runner — JDK 21. В панели Maven выполните для корневого проекта `Lifecycle → clean`, затем `Lifecycle → test` и `Lifecycle → package`. Maven-профиль `check` включает дополнительные проверки Checkstyle и SpotBugs; его нужно запустить отдельно перед сдачей.

Если команда Maven доступна в терминале:

```powershell
mvn clean verify
mvn install -P check
```

В проекте нет `mvnw.cmd`; на Windows без `mvn` в `PATH` используйте панель Maven в IntelliJ IDEA.

## Запуск с Docker Compose

Сначала успешно соберите JAR-файлы командой Maven или из IntelliJ IDEA, поскольку Dockerfile копируют готовые файлы `target/*.jar`. Затем из корня репозитория выполните:

```powershell
docker compose up -d --build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\smoke-stage1.ps1"
```

`smoke-stage1.ps1` проверяет Eureka, получение конфигурации из Config Server, доступность Gateway, HTTP 200 на `/categories` и `/events`, а также отсутствие автоматически опубликованных маршрутов внутренних сервисов.

PostgreSQL работает в именованных томах Docker Compose. **Не выполняйте `docker compose down -v`**, если требуется сохранить данные. Настройки `spring.jpa.hibernate.ddl-auto` остаются учебными и могут пересоздавать таблицы при перезапуске приложения независимо от томов.

## Проверка и сдача

Дипломный этап выполняется в ветке `spring-cloud`; предполагаемая база PR — личная `main`. До коммита убедитесь, что завершены Maven-тесты, профиль `check`, Docker smoke и просмотр изменений через `git diff --check` и `git status`.

Файл `.github/workflows/api-tests.yml` вызывает workflow исходного группового проекта. Его требования к названиям веток и порту статистики `9090` могут не соответствовать архитектуре дипломного этапа со случайными портами. Не изменяйте этот workflow и не возвращайте фиксированный порт без уточнения регламента дипломной сдачи.

Успешная техническая проверка инфраструктуры не заменяет сверку с полным оригинальным ТЗ и актуальными замечаниями ревьювера.
