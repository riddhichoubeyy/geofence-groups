@echo off
setlocal
set "GRADLE_VERSION=8.9"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "DIST_ROOT=%GRADLE_USER_HOME%\orbit-bootstrap\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%DIST_ROOT%\gradle-%GRADLE_VERSION%\bin\gradle.bat"
set "ZIP=%GRADLE_USER_HOME%\orbit-bootstrap\gradle-%GRADLE_VERSION%-bin.zip"
if not exist "%GRADLE_BIN%" (
  if not exist "%GRADLE_USER_HOME%\orbit-bootstrap" mkdir "%GRADLE_USER_HOME%\orbit-bootstrap"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'; if (Test-Path '%DIST_ROOT%') { Remove-Item -Recurse -Force '%DIST_ROOT%' }; New-Item -ItemType Directory -Force -Path '%DIST_ROOT%' | Out-Null; Expand-Archive -LiteralPath '%ZIP%' -DestinationPath '%DIST_ROOT%' -Force"
  if errorlevel 1 exit /b %errorlevel%
)
call "%GRADLE_BIN%" %*
exit /b %errorlevel%
