package servlet;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;

import controller.*;
import other.*;

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
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        try {
            // Récupérer les paramètres du formulaire
            HashMap<String, String> formData = Utils.getFormParameters(request);
            // Afficher les informations de débogage et traiter la requête
            String relativeURI = Utils.getRelativeURI(request);
            Utils.displayDebugInfo(out, relativeURI, methodList);
            Utils.displayFormData(out, formData); 
            Utils.executeMappingMethod(relativeURI, methodList, out, request, response, formData);
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
        } else {
            System.out.println("No controllers found");
        }
    }
    // End of Section 
}
