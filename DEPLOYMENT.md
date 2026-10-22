# Free staging: Render + Neon PostgreSQL

The React frontend runs as a Render Static Site and the Spring Boot backend as a
Render Free Web Service. Neon Free PostgreSQL stores the data separately. Both
Render services use the `work` branch for automatic deployments.

Render's free backend sleeps after 15 minutes without traffic and takes about a
minute to wake. Neon also scales idle compute to zero. Check the current
[Render free limits](https://render.com/docs/free) and
[Neon free limits](https://neon.com/docs/introduction/plans).

## 1. Create Neon PostgreSQL

1. Sign in at [Neon](https://console.neon.tech/) and create a project on the
   **Free** plan. Choose PostgreSQL 17 or later and a region near your Render API.
2. Open **Connect** and note the host, port, database name, username, and password.
   Use a **direct connection** (turn connection pooling off) for the migration.
3. The default database name is often `neondb`; using that name is fine. The
   migration copies your local `psl` tables into whichever Neon database you select.

The hosting code must be pushed to `work` before Render can use it. Creating
provider accounts or filling in forms does not push these workspace changes.

## 2. Copy your existing MySQL data once

Follow [the PostgreSQL migration guide](Backend/database/postgresql/README.md).
The Docker migration tool reads a consistent, read-only snapshot of local MySQL
and copies all nine application tables into an empty PostgreSQL database. It
preserves IDs, relationships, partnerships and scorecards, converts booleans,
checks row counts, and resets identity sequences for new records. Legacy extra
columns are preserved in a separate PostgreSQL archive table.

This is a separate hosted copy; later local edits do not sync automatically.
A MySQL SQL dump cannot be imported directly into PostgreSQL. Existing partnership
rows are copied, so another partnership backfill is unnecessary.

## 3. Configure the Render backend Web Service

Connect GitHub repository `BilalYousuf-SE/PSL_Cricket_Stats` and use:

| Setting | Value |
| --- | --- |
| Branch | `work` |
| Runtime | Docker |
| Root directory | `Backend` |
| Dockerfile path | `./Dockerfile` (relative to the root directory) |
| Docker build context | `.` |
| Instance type / compute plan | **Free** |
| Health check path | `/health` |

The project environment may be named Production; that is an organizational label.
Confirm the service's compute plan itself says Free.

Add these **backend** environment variables:

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `staging` |
| `DB_URL` | `jdbc:postgresql://YOUR_NEON_HOST:5432/YOUR_NEON_DATABASE` |
| `DB_USER` | Neon username |
| `DB_PASSWORD` | Neon password |
| `ADMIN_PASSWORD` | A new, long password for the hosted admin UI |

Keep credentials in Render's Environment editor. The JDBC URL contains the host
and database; username and password go in their separate variables. The staging
profile enables TLS with certificate and hostname verification using Java's trust
store. Render provides `PORT`; no custom CA certificate is needed for Neon.

After the prepared code is pushed and the data copy is complete, deploy the
service. Open its `/health` URL and check for `{"status":"UP"}`. A missing admin
password prevents hosted startup. Connection errors indicate that the Neon host,
database, credentials, or reachability need to be checked in the API settings.

## 4. Create the React Static Site

Create a **Static Site** from the same repository:

| Setting | Value |
| --- | --- |
| Branch | `work` |
| Root directory | `FrontEnd` |
| Build command | `npm ci --include=dev && npm run build` |
| Publish directory | `dist` |
| `NODE_VERSION` environment variable | `22` |
| `VITE_API_BASE_URL` environment variable | The API service's `https://…onrender.com` URL |

Under Redirects/Rewrites, add source `/*`, destination `/index.html`, action
**Rewrite**. This lets `/admin`, `/matchups`, and match pages work on direct visits
and refreshes. Frontend environment values take effect when the site is built.

Open the UI URL and check matchups and a match's Partnerships tab. Visit `/admin`
and sign in with the backend's `ADMIN_PASSWORD` to test edits. The backend protects
all writes. Frontend `VITE_*` values are public; put the admin password only on the
backend.

The root `render.yaml` describes these same two services if you later prefer
Render's **New → Blueprint** flow. It fills the UI's API URL automatically and
prompts for the backend secrets.

## 5. Test subsequent changes

Once configured, pushes to `work` deploy automatically. Open the same UI URL after
Render finishes its deploy. Neon retains data across code deployments. Future
database migrations must run against PostgreSQL separately; the old SQL files in
`Backend/database/` are specifically for the local MySQL database.

## Local development

Your existing MySQL Compose setup continues to work. It uses the MySQL driver
based on `DB_URL`, while the hosted staging profile selects PostgreSQL. Keep local
`DB_PASSWORD`, `DB_ROOT_PASSWORD`, and `ADMIN_PASSWORD` in the root `.env`:

```powershell
docker compose up -d --build
```

Remove obsolete frontend `VITE_ADMIN_PASSWORD` settings and rebuild. Admin sign-in
is now validated by the backend. Without a backend admin password, public reads
work and mutations return 503.
