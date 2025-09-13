#!/bin/bash

# Script de génération de package client UMLGen
# Crée une archive tar.gz distribuable avec tout le nécessaire

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "=== Génération du package client UMLGen ==="
echo

# Définition des chemins
CLIENT_DIR="./End_User_UMLGen"
TEMP_DIR="$CLIENT_DIR/UMLGen"
JAR_FILE="GenerateurUML.jar"
JAVAFX_LIB_DIR="./JavaFX_Lib"
EXT_DRIVER_DIR="./Ext_Driver"
SCRIPT_EU_DIR="./Script_EU"
ARCHIVE_NAME="UMLGen_Client.tar.gz"

# Vérifications préalables
echo -e "${BLUE}🔍 Vérifications préalables...${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Vérification du JAR principal
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}❌ Erreur: $JAR_FILE n'existe pas!${NC}"
    echo "Veuillez d'abord créer le JAR avec ./create_jar.sh"
    exit 1
fi
echo -e "   ${GREEN}✓${NC} JAR principal: $JAR_FILE trouvé"

# Vérification du script Start.sh source
if [ ! -d "$SCRIPT_EU_DIR" ]; then
    echo -e "${RED}❌ Erreur: Le script sourc $SCRIPT_EU_DIRE n'existe pas!${NC}"
    echo "Veuillez créer le dossier Script_EU avec les fichiers de démarage"
    exit 1
fi
echo -e "   ${GREEN}✓${NC} Script démarage source: $SCRIPT_EU_DIR trouvé"

# Vérification des bibliothèques JavaFX
if [ ! -d "$JAVAFX_LIB_DIR" ]; then
    echo -e "${RED}❌ Erreur: Le dossier $JAVAFX_LIB_DIR n'existe pas!${NC}"
    exit 1
fi
JAVAFX_COUNT=$(find "$JAVAFX_LIB_DIR" -name "*.jar" -type f | wc -l)
if [ $JAVAFX_COUNT -eq 0 ]; then
    echo -e "${RED}❌ Erreur: Aucun fichier JAR trouvé dans $JAVAFX_LIB_DIR!${NC}"
    exit 1
fi
echo -e "   ${GREEN}✓${NC} Bibliothèques JavaFX: $JAVAFX_COUNT fichiers"

# Vérification des drivers externes
if [ ! -d "$EXT_DRIVER_DIR" ]; then
    echo -e "${RED}❌ Erreur: Le dossier $EXT_DRIVER_DIR n'existe pas!${NC}"
    exit 1
fi
DRIVER_COUNT=$(find "$EXT_DRIVER_DIR" -name "*.jar" -type f | wc -l)
if [ $DRIVER_COUNT -eq 0 ]; then
    echo -e "${YELLOW}⚠${NC} Attention: Aucun driver trouvé dans $EXT_DRIVER_DIR"
else
    echo -e "   ${GREEN}✓${NC} Drivers externes: $DRIVER_COUNT fichiers"
fi

echo
echo -e "${BLUE}📦 Préparation du package client...${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Nettoyage et création du répertoire temporaire
if [ -d "$CLIENT_DIR" ]; then
    echo "Suppression de l'ancien package..."
    rm -rf "$CLIENT_DIR"
fi

mkdir -p "$TEMP_DIR"
echo -e "   ${GREEN}✓${NC} Répertoire de travail créé: $TEMP_DIR"

# Copie du JAR principal
cp "$JAR_FILE" "$TEMP_DIR/"
echo -e "   ${GREEN}✓${NC} JAR principal copié"

# Copie des bibliothèques JavaFX
cp -r "$JAVAFX_LIB_DIR" "$TEMP_DIR/"
echo -e "   ${GREEN}✓${NC} Bibliothèques JavaFX copiées ($JAVAFX_COUNT fichiers)"

# Copie des drivers externes
cp -r "$EXT_DRIVER_DIR" "$TEMP_DIR/"
echo -e "   ${GREEN}✓${NC} Drivers externes copiés ($DRIVER_COUNT fichiers)"

# Copie de tous les scripts utilisateur final
SCRIPT_EU_DIR="./Script_EU"
if [ -d "$SCRIPT_EU_DIR" ]; then
    SCRIPT_COUNT=$(find "$SCRIPT_EU_DIR" -type f | wc -l)
    cp -r ./Script_EU/* "$TEMP_DIR"
    
    # Rendre tous les .sh exécutables
    find "$TEMP_DIR/Script_EU" -name "*.sh" -type f -exec chmod +x {} \;
    
    echo -e "   ${GREEN}✓${NC} Scripts utilisateur copiés : $SCRIPT_COUNT fichiers du dossier Script_EU"
    
    # Créer aussi une copie du Start.sh à la racine pour compatibilité
    if [ -f "$TEMP_DIR/Script_EU/Start.sh" ]; then
        cp "$TEMP_DIR/Script_EU/*.*" "$TEMP_DIR/"
        chmod +x "$TEMP_DIR/Start.sh"
        echo -e "   ${GREEN}✓${NC} Start.sh copié également à la racine"
    fi
else
    echo -e "   ${RED}❌${NC} Erreur: Le dossier $SCRIPT_EU_DIR n'existe pas!"
    exit 1
fi

# echo "Appuyer sur Entrée pour continuer..."
# read a

# Création de l'archive
echo
echo -e "${BLUE}📂 Création de l'archive...${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

cd "$CLIENT_DIR"
tar -czf "$ARCHIVE_NAME" UMLGen/
ARCHIVE_SIZE=$(ls -lh "$ARCHIVE_NAME" | awk '{print $5}')

echo -e "   ${GREEN}✓${NC} Archive créée: $CLIENT_DIR/$ARCHIVE_NAME (taille: $ARCHIVE_SIZE)"

# Nettoyage du répertoire temporaire
rm -rf UMLGen/
echo -e "   ${GREEN}✓${NC} Répertoire temporaire nettoyé"

# Résumé final
echo
echo -e "${GREEN}🎉 Package client généré avec succès!${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "   📦 Archive: ${CYAN}$CLIENT_DIR/$ARCHIVE_NAME${NC} ($ARCHIVE_SIZE)"
echo -e "   📋 Contenu:"
echo -e "      • GenerateurUML.jar"
echo -e "      • JavaFX_Lib/ ($JAVAFX_COUNT fichiers)"
echo -e "      • Ext_Driver/ ($DRIVER_COUNT fichiers)"
echo -e "      • Start.sh (menu interactif Linux)"
echo -e "      • Start.bat (menu interactif MS.Windows)"

echo
echo -e "${YELLOW}📖 Instructions pour l'utilisateur final:${NC}"
echo "   1. Extraire l'archive UMLGen_Client.tar.gz"
echo "   2. Aller dans le dossier UMLGen/"
echo "   3. Exécuter: ./Start.sh sous linux ou Start.bat sous Windows"
echo
echo "=== Génération terminée ==="

