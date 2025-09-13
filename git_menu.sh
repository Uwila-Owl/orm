#!/bin/bash

# Dépôt Git ciblé
GIT_REPO="git@github.com:Uwila-Owl/orm.git"
DEFAULT_GIT_DIR="orm"

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Fonction pour le clonage avec choix du dossier seulement la première fois
clone_repo_if_needed() {
    if [ ! -d "$GIT_DIR/.git" ]; then
        echo -e "${CYAN}Le dépôt local n'existe pas.${NC}"
        echo "Voulez-vous :"
        echo "1) Cloner dans le dossier courant"
        echo "2) Cloner dans un nouveau dossier (par défaut: '$DEFAULT_GIT_DIR')"
        echo -n "Votre choix [1-2] : "
        read dir_choice

        if [ "$dir_choice" = "1" ]; then
            GIT_DIR="."
        else
            echo -n "Nom du dossier (laisser vide pour '$DEFAULT_GIT_DIR') : "
            read input_dir
            GIT_DIR="${input_dir:-$DEFAULT_GIT_DIR}"
        fi

        git clone "$GIT_REPO" "$GIT_DIR" || { echo -e "${RED}❌ Clonage échoué !${NC}"; exit 1; }
    else
        echo -e "${GREEN}✅ Dépôt déjà cloné en local dans '$GIT_DIR'.${NC}"
    fi
}

# Fonction pour afficher le titre
show_title() {
    clear
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║              GENERATEUR UML - MENU GESTION GIT           ║${NC}"
    echo -e "${CYAN}╠══════════════════════════════════════════════════════════╣${NC}"
    echo -e "${CYAN}║                  Projet ORM Git Manager                  ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
    echo
}

# Fonction pour afficher l'état Git
show_git_status() {
    echo -e "${YELLOW}📊 État Git:${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if [ -d "$GIT_DIR/.git" ]; then
        echo -e "   ${GREEN}✅${NC} Dépôt '${GIT_DIR}' présent en local"
    else
        echo -e "   ${RED}❌${NC} Dépôt '${GIT_DIR}' absent en local"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo
        return
    fi

    pushd "$GIT_DIR" >/dev/null || return

    BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
    git fetch --quiet

    LOCAL=$(git rev-parse @ 2>/dev/null)
    REMOTE=$(git rev-parse "@{u}" 2>/dev/null)
    BASE=$(git merge-base @ "@{u}" 2>/dev/null)

    popd >/dev/null 

    if [ "$LOCAL" = "$REMOTE" ]; then
        echo -e "   ${GREEN}✓${NC} Branche ${CYAN}${BRANCH}${NC} : à jour avec origin/${BRANCH}"
    elif [ "$LOCAL" = "$BASE" ]; then
        echo -e "   ${YELLOW}⬇${NC} Branche ${CYAN}${BRANCH}${NC} : en retard (pull recommandé)"
    elif [ "$REMOTE" = "$BASE" ]; then
        echo -e "   ${YELLOW}⬆${NC} Branche ${CYAN}${BRANCH}${NC} : en avance (push recommandé)"
    else
        echo -e "   ${RED}⚠${NC} Branche ${CYAN}${BRANCH}${NC} : divergence (pull + merge/rebase nécessaire)"
    fi

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
}

# Fonction pour afficher le menu Git
show_git_menu() {
    echo -e "${YELLOW}🔧 Actions disponibles:${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "   ${CYAN}1.${NC} 🗂 Cloner le projet"
    echo -e "   ${CYAN}2.${NC} 🔄 Mettre à jour la branche Test"
    echo -e "   ${CYAN}3.${NC} ➕ Ajouter tous les fichiers au git"
    echo -e "   ${CYAN}4.${NC} 📝 Commiter avec un message"
    echo -e "   ${CYAN}5.${NC} 🚀 Pousser les modifications sur le github"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "   ${CYAN}0.${NC} ⬅ Retour au Menu Principal"
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
        echo -e "${YELLOW}⚠ Le script ${script_name} n'est pas exécutable. Correction...${NC}"
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
        show_git_status
        show_git_menu
        
        echo -e -n "${YELLOW}👆 Votre choix: ${NC}"
        read choice
        echo
        
        case $choice in
            1) 
                clone_repo_if_needed
                wait_for_user
                ;;
            2) 
                git pull
                wait_for_user
                ;;
            3) 
                git add .
                wait_for_user
                ;;
            4) 
               echo -n "Message de commit: "
               read msg
               git commit -m "$msg"
               wait_for_user
               ;;
            5) 
                git push -u origin Test  
                wait_for_user
                ;;
            0)
                echo -e "${GREEN}⬅ Retour au menu principal...${NC}"
                return 0
                ;;
            *) 
                echo -e "${RED}❌ Choix invalide. Veuillez entrer un nombre entre 0 et 5.${NC}"
                wait_for_user
                ;;
        esac
    done
}

# Point d'entrée du script
GIT_DIR="$DEFAULT_GIT_DIR"
main "$@"
