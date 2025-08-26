#!/bin/bash

# Script de création du JAR exécutable GenerateurUML.jar
# Utilise les fichiers .class du dossier Class et le manifest du dossier Manifest

echo "=== Script de création JAR GenerateurUML ==="

# Définition des chemins
CLASS_DIR="./Class"
MANIFEST_DIR="./Manifest"
JAR_NAME="GenerateurUML.jar"
OUTPUT_DIR="."

# Vérification de l'existence du dossier Class
if [ ! -d "$CLASS_DIR" ]; then
    echo "Erreur: Le dossier $CLASS_DIR n'existe pas!"
    echo "Veuillez d'abord compiler les fichiers Java avec ./compile.sh"
    exit 1
fi

# Vérification qu'il y a des fichiers .class
CLASS_COUNT=$(find "$CLASS_DIR" -name "*.class" -type f | wc -l)
if [ $CLASS_COUNT -eq 0 ]; then
    echo "Erreur: Aucun fichier .class trouvé dans $CLASS_DIR!"
    echo "Veuillez d'abord compiler les fichiers Java avec ./compile.sh"
    exit 1
fi

echo "✓ $CLASS_COUNT fichiers .class trouvés dans $CLASS_DIR"

# Vérification de l'existence du dossier Manifest
if [ ! -d "$MANIFEST_DIR" ]; then
    echo "Erreur: Le dossier $MANIFEST_DIR n'existe pas!"
    exit 1
fi

# Recherche du fichier manifest
MANIFEST_FILE=""
for file in "$MANIFEST_DIR"/*; do
    if [ -f "$file" ]; then
        # Vérifier si c'est un fichier manifest valide (contient Main-Class)
        if grep -q "Main-Class:" "$file" 2>/dev/null; then
            MANIFEST_FILE="$file"
            break
        fi
    fi
done

if [ -z "$MANIFEST_FILE" ]; then
    echo "Erreur: Aucun fichier manifest valide trouvé dans $MANIFEST_DIR!"
    echo "Le manifest doit contenir au minimum une ligne 'Main-Class: nom.de.la.classe'"
    exit 1
fi

echo "✓ Fichier manifest trouvé: $(basename "$MANIFEST_FILE")"

# Affichage du contenu du manifest
echo "Contenu du manifest:"
echo "-------------------"
cat "$MANIFEST_FILE"
echo "-------------------"
echo

# Suppression de l'ancien JAR s'il existe
if [ -f "$OUTPUT_DIR/$JAR_NAME" ]; then
    echo "Suppression de l'ancien $JAR_NAME..."
    rm "$OUTPUT_DIR/$JAR_NAME"
fi

# Création du JAR
echo "Création du JAR $JAR_NAME..."
cd "$CLASS_DIR"
jar cfm "../$JAR_NAME" "../$MANIFEST_FILE" *

# Vérification du succès de la création
if [ $? -eq 0 ] && [ -f "../$JAR_NAME" ]; then
    cd ..
    JAR_SIZE=$(ls -lh "$JAR_NAME" | awk '{print $5}')
    echo "=> JAR créé avec succès: $JAR_NAME (taille: $JAR_SIZE)"
    
    # Vérification du contenu du JAR
    echo
    echo "Contenu du JAR:"
    echo "---------------"
    jar tf "$JAR_NAME" | head -20
    TOTAL_FILES=$(jar tf "$JAR_NAME" | wc -l)
    if [ $TOTAL_FILES -gt 20 ]; then
        echo "... et $(($TOTAL_FILES - 20)) autres fichiers"
    fi
    echo "---------------"
    echo "Total: $TOTAL_FILES fichiers dans le JAR"
    
    echo
    echo "=> $JAR_NAME est prêt à être exécuté!"
    echo "Pour lancer l'application:"
    echo "  java -jar $JAR_NAME"
    echo
    echo "=== Création du JAR terminée avec succès ==="
else
    cd ..
    echo "=> Erreur lors de la création du JAR!"
    echo "=== Création du JAR échouée ==="
    exit 1
fi
