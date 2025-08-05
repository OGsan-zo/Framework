package other;

import java.util.Objects;

public class VerbAction {
    private String verbe;  // Ex: GET, POST
    private String methode;  // Le nom de la méthode associée

    // Constructeur par défaut
    public VerbAction() {}

    // Constructeur avec paramètres
    public VerbAction(String verbe, String methode) {
        setMethode(methode);
        setVerbe(verbe);
    }

    // Getter et Setter pour methode
    public String getMethode() {
        return methode;
    }

    public void setMethode(String methode) {
        this.methode = methode;
    }

    // Getter et Setter pour verbe
    public String getVerbe() {
        return verbe;
    }

    public void setVerbe(String verbe) {
        this.verbe = verbe;
    }

    // Surcharge de la méthode equals() pour comparer les attributs verbe et methode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Vérifie si c'est la même instance
        if (o == null || getClass() != o.getClass()) return false; // Vérifie si c'est un autre type d'objet ou null
        VerbAction that = (VerbAction) o; // Cast l'objet dans la bonne classe
        return Objects.equals(verbe, that.verbe) && // Compare l'attribut verbe
            Objects.equals(methode, that.methode); // Compare l'attribut methode
    }


    // Surcharge de hashCode() pour être cohérent avec equals()
    @Override
    public int hashCode() {
        return Objects.hash(verbe, methode);
    }

    @Override
    public String toString() {
        return "VerbMethod{" +
                "verbe='" + verbe + '\'' +
                ", methode='" + methode + '\'' +
                '}';
    }
}
