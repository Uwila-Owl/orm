@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM Script de lancement de GenerateurUML.jar avec JavaFX et PostgreSQL (Windows)
REM Utilise les modules JavaFX du dossier JavaFX_Lib et le driver PostgreSQL du dossier Ext_Driver

echo === Lancement de GenerateurUML ===

REM Définition des chemins
set "JAVAFX_LIB_DIR=.\JavaFX_Lib\Win\lib"
set "EXT_DRIVER_DIR=.\Ext_Driver"
set "JAR_NAME=GenerateurUML.jar"
set "MAIN_CLASS=InterfaceGenerateurUML"

REM Vérification de l'existence du JAR principal
if not exist "%JAR_NAME%" (
    echo Erreur: %JAR_NAME% n'existe pas!
    echo Veuillez d'abord créer le JAR avec create_jar.bat
    pause
    exit /b 1
)

echo ✓ JAR principal trouvé: %JAR_NAME%

REM Vérification de l'existence du dossier JavaFX
if not exist "%JAVAFX_LIB_DIR%" (
    echo Erreur: Le dossier %JAVAFX_LIB_DIR% n'existe pas!
    pause
    exit /b 1
)

REM Vérification de l'existence du dossier des drivers
if not exist "%EXT_DRIVER_DIR%" (
    echo Erreur: Le dossier %EXT_DRIVER_DIR% n'existe pas!
    pause
    exit /b 1
)

REM Construction du module-path JavaFX
echo Construction du module-path JavaFX...
set "MODULE_PATH="
set "FIRST_JAR=true"

for %%f in ("%JAVAFX_LIB_DIR%\*.jar") do (
    if "!FIRST_JAR!"=="true" (
        set "MODULE_PATH=%%f"
        set "FIRST_JAR=false"
    ) else (
        set "MODULE_PATH=!MODULE_PATH!;%%f"
    )
)

if "%MODULE_PATH%"=="" (
    echo Erreur: Aucun fichier .jar trouvé dans %JAVAFX_LIB_DIR%!
    pause
    exit /b 1
)

echo ✓ Module-path JavaFX: %MODULE_PATH%

REM Construction du classpath avec les drivers externes
echo Construction du classpath...
set "CLASSPATH=.;%JAR_NAME%"

REM Ajout des drivers PostgreSQL
set "POSTGRES_FOUND=false"
for %%f in ("%EXT_DRIVER_DIR%\*postgresql*.jar") do (
    set "CLASSPATH=!CLASSPATH!;%%f"
    echo ✓ Driver PostgreSQL ajouté: %%~nxf
    set "POSTGRES_FOUND=true"
)

if "%POSTGRES_FOUND%"=="false" (
    echo Attention: Aucun driver PostgreSQL trouvé dans %EXT_DRIVER_DIR%
)

REM Ajout des autres drivers du dossier Ext_Driver
for %%f in ("%EXT_DRIVER_DIR%\*.jar") do (
    set "FILENAME=%%~nxf"
    echo !FILENAME! | findstr /i "postgresql" >nul
    if errorlevel 1 (
        set "CLASSPATH=!CLASSPATH!;%%f"
        echo ✓ Driver externe ajouté: %%~nxf
    )
)

echo Classpath complet: %CLASSPATH%
echo.

REM Définition des modules JavaFX à charger
set "MODULES=javafx.controls,javafx.fxml,javafx.graphics"

REM Commande de lancement
echo Lancement de l'application...
echo Commande exécutée:
echo java --module-path ".\JavaFX_Lib\Win\lib" --add-modules %MODULES% -Djava.library.path=.\JavaFX_Lib\Win\bin --enable-native-access=javafx.graphics -Dprism.order=sw -cp "%CLASSPATH%" %MAIN_CLASS%
echo.

REM Exécution de l'application

REM java --module-path "%MODULE_PATH%" --add-modules %MODULES% --enable-native-access=javafx.graphics -cp "%CLASSPATH%" --add-opens javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED -Dprism.order=es2 -Dprism.verbose=true %MAIN_CLASS%

java ^
--module-path ".\JavaFX_Lib\Win\lib" ^
--add-modules %MODULES% ^
-Djava.library.path=.\JavaFX_Lib\Win\bin ^
--enable-native-access=javafx.graphics ^
-Dprism.order=sw ^
-cp ".;GenerateurUML.jar;.\Ext_Driver\postgresql-42.7.7.jar" ^
InterfaceGenerateurUML


REM Vérification du code de sortie
set "EXIT_CODE=%ERRORLEVEL%"
echo.
if %EXIT_CODE% equ 0 (
    echo === Application fermée normalement ===
) else (
    echo === Application fermée avec le code d'erreur %EXIT_CODE% ===
)

echo.
echo Appuyez sur une touche pour fermer cette fenêtre...
pause >nul
exit /b %EXIT_CODE%