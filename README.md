# Cloud File Storage

## Возможности

### Основной функционал
- **Регистрация и аутентификация пользователей** - защита паролем с BCrypt хэшированием
- **Загрузка файлов** - поддержка множественной загрузки с прогресс-баром
- **Скачивание файлов** - одиночное и групповое (ZIP архив)
- **Управление папками** - создание, переименование, удаление
- **Перемещение и копирование** - Drag & Drop, Cut/Copy/Paste
- **Поиск файлов** - быстрый поиск по всему хранилищу

## Технологический стек

### Backend
| Технология      | Назначение                   |
|-----------------|------------------------------|
| Java            | Основной язык                |
| Spring Boot     | Фреймворк                    |
| Spring Security | Аутентификация и авторизация |
| Spring Data JPA | Работа с БД                  |
| PostgreSQL      | Основная база данных         |
| Redis           | Кэширование и сессии         |
| MinIO           | S3-совместимое хранилище     |
| Liquibase       | Миграции БД                  |
| Maven           | Сборка проекта               |
| Swagger         | Документация API             |

### DevOps
| Технология     | Назначение      |
|----------------|-----------------|
| Docker         | Контейнеризация |
| Docker Compose | Оркестрация     |

## Начало работы

### Предварительные требования

- **Docker** 
- **Docker Compose** 
- **Git** 
- 2GB свободной RAM
- 10GB свободного дискового пространства

### Установка и запуск

#### 1. Клонирование репозитория

```bash
git clone https://github.com/ferty460/file-storage.git
cd file-storage
```

#### 2. Настройка окружения
   
Создайте файл .env в корне проекта:

```bash
# PostgreSQL
POSTGRES_DB=filestorage
POSTGRES_USER=your_username
POSTGRES_PASSWORD=your_secure_password

# Redis
REDIS_PASSWORD=your_redis_password

# MinIO
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=your_minio_password
MINIO_BUCKET_NAME=file-storage
MINIO_ENDPOINT=http://minio:9000

# Версии
POSTGRES_VERSION=16
REDIS_VERSION=8.6-alpine
MINIO_VERSION=latest
```

#### 3. Запуск через Docker Compose
```bash
docker-compose up -d
```

#### 4. Доступ к приложению

- Веб-интерфейс: http://localhost
- API (Swagger): http://localhost:8080/swagger-ui.html