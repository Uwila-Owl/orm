@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM ─── Codes ANSI pour les couleurs ─────────────────────────────
for /F "delims=" %%A in ('echo prompt $E^| cmd') do set "ESC=%%A"
set "RED=%ESC%[31m"
set "GREEN=%ESC%[32m"
set "YELLOW=%ESC%[33m"
set "BLUE=%ESC%[34m"
set "CYAN=%ESC%[36m"
set "NC=%ESC%[0m"

echo === Génération du package client UMLGen ===
echo.

REM ─── Définition des chemins ───────────────────────────────────
set "CLIENT_DIR=End_User_UMLGen"
set "TMP_SUBDIR=UMLGen"
set "TEMP_DIR=%CLIENT_DIR%\%TMP_SUBDIR%"
set "JAR_FILE=GenerateurUML.jar"
set "JAVAFX_LIB_DIR=JavaFX_Lib"
set "EXT_DRIVER_DIR=Ext_Driver"
set "SCRIPT_EU_DIR=Script_EU"
set "ARCHIVE_TGZ=UMLGen_Client.tar.gz"
set "ARCHIVE_ZIP=UMLGen_Client.zip"

REM ─── Vérifications préalables ─────────────────────────────────
echo %BLUE%🔍 Vérifications préalables...%NC%
echo ───────────────────────────────────────────────────────────────

if not exist "%JAR_FILE%" (
    echo %RED%[ERREUR] Erreur: %JAR_FILE% introuvable.%NC%
    echo Veuillez exécuter create_jar.bat en amont.
    exit /b 1
)
echo    %GREEN%✓%NC% JAR principal trouvé.

if not exist "%SCRIPT_EU_DIR%\Start.bat" (
    echo %RED%[ERREUR] Erreur: Start.bat manquant dans %SCRIPT_EU_DIR%.%NC%
    exit /b 1
)
if not exist "%SCRIPT_EU_DIR%\Start.sh" (
    echo %YELLOW%⚠%NC% Avertissement: Start.sh manquant dans %SCRIPT_EU_DIR%.
) else (
    echo    %GREEN%✓%NC% Start.sh trouvé.
)
echo    %GREEN%✓%NC% Start.bat trouvé.

if not exist "%JAVAFX_LIB_DIR%" (
    echo %RED%[ERREUR] Erreur: dossier %JAVAFX_LIB_DIR% introuvable.%NC%
    exit /b 1
)
set /a JAVAFX_COUNT=0
for /r "%JAVAFX_LIB_DIR%" %%F in (*.jar) do (
    set /a JAVAFX_COUNT+=1
)
if !JAVAFX_COUNT! equ 0 (
    echo %RED%[ERREUR] Erreur: aucune librairie JavaFX dans %JAVAFX_LIB_DIR%.%NC%
    exit /b 1
)
echo    %GREEN%✓%NC% JavaFX jars: !JAVAFX_COUNT!

if not exist "%EXT_DRIVER_DIR%" (
    echo %YELLOW%⚠%NC% Avertissement: dossier %EXT_DRIVER_DIR% introuvable.
) else (
    set /a DRIVER_COUNT=0
    for /r "%EXT_DRIVER_DIR%" %%F in (*.jar) do (
        set /a DRIVER_COUNT+=1
    )
    if !DRIVER_COUNT! equ 0 (
        echo %YELLOW%⚠%NC% Aucun driver externe trouvé.
    ) else (
        echo    %GREEN%✓%NC% Drivers externes: !DRIVER_COUNT!
    )
)

echo.
echo %BLUE%📦 Préparation du package...%NC%
echo ───────────────────────────────────────────────────────────────

REM nettoyage de l'ancien package
if exist "%CLIENT_DIR%" (
    echo Suppression de %CLIENT_DIR%...
    rmdir /S /Q "%CLIENT_DIR%"
)

REM création des répertoires
mkdir "%TEMP_DIR%" 2>nul
echo    %GREEN%✓%NC% Répertoire de travail: %TEMP_DIR%

mkdir "%TEMP_DIR%\%JAVAFX_LIB_DIR%" 2>nul
mkdir "%TEMP_DIR%\%EXT_DRIVER_DIR%" 2>nul
mkdir "%TEMP_DIR%\%SCRIPT_EU_DIR%" 2>nul

REM copie du JAR
copy "%JAR_FILE%" "%TEMP_DIR%\" >nul
echo    %GREEN%✓%NC% %JAR_FILE% copié.

REM copie JavaFX
xcopy "%JAVAFX_LIB_DIR%\*.*" "%TEMP_DIR%\%JAVAFX_LIB_DIR%\" /E /I /Q >nul
echo    %GREEN%✓%NC% JavaFX copié [!JAVAFX_COUNT! fichiers].

REM copie drivers externes si présents
if exist "%EXT_DRIVER_DIR%" (
    xcopy "%EXT_DRIVER_DIR%\*.*" "%TEMP_DIR%\%EXT_DRIVER_DIR%\" /E /I /Q >nul
    if defined DRIVER_COUNT echo    %GREEN%✓%NC% Drivers copiés [!DRIVER_COUNT! fichiers].
)

REM copie scripts utilisateur
xcopy "%SCRIPT_EU_DIR%\*.*" "%TEMP_DIR%\%SCRIPT_EU_DIR%\" /E /I /Q >nul
echo    %GREEN%✓%NC% Scripts copiés de %SCRIPT_EU_DIR%.

REM Start.bat et Start.sh à la racine de UMLGen
if exist "%TEMP_DIR%\%SCRIPT_EU_DIR%\launch.bat" (
    copy "%TEMP_DIR%\%SCRIPT_EU_DIR%\launch.bat" "%TEMP_DIR%\" >nul
    echo    %GREEN%✓%NC% launch.bat ajouté à la racine.
)
if exist "%TEMP_DIR%\%SCRIPT_EU_DIR%\Start.bat" (
    copy "%TEMP_DIR%\%SCRIPT_EU_DIR%\Start.bat" "%TEMP_DIR%\" >nul
    echo    %GREEN%✓%NC% Start.bat ajouté à la racine.
)
if exist "%TEMP_DIR%\%SCRIPT_EU_DIR%\Start.sh" (
    copy "%TEMP_DIR%\%SCRIPT_EU_DIR%\Start.sh" "%TEMP_DIR%\" >nul
    echo    %GREEN%✓%NC% Start.sh ajouté à la racine.
)

REM Effacement Script_EU
rmdir /S /Q "%TEMP_DIR%\%SCRIPT_EU_DIR%"
echo    %GREEN%✓%NC% Nettoyage des scripts temporaires.    
echo.
echo %BLUE%📦 Création de l'archive...%NC%
echo ───────────────────────────────────────────────────────────────
cd /D "%CLIENT_DIR%" 

REM tentative tar
tar --version >nul 2>&1
if !errorlevel! equ 0 (
    tar -czf "%ARCHIVE_TGZ%" "%TMP_SUBDIR%" >nul 2>&1 && set "ARCHIVE=%ARCHIVE_TGZ%"
)

REM fallback ZIP via PowerShell
if not defined ARCHIVE (
    powershell -NoProfile -Command ^
      "Compress-Archive -Path '%TMP_SUBDIR%' -DestinationPath '%ARCHIVE_ZIP%' -Force" >nul 2>&1 && set "ARCHIVE=%ARCHIVE_ZIP%"
)

if not defined ARCHIVE (
    echo %RED%[ERREUR] Échec création archive.%NC%
    exit /b 1
)

for %%A in ("%ARCHIVE%") do set "ARCHIVE_SIZE=%%~zA"
echo    %GREEN%✓%NC% Archive créée: %CLIENT_DIR%\%ARCHIVE% [%ARCHIVE_SIZE% octets]

REM nettoyage du temporaire
rmdir /S /Q "%TMP_SUBDIR%"
popd

echo.
echo %GREEN%🎉 Package généré : %CLIENT_DIR%\%ARCHIVE% !%NC%
echo.

echo %YELLOW%📖 Instructions:%NC%
echo   1. Extraire %ARCHIVE%.
echo   2. Aller dans %TMP_SUBDIR%.
echo   3. Lancer Start.bat sous Windows ou ./Start.sh sous Linux.
echo.
echo === Fin ===
