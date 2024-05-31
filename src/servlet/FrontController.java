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
        try {
            super.init(config);

            // Récupérer le paramètre d'initialisation depuis ServletConfig
            controllerPackage = config.getInitParameter("base_package");

            if (controllerPackage == null) {
                throw new ServletException("Base package is not specified in web.xml");
            }

            // System.out.println("Controller package: " + controllerPackage); // Debug

            this.scanner = new ControllerScanner();
            this.controllers = scanner.findControllers(controllerPackage);

            // System.out.println("Found controllers: " + controllers); // Debug

            this.methodList = new HashMap<>();
            initMethodList();

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Initialization failed", e);
        }
    }

    private void initMethodList() {
        try {
            if (this.controllers != null) {
                for (Class<?> controller : this.controllers) {
                    System.out.println("Scanning controller: " + controller.getName()); // Debug
                    findMethodsAnnoted(controller);
                }
            } else {
                System.out.println("No controllers found"); // Debug
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        try {
            // Get the context path and request URI
            String contextPath = request.getContextPath();
            String requestURI = request.getRequestURI();

            // Remove the context path from the request URI
            String relativeURI = requestURI.substring(contextPath.length());

            System.out.println("Requested URL: " + relativeURI); // Debug

            out.println("<h1>Hello World</h1>");
            out.println("<h2>You are here now:</h2>");
            out.println("<h3>URL: " + relativeURI + "</h3>");

            if (methodList != null) {
                for (String key : methodList.keySet()) {
                    Mapping mapping = methodList.get(key);
                    out.println("Mapping - Path: " + key + ", Class: " + mapping.getClassName() + ", Method: " + mapping.getMethodName() + "<br>");
                }
            } else {
                System.out.println("methodList is null"); // Debug
            }

            Mapping mapping = methodList.get(relativeURI);
            if (mapping != null) {
                out.println("<p>Found mapping:</p>");
                out.println("<p>Class: " + mapping.getClassName() + "</p>");
                out.println("<p>Method: " + mapping.getMethodName() + "</p>");
                
                invokeMethod(mapping, out);

            } else {
                out.println("<p>Aucune méthode associée</p>");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    public void findMethodsAnnoted(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Get.class)) {
                Get getAnnotation = method.getAnnotation(Get.class);
                Mapping map = new Mapping(method.getName(), clazz.getName());
                methodList.put(getAnnotation.value(), map);
                // System.out.println("Method: " + method.getName() + ", Path: " + getAnnotation.value()); // Debug
            }
        }
    }

    private void invokeMethod(Mapping mapping, PrintWriter out) {
        try {
            Class<?> cls = Class.forName(mapping.getClassName());
            Method method = cls.getMethod(mapping.getMethodName());
            Object obj = cls.getConstructor().newInstance();
            Object result = method.invoke(obj);
            out.println("<p>Method " + mapping.getMethodName() + " executed successfully.</p>");
            out.println("<p>Result: " + result + "</p>");
        } catch (ClassNotFoundException e) {
            out.println("<p>Class not found: " + mapping.getClassName() + "</p>");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            out.println("<p>Method not found: " + mapping.getMethodName() + "</p>");
            e.printStackTrace();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            out.println("<p>Error invoking method: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }
}
