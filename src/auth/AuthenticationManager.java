// AuthenticationManager.java
package auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class AuthenticationManager {
    private static Properties properties;
    private static final String CONFIG_FILE = "auth.properties";
    private static String userSessionKey;
    private static String rolesSessionKey;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        properties = new Properties();
        try {
            // Essayer plusieurs méthodes de chargement
            InputStream input = null;
            
            // 1. D'abord essayer via le ClassLoader
            input = AuthenticationManager.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE);
                
            if (input == null) {
                // 2. Essayer via le context classloader du thread
                input = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(CONFIG_FILE);
            }
            
            if (input == null) {
                // 3. En dernier recours, chercher dans WEB-INF/classes
                String webInfPath = AuthenticationManager.class.getClassLoader()
                    .getResource("/").getPath();
                File configFile = new File(webInfPath + CONFIG_FILE);
                if (configFile.exists()) {
                    input = new FileInputStream(configFile);
                }
            }

            if (input != null) {
                properties.load(input);
                userSessionKey = properties.getProperty("auth.session.user");
                rolesSessionKey = properties.getProperty("auth.session.roles");
                input.close();
            } else {
                throw new RuntimeException("Could not find " + CONFIG_FILE);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load authentication configuration", e);
        }
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(userSessionKey) != null;
    }

    public static boolean hasRole(HttpServletRequest request, String requiredRole) {
        if (!isAuthenticated(request)) {
            System.out.println("Debug - Authentication check failed");
            System.out.println("Debug - Session exists: " + (request.getSession(false) != null));
            System.out.println("Debug - User key expected: " + userSessionKey);
            if (request.getSession(false) != null) {
                System.out.println("Debug - User attribute exists: " + 
                    (request.getSession().getAttribute(userSessionKey) != null));
            }
            return false;
        }
        
        HttpSession session = request.getSession(false);
        String userRole = (String) session.getAttribute(rolesSessionKey);
        
        System.out.println("Debug - Authentication successful");
        System.out.println("Debug - Required role: " + requiredRole);
        System.out.println("Debug - User role found: " + userRole);
        System.out.println("Debug - Role key used: " + rolesSessionKey);
        
        if ("public".equals(requiredRole)) return true;
        if (requiredRole.isEmpty()) return true;
        
        boolean hasRequiredRole = requiredRole.equals(userRole);
        System.out.println("Debug - Has required role: " + hasRequiredRole);
        
        return hasRequiredRole;
    }

    public static void setUserRole(HttpSession session, String role) {
        session.setAttribute(rolesSessionKey, role);
    }

    public static void setAuthenticated(HttpSession session, Object userInfo) {
        session.setAttribute(userSessionKey, userInfo);
    }
}