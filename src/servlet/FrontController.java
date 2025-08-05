package servlet;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpSession;
import controller.*;
import other.*;
import exception.AuthenticationException;
import exception.ValidationException;
import annotation.ValidateForm;
import auth.*;

@MultipartConfig
public class FrontController extends HttpServlet {

    private String controllerPackage;
    private ControllerScanner scanner;
    private List<Class<?>> controllers;
    private HashMap<String, Mapping> methodList;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllerPackage = Utils.initializeControllerPackage(config);
        scanAndInitializeControllers();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            processRequest(request, response);
        } catch (NoSuchMethodException | ClassNotFoundException | IOException | ServletException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (NoSuchMethodException | ClassNotFoundException | IOException | ServletException e) {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException, NoSuchMethodException, ClassNotFoundException {
        response.setContentType("text/html;charset=UTF-8");
        
        try {
            HashMap<String, String> formData = Utils.getFormParameters(request);
            String relativeURI = Utils.getRelativeURI(request);
            
            if (relativeURI.equals("") || relativeURI.equals("/")) {
                Utils.displayControllerList(response.getWriter(), methodList);
                return;
            }

            Mapping mapping = methodList.get(relativeURI);
            
            if (mapping == null) {
                Utils.handleError(request, response, 
                    HttpServletResponse.SC_NOT_FOUND, 
                    "Page non trouvée", 
                    "Aucun mapping trouvé pour l'URL: " + relativeURI, 
                    null);
                return;
            }
            
            if (!isHttpMethodValid(mapping, request.getMethod())) {
                Utils.handleError(request, response, 
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED, 
                    "Méthode non autorisée", 
                    "La méthode " + request.getMethod() + " n'est pas autorisée pour cette URL", 
                    null);
                return;
            }

            try {
                Class<?> controllerClass = Class.forName(mapping.getClassName());
                Method method = null;
                String httpMethod = request.getMethod();
                
                for (VerbAction verbAction : mapping.getVerbMethodes()) {
                    if (verbAction.getVerbe().equalsIgnoreCase(httpMethod)) {
                        method = Utils.findMethod(controllerClass, verbAction.getMethode());
                        break;
                    }
                }
                
                if (method != null) {
                    AuthenticationInterceptor.validateAuthentication(method, controllerClass, request);
                }
                
                Utils.executeMappingMethod(relativeURI, methodList, response.getWriter(), request, response, formData);
                
            } catch (AuthenticationException e) {
                request.getSession().setAttribute("requested_url", relativeURI);
                response.sendRedirect(request.getContextPath() + "/login-page");
                
            } catch (ValidationException ve) {
                Utils.handleError(request, response, 
                    HttpServletResponse.SC_BAD_REQUEST, 
                    "Erreur de validation", 
                    ve.getMessage(), 
                    ve);
                
            } catch (Exception e) {
                Utils.handleError(request, response, 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Erreur interne du serveur", 
                    "Une erreur inattendue s'est produite lors du traitement de votre requête", 
                    e);
            }
            
        } catch (Exception e) {
            Utils.handleError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Erreur critique", 
                "Une erreur critique s'est produite lors du traitement de votre requête", 
                e);
        }
    }

    private boolean isHttpMethodValid(Mapping mapping, String requestMethod) {
        for (VerbAction verbAction : mapping.getVerbMethodes()) {
            if (verbAction.getVerbe().equalsIgnoreCase(requestMethod)) {
                return true;
            }
        }
        return false;
    }


    // Section for "init()" Function 
    private void scanAndInitializeControllers() {
        try {

            this.scanner = new ControllerScanner();
            this.controllers = scanner.findControllers(controllerPackage);
            this.methodList = new HashMap<>();
            Utils.validateUniqueMappingValues(controllers);
            initMethodList();
        
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Initialization failed", e);
        }
    }

    private void initMethodList() {
        if (this.controllers != null) {
            for (Class<?> controller : this.controllers) {
                System.out.println("Scanning controller: " + controller.getName());
                Utils.findMethodsAnnotated(controller, methodList);
            }
        } 
        else 
        {    System.out.println("No controllers found");    }
    }
    // End of Section 
}
