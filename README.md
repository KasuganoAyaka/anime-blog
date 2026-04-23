# Anime Blog

[简体中文](./README.zh-CN.md)

Anime Blog is a full-stack blog platform built around anime-style personal publishing. It includes a Vue 3 frontend, a Spring Boot backend, and a MySQL schema with seed data and incremental SQL migrations. The project is designed for content publishing, community interaction, and site operations in one repository.

## Highlights

- Public blog pages with posts, tags, categories, about page, and search index
- Markdown-based post editing with image upload support
- User registration, email verification, login, refresh tokens, password reset, and session management
- Role-based management for station master, manager, and regular users
- Post submission and review workflow for non-admin contributors
- Comment publishing, moderation, reporting, and guest/anonymous comment protection
- Built-in music module with playlist management and media upload
- Real-time chat with WebSocket support, friend requests, blocks, and message search
- Contact form, friend link application flow, and admin reply handling
- Multi-language frontend support for `zh-CN`, `zh-TW`, and `en`
- Optional Cloudflare Turnstile and IP geolocation integrations

## Tech Stack

- Frontend: Vue 3, Vite 5, Vue Router, Pinia, Vue I18n, Element Plus, md-editor-v3
- Backend: Spring Boot 3.5, Spring Security, MyBatis-Plus, JWT, WebSocket, Java Mail
- Database: MySQL 8, SQL schema scripts, seed data, manual migration scripts
- Build tools: npm, Maven

## Repository Layout

```text
anime-blog/
├─ frontend/      # Vue 3 application
├─ backend/       # Spring Boot API and WebSocket server
├─ database/      # schema, seed data, and migrations
└─ .gitignore
```

## Core Modules

### Public Site

- Post listing, post detail, category and tag pages
- Search index endpoint for quick frontend search
- Music player and friend link display
- Contact form and friend link application

### Account and Security

- Username/email login
- Email code based registration and password reset
- Access token plus refresh token flow
- Session list and remote sign-out endpoints
- Optional Turnstile verification for login, registration email checks, and guest comments

### Content and Moderation

- Admin post management
- Contributor workspace and review-based publishing flow
- Comment review, rejection, deletion, and batch moderation
- Comment report review and resolution

### Social Features

- Private chat and global chat
- Friend requests and friend management
- User blocking and unread/read state tracking
- Message and contact search

## Quick Start

### 1. Prerequisites

- Node.js 18+
- npm 9+
- JDK 17
- Maven 3.9+
- MySQL 8.0+

### 2. Create the Database

Run the SQL files in the following order:

```text
database/init.sql
database/init_data.sql
database/migration_refresh_token.sql
database/migrations/*.sql
```

The default seed data creates an initial station master account:

- Username: `admin`
- Password: `admin123`

Change that password immediately after the first login.

### 3. Configure the Backend

Copy the template file and fill in your real values:

```bash
cd backend
cp .env.template .env
```

Minimum required items for the backend to run correctly:

- `DB_HOST`
- `DB_PORT`
- `DB_USERNAME`
- `DB_PASSWORD`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM_ADDRESS`
- `ANIME_BLOG_JWT_SECRET`

Important notes:

- The provided `start.sh` script loads `.env` automatically.
- If you run the backend from an IDE or with `mvn spring-boot:run`, make sure the same environment variables are exported into that process.

### 4. Start the Backend

For local development:

```bash
cd backend
mvn spring-boot:run
```

Default backend port:

```text
http://localhost:8080
```

For Linux server deployment with a packaged JAR:

```bash
cd backend
./start.sh start
```

### 5. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Default frontend port:

```text
http://localhost:5173
```

The Vite dev server proxies these paths to the backend:

- `/api`
- `/uploads`
- `/ws`

### 6. Build for Production

Frontend:

```bash
cd frontend
npm run build
```

Backend:

```bash
cd backend
mvn clean package
```

## Configuration Notes

- The backend reads the database name `anime_blog` by default.
- Uploaded assets are stored under `APP_UPLOAD_DIR`, which defaults to `./storage/uploads`.
- Frontend build artifacts such as `frontend/dist` and backend artifacts such as `backend/target` are already ignored.
- `backend/storage` is treated as runtime data and is not committed.
- The default local CORS setup allows the Vite dev server at `http://localhost:5173`.

## Useful Scripts and Files

- `frontend/package.json`: frontend commands
- `frontend/vite.config.js`: dev server and proxy settings
- `backend/.env.template`: deployment and environment template
- `backend/start.sh`: simple run/start/stop/restart/status script for packaged deployment
- `database/init.sql`: initial schema
- `database/init_data.sql`: seed data

## Roadmap-Friendly Areas

This repository already contains the foundation for:

- richer moderation workflows
- expanded module pages
- more social features around chat
- production deployment hardening
- automated migration management

## License

No license file is included yet. Add one before publishing the project as open source.
