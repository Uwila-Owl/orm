@echo off
REM Script de création du JAR exécutable GenerateurUML.jar
REM Utilise les fichiers .class du dossier Class et le manifest du dossier Manifest

REM Forcer UTF-8
chcp 65001 >nul

echo === Script de création JAR GenerateurUML ===
echo.

REM Définition des chemins
set CLASS_DIR=.\Class
set MANIFEST_DIR=.\Manifest
set ENCRYPTION_DIR=.\Encryption
set CONFIG_FILE=config.properties
set JAR_NAME=GenerateurUML.jar
set MANIFEST_FILE=%MANIFEST_DIR%\MANIFEST.MF

REM Vérification de l'existence du dossier Class
if not exist "%CLASS_DIR%" (
    echo Erreur: Le dossier %CLASS_DIR% n'existe pas!
    echo Veuillez d'abord compiler les fichiers Java avec compile.bat
    pause
    exit /b 1
)

REM Vérification qu'il y a des fichiers .class
dir "%CLASS_DIR%\*.class" >nul 2>&1
if %errorlevel% neq 0 (
    echo Erreur: Aucun fichier .class trouvé dans %CLASS_DIR%!
    echo Veuillez d'abord compiler les fichiers Java avec compile.bat
    pause
    exit /b 1
)

for /f %%i in ('dir "%CLASS_DIR%\*.class" /b 2^>nul ^| find /c /v ""') do set CLASS_COUNT=%%i
echo ✓ %CLASS_COUNT% fichiers .class trouvés dans %CLASS_DIR%

REM Vérification de l'existence du fichier manifest
if not exist "%MANIFEST_FILE%" (
    echo Erreur: Le fichier manifest %MANIFEST_FILE% n'existe pas!
    pause
    exit /b 1
)

echo ✓ Fichier manifest trouvé: %MANIFEST_FILE%

REM Vérification de l'existence du fichier config.properties
set CONFIG_PATH=%ENCRYPTION_DIR%\%CONFIG_FILE%
if not exist "%CONFIG_PATH%" (
    echo Erreur: Le fichier %CONFIG_PATH% n'existe pas!
    echo Ce fichier est nécessaire pour la connexion à la base de données.
    pause
    exit /b 1
)

echo ✓ Fichier config.properties trouvé: %CONFIG_PATH%
echo.

REM Affichage du contenu du manifest
echo Contenu du manifest:
echo -------------------
type "%MANIFEST_FILE%"
echo -------------------
echo.

REM Suppression de l'ancien JAR s'il existe
if exist "%JAR_NAME%" (
    echo Suppression de l'ancien %JAR_NAME%...
    del "%JAR_NAME%"
)

REM Copie du fichier config.properties dans le dossier Class
echo Copie du fichier config.properties...
copy "%CONFIG_PATH%" "%CLASS_DIR%\" >nul
if %errorlevel% neq 0 (
    echo Erreur: Impossible de copier config.properties!
    pause
    exit /b 1
)
echo ✓ config.properties copié dans %CLASS_DIR%

REM Création du JAR
echo Création du JAR %JAR_NAME%...
cd "%CLASS_DIR%"
jar cfm "..\%JAR_NAME%" "..\%MANIFEST_FILE%" *
set JAR_RESULT=%errorlevel%
cd ..

REM Vérification du succès de la création
if %JAR_RESULT% equ 0 if exist "%JAR_NAME%" (
    REM Nettoyage : suppression du config.properties temporaire du dossier Class
    if exist "%CLASS_DIR%\%CONFIG_FILE%" (
        del "%CLASS_DIR%\%CONFIG_FILE%" >nul
        echo ✓ Fichier config.properties temporaire supprimé du dossier Class
    )
    
    for %%A in ("%JAR_NAME%") do set JAR_SIZE=%%~zA
    set /a JAR_SIZE_KB=%JAR_SIZE%/1024
    echo =^> JAR créé avec succès: %JAR_NAME% (taille: %JAR_SIZE_KB% KB)
    echo.
    
    REM Vérification du contenu du JAR
    echo Contenu du JAR:
    echo ---------------
    jar tf "%JAR_NAME%" | findstr /n "^" | findstr "^[1-9]:" | findstr "^[1-2][0-9]:"
    for /f %%i in ('jar tf "%JAR_NAME%" ^| find /c /v ""') do set TOTAL_FILES=%%i
    echo ---------------
    echo Total: %TOTAL_FILES% fichiers dans le JAR
    
    REM Vérification que config.properties est bien inclus
    jar tf "%JAR_NAME%" | findstr "config.properties" >nul
    if %errorlevel% equ 0 (
        echo ✓ config.properties inclus dans le JAR
    ) else (
        echo ⚠ Attention: config.properties ne semble pas être dans le JAR
    )
    
    echo.
    echo =^> %JAR_NAME% est prêt à être exécuté!
    echo Pour lancer l'application:
    echo   launch.bat
    echo.
    echo === Création du JAR terminée avec succès ===
    exit /b 0
    ) else if exist "%CLASS_DIR%\%CONFIG_FILE%" (
        del "%CLASS_DIR%\%CONFIG_FILE%" >nul
    )
    
    echo =^> Erreur lors de la création du JAR!
    echo === Création du JAR échouée ===
    pause
    exit /b 1

echo.