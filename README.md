# Flow Studio — Mini-ERP

Mini-ERP en microservices (Spring Boot + Python FastAPI + Angular) pour studio de production vidéo.

## Architecture

| Service | Tech | Port | Rôle |
|---|---|---|---|
| eureka-server     | Spring Boot | 8761 | Service discovery |
| config-server     | Spring Boot | 8888 | Config centralisée |
| api-gateway       | Spring Cloud Gateway | 8100 | Routage + JWT |
| auth-service      | Spring Boot | — | Auth + JWT |
| rh-service        | Spring Boot | — | RH, recrutement, CV |
| project-service   | Spring Boot | — | Projets, jalons, briefs |
| dashboard-service | Spring Boot | — | KPIs agrégés |
| ai-service        | Python FastAPI | 8000 | CV classifier, matching, storyboard |
| frontend          | Angular 18 | 4200 | UI |

## Prérequis

- **Java 17** (Adoptium ou Oracle JDK)
- **Maven** (déjà via `./mvnw` dans chaque service)
- **Python 3.11+**
- **Node 18+** + npm
- **PostgreSQL** lancé sur :5432 avec 5 bases créées (`auth_db`, `rh_db`, `project_db`, `ai_db`, `dashboard_db`)
- **RabbitMQ** lancé sur :5672

## Démarrage rapide

### Option 1 — Windows natif (PowerShell)

```powershell
# Démarrer tout (infra + 7 services + frontend)
.\start.ps1

# Arrêter tout
.\stop.ps1
```

Si tu obtiens une erreur de policy execution, lance d'abord :
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Option 2 — Make (Linux / macOS / Git Bash sur Windows)

```bash
# Voir toutes les commandes
make

# Démarrer tout
make start

# État
make status

# Logs en direct
make logs

# Arrêter tout
make stop
```

## Premier lancement (install des dépendances)

```bash
# Avec Make
make install

# Ou manuellement
cd ai-service && pip install -r requirements.txt
cd ../frontend && npm install
```

Les services Java téléchargent leurs deps automatiquement au premier `mvnw spring-boot:run`.

## URLs après démarrage

- **Frontend** : http://localhost:4200
- **API Gateway** : http://localhost:8100/api/v1/...
- **Dashboard Eureka** : http://localhost:8761
- **RabbitMQ admin** : http://localhost:15672 (guest/guest)
- **Health AI** : http://localhost:8000/api/v1/ai/health

## Ordre de démarrage (pour debug manuel)

1. PostgreSQL + RabbitMQ
2. eureka-server (attendre 30s)
3. config-server (attendre 15s)
4. api-gateway + auth + rh + project + dashboard + ai-service en parallèle
5. frontend Angular

Le script `start.ps1` et le `Makefile` orchestrent automatiquement cet ordre avec des healthchecks par port.

## Logs

Les logs des services lancés via `make` ou `start.ps1` sont dans `.logs/`.

```bash
# Suivre un service
tail -f .logs/auth-service.log

# Make
make logs-auth
```

## Documentation

- `Documents/API_Endpoints.md` — Liste des 54 endpoints
- `Documents/database_design.md` — Schéma BDD par service
- `Documents/Development_Guide.md` — Guide pas-à-pas
- `frontend/README.md` — Doc spécifique du frontend
- `ai-service/README.md` — Doc du service IA

## Structure du repo

```
flow-studio-erp/
├── Makefile                  ← orchestration unifiée (Make)
├── start.ps1 / stop.ps1      ← orchestration Windows natif
├── eureka-server/            ← Service Discovery
├── config-server/            ← Config centralisée
├── config-repo/              ← YAMLs par service
├── api-gateway/              ← Gateway + JWT
├── auth-service/             ← MS1
├── rh-service/               ← MS2
├── project-service/          ← MS3
├── ai-service/               ← MS4 (Python)
├── dashboard-service/        ← MS5
├── frontend/                 ← Angular 18
├── database/                 ← Scripts init SQL par service
└── .logs/, .pids/            ← Runtime (gitignorés)
```
