# Projet_ORM
# Application de Modélisation UML/ERD

**Auteurs** : Léa MOULINNEUF, Eric NDIKUBWAYO, Lyna BOUZEFRANE  
**Année universitaire** : 2024–2025 – Licence 2  
**Projet** : Réalisation de programme

---

## Description

Cette application permet de créer des diagrammes **UML** et **ERD** de manière intuitive. Elle intègre la gestion de comptes utilisateurs, la sauvegarde de projets, l’import/export de diagrammes (XML/PNG) et une interface graphique riche basée sur JavaFX. Les données sensibles sont chiffrées (AES) et l’accès à la base de données PostgreSQL est sécurisé.

---

## Prérequis

- **Java** (version compatible, vérifiée automatiquement par les scripts de lancement)
- **JavaFX** (bibliothèques fournies dans le dossier `JavaFX Lib`)
- **PostgreSQL** (driver inclus)
- **Git** (optionnel, pour les hooks)

---

## Structure du projet

| Dossier | Description |
|--------|-------------|
| `Encryption` | Fichiers de configuration chiffrés (`config.properties`) et gestion des clés de chiffrement. |
| `Java Code` | Code source Java (logique métier, interface graphique, DAO). |
| `Backup` | Sauvegardes automatiques ou manuelles du projet. |
| `End user UMLGen` | Exécutables et scripts pour l’utilisateur final (déploiement). |
| `Ext driver` | Pilotes externes (ex. connecteur PostgreSQL). |
| `JavaFX Lib` | Bibliothèques JavaFX nécessaires à l’interface. |
| `Manifest` | Fichiers manifeste pour les archives JAR. |
| `Proguard70` | Outil et configurations pour l’obfuscation du code. |
| `Script_EU` | Scripts d’installation, mise à jour et lancement destinés aux utilisateurs. |

---

## Fichiers Java principaux

| Fichier | Rôle |
|---------|------|
| `ZoneModelisation.java` | Zone graphique de modélisation (gestion des entités, zoom, grille). |
| `BarreOutils.java` | Barre d’outils pour ajouter entités, attributs, relations. |
| `AttributDAO.java` | Accès aux données des attributs (CRUD). |
| `DiagramImporter.java` | Import de diagrammes depuis XML. |
| `DiagramExporter.java` | Export de diagrammes vers XML/PNG. |
| `InterfaceGenerateurUML.java` | Interface principale de l’application. |
| `NavigationMenu.java` | Menu de navigation (undo/redo, export, import). |
| `PanneauProprietes.java` | Panneau d’édition des propriétés d’une entité. |
| `MdpOublie.java` | Gestion de la récupération de mot de passe. |
| `EncryptConfig.java` | Chiffrement/déchiffrement des configurations (AES). |
| `EntiteDAO.java` | Accès aux données des entités. |
| `CryptoUtils.java` | Utilitaires de chiffrement. |
| `ConnexionBdd.java` | Gestion des connexions à la base de données. |
| `InsertionDonnees.java` | Insertion et chargement de données en base. |

---


## Lancement du programme

### Sous Linux
1. Placez-vous dans le dossier contenant `menu.sh`.
2. Rendez le script exécutable :  
   `bash`
   `chmod +x menu.sh`
3. Lancez l’application :
   
    `./menu.sh`

    En cas d’erreur, vérifiez la présence des bibliothèques JavaFX et l’intégrité du fichier config.properties.

### Sous Windows

1. Ouvrez l’invite de commandes dans le dossier contenant menu.bat.
2. Lancez l’application :

   `menu.bat`

    Si des erreurs surviennent, exécutez d’abord setup_config.ps1 (PowerShell) pour initialiser les configurations, puis relancez menu.bat.

    Assurez-vous que la variable d’environnement JAVA_HOME est correctement définie.


Utilisation de l’application
1. Création d’un compte

    Au premier lancement, créez un compte en remplissant les champs obligatoires : ID, Nom, Prénom, e-mail, mot de passe.

    Revenez à la page d’authentification, saisissez votre ID et mot de passe, puis validez (clic sur "OK" ou touche Entrée).

2. Démarrer un nouveau schéma

    Cliquez sur "Nouveau Schéma".

    En haut de la fenêtre, choisissez le type de diagramme : UML ou ERD.

3. Interface de modélisation

    Barre d’outils (gauche) : ajoutez des entités, des attributs, des clés primaires, etc.

    Panneau de propriétés (droite) : modifiez les éléments sélectionnés.

    Zone centrale : espace de travail où les blocs apparaissent.

        Clic gauche : déplacer un bloc.

        Clic droit : copier, couper, coller.

    Attention : lorsque vous créez deux blocs à la suite, ils se placent l’un sur l’autre. Pensez à les déplacer pour les rendre visibles.

4. Import / Export

    Utilisez le menu de navigation pour exporter votre diagramme au format XML ou PNG, ou importer un diagramme existant.

Dépendances externes (incluses dans le projet)

    postgresql-42.7.7.jar : pilote JDBC pour PostgreSQL.

    javafx-base.jar, javafx-controls.jar, etc. : bibliothèques JavaFX.

    gson-2.10.1.jar : manipulation JSON pour les imports/exports.

Notes supplémentaires

    Hooks Git : des scripts automatisés (ex. post-checkout, post-merge) sont présents pour exécuter des tâches lors des événements Git.

    ProGuard : le dossier Proguard70 contient les outils pour obfusquer le code avant distribution.

    Dépannage : si le programme ne se lance pas, vérifiez les logs de la base de données ou contactez les auteurs.

