package other;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import annotation.*;
import annotation.field.ModelField;
import annotation.methods.Get;
import annotation.methods.Post;
import annotation.methods.RestApi;
import annotation.methods.Url;
import exception.*;

public class Utils {
    static String pathDestinationFile = "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\Test\\assets\\file";  


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
    throws ServletException, IOException, NoSuchMethodException, ClassNotFoundException, ValidationException 
    {
        if (relativeURI == null || relativeURI.trim().isEmpty()) {
            out.println("<h1>Welcome to the Home Page!</h1>");
            return;
        }
        
        Mapping mapping = methodList.get(relativeURI);
        
        if (mapping == null) {
            // Si aucun mapping trouvé, renvoyer une erreur 404
            handleError404(request, response);
            return;
        }
        
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
        throws ServletException, IOException, ValidationException 
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
            
            if (e instanceof ValidationException) throw (ValidationException) e;

            System.out.println(e.getMessage() + "Suite de probleme" );
            e.printStackTrace();
            
            handleError("Error invoking method: " + e.getMessage(), request, response);
        }

        
        // Throw encore l'exception du bas 
    }

    public static Object executeControllerMethod(Mapping mapping, HttpServletRequest request, 
                                                Object controllerInstance, HttpServletResponse response) 
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException, ServletException, ValidationException 
    {
        Object obj = new Object(); 
        ValidateForm checkers = new ValidateForm();

        for (VerbAction verbAction : mapping.getVerbMethodes()) {
            Method method = findMethod(controllerInstance.getClass(), verbAction.getMethode());
            Object[] params = getMethodParams(method, request);
            obj = method.invoke(controllerInstance, params);
        }
        
        return obj;
        
        
        // Throw encore l'exception du bas 
    }

    // Get method parameters from the request
    public static Object[] getMethodParams(Method method, HttpServletRequest request) 
        throws ServletException, IOException, ValidationException 
    {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Param param = parameters[i].getAnnotation(Param.class);
            ModelParam modelParam = parameters[i].getAnnotation(ModelParam.class);

            if (parameters[i].getType().equals(FileUpload.class)) {
                // Récupérer le fichier
                Part filePart = request.getPart(param.name());
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                InputStream fileContent = filePart.getInputStream();

                // Convertir en byte[]
                byte[] fileData = new byte[fileContent.available()];
                fileContent.read(fileData);

                // Créer une instance de FileUpload
                FileUpload fileUpload = new FileUpload(fileName, pathDestinationFile , fileData);
                paramValues[i] = fileUpload;
            } 
            
            else {
                // Gérer les autres types de paramètres
                paramValues[i] = resolveParameterValue(parameters[i], param, modelParam, request);
            }
        }

        return paramValues;

        
        // Throw encore l'exception du bas 
    }


    private static Object resolveParameterValue(Parameter parameter, Param param, 
                                                ModelParam modelParam, HttpServletRequest request) 
        throws ServletException, ValidationException 
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

    private static Object resolveModelParam(Parameter parameter, ModelParam modelParam, HttpServletRequest request) 
        throws ServletException, ValidationException 
    {
        try {
            // Instanciation de l'objet
            Object paramInstance = parameter.getType().getDeclaredConstructor().newInstance();

            // Gestion du nom d'attribut
            String attributeName = modelParam.name();
            if (attributeName == null || attributeName.isEmpty()) {
                attributeName = parameter.getName();
            }

            // Population des champs
            populateModelFields(paramInstance, request, attributeName);
            
            // Obtention de l'URL de redirection depuis l'annotation
            String redirectUrl = modelParam.redirectOnError();
            if (redirectUrl == null || redirectUrl.isEmpty()) {
                throw new ServletException("redirectOnError must be specified in @ModelParam annotation");
            }
            
            try {
                // Validation
                ValidateForm validator = new ValidateForm();
                validator.validateObject(paramInstance);
                return paramInstance;
            } catch (ValidationException ve) {
                // Configuration de la ModelView pour la redirection
                ModelView errorView = new ModelView();
                errorView.setUrl(redirectUrl);
                
                // On utilise directement getValidationErrors() pour récupérer les erreurs
                errorView.add("fieldErrors", ve.getValidationErrors());
                
                // Pour les valeurs des champs, on doit les récupérer à partir de l'objet
                Map<String, String> fieldValues = new HashMap<>();
                for (Field field : paramInstance.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(paramInstance);
                    if (value != null) {
                        fieldValues.put(field.getName(), value.toString());
                    }
                }
                errorView.add("fieldValues", fieldValues);
                
                // Configuration de l'exception
                ve.setModelView(errorView);
                ve.setRedirectUrl(redirectUrl);
                
                throw ve;
            }
        } catch (Exception e) {
            if (e instanceof ValidationException) throw (ValidationException) e;
            throw new ServletException("Unable to instantiate parameter: " + parameter.getType().getName(), e);
        }
    }
    
    private static void populateModelFields(Object instance, HttpServletRequest request, String attributeName) 
        throws ServletException 
    {
        
        for (Field field : instance.getClass().getDeclaredFields()) {
            ModelField modelField = field.getAnnotation(ModelField.class);
            String paramName = (modelField != null && !modelField.name().isEmpty()) ? modelField.name() : field.getName();
            
            // Gérer le type FileUpload
            if (field.getType().equals(FileUpload.class)) {
                Part filePart;
                try {
                    // Récupérer le fichier depuis la requête
                    filePart = request.getPart(attributeName + "." + paramName);
                    
                    if (filePart != null && filePart.getSize() > 0) {
                        // Lire le contenu du fichier
                        InputStream fileContent = filePart.getInputStream();
                        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                        
                        // Convertir en byte[]
                        byte[] fileData = new byte[fileContent.available()];
                        fileContent.read(fileData);
                        
                        // Créer une instance de FileUpload et l'assigner au champ
                        FileUpload fileUpload = new FileUpload(fileName, Utils.pathDestinationFile, fileData);
                        field.setAccessible(true);
                        field.set(instance, fileUpload);
                    }

                } catch (IOException | ServletException e) {
                    throw new ServletException("Erreur lors du traitement du fichier : " + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    throw new ServletException("Impossible d'assigner le champ FileUpload : " + e.getMessage(), e);
                }
            } else {
                // Gérer les types de paramètres normaux
                String paramValue = request.getParameter(attributeName + "." + paramName);

                if (paramValue != null) {
                    setFieldValue(instance, field, paramValue);
                }
            }
        }
    }

    
    private static void setFieldValue(Object instance, Field field, String value) 
        throws ServletException 
    {
        try {
            field.setAccessible(true);
            // IF misy erreur ao @ lay verification de valeur
            // De tsy manao set
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
        response.getWriter().println(errorMessage);
        // request.getRequestDispatcher("/error.jsp").forward(request, response);
    }

    // Méthode pour gérer l'erreur 404
    public static void handleError404(HttpServletRequest request ,HttpServletResponse response) 
        throws ServletException, IOException 
    {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND); // Réponse HTTP 404
        response.getWriter().println("<h1>404 - Page Not Found</h1>");
        response.getWriter().println("<p>The requested URL was not found on this server.</p>");

        // Vous pouvez aussi rediriger vers une page dédiée comme :
        // request.getRequestDispatcher("/error404.jsp").forward(request, response);
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
                if (!isMappingDuplicate(methodList, url, verb)) 
                {    methodList.put(url, mapping);    } 

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