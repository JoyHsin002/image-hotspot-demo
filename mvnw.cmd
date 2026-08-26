@echo off
where mvn >nul 2>nul && (mvn %* & exit /b %errorlevel%)
set WRAPPER_VERSION=3.9.11
set MAVEN_HOME=%TEMP%\image-hotspot-maven-%WRAPPER_VERSION%
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  powershell -NoProfile -Command "$ErrorActionPreference='Stop'; $zip='%TEMP%\apache-maven-%WRAPPER_VERSION%-bin.zip'; Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%WRAPPER_VERSION%/apache-maven-%WRAPPER_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Force $zip '%TEMP%'; Move-Item -Force '%TEMP%\apache-maven-%WRAPPER_VERSION%' '%MAVEN_HOME%'"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
