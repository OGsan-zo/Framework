package other;

import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

public class Mapping {

    private String className;
    private Set<VerbAction> verbMethodes = new HashSet<>();

    public Mapping() {}

    public Mapping(String className, VerbAction verbMethod) {
        setClassName(className);
        this.verbMethodes = new HashSet<>();
        this.verbMethodes.add(verbMethod);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Set<VerbAction> getVerbMethodes() {
        return verbMethodes;
    }

    public void setVerbMethodes(Set<VerbAction> verbMethodes) {
        this.verbMethodes = verbMethodes;
    }

    public void addVerbMethod(VerbAction verbMethod) {
        this.verbMethodes.add(verbMethod);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mapping mapping = (Mapping) o;

        // Vérification si les noms de classe sont égaux
        if (!Objects.equals(className, mapping.className)) {
            return false;
        }

        // Vérification que chaque VerbMethod dans this.verbMethodes existe dans l'autre Mapping
        if (verbMethodes != null && !verbMethodes.isEmpty()) {
            for (VerbAction vm : verbMethodes) {
                if (!mapping.getVerbMethodes().contains(vm)) {
                    return false;
                }
            }
        }

        return true; // Si tout correspond, retourner true
    }

    

    @Override
    public int hashCode() {
        return Objects.hash(className, verbMethodes);
    }

    // Méthode toString
    @Override
    public String toString() {
        return "Mapping{" +
                "methodName='" + verbMethodes + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}
