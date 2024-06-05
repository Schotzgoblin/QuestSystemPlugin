set START_PATH=%~dp0%~1
cd %START_PATH%
java -Xmx1G -Xms1G -jar paper.jar --nogui
pause