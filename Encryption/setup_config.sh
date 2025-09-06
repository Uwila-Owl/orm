#!/bin/bash
set -e

# 1. Définir la clé AES (16 caractères exactement)
export APP_KEY="9582047136598302"
echo "🔑 Clé AES définie dans APP_KEY"

# 2. Compiler EncryptConfig
javac EncryptConfig.java
echo "✅ EncryptConfig compilé"

# 3. Générer les valeurs chiffrées et créer config.properties
java EncryptConfig > config.properties
echo "✅ Fichier config.properties généré"

# 4. Vérification
echo "📄 Contenu du fichier config.properties :"
cat config.properties
