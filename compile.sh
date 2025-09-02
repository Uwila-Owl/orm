#!/bin/bash

# Script de compilation Java
# Compile les fichiers .java du dossier Java_Code et déplace les .class vers Class

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}=== Script de compilation Java ==="

# Définition des chemins
JAVA_SRC_DIR="./Java_Code"
CLASS_DEST_DIR="./Class"
JAVAFX_LIB_DIR="./JavaFX_Lib/Lnx/lib"

# Vérification de l'existence du dossier source
if [ ! -d "$JAVA_SRC_DIR" ]; then
    echo -e "${RED}✗${NC} Erreur: Le dossier $JAVA_SRC_DIR n'existe pas!"
    exit 1
fi

# Vérification de l'existence du dossier JavaFX
if [ ! -d "$JAVAFX_LIB_DIR" ]; then
    echo -e "${RED}✗${NC} Erreur: Le dossier $JAVAFX_LIB_DIR n'existe pas!"
    exit 1
fi

# Création du dossier de destination s'il n'existe pas
if [ ! -d "$CLASS_DEST_DIR" ]; then
    echo -e "${CYAN}Création du dossier $CLASS_DEST_DIR..."
    mkdir -p "$CLASS_DEST_DIR"
fi

# Suppression de tous les fichiers .class existants dans le dossier Class
echo -e "${CYAN}Nettoyage des anciens fichiers .class..."
find "$CLASS_DEST_DIR" -name "*.class" -type f -delete
echo -e "${CYAN}Anciens fichiers .class supprimés."

# Recherche et compilation des fichiers .java
echo -e "${CYAN}Recherche des fichiers .java dans $JAVA_SRC_DIR..."
JAVA_FILES=$(find "$JAVA_SRC_DIR" -name "*.java" -type f)

if [ -z "$JAVA_FILES" ]; then
    echo -e "${RED}✗${NC}Aucun fichier .java trouvé dans $JAVA_SRC_DIR"
    exit 1
fi

echo -e "${CYAN}🔄 Fichiers .java trouvés:"
echo "$JAVA_FILES"
echo

# Construction du classpath JavaFX
echo -e "${BLUE}🔄 Construction du classpath JavaFX..."
CLASSPATH=""
for jar in "$JAVAFX_LIB_DIR"/*.jar; do
    if [ -f "$jar" ]; then
        if [ -z "$CLASSPATH" ]; then
            CLASSPATH="$jar"
        else
            CLASSPATH="$CLASSPATH:$jar"
        fi
    fi
done

if [ -z "$CLASSPATH" ]; then
    echo -e "${YELLOW}⚠${NC} Attention: Aucun fichier .jar trouvé dans $JAVAFX_LIB_DIR"
    echo -e "${YELLOW}⚠${NC} Compilation sans JavaFX..."
else
    echo -e "${YELLOW}⚠${NC} Classpath JavaFX: $CLASSPATH"
fi
echo

# Compilation des fichiers Java
echo -e "${BLUE}🔄 Compilation en cours..."
if [ -z "$CLASSPATH" ]; then
    javac -d "$CLASS_DEST_DIR" $JAVA_FILES
else
    javac -cp "$CLASSPATH" -d "$CLASS_DEST_DIR" $JAVA_FILES
fi

# Vérification du statut de compilation
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Compilation réussie!"
    
    # Comptage des fichiers .class générés
    CLASS_COUNT=$(find "$CLASS_DEST_DIR" -name "*.class" -type f | wc -l)
    echo -e "${GREEN}✓$ $CLASS_COUNT fichiers .class générés dans $CLASS_DEST_DIR"
    
    echo -e "${GREEN}✓$ === Compilation terminée avec succès ==="
else
    echo -e "${RED}✗${NC} ✗ Erreur lors de la compilation!"
    echo -e "${RED}✗${NC} === Compilation échouée ==="
    exit 1
fi