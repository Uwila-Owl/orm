@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM Script de démarrage UMLGen Client pour Windows
REM Menu interactif pour l'utilisateur final

:main_loop
cls
call :show_title
call :show_system_status
call :show_menu

set /p "choice=Votre choix: "
echo.

if "%choice%"=="1" (
    call :launch_application
    call :wait_for_user
    goto main_loop
)
if "%choice%"=="2" (
    echo Rafraîchissement des informations...
    timeout /t 1 /nobreak >nul
    goto main_loop
)
if "%choice%"=="0" (
    echo Au revoir!
    goto :eof
)

echo Choix invalide. Veuillez entrer 1, 2 ou 0.
call :wait_for_user
goto main_loop

:show_title
echo ╔══════════════════════════════════════════════════════════╗
echo ║                    GENERATEUR UML                        ║
echo ╠══════════════════════════════════════════════════════════╣
echo ║                   Version Client v1.0                    ║
echo ╚══════════════════════════════════════════════════════════╝
echo.
goto :eof

:show_system_status
echo État du système:
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

REM Vérification de Java
java -version >nul 2>&1
if errorlevel 1 (
    echo    ✗ Java: Non installé ou non accessible
    echo        ➤ Veuillez installer Java 24 ou plus récent
) else (
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr "version"') do (
        set "java_version=%%v"
        set "java_version=!java_version:"=!"
        echo    ✓ Java: version !java_version! détectée
    )
)

REM Vérification des bibliothèques JavaFX
if exist ".\JavaFX_Lib\" (
    set javafx_count=0
    for /r ".\JavaFX_Lib\" %%f in (*.jar) do (
        set /a javafx_count+=1
    )
    if !javafx_count! gtr 0 (
        echo    ✓ Bibliothèques JavaFX: !javafx_count! fichiers présents
    ) else (
        echo    ✗ Bibliothèques JavaFX: Dossier vide
    )
) else (
    echo    ✗ Bibliothèques JavaFX: Dossier manquant
)

REM Vérification des drivers externes
if exist ".\Ext_Driver\" (
    set driver_count=0
    for %%f in (".\Ext_Driver\*.jar") do (
        set /a driver_count+=1
    )
    if !driver_count! gtr 0 (
        echo    ✓ Drivers de base de données: !driver_count! fichiers présents
    ) else (
        echo    ⚠ Drivers de base de données: Aucun driver trouvé
    )
) else (
    echo    ✗ Drivers de base de données: Dossier manquant
)

REM Test de connexion internet
ping -n 1 8.8.8.8 >nul 2>&1
if errorlevel 1 (
    ping -n 1 google.com >nul 2>&1
    if errorlevel 1 (
        echo    ✗ Connexion internet: Non disponible
        echo        ➤ Une connexion internet peut être requise pour certaines fonctionnalités
    ) else (
        echo    ✓ Connexion internet: Disponible
    )
) else (
    echo    ✓ Connexion internet: Disponible
)

echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
goto :eof

:show_menu
echo Actions disponibles:
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo    1. Démarrer l'application UMLGen
echo    2. Rafraîchir les informations système
echo    0. Quitter
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
goto :eof

:launch_application
echo Lancement de UMLGen...
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

REM Vérification préalable de Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Erreur: Java n'est pas installé!
    echo Veuillez installer Java 24 ou plus récent et réessayer.
    goto :eof
)

REM Vérification du JAR principal
if not exist "GenerateurUML.jar" (
    echo ❌ Erreur: GenerateurUML.jar introuvable!
    echo Vérifiez que vous êtes dans le bon répertoire.
    goto :eof
)

REM Construction du module-path JavaFX
set "MODULE_PATH="
set "FIRST_JAR=true"
for /r ".\JavaFX_Lib\" %%f in (*.jar) do (
    if "!FIRST_JAR!"=="true" (
        set "MODULE_PATH=%%f"
        set "FIRST_JAR=false"
    ) else (
        set "MODULE_PATH=!MODULE_PATH!;%%f"
    )
)

if "%MODULE_PATH%"=="" (
    echo ❌ Erreur: Aucune bibliothèque JavaFX trouvée!
    goto :eof
)

REM Construction du classpath
set "CLASSPATH=.;GenerateurUML.jar"
for %%f in (".\Ext_Driver\*.jar") do (
    set "CLASSPATH=!CLASSPATH!;%%f"
)

echo Démarrage de l'application...
echo.

REM Lancement de l'application
call launch.bat

set "exit_code=%errorlevel%"
echo.
if %exit_code% equ 0 (
    echo ✅ Application fermée normalement
) else (
    echo ❌ Application fermée avec une erreur ^(code: %exit_code%^)
)
goto :eof

:wait_for_user
echo.
echo Appuyez sur une touche pour continuer...
pause >nul
goto :eof