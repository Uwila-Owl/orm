mode con: cols=120 lines=50
@echo off

REM Forcer UTF-8
chcp 65001 >nul
setlocal enabledelayedexpansion

rem on saute directement au menu principal
goto main

REM ===================== FONCTIONS D'AFFICHAGE ==========================
:show_title
cls
echo.
echo ================================================================
echo               GENERATEUR UML - MENU PRINCIPAL
echo ================================================================
echo                    Projet ORM Manager v8
echo ================================================================
echo.
goto :eof

:show_status
echo Etat actuel du projet:
echo ================================================================
if exist ".\End_User_UMLGen" (
    set "TAR_COUNT=0"
    for %%F in (".\End_User_UMLGen\*.tar.gz" ".\End_User_UMLGen\*.zip") do (
        if exist %%F (
            set /a TAR_COUNT+=1
            set "TAR_FILE=%%F"
        )
    )
    if !TAR_COUNT! gtr 0 (
        for %%A in ("!TAR_FILE!") do set "TAR_SIZE=%%~zA" & set "TAR_NAME=%%~nxA"
        echo    [OK] Version client: !TAR_NAME! [!TAR_SIZE! octets]
    ) else (
        echo    [WARNING] Version client: dossier present mais aucune archive
    )
) else (
    echo    [WARNING] Version client: aucune version generee
)
echo.
if exist ".\Script_EU" (
    set "SCRIPT_COUNT=0"
    for %%F in (".\Script_EU\*.sh" ".\Script_EU\*.bat") do (
        if exist %%F set /a SCRIPT_COUNT+=1
    )
    if !SCRIPT_COUNT! gtr 0 (
        echo    [OK] Scripts utilisateur final: !SCRIPT_COUNT! fichiers dans /Script_EU
    ) else (
        echo    [WARNING] Dossier present mais aucun script
    )
) else (
    echo    [WARNING] Scripts utilisateur final: dossier /Script_EU manquant
)
echo.

set "JAVA_VERSION_REQUIRED=24"

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "full_version=%%g"
)

if not defined full_version (
    echo Java non trouve.
    goto :end
)

REM Supprimer les guillemets de la version
set "full_version=%full_version:"=%"

REM Extraire les deux premiers chiffres de la version
for /f "delims=." %%a in ('echo %full_version%') do (
    set "major_version=%%a"
)

echo    [OK] Version de Java detectee : %full_version%
echo.

if "%major_version%"=="%JAVA_VERSION_REQUIRED%" (
    echo    [OK] Java version %JAVA_VERSION_REQUIRED% compatible.
) else (
    echo    [WARNING] Java incompatible => %full_version% Requis min. 24.x
)
:end

echo.
set "JAVA_COUNT=0" & for /r ".\Java_Code" %%F in (*.java) do set /a JAVA_COUNT+=1
if !JAVA_COUNT! gtr 0 (
    echo    [OK] Sources Java: !JAVA_COUNT! fichiers trouves
) else (
    echo    [ERROR] Sources Java: aucun fichier trouve
)
echo.
set "CLASS_COUNT=0" & for /r ".\Class" %%F in (*.class) do set /a CLASS_COUNT+=1
if !CLASS_COUNT! gtr 0 (
    echo    [OK] Classes compilees: !CLASS_COUNT! fichiers
) else (
    echo    [WARNING] Classes compilees: aucune [compilation necessaire]
)
echo.
if exist "GenerateurUML.jar" (
    for %%A in ("GenerateurUML.jar") do (
        set "JAR_SIZE=%%~zA" 
        set "JAR_DATE=%%~tA"
    )
    echo    [OK] JAR executable: GenerateurUML.jar [!JAR_SIZE! octets, !JAR_DATE!]
) else (
    echo    [WARNING] JAR executable: absent [creation necessaire]
)
echo.
set "JAVAFX_COUNT=0" & for /r ".\JavaFX_Lib"  %%F in (*.jar) do set /a JAVAFX_COUNT+=1
set "DRIVER_COUNT=0" & for /r ".\Ext_Driver" %%F in (*.jar) do set /a DRIVER_COUNT+=1
echo    [OK] Bibliotheques JavaFX: !JAVAFX_COUNT! fichiers
echo    [OK] Drivers externes   : !DRIVER_COUNT! fichiers
echo ================================================================
echo.
goto :eof

:show_menu
echo Actions disponibles:
echo ================================================================
echo    1. Compiler les sources Java [compile.bat]
echo    2. Creer le JAR executable [create_jar.bat]
echo    3. Lancer l’application [launch.bat]
echo ================================================================
echo    4. Workflow complet [1 - 2 - 3]
echo    5. Purger le projet [nettoyer Class + JAR]
echo    6. Generer package client [create_client.bat]
echo ================================================================
echo    7. Gestion Git
echo    8. Purge avancee [+ version client]
echo ================================================================
echo    9. Rafraichir l’état
echo    0. Quitter
echo ================================================================
echo.
goto :eof

:execute_script
set "script_name=%~1"
set "script_desc=%~2"
echo Execution: %script_desc%
echo ================================================================
if not exist "%script_name%" (
    echo ERREUR: Le script %script_name% n'existe pas!
    goto :eof
)
call "%script_name%"
set "exit_code=%errorlevel%"
if %exit_code%==0 (
    echo %script_desc% termine avec succes!
) else (
    echo %script_desc% a echoue (code %exit_code%)
)
goto :eof

:wait_for_user
echo.
echo Appuyez sur [Entree] pour continuer...
pause >nul
goto :eof

REM ===================== MENU PRINCIPAL ==============================
:main
:menu_loop
call :show_title
call :show_status
call :show_menu

set /p "choice=Votre choix: "
echo.
if "%choice%"=="1" (
    call :execute_script compile.bat "Compilation des sources Java"
    call :wait_for_user
    goto menu_loop
)
if "%choice%"=="2" (
    call :execute_script create_jar.bat "Creation du JAR executable"
    call :wait_for_user
    goto menu_loop
)
if "%choice%"=="3" (
    call :execute_script launch.bat "Lancement de l’application"
    call :wait_for_user
    goto menu_loop
)
if "%choice%"=="4" (
    echo Workflow complet...
    echo ================================================================
    call :execute_script compile.bat "Compilation des sources Java"
    if !errorlevel! equ 0 (
        call :execute_script create_jar.bat "Creation du JAR executable"
        if !errorlevel! equ 0 (
            call :execute_script launch.bat "Lancement de l’application"
        ) else (
            echo Workflow interrompu a l’etape de creation du JAR
        )
    ) else (
        echo Workflow interrompu a l’etape de compilation
    )
    call :wait_for_user
    goto menu_loop
)
if "%choice%"=="5" (
    echo Purge du projet...
    echo ================================================================
    if exist ".\Class" (
        set "COUNT=0" & for /r ".\Class" %%F in (*.class) do set /a COUNT+=1
        if !COUNT! gtr 0 (
            del /q /s ".\Class\*.class" >nul 2>&1
            echo [OK] !COUNT! fichiers .class supprimes
        ) else (
            echo Aucun fichier .class a supprimer
        )
    ) else (
        echo Dossier /Class inexistant
    )
    if exist "GenerateurUML.jar" (
        del "GenerateurUML.jar"
        echo [OK] GenerateurUML.jar supprime
    ) else (
        echo GenerateurUML.jar deja absent
    )
    echo Purge terminee! Projet remis a zero.
    call :wait_for_user
    goto menu_loop
)
if "%choice%"=="6" (
    call :execute_script create_client.bat "Generation du package client"
    call :wait_for_user
    goto menu_loop
)
if "%choice%"=="7" (
    call :execute_script git_menu.bat "Menu Git"
    call :wait_for_user
    goto menu_loop
)
if "%choice%"=="8" (
    echo Purge avancee du projet...
    echo ================================================================
    if exist ".\Class" (
        set "COUNT=0" & for /r ".\Class" %%F in (*.class) do set /a COUNT+=1
        if !COUNT! gtr 0 (
            del /q /s ".\Class\*.class" >nul 2>&1
            echo [OK] !COUNT! fichiers .class supprimes
        ) else (
            echo Aucun fichier .class a supprimer
        )
    ) else (
        echo Dossier /Class inexistant
    )
    if exist "GenerateurUML.jar" (
        del "GenerateurUML.jar"
        echo [OK] GenerateurUML.jar supprime
    ) else (
        echo GenerateurUML.jar deja absent
    )
    if exist ".\End_User_UMLGen" (
        set "COUNT=0" & for %%F in (".\End_User_UMLGen\*.tar.gz" ".\End_User_UMLGen\*.zip") do if exist %%F set /a COUNT+=1
        if !COUNT! gtr 0 (
            del /q ".\End_User_UMLGen\*.tar.gz" >nul 2>&1
            del /q ".\End_User_UMLGen\*.zip" >nul 2>&1
            echo [OK] !COUNT! archive[s] client supprimee[s]
        ) else (
            echo Aucune archive client a supprimer
        )
        dir /b ".\End_User_UMLGen" 2>nul | findstr . >nul
        if !errorlevel! neq 0 (
            rmdir ".\End_User_UMLGen"
            echo [OK] Dossier End_User_UMLGen vide supprime
        )
    ) else (
        echo Dossier End_User_UMLGen deja absent
    )
    echo Purge avancee terminee! Projet remis a zero.
    call :wait_for_user
    goto menu_loop
)
if "%choice%"=="9" (
    echo Rafraichissement de l’état...
    timeout /t 1 >nul
    goto menu_loop
)
if "%choice%"=="0" (
    echo Au revoir et bon developpement!
    pause >nul
    endlocal
    exit
)
echo Choix invalide. Veuillez entrer un nombre entre 0 et 9.
call :wait_for_user
goto menu_loop
