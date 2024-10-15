package other;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.google.gson.Gson;
import annotation.*;
import annotation.field.ModelField;
import annotation.methods.Get;
import annotation.methods.Post;
import annotation.methods.RestApi;
import annotation.methods.Url;

public class Utils {

    // Initialize controller base package from web.xml
    public static String initializeControllerPackage(ServletConfig config) 
        throws ServletException 
    {
        String controllerPackage = config.getInitParameter("base_package");
        
        if (controllerPackage == null) 
        {    throw new ServletException("Base package is not specified in web.xml");    }
        
        return controllerPackage;
    }

    public static void validateUniqueMappingValues(List<Class<?>> controllers) 
        throws ServletException 
    {
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

    private static void validateUrlUniqueness( String url, HashMap<String, String> urlMethodMap,
                                                            Class<?> controller, Method method ) 
        throws ServletException 
    {
        if (url == null) throw new ServletException("URL mapping value is null for method: " + method.getName());

        if (urlMethodMap.containsKey(url)) {
            String existingMethod = urlMethodMap.get(url);
            throw new ServletException(String.format("Duplicate mapping value '%s' found. URL already exists for method: %s and method: %s.", url, existingMethod, method.getName()));
        }

        urlMethodMap.put(url, controller.getName() + "." + method.getName());
    }

    public static String getRelativeURI(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    public static void displayDebugInfo(PrintWriter out, String relativeURI, HashMap<String, Mapping> methodList) {
        out.println("<h1>FrameWork : </h1>");
        out.println("<h2>Requested URL: " + relativeURI + "</h2>");
        methodList.forEach((key, mapping) -> {
            for (VerbAction verbAction : mapping.getVerbMethodes()) {
                out.println("Mapping - Path: " + key + "|           Class: " + mapping.getClassName() +
                    ",              Method: " + verbAction.getMethode() + "<br>");
            }
        });
    }

    public static void displayFormData(PrintWriter out, HashMap<String, String> formData) {
        formData.forEach((key, value) -> out.println("<p>" + key + ": " + value + "</p>"));
    }

    public static void executeMappingMethod(String relativeURI, 
                                            HashMap<String, Mapping> methodList,
                                            PrintWriter out, HttpServletRequest request, 
                                            HttpServletResponse response, HashMap<String, 
                                            String> formData) 
        throws ServletException, IOException, NoSuchMethodException, ClassNotFoundException 
    {
        Mapping mapping = methodList.get(relativeURI);
        
        if (mapping == null) 
        {    throw new ServletException("No associated method found for URL: " + relativeURI);      }
        
        if (!isHttpMethodValid(mapping, request.getMethod())) {
            handleError("<h1>400</h1>  HTTP method " + request.getMethod() + " is not allowed for this endpoint.", request, response);
            return;
        }

        out.println("<p>Executing method:</p>");
        invokeMethod(mapping, out, request, response, formData);
    }

    private static Method findMethod(Class<?> clazz, String methodName) 
        throws NoSuchMethodException 
    {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) return method;
        }

        throw new NoSuchMethodException("Method " + methodName + " not found in class " + clazz.getName());
    }

    public static void invokeMethod(Mapping mapping, PrintWriter out, 
                                    HttpServletRequest request, HttpServletResponse response, 
                                    HashMap<String, String> formData) 
        throws ServletException, IOException 
    {
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controllerInstance = controllerClass.getConstructor().newInstance();
            initializeMySessionAttributes(controllerInstance, request);

            Object result = executeControllerMethod(mapping, request, controllerInstance, response);
            for (VerbAction verbAction : mapping.getVerbMethodes()) {
                processMethodResult(result, findMethod(controllerClass, verbAction.getMethode()), out, request, response);
            }
            
    } catch (Exception e) {
            e.printStackTrace();
            handleError("Error invoking method: " + e.getMessage(), request, response);
        }
    }

    public static Object executeControllerMethod(Mapping mapping, HttpServletRequest request, 
                                                    Object controllerInstance, HttpServletResponse response) 
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException, ServletException 
    {
        Object obj = new Object(); 

        for (VerbAction verbAction : mapping.getVerbMethodes()) {
            Method method = findMethod(controllerInstance.getClass(), verbAction.getMethode());
            Object[] params = getMethodParams(method, request);
            obj = method.invoke(controllerInstance, params);
        }
        
        return obj;
        
    }

    // Get method parameters from the request
    public static Object[] getMethodParams(Method method, HttpServletRequest request) 
        throws ServletException 
    {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Param param = parameters[i].getAnnotation(Param.class);
            ModelParam modelParam = parameters[i].getAnnotation(ModelParam.class);
            paramValues[i] = resolveParameterValue(parameters[i], param, modelParam, request);
        }

        return paramValues;
    }

    private static Object resolveParameterValue(Parameter parameter, Param param, 
                                                ModelParam modelParam, HttpServletRequest request) 
        throws ServletException 
    {
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

    private static Object resolveModelParam(Parameter parameter, ModelParam modelParam, 
                                            HttpServletRequest request) 
        throws ServletException 
    {
        try {
            Object paramInstance = parameter.getType().getDeclaredConstructor().newInstance();
            populateModelFields(paramInstance, request, modelParam.name());
            return paramInstance;
        } 
        catch (Exception e) 
        {    throw new ServletException("Unable to instantiate parameter: " + parameter.getType().getName(), e);    }
    }

    private static void populateModelFields(Object instance, HttpServletRequest request, String attributeName) 
        throws ServletException 
    {
        for (Field field : instance.getClass().getDeclaredFields()) {
            ModelField modelField = field.getAnnotation(ModelField.class);
            String paramName = (modelField != null && !modelField.name().isEmpty()) ? modelField.name() : field.getName();
            String paramValue = request.getParameter(attributeName + "." + paramName);
            
            if (paramValue != null) 
            {    setFieldValue(instance, field, paramValue);    }
        }
    }

    private static void setFieldValue(Object instance, Field field, String value) 
        throws ServletException 
    {
        try {
            field.setAccessible(true);
            field.set(instance, convertToParameterType(field.getType(), value));
        } 
        
        catch (IllegalAccessException e) 
        {    throw new ServletException("Unable to set field value: " + field.getName(), e);    }
    }

    private static Object convertToParameterType(Class<?> type, String value) {
        if (value == null || value.isEmpty()) return getDefaultParameterValue(type);
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
    }

    private static String getDefaultParameterValue(Class<?> type) {
        if (type.equals(String.class)) return "";
        if (type.equals(int.class) || type.equals(Integer.class)) return "0";
        if (type.equals(long.class) || type.equals(Long.class)) return "0";
        if (type.equals(double.class) || type.equals(Double.class)) return "0.0";
        if (type.equals(boolean.class) || type.equals(Boolean.class)) return "false";
        return null;
    }

    private static boolean isHttpMethodValid(Mapping mapping, String requestMethod) {
        for (VerbAction verbAction : mapping.getVerbMethodes()) {
            String mappedVerb = verbAction.getVerbe();
            if (mappedVerb.equalsIgnoreCase(requestMethod)) {
                return true;
            }
        }
        return false;
    }

    public static void initializeMySessionAttributes(Object controllerInstance, HttpServletRequest request) 
        throws IllegalAccessException 
    {
        for (Field field : controllerInstance.getClass().getDeclaredFields()) {
            if (field.getType().equals(MySession.class)) {
                field.setAccessible(true);
                field.set(controllerInstance, new MySession(request.getSession()));
            }
        }
    }

    public static void processMethodResult(Object result, Method method, 
                                            PrintWriter out, HttpServletRequest request, 
                                            HttpServletResponse response) 
        throws ServletException, IOException 
    {
        if (result == null) 
        {    out.println("<p>Method executed, no result to display.</p>");   return;    }

        out.println("<p>Method result:</p>");
        
        if (result instanceof ModelView) 
        {    handleModelView((ModelView) result, request, response);     }

        else if (method.isAnnotationPresent(RestApi.class)) 
        {
            response.setContentType("application/json");
            Gson gson = new Gson();
            out.println(gson.toJson(result));
        } 

        else 
        {    out.println(result.toString());   }
    }

    // Handle a ModelView result (Forward to JSP)
    public static void handleModelView(ModelView modelView, HttpServletRequest request, 
                                        HttpServletResponse response) 
        throws ServletException, IOException 
    {
        modelView.getData().forEach(request::setAttribute);
        request.getRequestDispatcher("/" + modelView.getUrl()).forward(request, response);
    }

    // Handle errors (forward to error page)
    public static void handleError(String errorMessage, HttpServletRequest request, 
                                    HttpServletResponse response) 
        throws ServletException, IOException 
    {
        request.setAttribute("errorMessage", errorMessage);
        request.getRequestDispatcher("/error.jsp").forward(request, response);
    }

    public static String setVerbString( Method method ) {

        if (method.isAnnotationPresent(Get.class)) 
        {    return "get";      } 
        else if (method.isAnnotationPresent(Post.class)) 
        {    return "post";     }

        // Si aucune annotation n'est trouvée, get automatique    
        return "get";
    }

    public static HashMap<String, String> getFormParameters(HttpServletRequest request) {
        HashMap<String, String> formData = new HashMap<>();
        
        // Récupérer tous les paramètres du formulaire
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                formData.put(key, values[0]);
            }
        });
        
        return formData;
    }
    
    public static void findMethodsAnnotated(Class<?> controllerClass, HashMap<String, Mapping> methodList) {
        Method[] methods = controllerClass.getDeclaredMethods();
    
        for (Method method : methods) {
            if (method.isAnnotationPresent(Url.class)) {
                Url urlAnnotation = method.getAnnotation(Url.class);
                String url = urlAnnotation.value();
    
                // Vérifier le verbe HTTP
                String verb = setVerbString(method);
                
                // Créer le mapping à ajouter
                Mapping mapping = new Mapping(controllerClass.getName(), new VerbAction(verb, method.getName()));
    
                // Vérifier si l'URL existe déjà dans le methodList avec la même action (GET/POST)
                if (!isMappingDuplicate(methodList, url, verb)) {
                    methodList.put(url, mapping);
                } 
                else 
                {    System.out.println("Duplicate method found for URL: " + url + " with HTTP verb: " + verb);     }
            }
        }
    }
    
    // Vérifier si l'URL et l'action (GET/POST) existent déjà dans le methodList
    public static boolean isMappingDuplicate(HashMap<String, Mapping> methodList, String url, String verb) {
        Mapping existingMapping = methodList.get(url);
        if (existingMapping != null) {
            // Parcourir tous les VerbActions dans le mapping existant
            for (VerbAction verbAction : existingMapping.getVerbMethodes()) {
                // Vérifier si le verbe existe déjà
                if (verbAction.getVerbe().equalsIgnoreCase(verb)) {
                    return true; // Duplication trouvée
                }
            }
        }
        return false;
    }
    

}
