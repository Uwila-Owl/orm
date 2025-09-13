@echo off
setlocal EnableDelayedExpansion

:: Configuration
set "GIT_REPO=git@github.com:Uwila-Owl/orm.git"
set "DEFAULT_GIT_DIR=orm"
set "GIT_DIR=%DEFAULT_GIT_DIR%"

:main
  cls
  call :show_title
  call :show_git_status
  call :show_git_menu

  set /p choice=Votre choix [0-5] :
  echo.

  if "%choice%"=="1" (
    call :clone_repo_if_needed
    pause
    goto main
  )
  if "%choice%"=="2" (
    call :run_in_repo "git pull"
    pause
    goto main
  )
  if "%choice%"=="3" (
    call :run_in_repo "git add ."
    pause
    goto main
  )
  if "%choice%"=="4" (
    set /p msg=Message de commit : 
    call :run_in_repo "git commit -m \"%msg%\""
    pause
    goto main
  )
  if "%choice%"=="5" (
    call :run_in_repo "git push -u origin Test"
    pause
    goto main
  )
  if "%choice%"=="0" (
    echo Retour au menu principal...
    goto :eof
  )

  echo Choix invalide. Veuillez entrer un nombre entre 0 et 5.
  pause
  goto main

:: Affiche le titre
:show_title
  echo ===============================================
  echo           GENERATEUR UML - MENU GESTION GIT
  echo           Projet ORM Git Manager
  echo ===============================================
  echo.
  goto :eof

:: Vérifie l'état Git et compare local/remote
:show_git_status
  echo Etat Git :
  echo -----------------------------------------------
  if exist "%GIT_DIR%\.git\" (
    echo    [OK] Depot '%GIT_DIR%' present en local
    pushd "%GIT_DIR%" >nul 2>&1

    for /f "delims=" %%B in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set "BRANCH=%%B"
    git fetch --quiet
    for /f %%L in ('git rev-parse @ 2^>nul') do set "LOCAL=%%L"
    for /f %%R in ('git rev-parse @{u} 2^>nul') do set "REMOTE=%%R"
    for /f %%M in ('git merge-base @ @{u} 2^>nul') do set "BASE=%%M"

    popd >nul

    if "%LOCAL%"=="%REMOTE%" (
      echo    Branche %BRANCH% : a jour avec origin/%BRANCH%
    ) else if "%LOCAL%"=="%BASE%" (
      echo    Branche %BRANCH% : en retard (pull recommande)
    ) else if "%REMOTE%"=="%BASE%" (
      echo    Branche %BRANCH% : en avance (push recommande)
    ) else (
      echo    Branche %BRANCH% : divergence (pull + merge/rebase necessaire)
    )
  ) else (
    echo    [ERREUR] Depot '%GIT_DIR%' absent en local
  )
  echo -----------------------------------------------
  echo.
  goto :eof

:: Affiche le menu des actions
:show_git_menu
  echo Actions disponibles :
  echo -----------------------------------------------
  echo   1. Cloner le projet
  echo   2. Mettre a jour la branche Test
  echo   3. Ajouter tous les fichiers au git
  echo   4. Commiter avec un message
  echo   5. Pousser les modifications sur Github
  echo -----------------------------------------------
  echo   0. Retour au Menu Principal
  echo -----------------------------------------------
  echo.
  goto :eof

:: Clonage avec choix du dossier
:clone_repo_if_needed
  if not exist "%GIT_DIR%\.git\" (
    echo Le depot local n'existe pas.
    echo 1) Cloner dans le dossier courant
    echo 2) Cloner dans un nouveau dossier (par defaut: '%DEFAULT_GIT_DIR%')
    set /p dir_choice=Votre choix [1-2] :

    if "%dir_choice%"=="1" (
      set "GIT_DIR=."
    ) else (
      set /p input_dir=Nom du dossier (laisser vide pour '%DEFAULT_GIT_DIR%') :
      if "%input_dir%"=="" (
        set "GIT_DIR=%DEFAULT_GIT_DIR%"
      ) else (
        set "GIT_DIR=%input_dir%"
      )
    )

    git clone "%GIT_REPO%" "%GIT_DIR%" || (
      echo Erreur : clonage echoue !
      exit /b 1
    )
  ) else (
    echo Depot deja clone en local dans '%GIT_DIR%'
  )
  goto :eof

:: Exécute une commande Git dans le dossier du dépôt
:run_in_repo
  set "cmd=%~1"
  if not exist "%GIT_DIR%\.git\" (
    echo Depot non trouve. Veuillez d'abord cloner.
    goto :eof
  )
  pushd "%GIT_DIR%" >nul 2>&1
    %cmd%
  popd >nul
  goto :eof
