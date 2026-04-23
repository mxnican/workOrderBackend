# Work Order Backend

这是工单系统的后端项目，基于 Spring Boot + MySQL。

前端仓库在这里：[Work Order Frontend](https://github.com/mxnican/workOrderFrontend)

## 新电脑第一次使用

### 1. 准备环境

- JDK 17
- Maven 3.8+，或者直接使用项目自带的 Maven
- MySQL 8

### 2. 克隆项目

先把仓库 `clone` 到本地，再进入后端目录：

```powershell
git clone https://github.com/mxnican/workOrderBackend.git
cd workOrderBackend
```

### 3. 创建数据库

先启动本机 MySQL，然后创建数据库：

```sql
CREATE DATABASE work_order
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

默认数据库名就是 `work_order`。

### 4. 配置数据库账号

如果你的 MySQL 账号不是默认的 `root / 123456`，先在 PowerShell 里设置环境变量：

```powershell
$env:WORKORDER_DB_URL='jdbc:mysql://127.0.0.1:3306/work_order?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:WORKORDER_DB_USERNAME='root'
$env:WORKORDER_DB_PASSWORD='your_password'
```

### 5. 启动后端

```powershell
$env:JAVA_HOME='D:\wly\tools\temurin17\jdk-17.0.18+8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

启动成功后，后端默认监听 `http://127.0.0.1:8080`。

### 6. 启动前端

后端起来以后，再启动前端项目：

```powershell
cd ..\workOrderFrontend
npm install
npm run dev
```

浏览器打开 `http://127.0.0.1:5173`。

## 一页式启动顺序

如果你只想先把整套系统跑起来，推荐这个顺序：

1. 启动 MySQL。
2. 创建 `work_order` 数据库。
3. 启动后端。
4. 启动前端。
5. 用浏览器打开前端地址登录使用。

## 数据是怎么初始化的

- 启动时会自动执行 `src/main/resources/workorder-schema.sql`，创建表结构。
- 如果 `wo_user` 表为空，系统会自动插入一组演示数据。
- 演示账号是：
  - `user / user123`
  - `admin / admin123`

## 数据和文件保存在哪里

### 数据库

- 业务数据保存在 MySQL 的 `work_order` 数据库中。
- 用户、反馈、工单、回复等信息都在数据库里。
- 用户头像地址保存在 `wo_user.avatar_url`。
- 反馈内容中的图片和附件元数据，会和反馈记录一起保存到数据库里，实际文件不会直接存进 MySQL。

### 上传文件

- 图片和附件的文件本体保存在本机磁盘。
- 默认目录是：

```text
${user.dir}/uploads
```

- 如果你在仓库根目录启动后端，默认就是：

```text
workOrderBackend/uploads
```

- 你也可以用环境变量自定义保存目录：

```powershell
$env:WORKORDER_UPLOAD_DIR='D:\wly\workOrderBackend\uploads'
```

- 上传后文件会按类型和日期分目录保存，例如：
  - `uploads/image/YYYY/MM/DD/...`
  - `uploads/file/YYYY/MM/DD/...`
- 前端访问文件时，后端会通过 `/api/files/...` 提供下载或预览。

## 运行后可以访问的地址

- 健康检查：`http://127.0.0.1:8080/api/health`
- Swagger：`http://127.0.0.1:8080/swagger-ui.html`

## 常用命令

```powershell
mvn spring-boot:run
mvn test
```
