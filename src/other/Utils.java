package other;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.google.gson.Gson;
import annotation.*;

public class Utils {

    // Initialize controller base package from web.xml
    public static String initializeControllerPackage(ServletConfig config) throws ServletException {
        String controllerPackage = config.getInitParameter("base_package");
        if (controllerPackage == null) {
            throw new ServletException("Base package is not specified in web.xml");
        }
        return controllerPackage;
    }

    // Validate uniqueness of URL mapping across all controllers
    public static void validateUniqueMappingValues(List<Class<?>> controllers) throws ServletException {
        if (controllers == null) throw new ServletException("The controllers list is null");
        HashMap<String, String> urlMethodMap = new HashMap<>();

        for (Class<?> controller : controllers) {
            if (controller == null) continue;
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Url.class)) {
                    String url = method.getAnnotation(Url.class).value();
                    validateUrlUniqueness(url, urlMethodMap, controller, method);
                }
            }
        }
    }

    // Helper method to validate URL uniqueness
    private static void validateUrlUniqueness(String url, HashMap<String, String> urlMethodMap, Class<?> controller, Method method) throws ServletException {
        if (url == null) throw new ServletException("URL mapping value is null for method: " + method.getName());
        if (urlMethodMap.containsKey(url)) {
            String existingMethod = urlMethodMap.get(url);
            throw new ServletException(String.format("Duplicate mapping value '%s' found. URL already exists for method: %s and method: %s.", url, existingMethod, method.getName()));
        }
        urlMethodMap.put(url, controller.getName() + "." + method.getName());
    }

    // Get relative URI
    public static String getRelativeURI(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    // Display debug information
    public static void displayDebugInfo(PrintWriter out, String relativeURI, HashMap<String, Mapping> methodList) {
        out.println("<h1>Debug Information</h1>");
        out.println("<h2>Requested URL: " + relativeURI + "</h2>");
        methodList.forEach((key, mapping) -> out.println("Mapping - Path: " + key + ", Class: " + mapping.getClassName() + ", Method: " + mapping.getMethodName() + "<br>"));
    }

    // Display form data
    public static void displayFormData(PrintWriter out, HashMap<String, String> formData) {
        formData.forEach((key, value) -> out.println("<p>" + key + ": " + value + "</p>"));
    }

    // Execute a mapping method (Main execution logic)
    public static void executeMappingMethod(String relativeURI, HashMap<String, Mapping> methodList, PrintWriter out, HttpServletRequest request, HttpServletResponse response, HashMap<String, String> formData) throws ServletException, IOException, NoSuchMethodException, ClassNotFoundException {
        Mapping mapping = methodList.get(relativeURI);
        if (mapping == null) {
            throw new ServletException("No associated method found for URL: " + relativeURI);
        }
        Method method = findMethod(Class.forName(mapping.getClassName()), mapping.getMethodName());

        if (!isHttpMethodValid(method, request.getMethod())) {
            handleError("HTTP method " + request.getMethod() + " is not allowed for this endpoint.", request, response);
            return;
        }

        out.println("<p>Executing method:</p>");
        invokeMethod(mapping, out, request, response, formData);
    }

    // Find a method in a given class by name
    private static Method findMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) return method;
        }
        throw new NoSuchMethodException("Method " + methodName + " not found in class " + clazz.getName());
    }

    // Invoke a method using reflection
    public static void invokeMethod(Mapping mapping, PrintWriter out, HttpServletRequest request, HttpServletResponse response, HashMap<String, String> formData) throws ServletException, IOException {
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.getConstructor().newInstance();
            initializeMySessionAttributes(controllerInstance, request);

            Object result = executeControllerMethod(mapping, request, controllerInstance, response);
            processMethodResult(result, findMethod(controllerClass, mapping.getMethodName()), out, request, response);
        } catch (Exception e) {
            e.printStackTrace();
            handleError("Error invoking method: " + e.getMessage(), request, response);
        }
    }

    // Execute a controller method with parameters
    public static Object executeControllerMethod(Mapping mapping, HttpServletRequest request, Object controllerInstance, HttpServletResponse response) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException, ServletException {
        Method method = findMethod(controllerInstance.getClass(), mapping.getMethodName());
        Object[] params = getMethodParams(method, request);
        return method.invoke(controllerInstance, params);
    }

    // Get method parameters from the request
    public static Object[] getMethodParams(Method method, HttpServletRequest request) throws ServletException {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Param param = parameters[i].getAnnotation(Param.class);
            ModelParam modelParam = parameters[i].getAnnotation(ModelParam.class);
            paramValues[i] = resolveParameterValue(parameters[i], param, modelParam, request);
        }

        return paramValues;
    }

    // Resolve a method parameter value
    private static Object resolveParameterValue(Parameter parameter, Param param, ModelParam modelParam, HttpServletRequest request) throws ServletException {
        if (param != null) {
            String paramName = param.name().isEmpty() ? parameter.getName() : param.name();
            return convertToParameterType(parameter.getType(), request.getParameter(paramName));
        }
        if (modelParam != null) {
            return resolveModelParam(parameter, modelParam, request);
        }
        if (parameter.getType().equals(MySession.class)) {
            return new MySession(request.getSession());
        }
        return null;
    }

    // Resolve model parameter (populate object fields)
    private static Object resolveModelParam(Parameter parameter, ModelParam modelParam, HttpServletRequest request) throws ServletException {
        try {
            Object paramInstance = parameter.getType().getDeclaredConstructor().newInstance();
            populateModelFields(paramInstance, request, modelParam.name());
            return paramInstance;
        } catch (Exception e) {
            throw new ServletException("Unable to instantiate parameter: " + parameter.getType().getName(), e);
        }
    }

    // Populate object fields from request parameters
    private static void populateModelFields(Object instance, HttpServletRequest request, String attributeName) throws ServletException {
        for (Field field : instance.getClass().getDeclaredFields()) {
            ModelField modelField = field.getAnnotation(ModelField.class);
            String paramName = (modelField != null && !modelField.name().isEmpty()) ? modelField.name() : field.getName();
            String paramValue = request.getParameter(attributeName + "." + paramName);
            if (paramValue != null) {
                setFieldValue(instance, field, paramValue);
            }
        }
    }

    // Set a field value using reflection
    private static void setFieldValue(Object instance, Field field, String value) throws ServletException {
        try {
            field.setAccessible(true);
            field.set(instance, convertToParameterType(field.getType(), value));
        } catch (IllegalAccessException e) {
            throw new ServletException("Unable to set field value: " + field.getName(), e);
        }
    }

    // Convert a string to the correct parameter type
    private static Object convertToParameterType(Class<?> type, String value) {
        if (value == null || value.isEmpty()) return getDefaultParameterValue(type);
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
    }

    // Return default parameter values for various types
    private static String getDefaultParameterValue(Class<?> type) {
        if (type.equals(String.class)) return "";
        if (type.equals(int.class) || type.equals(Integer.class)) return "0";
        if (type.equals(long.class) || type.equals(Long.class)) return "0";
        if (type.equals(double.class) || type.equals(Double.class)) return "0.0";
        if (type.equals(boolean.class) || type.equals(Boolean.class)) return "false";
        return null;
    }

    // Check if the HTTP method is valid for the requested method
    private static boolean isHttpMethodValid(Method method, String requestMethod) {
        // Vérifie si la méthode a l'annotation @Get
        if (method.isAnnotationPresent(Get.class)) {
            return requestMethod.equalsIgnoreCase("GET");
        }
        
        // Vérifie si la méthode a l'annotation @Post
        if (method.isAnnotationPresent(Post.class)) {
            return requestMethod.equalsIgnoreCase("POST");
        }
        
        // Si aucune annotation, on accepte la méthode par défaut (GET)
        return requestMethod.equalsIgnoreCase("GET");
    }


    // Initialize session attributes (MySession)
    public static void initializeMySessionAttributes(Object controllerInstance, HttpServletRequest request) throws IllegalAccessException {
        for (Field field : controllerInstance.getClass().getDeclaredFields()) {
            if (field.getType().equals(MySession.class)) {
                field.setAccessible(true);
                field.set(controllerInstance, new MySession(request.getSession()));
            }
        }
    }

    // Process the result of a method execution
    public static void processMethodResult(Object result, Method method, PrintWriter out, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (result == null) {
            out.println("<p>Method executed, no result to display.</p>");
            return;
        }

        out.println("<p>Method result:</p>");
        if (result instanceof ModelView) {
            handleModelView((ModelView) result, request, response);
        } else if (method.isAnnotationPresent(RestApi.class)) {
            response.setContentType("application/json");
            Gson gson = new Gson();
            out.println(gson.toJson(result));
        } else {
            out.println(result.toString());
        }
    }

    // Handle a ModelView result (Forward to JSP)
    public static void handleModelView(ModelView modelView, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        modelView.getData().forEach(request::setAttribute);
        request.getRequestDispatcher("/" + modelView.getUrl()).forward(request, response);
    }

    // Handle errors (forward to error page)
    public static void handleError(String errorMessage, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("errorMessage", errorMessage);
        request.getRequestDispatcher("/error.jsp").forward(request, response);
    }

    public static void findMethodsAnnotated(Class<?> controllerClass, HashMap<String, Mapping> methodList) {
        Method[] methods = controllerClass.getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(Url.class)) {
                Url urlAnnotation = method.getAnnotation(Url.class);
                String url = urlAnnotation.value();
                
                // Stocker la méthode dans la liste des mappings
                Mapping mapping = new Mapping(method.getName(), controllerClass.getName());
                methodList.put(url, mapping);
            }
        }
    }

    public static HashMap<String, String> getFormParameters(HttpServletRequest request) {
        HashMap<String, String> formData = new HashMap<>();
        
        // Récupérer tous les paramètres du formulaire
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                formData.put(key, values[0]); // Assigner la première valeur du tableau
            }
        });
        
        return formData;
    }
    
    
}
