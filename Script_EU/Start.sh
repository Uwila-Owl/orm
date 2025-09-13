#!/bin/bash

# Script de démarrage UMLGen Client
# Menu interactif pour l'utilisateur final

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Fonction pour afficher le titre
show_title() {
    clear
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                    🚀 GENERATEUR UML                     ║${NC}"
    echo -e "${CYAN}╠══════════════════════════════════════════════════════════╣${NC}"
    echo -e "${CYAN}║                   Version Client v1.0                   ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
    echo
}

# Fonction pour tester la connexion internet
test_internet() {
    if ping -c 1 8.8.8.8 >/dev/null 2>&1 || ping -c 1 google.com >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Fonction pour afficher l'état du système
show_system_status() {
    echo -e "${BLUE}🔍 État du système:${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Vérification de Java
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | grep "version" | cut -d'"' -f2)
        JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
        
        # Conversion pour les versions comme "1.8.x" -> "8"
        if [ "$JAVA_MAJOR" = "1" ]; then
            JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f2)
        fi
        
        if [ "$JAVA_MAJOR" -ge 24 ]; then
            echo -e "   ${GREEN}✓${NC} Java: version $JAVA_VERSION (compatible)"
        elif [ "$JAVA_MAJOR" -gt 0 ]; then
            echo -e "   ${YELLOW}⚠${NC} Java: version $JAVA_VERSION (recommandé: Java 24+)"
        else
            echo -e "   ${RED}✗${NC} Java: version non détectable"
        fi
    else
        echo -e "   ${RED}✗${NC} Java: Non installé ou non accessible"
        echo -e "       ${YELLOW}➤ Veuillez installer Java 24 ou plus récent${NC}"
    fi
    
    # Vérification des bibliothèques JavaFX
    if [ -d "./JavaFX_Lib/Lnx" ]; then
        JAVAFX_COUNT=$(find ./JavaFX_Lib/Lnx -name "*.jar" -type f 2>/dev/null | wc -l)
        if [ $JAVAFX_COUNT -gt 0 ]; then
            echo -e "   ${GREEN}✓${NC} Bibliothèques JavaFX: $JAVAFX_COUNT fichiers présents"
        else
            echo -e "   ${RED}✗${NC} Bibliothèques JavaFX: Dossier vide"
        fi
    else
        echo -e "   ${RED}✗${NC} Bibliothèques JavaFX: Dossier manquant"
    fi
    
    # Vérification des drivers externes
    if [ -d "./Ext_Driver" ]; then
        DRIVER_COUNT=$(find ./Ext_Driver -name "*.jar" -type f 2>/dev/null | wc -l)
        if [ $DRIVER_COUNT -gt 0 ]; then
            echo -e "   ${GREEN}✓${NC} Drivers de base de données: $DRIVER_COUNT fichiers présents"
        else
            echo -e "   ${YELLOW}⚠${NC} Drivers de base de données: Aucun driver trouvé"
        fi
    else
        echo -e "   ${RED}✗${NC} Drivers de base de données: Dossier manquant"
    fi
    
    # Vérification de la connexion internet
    echo -n "   "
    if test_internet; then
        echo -e "${GREEN}✓${NC} Connexion internet: Disponible"
    else
        echo -e "${RED}✗${NC} Connexion internet: Non disponible"
        echo -e "       ${YELLOW}➤ Une connexion internet peut être requise pour certaines fonctionnalités${NC}"
    fi
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
}

# Fonction pour afficher le menu
show_menu() {
    echo -e "${YELLOW}🎯 Actions disponibles:${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "   ${CYAN}1.${NC} 🚀 Démarrer l'application UMLGen"
    echo -e "   ${CYAN}2.${NC} 🔄 Rafraîchir les informations système"
    echo -e "   ${CYAN}0.${NC} ❌ Quitter"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
}

# Fonction pour lancer l'application
launch_application() {
    echo -e "${BLUE}🚀 Lancement de UMLGen...${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Vérification préalable de Java
    if ! command -v java >/dev/null 2>&1; then
        echo -e "${RED}❌ Erreur: Java n'est pas installé!${NC}"
        echo -e "Veuillez installer Java 24 ou plus récent et réessayer."
        return 1
    fi
    
    # Vérification du JAR principal
    if [ ! -f "GenerateurUML.jar" ]; then
        echo -e "${RED}❌ Erreur: GenerateurUML.jar introuvable!${NC}"
        echo -e "Vérifiez que vous êtes dans le bon répertoire."
        return 1
    fi
    
    # Construction du module-path JavaFX
    JAVAFX_JARS=$(find "./JavaFX_Lib/Lnx" -name "*.jar" -type f 2>/dev/null)
    if [ -z "$JAVAFX_JARS" ]; then
        echo -e "${RED}❌ Erreur: Aucune bibliothèque JavaFX trouvée!${NC}"
        return 1
    fi
    
    MODULE_PATH=""
    for jar in $JAVAFX_JARS; do
        if [ -z "$MODULE_PATH" ]; then
            MODULE_PATH="$jar"
        else
            MODULE_PATH="$MODULE_PATH:$jar"
        fi
    done
    
    # Construction du classpath
    CLASSPATH=".:GenerateurUML.jar"
    
    # Ajout des drivers
    DRIVER_JARS=$(find "./Ext_Driver" -name "*.jar" -type f 2>/dev/null)
    for jar in $DRIVER_JARS; do
        CLASSPATH="$CLASSPATH:$jar"
    done
    
    echo "Démarrage de l'application..."
    echo
    
    # Lancement de l'application
    java --module-path "$MODULE_PATH" \
         --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing \
         --enable-native-access=javafx.graphics \
         -cp "$CLASSPATH" \
         FenetreLogin
    
    local exit_code=$?
    echo
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✅ Application fermée normalement${NC}"
    else
        echo -e "${RED}❌ Application fermée avec une erreur (code: $exit_code)${NC}"
    fi
    
    return $exit_code
}

# Fonction pour attendre une action utilisateur
wait_for_user() {
    echo
    echo -e "${CYAN}Appuyez sur [Entrée] pour continuer...${NC}"
    read
}

# Fonction principale
main() {
    while true; do
        show_title
        show_system_status
        show_menu
        
        echo -e -n "${YELLOW}👆 Votre choix: ${NC}"
        read choice
        echo
        
        case $choice in
            1)
                launch_application
                wait_for_user
                ;;
            2)
                echo -e "${BLUE}🔄 Rafraîchissement des informations...${NC}"
                sleep 1
                ;;
            0)
                echo -e "${GREEN}👋 Au revoir!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}❌ Choix invalide. Veuillez entrer 1, 2 ou 0.${NC}"
                wait_for_user
                ;;
        esac
    done
}

# Point d'entrée du script
main "$@"
