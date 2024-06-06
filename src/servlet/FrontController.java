package servlet;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;

import annotation.Get;
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
        initializeControllerPackage(config);
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
            throws IOException , ServletException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        try {
            String relativeURI = getRelativeURI(request);
            displayDebugInfo(out, relativeURI);
            executeMappingMethod(relativeURI, out, request, response);
        } finally {
            out.close();
        }
    }

    private void initializeControllerPackage(ServletConfig config) 
            throws ServletException {
        controllerPackage = config.getInitParameter("base_package");
        if (controllerPackage == null) {
            throw new ServletException("Base package is not specified in web.xml");
        }
    }

    private void scanAndInitializeControllers() {
        try {
            this.scanner = new ControllerScanner();
            this.controllers = scanner.findControllers(controllerPackage);
            this.methodList = new HashMap<>();
            validateUniqueMappingValues(controllers);
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
                findMethodsAnnotated(controller);
            }
        } else {
            System.out.println("No controllers found");
        }
    }

    public void findMethodsAnnotated(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Get.class)) {
                Get getAnnotation = method.getAnnotation(Get.class);
                Mapping map = new Mapping(method.getName(), clazz.getName());
                methodList.put(getAnnotation.value(), map);
            }
        }
    }

    private void validateUniqueMappingValues(List<Class<?>> controllers) throws ServletException {
        if (controllers == null) {
            throw new ServletException("The controllers list is null");
        }
    
        HashMap<String, String> urlMethodMap = new HashMap<>();
    
        for (Class<?> controller : controllers) {
            if (controller == null) {
                continue;
            }
            
            Method[] methods = controller.getDeclaredMethods();
            if (methods == null) {
                continue;
            }
    
            for (Method method : methods) {
                if (method == null) {
                    continue;
                }
    
                if (method.isAnnotationPresent(Get.class)) {
                    Get getAnnotation = method.getAnnotation(Get.class);
                    String url = getAnnotation.value();
                    
                    if (url == null) {
                        throw new ServletException("URL mapping value is null for method: " + method.getName());
                    }
    
                    if (urlMethodMap.containsKey(url)) {
                        String existingMethod = urlMethodMap.get(url);
                        
                        throw new ServletException(String.format(
                                "Duplicate mapping value '%s' found. URL already exists for method: %s and method: %s. " +
                                        "A URL mapping value must be unique across all controllers.",
                                url, existingMethod, method.getName()));
                    }
    
                    urlMethodMap.put(url, controller.getName() + "." + method.getName());
                }
            }
        }
    }

    private String getRelativeURI(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        return requestURI.substring(contextPath.length());
    }

    private void displayDebugInfo(PrintWriter out, String relativeURI) {
        System.out.println("Requested URL: " + relativeURI);
        out.println("<h1>Hello World</h1>");
        out.println("<h2>You are here now:</h2>");
        out.println("<h3>URL: " + relativeURI + "</h3>");
        methodList.forEach((key, mapping) -> out.println("Mapping - Path: " + key + ", Class: " + mapping.getClassName() + ", Method: " + mapping.getMethodName() + "<br>"));
    }

    private void executeMappingMethod(String relativeURI, PrintWriter out, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Mapping mapping = methodList.get(relativeURI);
        if (mapping == null) {
            throw new ServletException("No associated method found for URL: " + relativeURI);
        }
    
        out.println("<p>Found mapping:</p>");
        out.println("<p>Class: " + mapping.getClassName() + "</p>");
        out.println("<p>Method: " + mapping.getMethodName() + "</p>");
        invokeMethod(mapping, out, request, response);
    }

    private void invokeMethod(Mapping mapping, PrintWriter out, HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            Object result = executeControllerMethod(mapping);
            processMethodResult(result, out, request, response);
        } catch (Exception e) {
            out.println("<p>Error invoking method: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }

    private Object executeControllerMethod(Mapping mapping) 
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> cls = Class.forName(mapping.getClassName());
        Method method = cls.getMethod(mapping.getMethodName());
        Object obj = cls.getConstructor().newInstance();
        return method.invoke(obj);
    }

    private void processMethodResult(Object result, PrintWriter out, HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        if (result instanceof String) {
            out.println("<p>Result: " + result + "</p>");
        } else if (result instanceof ModelView) {
            handleModelView((ModelView) result, request, response);
        } else {
            throw new ServletException("Unsupported return type: " + result.getClass().getName());
        }
        out.println("<p>Method executed successfully.</p>");
    }

    private void handleModelView(ModelView modelView, HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String url = modelView.getUrl();
        if (url == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "La vue spécifiée est introuvable");
            return;
        }
        modelView.getData().forEach(request::setAttribute);
        RequestDispatcher dispatcher = request.getRequestDispatcher(url);
        dispatcher.forward(request, response);
    }
}
