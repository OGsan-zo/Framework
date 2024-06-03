package other ;

import java.util.HashMap;

public class ModelView {
    private HashMap<String, Object> data;
    private String url;

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