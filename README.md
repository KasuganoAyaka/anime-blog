<h1 align="center">
  <img src="./frontend/public/logo.png" alt="Ayaka Blog logo" width="44" />
  Ayaka Blog
</h1>

<p align="center">
  <img src="./docs/assets/readme-banner.png" alt="Ayaka Blog banner" width="100%" />
</p>

<p align="center">
  <strong>A full-stack blog platform with a two-dimensional Miku-inspired vibe, featuring soft gradients, rounded cards, hot tags, music, chat, and publishing workflows.</strong>
</p>

<p align="center">
  <a href="./README.zh-CN.md">简体中文</a> |
  <a href="https://blog.ayakacloud.cn/">Live Site</a> |
  <a href="./LICENSE">MIT License</a>
</p>

<p align="center">
  <img alt="Vue 3" src="https://img.shields.io/badge/Vue_3-69C9D0?style=for-the-badge&logo=vuedotjs&logoColor=white" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-FF8FA3?style=for-the-badge&logo=springboot&logoColor=white" />
  <img alt="MySQL 8" src="https://img.shields.io/badge/MySQL_8-7AA8FF?style=for-the-badge&logo=mysql&logoColor=white" />
  <img alt="WebSocket" src="https://img.shields.io/badge/WebSocket-5EC6C5?style=for-the-badge&logo=socketdotio&logoColor=white" />
  <img alt="MIT License" src="https://img.shields.io/badge/License-MIT-F6A6B2?style=for-the-badge" />
</p>

<p align="center">
  Homepage • Posts • Hot Tags • Friend Links • Music Player • Realtime Chat
</p>


## Station Overview

> Ayaka Blog combines a Vue 3 frontend, a Spring Boot backend, and MySQL database scripts in one repository. It is built around the feel of a personal station rather than a generic CMS: article publishing, moderation, music, friend links, multilingual content, and community interaction all live inside the same codebase.

## Site Sections

<table>
  <tr>
    <td width="50%" valign="top">
      <strong>Homepage mood</strong><br />
      Home feed, post cards, categories, tags, about page, search index, friend links
    </td>
    <td width="50%" valign="top">
      <strong>Writing space</strong><br />
      Markdown editor, cover image upload, contributor workspace, review-based publishing
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <strong>Account corner</strong><br />
      Registration, email verification, login, refresh token flow, password reset, session management
    </td>
    <td width="50%" valign="top">
      <strong>Moderation desk</strong><br />
      Role-based permissions, post review, comment review, reporting, rejection, deletion, batch handling
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <strong>Social room</strong><br />
      Realtime chat, friend requests, block list, read state tracking, contact search
    </td>
    <td width="50%" valign="top">
      <strong>Extra touches</strong><br />
      Music playlist management, media upload, optional Turnstile, optional IP geolocation
    </td>
  </tr>
</table>

## Visual Style

<table>
  <tr>
    <td width="50%" valign="top">
      <strong>Accent color</strong><br />
      Cyan-first palette inspired by the live site's <code>#39c5bb</code> accent
    </td>
    <td width="50%" valign="top">
      <strong>Secondary glow</strong><br />
      Soft blue-violet gradients matching card and button highlights
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <strong>Surface language</strong><br />
      Rounded cards, lightweight glassmorphism, airy spacing
    </td>
    <td width="50%" valign="top">
      <strong>Product tone</strong><br />
      Personal, calm, slightly playful, and creator-oriented
    </td>
  </tr>
</table>

## Tech Stack

<table>
  <tr>
    <td width="20%" valign="top"><strong>Frontend</strong></td>
    <td width="80%" valign="top">Vue 3, Vite 5, Vue Router, Pinia, Vue I18n, Element Plus, md-editor-v3</td>
  </tr>
  <tr>
    <td width="20%" valign="top"><strong>Backend</strong></td>
    <td width="80%" valign="top">Spring Boot 3.5, Spring Security, MyBatis-Plus, JWT, WebSocket, Java Mail</td>
  </tr>
  <tr>
    <td width="20%" valign="top"><strong>Database</strong></td>
    <td width="80%" valign="top">MySQL 8, schema scripts, seed data, manual SQL migrations</td>
  </tr>
  <tr>
    <td width="20%" valign="top"><strong>Tooling</strong></td>
    <td width="80%" valign="top">npm, Maven</td>
  </tr>
</table>

## Repository Layout

```text
anime-blog/
├─ frontend/      # Vue 3 application
├─ backend/       # Spring Boot API and WebSocket server
├─ database/      # schema, seed data, and migrations
└─ .gitignore
```

## Open The Project

### 1. Prerequisites

- Node.js 18+
- npm 9+
- JDK 17
- Maven 3.9+
- MySQL 8.0+

### 2. Initialize the Database

Run the SQL files in this order:

```text
database/init.sql
database/init_data.sql
database/migration_refresh_token.sql
database/migrations/*.sql
```

The seed data creates an initial station master account:

- Username: `admin`
- Password: `admin123`

Change that password immediately after the first login.

### 3. Configure the Backend

Copy the template file and fill in real values:

```bash
cd backend
cp .env.template .env
```

Minimum required configuration:

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

Notes:

- `backend/start.sh` loads `.env` automatically.
- If you start the backend from an IDE or with `mvn spring-boot:run`, export the same environment variables into that process first.

### 4. Start the Backend Service

For local development:

```bash
cd backend
mvn spring-boot:run
```

Default backend address:

```text
http://localhost:8080
```

For Linux server deployment with a packaged JAR:

```bash
cd backend
./start.sh start
```

### 5. Start the Frontend Site

```bash
cd frontend
npm install
npm run dev
```

Default frontend address:

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

## Station Notes

> The backend uses the database name `anime_blog` by default.  
> Uploaded assets are stored under `APP_UPLOAD_DIR`, which defaults to `./storage/uploads`.  
> `frontend/dist` and `backend/target` are already ignored.  
> `backend/storage` is treated as runtime data and is not committed.  
> The default local CORS setup allows the Vite dev server at `http://localhost:5173`.

## Useful Files

<table>
  <tr>
    <td width="34%" valign="top"><code>frontend/package.json</code></td>
    <td width="66%" valign="top">frontend commands</td>
  </tr>
  <tr>
    <td width="34%" valign="top"><code>frontend/vite.config.js</code></td>
    <td width="66%" valign="top">dev server and proxy settings</td>
  </tr>
  <tr>
    <td width="34%" valign="top"><code>backend/.env.template</code></td>
    <td width="66%" valign="top">deployment and environment template</td>
  </tr>
  <tr>
    <td width="34%" valign="top"><code>backend/start.sh</code></td>
    <td width="66%" valign="top">packaged deployment helper script</td>
  </tr>
  <tr>
    <td width="34%" valign="top"><code>database/init.sql</code></td>
    <td width="66%" valign="top">initial schema</td>
  </tr>
  <tr>
    <td width="34%" valign="top"><code>database/init_data.sql</code></td>
    <td width="66%" valign="top">seed data</td>
  </tr>
</table>

## Future Directions

> This repository already has a strong base for richer moderation workflows, expanded module pages, stronger social features around chat and notifications, more production-ready deployment hardening, and automated migration management.

## License

This project is licensed under the MIT License. See the [LICENSE](./LICENSE) file for details.
