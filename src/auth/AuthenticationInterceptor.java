package auth;

import annotation.auth.Authentication;
import auth.AuthenticationManager;
import exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

public class AuthenticationInterceptor {
    
    public static void validateAuthentication(Method method, Class<?> clazz, HttpServletRequest request) 
        throws AuthenticationException 
    {
        // Vérifier d'abord l'annotation au niveau de la classe
        Authentication classAuth = clazz.getAnnotation(Authentication.class);
        Authentication methodAuth = method.getAnnotation(Authentication.class);
        
        // Si la méthode a ignoreAuth=true, on ignore toute authentification
        if (methodAuth != null && methodAuth.ignoreAuth()) {
            return;
        }
        
        // Si ni la classe ni la méthode n'ont d'annotation, pas de vérification
        if (classAuth == null && methodAuth == null) {
            return;
        }
        
        // Utiliser l'annotation de la méthode si elle existe, sinon celle de la classe
        Authentication effectiveAuth = (methodAuth != null) ? methodAuth : classAuth;
        
        // Vérifier si l'utilisateur est authentifié
        if (!AuthenticationManager.isAuthenticated(request)) {
            throw new AuthenticationException("User must be authenticated to access this resource");
        }
        
        // Vérifier le rôle si spécifié
        String requiredRole = effectiveAuth.value();
        if (!AuthenticationManager.hasRole(request, requiredRole)) {
            throw new AuthenticationException("User does not have the required role: " + requiredRole);
        }
    }
}
