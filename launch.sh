#!/bin/bash

# Script de lancement de GenerateurUML.jar avec JavaFX et PostgreSQL
# Utilise les modules JavaFX du dossier JavaFX_Lib et le driver PostgreSQL du dossier Ext_Driver

echo "=== Lancement de GenerateurUML ==="

# Définition des chemins
JAVAFX_LIB_DIR="./JavaFX_Lib/Lnx/lib"
EXT_DRIVER_DIR="./Ext_Driver"
JAR_NAME="GenerateurUML.jar"
MAIN_CLASS="FenetreLogin"

# Vérification de l'existence du JAR principal
if [ ! -f "$JAR_NAME" ]; then
    echo "Erreur: $JAR_NAME n'existe pas!"
    echo "Veuillez d'abord créer le JAR avec ./create_jar.sh"
    exit 1
fi

echo "=> JAR principal trouvé: $JAR_NAME"

# Vérification de l'existence du dossier JavaFX
if [ ! -d "$JAVAFX_LIB_DIR" ]; then
    echo "Erreur: Le dossier $JAVAFX_LIB_DIR n'existe pas!"
    exit 1
fi

# Vérification de l'existence du dossier des drivers
if [ ! -d "$EXT_DRIVER_DIR" ]; then
    echo "Erreur: Le dossier $EXT_DRIVER_DIR n'existe pas!"
    exit 1
fi

# Construction du module-path JavaFX
echo "Construction du module-path JavaFX..."
JAVAFX_JARS=$(find "$JAVAFX_LIB_DIR" -name "*.jar" -type f)
if [ -z "$JAVAFX_JARS" ]; then
    echo "Erreur: Aucun fichier .jar trouvé dans $JAVAFX_LIB_DIR!"
    exit 1
fi

MODULE_PATH=""
for jar in $JAVAFX_JARS; do
    if [ -z "$MODULE_PATH" ]; then
        MODULE_PATH="$jar"
    else
        MODULE_PATH="$MODULE_PATH:$jar"
    fi
done

echo "=> Module-path JavaFX: $MODULE_PATH"

# Construction du classpath avec les drivers externes
echo "Construction du classpath..."
CLASSPATH=".:$JAR_NAME"

# Ajout des drivers PostgreSQL
POSTGRES_JARS=$(find "$EXT_DRIVER_DIR" -name "*postgresql*.jar" -type f)
if [ -n "$POSTGRES_JARS" ]; then
    for jar in $POSTGRES_JARS; do
        CLASSPATH="$CLASSPATH:$jar"
        echo "=> Driver PostgreSQL ajouté: $(basename "$jar")"
    done
else
    echo "Attention: Aucun driver PostgreSQL trouvé dans $EXT_DRIVER_DIR"
fi

# Ajout des autres drivers du dossier Ext_Driver
OTHER_JARS=$(find "$EXT_DRIVER_DIR" -name "*.jar" -not -name "*postgresql*" -type f)
if [ -n "$OTHER_JARS" ]; then
    for jar in $OTHER_JARS; do
        CLASSPATH="$CLASSPATH:$jar"
        echo "=> Driver externe ajouté: $(basename "$jar")"
    done
fi

echo "Classpath complet: $CLASSPATH"
echo

# Définition des modules JavaFX à charger
MODULES="javafx.controls,javafx.fxml,javafx.graphics,javafx.swing"

# Commande de lancement
echo "Lancement de l'application..."
echo "Commande exécutée:"
echo "java --module-path \"$MODULE_PATH\" --add-modules $MODULES --enable-native-access=javafx.graphics -cp \"$CLASSPATH\" $MAIN_CLASS"
echo

# Exécution de l'application
java --module-path "$MODULE_PATH" \
     --add-modules $MODULES \
     --enable-native-access=javafx.graphics \
     -cp "$CLASSPATH" \
     $MAIN_CLASS

# Vérification du code de sortie
EXIT_CODE=$?
echo
if [ $EXIT_CODE -eq 0 ]; then
    echo "=== Application fermée normalement ==="
else
    echo "=== Application fermée avec le code d'erreur $EXIT_CODE ==="
fi

exit $EXIT_CODE
