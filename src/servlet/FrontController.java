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

    private void initializeControllerPackage(ServletConfig config) throws ServletException {
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        if (mapping != null) {
            out.println("<p>Found mapping:</p>");
            out.println("<p>Class: " + mapping.getClassName() + "</p>");
            out.println("<p>Method: " + mapping.getMethodName() + "</p>");
            invokeMethod(mapping, out, request, response);
        } else {
            out.println("<p>No associated method found</p>");
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

    private void invokeMethod(Mapping mapping, PrintWriter out, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Object result = executeControllerMethod(mapping);
            processMethodResult(result, out, request, response);
        } catch (Exception e) {
            out.println("<p>Error invoking method: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }

    private Object executeControllerMethod(Mapping mapping) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> cls = Class.forName(mapping.getClassName());
        Method method = cls.getMethod(mapping.getMethodName());
        Object obj = cls.getConstructor().newInstance();
        return method.invoke(obj);
    }

    private void processMethodResult(Object result, PrintWriter out, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (result instanceof String) {
            out.println("<p>Result: " + result + "</p>");
        } else if (result instanceof ModelView) {
            handleModelView((ModelView) result, request, response);
        } else {
            out.println("<p>Unsupported return type: " + result.getClass().getName() + "</p>");
        }
        out.println("<p>Method executed successfully.</p>");
    }

    private void handleModelView(ModelView modelView, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = modelView.getUrl();
        modelView.getData().forEach(request::setAttribute);
        RequestDispatcher dispatcher = request.getRequestDispatcher(url);
        dispatcher.forward(request, response);
    }
}
