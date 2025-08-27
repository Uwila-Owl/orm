@echo off
cd Class
jar cfm "..\GenerateurUML.jar" "..\Manifest\MANIFEST.MF" *.class
cd ..
echo === Création du JAR terminée avec succès ===

