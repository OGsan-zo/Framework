package other ;

import exception.ValidationError;

import java.util.HashMap;
import java.util.Map;


public class ModelView {
    private HashMap<String, Object> data;
    private String url;
    
    private ValidationError validationError;
    private boolean hasErrors;

    private boolean redirect = false; // Nouveau champ

    public ModelView() {
        this.data = new HashMap<>();
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void add(String name, Object value) {
        this.data.put(name, value);
    }

    public void setValidationError(ValidationError error) {
        this.validationError = error;
        this.hasErrors = true;
        // Ajouter les erreurs et les valeurs dans data pour les rendre accessibles à la vue
        this.data.put("fieldErrors", error.getFieldErrors());
        this.data.put("fieldValues", error.getFieldValues());
    }

    public boolean hasErrors() {
        return hasErrors;
    }

     public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    public static void main(String[] args) {
        // Création d'une instance de ModelView
        ModelView mv = new ModelView();

        // Définition de l'URL
        mv.setUrl("https://example.com");

        // Ajout de données
        mv.add("username", "john_doe");
        mv.add("age", 30);
        mv.add("isLoggedIn", true);

        // Affichage des données
        System.out.println("URL: " + mv.getUrl());
        System.out.println("Data: " + mv.getData());
    }
}