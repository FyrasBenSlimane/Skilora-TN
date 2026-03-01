@echo off
echo ===========================================
echo Running Login Test via Maven
echo ===========================================
echo.

set MAVEN_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3
"%MAVEN_HOME%\bin\mvn.cmd" clean compile exec:java -Dexec.mainClass="com.skilora.TestLogin" -Dexec.classpathScope=compile

pause
