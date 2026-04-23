# Work Order Backend

## Prerequisites
- JDK 17: `D:\wly\tools\temurin17\jdk-17.0.18+8`
- Maven 3.8+ or bundled Maven
- MySQL 8 running locally, or set `WORKORDER_DB_URL`, `WORKORDER_DB_USERNAME`, and `WORKORDER_DB_PASSWORD`

## Database
- Default database name: `work_order`
- Tables are created from `src/main/resources/schema.sql`
- Demo seed data is inserted on startup when `wo_user` is empty

## Run
```powershell
$env:JAVA_HOME='D:\wly\tools\temurin17\jdk-17.0.18+8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

If your MySQL account is not `root / 123456`, override the env vars before running:
```powershell
$env:WORKORDER_DB_URL='jdbc:mysql://127.0.0.1:3306/work_order?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:WORKORDER_DB_USERNAME='root'
$env:WORKORDER_DB_PASSWORD='your_password'
```

## Endpoints
- `GET /api/health`
- `GET /api/feedback/page`
- `GET /api/feedback/{id}`
- `POST /api/feedback`
- `POST /api/feedback/{id}/reply`
- `GET /api/work-order/page`
- `POST /api/work-order/{id}/status`
