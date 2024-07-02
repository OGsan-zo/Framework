package other;

import jakarta.servlet.http.HttpSession;

public class MySession {
    
    private HttpSession session;

    public MySession(){}

    public MySession(HttpSession session) {
        this.session = session;
    }

    public Object get(String key) {
        System.out.println("Valeur obtenue :" +session.getAttribute(key) );
        return session.getAttribute(key);
    }

    public void add(String key, Object object) {
        System.out.println("Ajouter dans la session");
        session.setAttribute(key, object);
    }

    public void delete(String key) {
        System.out.println("Deconnexion de la session" + key );
        session.removeAttribute(key);
    }
}
