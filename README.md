L1JTW Server
===

This repo is forked from [l1j-tw-99nets](https://github.com/nonono44/l1j-tw-99nets), now you could simply launch l1jtw server by docker.

## IDEA + Java 21 启动

仓库根目录的 Maven 工程包含 `l1jtw-350c`、`l1jtw-380c` 两个独立模块。
Maven 使用 JDK 21 编译源码，并提供 JAXB 2.3 API/运行时；数据库驱动使用
MySQL Connector/J 5.1.49（保留 `com.mysql.jdbc.Driver`），连接池使用 c3p0 0.9.5.5。
这套配置直接运行源码入口，不需要先构建旧版 `l1jserver.jar`。

1. 建议使用 [IDEA 2023.3 或更新版本](https://www.jetbrains.com/idea/whatsnew/2023-3/)。
   在 IDEA 中打开仓库根目录的 `pom.xml`，选择作为 Maven 项目打开，等待依赖下载完成。
2. 在 **File → Project Structure → Project SDK** 选择 JDK 21；同时将
   **Settings → Build Tools → Maven → Importing / Runner** 中的 JDK/JRE 设为 JDK 21。
   Maven 工程导入时不要将两个版本的源码合并为一个模块。
3. 准备数据库与地图，步骤见下文。
4. 在运行配置中选择 **L1J-TW 3.50c (Java 21)** 或 **L1J-TW 3.80c (Java 21)**，
   点击 Run 或 Debug。两版默认都监听 TCP 2000，因此每次只运行一版。

已提交的 `.run` 配置会自动指定主类 `l1j.server.Server`、对应版本的模块和工作目录。
如果手动新建 Application 配置，也必须把工作目录设为对应版本目录，例如
`$PROJECT_DIR$/L1J-TW_3.50c`；服务端通过相对路径读取 `config`、`data` 和 `maps`。
IDEA 启动不使用 `Loader`、`config/java.properties` 或 Docker 的 `ServerStart.sh`。

### 数据库准备（首次启动）

需要本机可访问的 MySQL/MariaDB。为需要运行的版本创建空数据库，并将该版本的
`db/l1jdb_Taiwan.sql` 导入一次：

| 版本 | 数据库名 | SQL 文件 |
| --- | --- | --- |
| 3.50c | `l1jdb350` | `L1J-TW_3.50c/db/l1jdb_Taiwan.sql` |
| 3.80c | `l1jdb380` | `L1J-TW_3.80c/db/l1jdb_Taiwan.sql` |

可以使用 IDEA 的数据库工具或其他数据库客户端导入。SQL 包含建表和初始化数据，
请导入新建的空库。旧 SQL 使用 MyISAM，并包含旧版日期默认值；如果导入时提示
`Invalid default value` 或零日期错误，需要在导入连接的会话中关闭严格日期校验，
例如先执行 `SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';`，再在同一会话导入。

修改所选版本 `config/server.properties` 中的 `URL`、`Login`、`Password`，
使其指向实际数据库。默认是本机 3306、用户 `root`、密码 `admin`。
IDEA 直接启动不会自动创建数据库，也不会读取 Docker 的 `.env` 文件。

### 3.50c 地图准备（首次启动）

3.50c 的地图随仓库以 ZIP 保存，需要先解压。下面的 PowerShell 命令在仓库根目录执行，
使用与原 Docker 启动脚本一致的 `352_maps.zip`：

```powershell
Expand-Archive -LiteralPath .\L1J-TW_3.50c\maps\352_maps.zip -DestinationPath .\L1J-TW_3.50c\maps
```

确认 `L1J-TW_3.50c/maps/0.txt` 等地图文件存在即可；已经解压过时无需重复执行。
3.80c 已包含解压后的地图。

### 命令行验证

将 `JAVA_HOME` 指向 JDK 21 后，在仓库根目录执行（Maven 3.9+）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
mvn clean verify
```

两个模块均会验证实际的 Boss 周期、宝箱 XML 可以通过外部 JAXB 读取，
并检查启动依赖及连接池配置是否在 classpath 上。测试不连接数据库。
`mvn -v` 应显示 Java 21；本机的 `java -version` 与 Maven 使用的 JDK 可能不同。

## Usage

Create `.env`

```
cp .env.example .env
```

Start specific version of server

```
docker-compose up v350c
docker-compose up v380c
```

## Build Server

Setup development environment

```
docker-compose up dev
```

Execute shell inside dev container

```
docker-compose exec dev sh
```

Start compilation

```
make
```
