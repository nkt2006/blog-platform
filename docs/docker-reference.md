# Обязательный преподавательский пример в Docker

Скачайте архив с [КР №2](https://online-edu.mirea.ru/mod/assign/view.php?id=933004) и распакуйте весь проект.
Каталоги `backend`, `javafx-client`, `android-client` должны оставаться вместе.
Используйте корневой `.env` из задания с параметрами базы, JWT и начального администратора.
Если `.env` отсутствует в распакованном архиве, создайте его по конфигурации на странице преподавателя.

Запустите Docker Desktop. Из папки примера выполните (путь второго файла замените на путь к нашей платформе):

```bash
docker compose -p blog-kr2-reference --env-file .env \
  -f backend/docker-compose.yml \
  -f /path/to/blog-platform/reference/docker-compose.override.yml \
  up -d --wait
```

Override использует локальный веб-порт 18080 и PostgreSQL 55432:5432. Он исправляет опубликованный внутренний порт PostgreSQL в скачанном compose.
В `.env` оставьте `APP_PORT=8080`: это порт backend внутри контейнера. Для веб-запросов задайте `CORS_ALLOWED_ORIGINS=http://localhost:18080`.

Проверка:

```bash
docker compose -p blog-kr2-reference --env-file .env \
  -f backend/docker-compose.yml \
  -f /path/to/blog-platform/reference/docker-compose.override.yml ps
curl http://localhost:18080/actuator/health
```

Откройте `http://localhost:18080/`. Затем запустите пример на Java 21:

```bash
cd javafx-client
mvn -Dstudenthub.api=http://127.0.0.1:18080/api javafx:run
```

Логин и первоначальный пароль берутся из `.env` примера. Если система требует смены пароля, владелец учётной записи выполняет её самостоятельно.
Первый запуск Maven/backend может занимать несколько минут из-за загрузки зависимостей.

Остановка выполняется теми же параметрами compose и командой `down`. Не добавляйте удаление volume: там хранится база примера.

Пример использует REST API. Собственный JavaFX-клиент блог-платформы подключается напрямую к базе КР №1 через JDBC, как требует задание.

## Уже установленный пример на этом Mac

Архив распакован в `/Users/nikitaabalaev/Downloads/student-hub-suite-rebuilt`.
В его корне создан локальный `.env` с демонстрационными настройками (не хранится в нашем Git).
PostgreSQL и backend запущены в Docker-проекте `blog-kr2-reference`.
Веб-часть: [localhost:18080](http://localhost:18080/), healthcheck: [actuator/health](http://localhost:18080/actuator/health).

Для повторного запуска из этой папки:

```bash
docker compose -p blog-kr2-reference --env-file .env \
  -f backend/docker-compose.yml \
  -f /Users/nikitaabalaev/Desktop/blog-platform/reference/docker-compose.override.yml \
  up -d --wait
```

При первоначальном запуске временно использовалась копия backend в `/tmp` для диагностики зависания Docker.
Приведённая выше команда переключает контейнер на исходники из распакованного архива; данные PostgreSQL остаются в том же volume.

Клиент также собран в `javafx-client/target/desktop/StudentHubReference.app` с адресом API `http://127.0.0.1:18080/api`.
Он успешно подключился к backend. При первом входе открывается окно установки постоянного пароля — этот шаг выполняет владелец самостоятельно.
Сборка `.app` предназначена только для этого Mac; для других систем используйте Maven-команду выше.
