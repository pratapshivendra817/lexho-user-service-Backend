@echo off
setlocal
set "FLYWAY_URL=jdbc:mysql://localhost:3306/user_servicedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
set "FLYWAY_USER=%DB_USERNAME%"
if "%FLYWAY_USER%"=="" set "FLYWAY_USER=root"
set "FLYWAY_PASSWORD=%DB_PASSWORD%"
call ..\mvnw.cmd org.flywaydb:flyway-maven-plugin:11.7.2:repair -Dflyway.url="%FLYWAY_URL%" -Dflyway.user="%FLYWAY_USER%" -Dflyway.password="%FLYWAY_PASSWORD%" -Dflyway.locations=classpath:db/migration/application-module,classpath:db/migration/user-service -f pom.xml
endlocal
