# FRAMEWORK

## Utilisation : 
- Creer un package pour mettre vos 'Model' : 
    - Exemple : controller 

- Corriger la valeur de base_package dans  'web.xml' en le nom de votre package de vos 'Model' 

- Annoter : 
    - vos Models par l'annotation :  @Controller 
    - vos methodes par l'annotation : @Get 

- Type de retour fonctionnel : 
    - STRING 
    - MODELVIEW 

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
