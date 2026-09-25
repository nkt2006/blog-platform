BEGIN;

INSERT INTO users (name, email) VALUES
    ('Максим Изосимов', 'maxim@example.com'),
    ('Никита Абаляев', 'nikita@example.com'),
    ('Леонид Тараканов', 'leonid@example.com'),
    ('Анна Смирнова', 'anna@example.com'),
    ('Илья Волков', 'ilya@example.com')
ON CONFLICT (email) DO NOTHING;

INSERT INTO blog_posts
    (author_id, title, slug, content, status, created_at, published_at)
VALUES
    ((SELECT id FROM users WHERE email = 'maxim@example.com'),
     'Как устроена блог-платформа', 'blog-platform-architecture',
     'Обзор основных слоев приложения и их ответственности.',
     'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '19 days'),
    ((SELECT id FROM users WHERE email = 'nikita@example.com'),
     'Основы JDBC', 'jdbc-basics',
     'Подключение к PostgreSQL и выполнение параметризованных запросов.',
     'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '17 days'),
    ((SELECT id FROM users WHERE email = 'leonid@example.com'),
     'Работа с коллекциями', 'java-collections',
     'Фильтрация и сортировка публикаций с использованием Stream API.',
     'REVIEW', CURRENT_TIMESTAMP - INTERVAL '15 days', NULL),
    ((SELECT id FROM users WHERE email = 'anna@example.com'),
     'Черновик про PostgreSQL', 'postgresql-draft',
     'Заметки о проектировании таблиц и ограничений.',
     'DRAFT', CURRENT_TIMESTAMP - INTERVAL '12 days', NULL),
    ((SELECT id FROM users WHERE email = 'ilya@example.com'),
     'Экспорт данных в Excel', 'excel-export',
     'Создание таблиц XLSX с помощью Apache POI.',
     'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '9 days'),
    ((SELECT id FROM users WHERE email = 'maxim@example.com'),
     'Обработка исключений', 'exception-handling',
     'Как не завершать консольное приложение при ошибках пользователя.',
     'REVIEW', CURRENT_TIMESTAMP - INTERVAL '8 days', NULL),
    ((SELECT id FROM users WHERE email = 'nikita@example.com'),
     'Уникальные адреса публикаций', 'unique-slugs',
     'Зачем публикациям нужен уникальный адрес.',
     'DRAFT', CURRENT_TIMESTAMP - INTERVAL '7 days', NULL),
    ((SELECT id FROM users WHERE email = 'leonid@example.com'),
     'Архитектура Repository', 'repository-pattern',
     'Разделение бизнес-логики и доступа к данным.',
     'PUBLISHED', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    ((SELECT id FROM users WHERE email = 'anna@example.com'),
     'Первая версия платформы', 'first-platform-version',
     'Архивная публикация о первой версии проекта.',
     'ARCHIVED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '29 days'),
    ((SELECT id FROM users WHERE email = 'ilya@example.com'),
     'Подготовка к защите', 'project-defense',
     'Список ключевых решений, которые нужно объяснить на защите.',
     'DRAFT', CURRENT_TIMESTAMP - INTERVAL '2 days', NULL)
ON CONFLICT (slug) DO NOTHING;

COMMIT;
