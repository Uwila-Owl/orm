@echo off
setlocal enabledelayedexpansion

REM Forcer UTF-8
chcp 65001 >nul

REM Script de compilation Java sous Windows
echo === Script de compilation Java (Windows) ===

set JAVA_SRC_DIR=.\Java_Code
set CLASS_DEST_DIR=.\Class
set JAVAFX_LIB_DIR=.\JavaFX_Lib\Win\lib

REM Vérification des dossiers
if not exist %JAVA_SRC_DIR% (
    echo ✗ Erreur: Le dossier %JAVA_SRC_DIR% n'existe pas!
    exit /b 1
)

if not exist %JAVAFX_LIB_DIR% (
    echo ✗ Erreur: Le dossier %JAVAFX_LIB_DIR% n'existe pas!
    exit /b 1
)

if not exist %CLASS_DEST_DIR% (
    echo Création du dossier %CLASS_DEST_DIR%...
    mkdir %CLASS_DEST_DIR%
)

echo Nettoyage des anciens fichiers .class...
del /s /q %CLASS_DEST_DIR%\*.class >nul 2>&1

echo Recherche des fichiers .java...
set FILES=
for /r %JAVA_SRC_DIR% %%f in (*.java) do (
    set FILES=!FILES! "%%f"
)

if "%FILES%"=="" (
    echo ✗ Aucun fichier .java trouvé
    exit /b 1
)

echo Fichiers trouvés: %FILES%

REM Construction du classpath JavaFX
set CLASSPATH=
for %%j in (%JAVAFX_LIB_DIR%\*.jar) do (
    if "!CLASSPATH!"=="" (
        set CLASSPATH=%%j
    ) else (
        set CLASSPATH=!CLASSPATH!;%%j
    )
)

if "%CLASSPATH%"=="" (
    echo ⚠ Aucun .jar trouvé, compilation sans JavaFX
    javac -d %CLASS_DEST_DIR% %FILES%
) else (
    echo ⚠ Classpath JavaFX: %CLASSPATH%
    javac -cp "%CLASSPATH%" -d %CLASS_DEST_DIR% %FILES%
)

if %errorlevel%==0 (
    echo ✓ Compilation réussie!
    dir /s /b %CLASS_DEST_DIR%\*.class | find /c ".class"
) else (
    echo ✗ Erreur lors de la compilation
    exit /b 1
)

