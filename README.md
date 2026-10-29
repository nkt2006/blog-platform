# Блог-платформа — КР №1 и КР №2

Настольное JavaFX-приложение для работы с авторами, публикациями и комментариями блога. Архитектура: FXML / Controller → Service → Repository / JDBC → PostgreSQL. Консоль КР №1 сохранена. Экспорт текущего списка поддерживает `.xlsx` и `.csv`.

## Быстрый запуск КР №2

Нужны Java 21, Maven 3.9+ и PostgreSQL из КР №1. На этом Mac:

```bash
cd ~/Desktop/blog-platform
./run.sh
```

На других компьютерах с Java 21: `mvn javafx:run`.
Настройки находятся в `src/main/resources/database.properties`; локальные значения задаются в `database.local.properties` или переменными `BLOG_DB_*`.
Инструкция и сценарий защиты: [docs/kr2-guide.md](docs/kr2-guide.md). Обязательный пример Docker: [docs/docker-reference.md](docs/docker-reference.md).

## Общий контракт

- Java 21, JavaFX 21, FXML, CSS, Maven, PostgreSQL, JDBC и Apache POI для `.xlsx`.
- Пакет `ru.mirea.blogplatform.model`: `User`, `BlogPost`, `Comment` и их перечисления `UserRole`, `PostStatus`, `CommentStatus`.
- `BlogPost.authorId` ссылается на `User.id`; в БД этому соответствует `blog_posts.author_id → users.id`.
- `BlogPost.id` равен `null` до создания записи. Метод `BlogPostRepository.create` возвращает пост с полученным из БД ID.
- `createdAt` хранит время создания; `publishedAt` равен `null` до публикации.
- `BlogPostRepository.update` и `deleteById` возвращают `false`, если ID отсутствует. `findById` возвращает `Optional.empty()` в этом случае.
- У каждой из трёх сущностей есть модель, JDBC-репозиторий, сервис с CRUD, enum, собственное исключение, фильтрация, прямые и обратные сортировки и управление через консоль. SQL пишется только в JDBC-реализациях.
- Поиск, фильтрация, сортировка и статистика находятся в сервисах, которые получают данные через интерфейсы репозиториев.
- Проверка бизнес-правил находится в сервисе; ограничения `PRIMARY KEY`, `FOREIGN KEY`, `NOT NULL` и `UNIQUE` дополнительно защищают данные в БД.

## Состав проекта

| Компонент | Ответственность |
| --- | --- |
| `fx/controller/`, `fx/form/`, `resources/fx/` | графические таблицы и формы, FXML и CSS |
| `ConsoleUi`, `UserConsoleUi`, `CommentConsoleUi` | дополнительный консольный интерфейс КР №1 |
| `BlogPostService`, `UserService`, `CommentService` | бизнес-правила, CRUD, поиск, фильтры и сортировки |
| `JdbcUserRepository`, `JdbcBlogPostRepository`, `JdbcCommentRepository` | параметризованные SQL-запросы и преобразование строк БД в объекты |
| `ExcelExporter` | экспорт авторов, публикаций и комментариев в Excel |

Подробные правила сервиса описаны в [`docs/service-console.md`](docs/service-console.md), ER-диаграмма — в [`docs/er-diagram.md`](docs/er-diagram.md).

## Настройка PostgreSQL

По умолчанию приложение подключается к `jdbc:postgresql://localhost:5432/blog_platform` с пользователем `postgres`. Пароль задайте в локальном файле настроек или переменной окружения; в Java-коде он отсутствует. Настройки можно переопределить переменными окружения:

```shell
export BLOG_DB_URL='jdbc:postgresql://localhost:5432/blog_platform'
export BLOG_DB_USER='postgres'
export BLOG_DB_PASSWORD='ваш_локальный_пароль'
```

Создайте базу и примените схему с начальными данными:

```shell
createdb -U postgres blog_platform
psql -U postgres -d blog_platform -f src/main/resources/db/schema.sql
psql -U postgres -d blog_platform -f src/main/resources/db/seed.sql
```

Оба SQL-скрипта можно запускать повторно: таблицы создаются через `IF NOT EXISTS`, а уже существующие записи начального набора не перезаписываются. Начальный набор содержит 5 авторов, 10 публикаций и 5 комментариев с разными статусами. `schema.sql` добавляет поле роли и в базу, созданную прежней версией проекта.

Запуск дополнительной консольной версии КР №1 из корня проекта:

```shell
mvn compile exec:java
```

В консольной версии пункт `16` открывает меню авторов, пункт `17` — меню комментариев; пункт `15` экспортирует все данные в Excel с тремя листами. Образец начальных данных находится в `docs/sample-blog-platform.xlsx`.
В JavaFX используются вкладки. Кнопка «Экспорт текущего списка» открывает FileChooser и сохраняет видимые строки активной таблицы в XLSX или CSV.

## JDBC и экспорт

- `DatabaseManager` создаёт подключения с настройками из окружения.
- `JdbcUserRepository` реализует CRUD авторов и проверку уникальности email.
- `JdbcBlogPostRepository` реализует CRUD публикаций и проверяет уникальность адреса публикации (`slug` в коде и БД).
- `JdbcCommentRepository` реализует CRUD комментариев.
- Все запросы используют `PreparedStatement` и `try-with-resources`.
- `ExcelExporter` получает данные через интерфейсы репозиториев и создаёт книгу с листами `Авторы`, `Публикации` и `Комментарии`.
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

Если `BLOG_TEST_DB_URL` не задан, JDBC-интеграционные тесты пропускаются; остальные тесты работают без БД. Не указывайте здесь базу с реальными данными: тесты создают и удаляют записи.

## Распределение для подготовки к защите

Если в команде три человека, можно распределить демонстрацию так: Никита — публикации, Максим — авторы, Леонид — комментарии. Это план подготовки, а не утверждение о том, кто написал соответствующий код. Каждый участник должен уметь объяснить всю архитектуру и самостоятельно написать свой фрагмент ООП/JDBC на защите.
