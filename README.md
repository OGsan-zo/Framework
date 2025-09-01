# FRAMEWORK

## Utilisation
1. Créez un package pour vos Models, par exemple : Controller
2. Corrigez la valeur de `base_package` dans `web.xml` pour correspondre au nom de votre package
3. Annoter vos éléments :
    - Models avec `@Controller`
    - Méthodes avec `@Url`, `@Get`, `@Post`
    - Arguments de fonction :
        - `@ModelParam` pour vos classes personnelles
        - `@Param` pour les types de base : int, String, etc.
4. Types de retour fonctionnels :
    - STRING
    - MODELVIEW
5. Ajouter `FileUpload` pour gérer l’ajout de fichiers dans vos classes


## ETAPES SPRINT : 
### Sprint 0
- Création des branches Git : main, sprint0-ETU
- Utilisation des pull requests dans Git
- Création des dossiers de travail et de déploiement
- Création du Controller `FrontController` avec les méthodes :
    - `processRequest`
    - `doGET`
    - `doPost`

### Sprint 1
- Affichage du lien inséré par l’utilisateur :
    ```java
    Out.println("Lien inséré par l'utilisateur");
    ```

### Sprint 2
- Création des annotations :
    - `@Controller` et `@Get`
- Insertion des annotations sur les Models
- Création de la classe `Mapping` contenant :
    - `String className`
    - `String methodName`
- Ajout d’un `HashMap` dans `FrontController` :
    - Clé : String
    - Valeur : Mapping
- Création de la fonction `init()` dans `FrontController` :
    - Initialise un scanner de Controllers

### Sprint 3
- Exécution des méthodes retournant un `String`

### Sprint 4
- Création de la classe `ModelView` :
    - `HashMap<String, Object> data` : nom et valeur
    - `String Url`
    - Fonction `addObject()`
- Gestion des types de retour :
    - Type `String` : affichage direct de la valeur sur la page
    - Type `ModelView` : redirection vers la view via `RequestDispatcher` :
        - `request.setAttributes(String value, Object data)`
        - `request.forward()`

### Sprint 5
- Gestion des exceptions :

**Lors du BUILD :**
- Scanner le package pour vérifier son existence
- Afficher les erreurs (`printStackTrace`) pour les classes ayant une URL dupliquée

**Lors des REQUEST :**
- Type de retour : afficher un message si le type de retour est introuvable
- Erreur 404 si aucune page correspondante n’est trouvée pour la méthode de l’utilisateur

### Sprint 6
- Gestion des paramètres pour formulaire :
    - Annotation des paramètres entrants de types de base avec `@Param`
      

- Sprint 9 : 
    - RESTAPI : 
        - Creation annotation RestApi 
        - Transformation en JSON du resultat attendue 
        
- Sprint 10 : 
    - Ajout annotation URL : 
    - Verification des methodes et d'utilisation des annotations
 
- Sprint 11 :
    - Gestion et apparition des erreurs

- Sprint 12 :
    - Ajout class FileUpload
    - Ajout d'upload fichier 
