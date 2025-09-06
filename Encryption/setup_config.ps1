# 1. Définir la clé AES
$env:APP_KEY="9582047136598302"
Write-Output "🔑 Clé AES définie dans APP_KEY"

# 2. Compiler EncryptConfig
javac EncryptConfig.java
Write-Output "✅ EncryptConfig compilé"

# 3. Générer les valeurs chiffrées et créer config.properties
java EncryptConfig | Out-File -Encoding utf8 config.properties
Write-Output "✅ Fichier config.properties généré"

# 4. Vérification
Write-Output "📄 Contenu du fichier config.properties :"
Get-Content config.properties
