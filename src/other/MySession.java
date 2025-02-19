package other;

import jakarta.servlet.http.HttpSession;

public class MySession {
    private HttpSession session;

    public MySession(){}

    public MySession(HttpSession session) {
        this.session = session;
    }

    public Object get(String key) {
        Object value = session.getAttribute(key);
        System.out.println("Session GET - Key: " + key + ", Value: " + value);
        return value;
    }

    public void add(String key, Object object) {
        System.out.println("Session ADD - Key: " + key + ", Value: " + object);
        session.setAttribute(key, object);
    }

    public void delete(String key) {
        System.out.println("Session DELETE - Key: " + key);
        session.removeAttribute(key);
    }
}