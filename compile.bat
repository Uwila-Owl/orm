@echo off
setlocal enabledelayedexpansion

echo ================================================
echo Script de compilation Java
echo ================================================

REM Définition des répertoires
set SOURCE_DIR=%~dp0Java_Code\
set CLASS_DIR=%~dp0Class
set JAVAFX_HOME=%~dp0JavaFX_Lib\
set LIBS=%JAVAFX_HOME%javafx.controls.jar;%JAVAFX_HOME%javafx.fxml.jar;%JAVAFX_HOME%javafx.graphics.jar 

REM Ajoutez d'autres bibliothèques si besoin

REM Vérification de l'existence du répertoire source
if not exist "%SOURCE_DIR%" (
    echo ERREUR: Le repertoire %SOURCE_DIR% n'existe pas!
    pause
    exit /b 1
)

REM Création du répertoire de destination s'il n'existe pas
if not exist "%CLASS_DIR%" (
    echo Creation du repertoire %CLASS_DIR%...
    mkdir "%CLASS_DIR%"
)

REM Suppression de tous les fichiers .class existants
echo Suppression des anciens fichiers .class...
del /q "%CLASS_DIR%\*.class" 2>nul
if exist "%CLASS_DIR%\*.class" (
    echo Suppression recursive des .class dans les sous-dossiers...
    for /r "%CLASS_DIR%" %%f in (*.class) do del "%%f"
)

REM Compilation des fichiers .java
echo Compilation des fichiers Java...
set JAVA_FILES_FOUND=0

REM Recherche des fichiers .java
for /r "%SOURCE_DIR%" %%f in (*.java) do (
    set JAVA_FILES_FOUND=1
    goto :compile
)

:compile
if %JAVA_FILES_FOUND%==0 (
    echo ATTENTION: Aucun fichier .java trouve dans %SOURCE_DIR%
    pause
    exit /b 1
)

REM Compilation avec javac

echo Source: "%SOURCE_DIR%*.java"
echo Class: "%CLASS_DIR%"


javac -d %CLASS_DIR% -cp %LIBS% %SOURCE_DIR%*.java

REM Vérification du succès de la compilation
if %errorlevel%==0 (
    echo ================================================
    echo COMPILATION REUSSIE!
    echo Les fichiers .class ont ete places dans %CLASS_DIR%
    echo ================================================
    
    REM Affichage des fichiers compilés
    echo Fichiers .class generes:
    dir /b "%CLASS_DIR%\*.class" 2>nul
    
) else (
    echo ================================================
    echo ERREUR DE COMPILATION!
    echo Veuillez verifier vos fichiers Java.
    echo ================================================
)

pause
