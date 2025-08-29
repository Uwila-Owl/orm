@echo off
REM Forcer UTF-8
chcp 65001 >nuls

cd Class
jar cfm "..\GenerateurUML.jar" "..\Manifest\MANIFEST.MF" *.class
cd ..
echo === Création du JAR terminée avec succès ===

Pause