#!/bin/bash

# Menu principal pour la gestion du projet GenerateurUML
# Permet de lancer facilement la compilation, création JAR et exécution

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
    echo -e "${CYAN}║              🚀 GENERATEUR UML - MENU PRINCIPAL          ║${NC}"
    echo -e "${CYAN}╠══════════════════════════════════════════════════════════╣${NC}"
    echo -e "${CYAN}║                    Projet ORM Manager v8                 ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
    echo
}

# Fonction pour afficher l'état des fichiers
show_status() {
    echo -e "${BLUE}📊 État actuel du projet:${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Vérification de la version client
    if [ -d "./End_User_UMLGen" ]; then
        TAR_COUNT=$(find ./End_User_UMLGen -name "*.tar.gz" -type f 2>/dev/null | wc -l)
        if [ $TAR_COUNT -gt 0 ]; then
            TAR_FILE=$(find ./End_User_UMLGen -name "*.tar.gz" -type f 2>/dev/null | head -1)
            TAR_SIZE=$(ls -lh "$TAR_FILE" | awk '{print $5}')
            TAR_NAME=$(basename "$TAR_FILE")
            echo -e "   ${GREEN}[OK]${NC} Version client: $TAR_NAME ($TAR_SIZE)"
        else
            echo -e "   ${YELLOW}[WARNING]${NC} Version client: Dossier présent mais aucune archive"
        fi
    else
        echo -e "   ${YELLOW}[WARNING]${NC} Version client: Aucune version générée"
    fi
    
    # Vérification des scripts utilisateur
    if [ -d "./Script_EU" ]; then
        SCRIPT_COUNT=$(find ./Script_EU -name "*.sh" -type f 2>/dev/null | wc -l)
        if [ $SCRIPT_COUNT -gt 0 ]; then
            echo -e "   ${GREEN}[OK]${NC} Scripts utilisateur final: $SCRIPT_COUNT fichiers dans /Script_EU"
        else
            echo -e "   ${YELLOW}[WARNING]${NC} Scripts utilisateur final: Dossier présent mais aucun script"
        fi
    else
        echo -e "   ${YELLOW}[WARNING]${NC} Scripts utilisateur final: Dossier /Script_EU manquant"
    fi
    
    # Vérification de Java
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | grep "version" | cut -d'"' -f2)
        JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
        
        # Conversion pour les versions comme "1.8.x" -> "8"
        if [ "$JAVA_MAJOR" = "1" ]; then
            JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f2)
        fi
        
        if [ "$JAVA_MAJOR" -ge 24 ]; then
            echo -e "   ${GREEN}[OK]${NC} Java: version $JAVA_VERSION (compatible)"
        elif [ "$JAVA_MAJOR" -gt 0 ]; then
            echo -e "   ${YELLOW}[WARNING]${NC} Java: version $JAVA_VERSION (recommandé: Java 24+)"
        else
            echo -e "   ${RED}[ERREUR]${NC} Java: version non détectable"
        fi
    else
        echo -e "   ${RED}[ERREUR]${NC} Java: Non installé ou non accessible"
    fi
    
    # Vérification des sources Java
    JAVA_COUNT=$(find ./Java_Code -name "*.java" -type f 2>/dev/null | wc -l)
    if [ $JAVA_COUNT -gt 0 ]; then
        echo -e "   ${GREEN}[OK]${NC} Sources Java: ${JAVA_COUNT} fichiers trouvés"
    else
        echo -e "   ${RED}[ERREUR]${NC} Sources Java: Aucun fichier trouvé"
    fi
    
    # Vérification des classes compilées
    CLASS_COUNT=$(find ./Class -name "*.class" -type f 2>/dev/null | wc -l)
    if [ $CLASS_COUNT -gt 0 ]; then
        echo -e "   ${GREEN}[OK]${NC} Classes compilées: ${CLASS_COUNT} fichiers"
    else
        echo -e "   ${RED}[WARNING]${NC} Classes compilées: Aucune (compilation nécessaire)"
    fi
    
    # Vérification du JAR
    if [ -f "GenerateurUML.jar" ]; then
        JAR_SIZE=$(ls -lh GenerateurUML.jar | awk '{print $5}')
        JAR_DATE=$(ls -l GenerateurUML.jar | awk '{print $6" "$7" "$8}')
        echo -e "   ${GREEN}[OK]${NC} JAR exécutable: GenerateurUML.jar (${JAR_SIZE}, ${JAR_DATE})"
    else
        echo -e "   ${RED}[WARNING]${NC} JAR exécutable: Absent (création nécessaire)"
    fi
    
    # Vérification des dépendances
    JAVAFX_COUNT=$(find ./JavaFX_Lib -name "*.jar" -type f 2>/dev/null | wc -l)
    DRIVER_COUNT=$(find ./Ext_Driver -name "*.jar" -type f 2>/dev/null | wc -l)
    echo -e "   ${GREEN}[OK]${NC} Bibliothèques JavaFX: ${JAVAFX_COUNT} fichiers"
    echo -e "   ${GREEN}[OK]${NC} Drivers externes: ${DRIVER_COUNT} fichiers"
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
}

# Fonction pour afficher le menu
show_menu() {
    echo -e "${YELLOW}🔧 Actions disponibles:${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "   ${CYAN}1.${NC} 🔨 Compiler les sources Java (compile.sh)"
    echo -e "   ${CYAN}2.${NC} 📦 Créer le JAR exécutable (create_jar.sh)"
    echo -e "   ${CYAN}3.${NC} 🚀 Lancer l'application (launch.sh)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "   ${CYAN}4.${NC} ⚡ Workflow complet (1 → 2 → 3)"
    echo -e "   ${CYAN}5.${NC} 🧹 Purger le projet (nettoyer Class + JAR)"
    echo -e "   ${CYAN}6.${NC} 📦 Générer package client (create_client.sh)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "   ${CYAN}7.${NC} 🔄 Gestion Git" 
    echo -e "   ${CYAN}8.${NC} 🗑️ Purge avancée (+ version client)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "   ${CYAN}9.${NC} 📊 Rafraîchir l'état"
    echo -e "   ${CYAN}0.${NC} ❌ Quitter"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
}

# Fonction pour exécuter un script avec gestion d'erreur
execute_script() {
    local script_name=$1
    local script_description=$2
    
    echo -e "${BLUE}🔄 Exécution: ${script_description}${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    if [ ! -f "$script_name" ]; then
        echo -e "${RED}❌ Erreur: Le script ${script_name} n'existe pas!${NC}"
        return 1
    fi
    
    if [ ! -x "$script_name" ]; then
        echo -e "${YELLOW}[WARNING] Le script ${script_name} n'est pas exécutable. Correction...${NC}"
        chmod +x "$script_name"
    fi
    
    "./$script_name"
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✅ ${script_description} terminé avec succès!${NC}"
    else
        echo -e "${RED}❌ ${script_description} a échoué (code: ${exit_code})${NC}"
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
        show_status
        show_menu
        
        echo -e -n "${YELLOW}👆 Votre choix: ${NC}"
        read choice
        echo
        
        case $choice in
            1)
                execute_script "compile.sh" "Compilation des sources Java"
                wait_for_user
                ;;
            2)
                execute_script "create_jar.sh" "Création du JAR exécutable"
                wait_for_user
                ;;
            3)
                execute_script "launch.sh" "Lancement de l'application"
                wait_for_user
                ;;
            4)
                echo -e "${BLUE}🔄 Exécution du workflow complet...${NC}"
                echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                
                execute_script "compile.sh" "Compilation des sources Java"
                if [ $? -eq 0 ]; then
                    execute_script "create_jar.sh" "Création du JAR exécutable"
                    if [ $? -eq 0 ]; then
                        execute_script "launch.sh" "Lancement de l'application"
                    else
                        echo -e "${RED}❌ Workflow interrompu à l'étape de création du JAR${NC}"
                    fi
                else
                    echo -e "${RED}❌ Workflow interrompu à l'étape de compilation${NC}"
                fi
                wait_for_user
                ;;
            5)
                echo -e "${BLUE}🧹 Purge du projet...${NC}"
                echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                
                # Suppression des fichiers .class
                if [ -d "./Class" ]; then
                    CLASS_COUNT_BEFORE=$(find ./Class -name "*.class" -type f 2>/dev/null | wc -l)
                    if [ $CLASS_COUNT_BEFORE -gt 0 ]; then
                        find ./Class -name "*.class" -type f -delete
                        echo -e "   ${GREEN}[OK]${NC} ${CLASS_COUNT_BEFORE} fichiers .class supprimés du dossier /Class"
                    else
                        echo -e "   ${YELLOW}ℹ${NC} Aucun fichier .class à supprimer dans /Class"
                    fi
                else
                    echo -e "   ${YELLOW}ℹ${NC} Dossier /Class inexistant"
                fi
                
                # Suppression du JAR
                if [ -f "GenerateurUML.jar" ]; then
                    rm "GenerateurUML.jar"
                    echo -e "   ${GREEN}[OK]${NC} GenerateurUML.jar supprimé"
                else
                    echo -e "   ${YELLOW}ℹ${NC} GenerateurUML.jar déjà absent"
                fi
                
                echo -e "${GREEN}🧹 Purge terminée! Projet remis à zéro.${NC}"
                wait_for_user
                ;;
            6)
                execute_script "create_client.sh" "Génération du package client"
                wait_for_user
                ;;
            7)
                execute_script "git_menu.sh" "Menu Git"
                wait_for_user
                ;;
            8)
                echo -e "${BLUE}🗑️ Purge avancée du projet...${NC}"
                echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                
                # Suppression des fichiers .class
                if [ -d "./Class" ]; then
                    CLASS_COUNT_BEFORE=$(find ./Class -name "*.class" -type f 2>/dev/null | wc -l)
                    if [ $CLASS_COUNT_BEFORE -gt 0 ]; then
                        find ./Class -name "*.class" -type f -delete
                        echo -e "   ${GREEN}[OK]${NC} ${CLASS_COUNT_BEFORE} fichiers .class supprimés du dossier /Class"
                    else
                        echo -e "   ${YELLOW}ℹ${NC} Aucun fichier .class à supprimer dans /Class"
                    fi
                else
                    echo -e "   ${YELLOW}ℹ${NC} Dossier /Class inexistant"
                fi
                
                # Suppression du JAR
                if [ -f "GenerateurUML.jar" ]; then
                    rm "GenerateurUML.jar"
                    echo -e "   ${GREEN}[OK]${NC} GenerateurUML.jar supprimé"
                else
                    echo -e "   ${YELLOW}ℹ${NC} GenerateurUML.jar déjà absent"
                fi
                
                # Suppression des archives client
                if [ -d "./End_User_UMLGen" ]; then
                    TAR_COUNT_BEFORE=$(find ./End_User_UMLGen -name "*.tar.gz" -type f 2>/dev/null | wc -l)
                    if [ $TAR_COUNT_BEFORE -gt 0 ]; then
                        find ./End_User_UMLGen -name "*.tar.gz" -type f -delete
                        echo -e "   ${GREEN}[OK]${NC} ${TAR_COUNT_BEFORE} archive(s) client supprimée(s)"
                    else
                        echo -e "   ${YELLOW}ℹ${NC} Aucune archive client à supprimer"
                    fi
                    
                    # Suppression du dossier s'il est vide
                    if [ -z "$(ls -A ./End_User_UMLGen 2>/dev/null)" ]; then
                        rmdir ./End_User_UMLGen
                        echo -e "   ${GREEN}[OK]${NC} Dossier End_User_UMLGen vide supprimé"
                    fi
                else
                    echo -e "   ${YELLOW}ℹ${NC} Dossier End_User_UMLGen déjà absent"
                fi
                
                echo -e "${GREEN}🗑️ Purge avancée terminée! Projet complètement remis à zéro.${NC}"
                wait_for_user
                ;;
            9)
                echo -e "${BLUE}🔄 Rafraîchissement de l'état...${NC}"
                sleep 1
                ;;
            0)
                echo -e "${GREEN}👋 Au revoir et bon développement!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}❌ Choix invalide. Veuillez entrer un nombre entre 0 et 9.${NC}"
                wait_for_user
                ;;
        esac
    done
}

# Point d'entrée du script
main "$@"
