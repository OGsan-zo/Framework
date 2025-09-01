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
- Sprint 0 : 
    - Creation de branche de GIT : main , sprint0-ETU 
    - Utilisation de pullRequest dans git
    - Creation des dossiers de travail et de deploiement 
    - Creation de Controller FrontController : 
        - Avec les methodes : 
            - processRequest 
            - doGET 
            - doPost 
- Sprint 1 : 
    - Affciaher le lien inserer : 
        - Out.println("Lien inserer par l'utilisateur")

- Sprint 2 : 
    - Creation des  Annotations :
        - Controller et Get 
    - Insertion des annotations sur les models 
    - Creation de la classe Mapping : 
        - Contenant : 
            - String className 
            - String methodName 
    - Inserer un HASHMAP dans le FrontController : 
        - HashMap : 
            - String 
            - Class Mapping 
    - Creer une fonction dans le FrontController : 
        - init() : 
            - Creer un Scanner de Controller ; 
        
- Sprint 3 : 
    - Executer des methode retournant le String 
- Sprint 4 : 
    - Creation de la classe ModelView : 
        - HashMap <String , Object> data   : nom , valeur 
        - String Url 
        - Une fonction addObject( )
    - Gerer les types de retour :
        - Type String : On affiche seulement la valeur sur la page d'affichage 
        - Type ModeView : On renvoie l'utilisateur dans l'URL de la view par la methode ReqeuestDispatcher : 
            - request.setAttributes(String value , Object data) 
        - Redirection vers la view par request.forward()

- Sprint 5 : 
    - Gestion des Exceptions : 
        - Lors du BUILD : 
            - scanner le package : s'il existe 
            - Printstacktrace vers les classes ayant un URL de meme valeur 
        - REQUEST :
            - Type de retour : afficher que le type de retour est introuvable 
            - Erreur 404 si on ne trouve pas de page lier a la methode placer par l'utilisateur 

- Sprint 6 : 
    - Gestion des Parametres pour formulaire : 
        - Annotation des parametres entrant de type de base avec @Param 
        

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
