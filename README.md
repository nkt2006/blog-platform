# Блог-платформа — КР №1

Консольное Java-приложение для работы с публикациями блога. Архитектура: `ConsoleUi → BlogPostService → интерфейсы репозиториев → JDBC → PostgreSQL`. Экспорт `.xlsx` выполняется через Apache POI.

## Общий контракт

- Java 17, Maven, PostgreSQL, JDBC и Apache POI для `.xlsx`.
- Пакет `ru.mirea.blogplatform.model`: `User`, `BlogPost`, `PostStatus`.
- `BlogPost.authorId` ссылается на `User.id`; в БД этому соответствует `blog_posts.author_id → users.id`.
- `BlogPost.id` равен `null` до создания записи. Метод `BlogPostRepository.create` возвращает пост с полученным из БД ID.
- `createdAt` хранит время создания; `publishedAt` равен `null` до публикации.
- `BlogPostRepository.update` и `deleteById` возвращают `false`, если ID отсутствует. `findById` возвращает `Optional.empty()` в этом случае.
- Поиск, фильтрация, сортировка и статистика находятся в сервисе, который получает данные через интерфейсы репозиториев. SQL пишется только в JDBC-реализациях.
- Проверка бизнес-правил находится в сервисе; ограничения `PRIMARY KEY`, `FOREIGN KEY`, `NOT NULL` и `UNIQUE` дополнительно защищают данные в БД.

## Состав проекта

| Компонент | Ответственность |
| --- | --- |
| `ConsoleUi` | циклическое меню, ввод, вывод, обработка ошибок |
| `BlogPostService` | бизнес-правила, CRUD, поиск, фильтры, сортировки и статистика |
| `JdbcUserRepository`, `JdbcBlogPostRepository` | параметризованные SQL-запросы и преобразование строк БД в объекты |
| `ExcelExporter` | экспорт авторов и публикаций в Excel |

Подробные правила сервиса описаны в [`docs/service-console.md`](docs/service-console.md), ER-диаграмма — в [`docs/er-diagram.md`](docs/er-diagram.md).

## Настройка PostgreSQL

Для запуска нужны JDK 17, Maven и PostgreSQL. По умолчанию приложение подключается к `jdbc:postgresql://localhost:5432/blog_platform` с пользователем `postgres` и паролем `postgres`. Настройки можно переопределить переменными окружения:

```shell
export BLOG_DB_URL='jdbc:postgresql://localhost:5432/blog_platform'
export BLOG_DB_USER='postgres'
export BLOG_DB_PASSWORD='postgres'
```

Создайте базу и примените схему с начальными данными:

```shell
createdb -U postgres blog_platform
psql -U postgres -d blog_platform -f src/main/resources/db/schema.sql
psql -U postgres -d blog_platform -f src/main/resources/db/seed.sql
```

Оба SQL-скрипта можно запускать повторно: таблицы создаются через `IF NOT EXISTS`, а уже существующие записи начального набора не перезаписываются. Начальный набор содержит 5 авторов, 10 публикаций и четыре статуса.

Запуск приложения из корня проекта:

```shell
mvn compile exec:java
```

Пункт `15` меню создаёт Excel-файл. Если оставить путь пустым, он будет сохранён в `exports/blog-platform.xlsx`. Образец экспорта начальных данных находится в `docs/sample-blog-platform.xlsx`.

## JDBC и экспорт

- `DatabaseManager` создаёт подключения с настройками из окружения.
- `JdbcUserRepository` читает авторов.
- `JdbcBlogPostRepository` реализует CRUD публикаций и проверяет уникальность адреса публикации (`slug` в коде и БД).
- Все запросы используют `PreparedStatement` и `try-with-resources`.
- `ExcelExporter` получает данные через интерфейсы репозиториев и создаёт книгу с листами `Авторы` и `Публикации`.
- `Main` соединяет компоненты и запускает меню.

## Проверка

```shell
mvn clean test
```

Для запуска также JDBC-интеграционного теста нужна отдельная тестовая база с применёнными `schema.sql` и `seed.sql`:

```shell
export BLOG_TEST_DB_URL='jdbc:postgresql://localhost:5432/blog_platform_test'
export BLOG_TEST_DB_USER='postgres'
export BLOG_TEST_DB_PASSWORD='postgres'
mvn clean test
```

Если `BLOG_TEST_DB_URL` не задан, JDBC-интеграционный тест пропускается; остальные тесты работают без БД.
