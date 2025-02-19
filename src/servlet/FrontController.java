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
    throws IOException, ServletException, NoSuchMethodException, ClassNotFoundException 
    {
        PrintWriter out = response.getWriter();

        try {
            HashMap<String, String> formData = Utils.getFormParameters(request);
            String relativeURI = Utils.getRelativeURI(request);
            
            // Récupérer la méthode à exécuter
            Mapping mapping = methodList.get(relativeURI);
            if (mapping != null) {
                Class<?> controllerClass = Class.forName(mapping.getClassName());
                Method method = null;
                
                // Trouver la méthode correspondant au verbe HTTP
                String httpMethod = request.getMethod();
                for (VerbAction verbAction : mapping.getVerbMethodes()) {
                    if (verbAction.getVerbe().equalsIgnoreCase(httpMethod)) {
                        method = Utils.findMethod(controllerClass, verbAction.getMethode());
                        break;
                    }
                }
                
                if (method != null) {
                    // Vérifier l'authentification avec la classe ET la méthode
                    try {
                        AuthenticationInterceptor.validateAuthentication(method, controllerClass, request);
                    
                    } catch (AuthenticationException e) {
                        System.err.println("Message d'erreur dans l'authentification de la methode ou dela classe = " +e.getMessage());
                        // Stocker l'URL demandée pour redirection après login
                        request.getSession().setAttribute("requested_url", relativeURI);
                        response.sendRedirect(request.getContextPath() + "/login-page");
                        return;
                    }
                    
                    // Continuer avec l'exécution normale
                    Utils.displayDebugInfo(out, relativeURI, methodList);
                    Utils.displayFormData(out, formData); 
                    Utils.executeMappingMethod(relativeURI, methodList, out, request, response, formData);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (ValidationException ve) {
            ModelView errorView = ve.getModelView();
            Utils.handleModelView(errorView, request, response);
        } finally {
            out.close();
        }
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
