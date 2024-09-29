package other;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import annotation.ModelField;
import annotation.ModelParam;
import java.lang.reflect.Field;
import com.google.gson.Gson;

import annotation.Get;
import annotation.Param;
import annotation.RestApi;

public class Utils {

    
    public static String initializeControllerPackage(ServletConfig config) 
        throws ServletException {
        String controllerPackage = config.getInitParameter("base_package");
        if (controllerPackage == null) {
            throw new ServletException("Base package is not specified in web.xml");
        }
        return controllerPackage;
    }

    public static void validateUniqueMappingValues(List<Class<?>> controllers) 
        throws ServletException {
        if (controllers == null) {
            throw new ServletException("The controllers list is null");
        }
    
        HashMap<String, String> urlMethodMap = new HashMap<>();
    
        for (Class<?> controller : controllers) {
            if (controller == null) {
                continue;
            }
            
            // Boucle des methodes du controlleurs 
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

    public static String getRelativeURI(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        return requestURI.substring(contextPath.length());
    }

    public static void displayDebugInfo(PrintWriter out, String relativeURI, HashMap<String, Mapping> methodList) {
        System.out.println("Requested URL: " + relativeURI);
        out.println("<h1>Hello World</h1>");
        out.println("<h2>You are here now:</h2>");
        out.println("<h3>URL: " + relativeURI + "</h3>");
        methodList.forEach((key, mapping) -> out.println("Mapping - Path: " + key + ", Class: " + mapping.getClassName() + ", Method: " + mapping.getMethodName() + "<br>"));
    }

    public static void displayFormData(PrintWriter out, HashMap<String, String> formData) {
        formData.forEach((key, value) -> out.println("<p>" + key + ": " + value + "</p>"));
    }

    public static void executeMappingMethod(String relativeURI, HashMap<String, Mapping> methodList, PrintWriter out, HttpServletRequest request, HttpServletResponse response, HashMap<String, String> formData) throws ServletException, IOException {
        Mapping mapping = methodList.get(relativeURI);
        if (mapping == null) {
            throw new ServletException("No associated method found for URL: " + relativeURI);
        }
    
        out.println("<p>Found mapping:</p>");
        out.println("<p>Class: " + mapping.getClassName() + "</p>");
        out.println("<p>Method: " + mapping.getMethodName() + "</p>");
        invokeMethod(mapping, out, request, response, formData);
    }

    public static void invokeMethod(Mapping mapping, PrintWriter out, HttpServletRequest request, HttpServletResponse response, HashMap<String, String> formData) throws ServletException, IOException {
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.getConstructor().newInstance();
            
            // Initialize MySession attributes
            initializeMySessionAttributes(controllerInstance, request);
            
            Method method = findMethodWithRequestParams(controllerClass, mapping.getMethodName());
            
            System.out.println("Methode utiliser par invoke methode " + method);  //Debug
            
            executeControllerMethod(mapping, request, controllerInstance, response);
        
            // processMethodResult(result, method , out, request, response);
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            
            // Set the error message as a request attribute
            // request.setAttribute("errorMessage", e.getMessage());
        
            // Forward the request to the error page
            // RequestDispatcher dispatcher = request.getRequestDispatcher("/2772.jsp");
            // dispatcher.forward(request, response);
        }
    }
    
    public static Object executeControllerMethod(Mapping mapping, HttpServletRequest request, Object controllerInstance, HttpServletResponse response)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ServletException, IOException {
        Class<?> cls = controllerInstance.getClass();
        Method method = findMethodWithRequestParams(cls, mapping.getMethodName());

        Object[] params = getMethodParams(method, request);
        Object result = method.invoke(controllerInstance, params);
        
        try (PrintWriter out = response.getWriter()) {
            System.out.println("Process En cours d'execution ");
            System.out.println("Le resultat de la fonction => "+result);
            processMethodResult(result, method, out, request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static Method findMethodWithRequestParams(Class<?> cls, String methodName) throws NoSuchMethodException {
        Method[] methods = cls.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException("Method " + methodName + " not found in class " + cls.getName());
    }

    public static Object[] getMethodParams(Method method, HttpServletRequest request)
        throws ServletException {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Param param = parameters[i].getAnnotation(Param.class);
            ModelParam modelParam = parameters[i].getAnnotation(ModelParam.class);

            // Annotation : PARAM
            if (param != null) {
                String paramName = param.name();
                if (paramName == null || paramName.isEmpty() ) {
                    paramName = parameters[i].getName(); 
                }
                String paramValue = request.getParameter(paramName);
                paramValues[i] = convertToParameterType(parameters[i].getType(), paramValue);

            // Annotation : MODELPARAM
            } else if (modelParam != null) {
                Class<?> paramType = parameters[i].getType();
                Object paramInstance;
                try {
                    paramInstance = paramType.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new ServletException("Unable to instantiate parameter: " + paramType.getName(), e);
                }

                String attributeName = modelParam.name();
                if (attributeName == null || attributeName.isEmpty()) {
                    attributeName = parameters[i].getName();
                }

                // Parametre OBJECT : execution
                populateModelFields(paramInstance, request, attributeName);
                System.out.println("Nom attribute : " + attributeName);

                paramValues[i] = paramInstance;

            // Annotation : MYSESSION
            } else if (parameters[i].getType().equals(MySession.class)) {
                paramValues[i] = new MySession(request.getSession());
                
            } else {
                // throw new ServletException(" ETU 002772 ; Nom de l'erreur :  "+" Cannot find param or modelParam annotation for parameter: " + parameters[i].getName());
            }
        }

        return paramValues;
    }

    private static void populateModelFields( Object instance, HttpServletRequest request , String nameModelAttribute ) throws ServletException {
        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields) {
            ModelField modelField = field.getAnnotation(ModelField.class);
            String paramName = (modelField != null && !modelField.name().isEmpty()) ? modelField.name() : field.getName();
            String paramValue = request.getParameter(nameModelAttribute+"."+paramName);
            if (paramValue != null) {
                field.setAccessible(true);
                try {
                    field.set(instance, convertToParameterType(field.getType(), paramValue));
                } catch (IllegalAccessException e) {
                    throw new ServletException("Unable to set field value: " + field.getName(), e);
                }
            }
        }
    }
    
    private static Object convertToParameterType(Class<?> type, String value) {
        if (value == null || value.isEmpty()) {
            return getDefaultParameterValue(type); // Handle empty or null values
        }
        
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid integer value: " + value);
            }
        } else if (type == long.class || type == Long.class) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid long value: " + value);
            }
        } else if (type == double.class || type == Double.class) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid double value: " + value);
            }
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        // Add more type conversions as needed
        throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
    }
    
    private static String getDefaultParameterValue(Class<?> type) {
        if (type.equals(String.class)) {
            return ""; // default empty string
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return "0"; // default integer value
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return "false"; // default boolean value
        } 
        // Handle other default cases as needed
        return null;
    }

    public static void processMethodResult(Object result, Method method, PrintWriter out, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isJson = method.isAnnotationPresent(RestApi.class);

        if (isJson) {
            response.setContentType("application/json");
        } else {
            response.setContentType("text/html");
        }

        if (result instanceof ModelView) {
            handleModelView((ModelView) result, method, request, response, out);
        } else {
            // Appeler outputResponse pour afficher la réponse en JSON ou en texte normal
            outputResponse(result, isJson, out);
        }

        out.println("<p>Method executed successfully.</p>");
    }

    public static void outputResponse(Object result, boolean isJson, PrintWriter out) {
        if (isJson) {
            // Si JSON est requis, convertir en JSON
            Gson gson = new Gson();
            out.println(gson.toJson(result));
        } else {
            // Sinon, afficher en texte normal
            out.println(result instanceof String ? (String) result : result.toString());
        }
    }

    public static void handleModelView(ModelView modelView, Method method, HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
        throws ServletException, IOException {
    
    if (method.isAnnotationPresent(RestApi.class)) {
        try {
            // Si la méthode est annotée avec @RestApi, on convertit le résultat en JSON
            String jsonResult = convertToJson(modelView.getData(), out);
            response.setContentType("application/json");
            out.println(jsonResult);
        } catch (Exception e) {
            throw new ServletException("Erreur lors de la conversion en JSON", e);
        }
    
    } else {
        String url = modelView.getUrl();
        if (url == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "La vue spécifiée est introuvable");
            return;
        }

        // On place les données de ModelView dans les attributs de la requête
        modelView.getData().forEach(request::setAttribute);
        
        // Redirection vers la vue (JSP, HTML, etc.)
        RequestDispatcher dispatcher = request.getRequestDispatcher(url);
        dispatcher.forward(request, response);
    }
}


    public static void findMethodsAnnotated(Class<?> clazz, HashMap<String, Mapping> methodList) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Get.class)) {
                Get getAnnotation = method.getAnnotation(Get.class);
                Mapping map = new Mapping(method.getName(), clazz.getName());
                methodList.put(getAnnotation.value(), map);
            }
        }
    }

    public static HashMap<String, String> getFormParameters(HttpServletRequest request) {
        HashMap<String, String> formData = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            String[] strValues = (String[]) values;
            if (strValues.length > 0) {
                formData.put(key.toString(), strValues[0]);
            }
        });
        return formData;
    }

    public static void initializeMySessionAttributes(Object controllerInstance, HttpServletRequest request) throws IllegalAccessException {
        Field[] fields = controllerInstance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(MySession.class)) {
                field.setAccessible(true);
                field.set(controllerInstance, new MySession(request.getSession()));
            }
        }
    }

    public static String convertToJson(Object object , PrintWriter out) throws ServletException {
        out.println("DEBUG - CONVERT_JSON ");
        try {
            Gson gson = new Gson();
            out.println(" Convert JSON result : "+ gson.toJson(object));
            System.out.println(gson.toJson(object));
            return gson.toJson(object);
        } catch (Exception e) {
            throw new ServletException("Erreur lors de la conversion en JSON", e);
        }
    }
    

}
