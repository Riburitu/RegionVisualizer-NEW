@echo off
echo Compilando mod...
gradlew clean build --refresh-dependencies > build_log.txt 2>&1

if errorlevel 1 (
    echo Ha ocurrido un error durante la compilacion. Revisa build_log.txt para mas detalles.
) else (
    echo Compilacion completada con exito.
    echo El archivo jar generado se encuentra en build\libs\
)

echo.
echo Presiona cualquier tecla para salir...
pause >nul
