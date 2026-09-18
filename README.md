# Блог-платформа — КР №1

Консольное Java-приложение для работы с публикациями блога. Сейчас в `main` находится общая основа проекта; интерфейс, бизнес-логика, JDBC и экспорт будут добавлены в отдельных ветках.

## Общий контракт

- Java 17, Maven, PostgreSQL, JDBC и Apache POI для `.xlsx`.
- Пакет `ru.mirea.blogplatform.model`: `User`, `BlogPost`, `PostStatus`.
- `BlogPost.authorId` ссылается на `User.id`; в БД этому соответствует `blog_posts.author_id → users.id`.
- `BlogPost.id` равен `null` до создания записи. Метод `BlogPostRepository.create` возвращает пост с полученным из БД ID.
- `createdAt` хранит время создания; `publishedAt` равен `null` до публикации.
- `BlogPostRepository.update` и `deleteById` возвращают `false`, если ID отсутствует. `findById` возвращает `Optional.empty()` в этом случае.
- Поиск, фильтрация, сортировка и статистика находятся в сервисе, который получает данные через интерфейсы репозиториев. SQL пишется только в JDBC-реализациях.
- Проверка бизнес-правил находится в сервисе; ограничения `PRIMARY KEY`, `FOREIGN KEY`, `NOT NULL` и `UNIQUE` дополнительно защищают данные в БД.

## Структура работы

| Ветка | Ответственность |
| --- | --- |
| `feature/service-console` | сервис, бизнес-правила, поиск/фильтры/сортировки/статистика, консольное меню, обработка ошибок ввода |
| `feature/database-export` | SQL-схема и начальные данные, JDBC-реализации репозиториев, экспорт `.xlsx`, ER-диаграмма, инструкция запуска |

После объединения веток необходимо проверить запуск с чистой БД и все операции из задания. Для сборки потребуется JDK 17 и Maven; команда проверки: `mvn test`.

## Настройка PostgreSQL

По умолчанию приложение подключается к `jdbc:postgresql://localhost:5432/blog_platform` с пользователем `postgres` и паролем `postgres`. Настройки можно переопределить переменными окружения:

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

Оба SQL-скрипта можно запускать повторно: таблицы создаются через `IF NOT EXISTS`, а тестовые записи обновляются по уникальным email и slug. Начальный набор содержит 5 авторов, 10 публикаций и четыре статуса.

## JDBC и экспорт

- `DatabaseManager` создаёт подключения с настройками из окружения.
- `JdbcUserRepository` читает авторов.
- `JdbcBlogPostRepository` реализует CRUD публикаций и проверки уникальности slug.
- Все запросы используют `PreparedStatement` и `try-with-resources`.
- `ExcelExporter` получает данные через интерфейсы репозиториев и создаёт книгу с листами `Авторы` и `Публикации`.

Пример подключения компонентов:

```java
DatabaseManager databaseManager = new DatabaseManager();
UserRepository userRepository = new JdbcUserRepository(databaseManager);
BlogPostRepository postRepository = new JdbcBlogPostRepository(databaseManager);

ExcelExporter exporter = new ExcelExporter(userRepository, postRepository);
Path file = exporter.export(Path.of("exports/blog-platform.xlsx"));
System.out.println("Данные сохранены: " + file);
```

ER-диаграмма находится в [`docs/er-diagram.md`](docs/er-diagram.md).

## Проверка

```shell
mvn clean test
```

После объединения с `feature/service-console` нужно проверить полный сценарий на чистой базе: создание, чтение, изменение и удаление публикаций, обработку неверного ввода, поиск, фильтрацию, сортировку, статистику и формирование `.xlsx`.
