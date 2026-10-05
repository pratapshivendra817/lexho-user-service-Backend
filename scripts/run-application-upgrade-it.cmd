@echo off
setlocal

if "%IT_DB_USERNAME%"=="" set "IT_DB_USERNAME=root"
if "%IT_DB_PASSWORD%"=="" (
  echo IT_DB_PASSWORD is required.
  exit /b 1
)

call .\mvnw.cmd -pl application-module ^
  -Dlexho.it.mysql.application.upgrade=true ^
  "-DIT_DB_URL=jdbc:mysql://127.0.0.1:3306/lexho_application_upgrade_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" ^
  -DIT_DB_USERNAME=%IT_DB_USERNAME% ^
  -DIT_DB_PASSWORD=%IT_DB_PASSWORD% ^
  -Dtest=ApplicationMysqlUpgradeMigrationIT ^
  test

exit /b %ERRORLEVEL%
