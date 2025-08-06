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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import annotation.*;
import annotation.field.ModelField;
import annotation.methods.Get;
import annotation.methods.Post;
import annotation.methods.RestApi;
import annotation.methods.Url;
import exception.*;

public class Utils {
    private static String pathDestinationFile;

    static {
        // Charger les propriétés depuis auth.properties
        Properties properties = new Properties();
        try (InputStream input = Utils.class.getClassLoader().getResourceAsStream("auth.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find auth.properties");
            }
            properties.load(input);
            pathDestinationFile = properties.getProperty("file_storage_path");
            if (pathDestinationFile == null || pathDestinationFile.trim().isEmpty()) {
                throw new RuntimeException("file_storage_path not specified in auth.properties");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading auth.properties: " + e.getMessage(), e);
        }
    }

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

    public static void displayControllerList(PrintWriter out, HashMap<String, Mapping> methodList) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <title>Framework Controllers</title>");
        out.println("    <style>");
        out.println("        body { font-family: Arial, sans-serif; margin: 20px; }");
        out.println("        h1 { color: #2c3e50; }");
        out.println("        .controller-block { background: #f8f9fa; border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin-bottom: 20px; }");
        out.println("        .url { font-weight: bold; color: #e74c3c; }");
        out.println("        .method { margin-left: 20px; }");
        out.println("        .verb { display: inline-block; width: 60px; font-weight: bold; }");
        out.println("        .verb.get { color: #27ae60; }");
        out.println("        .verb.post { color: #e67e22; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Liste des contrôleurs disponibles</h1>");

        // Organiser par classe contrôleur
        Map<String, List<Map.Entry<String, Mapping>>> controllers = new HashMap<>();
        methodList.entrySet().forEach(entry -> {
            String className = entry.getValue().getClassName();
            controllers.computeIfAbsent(className, k -> new ArrayList<>()).add(entry);
        });

        controllers.forEach((className, entries) -> {
            out.println("<div class='controller-block'>");
            out.println("    <h2>Classe: " + className + "</h2>");
            
            entries.forEach(entry -> {
                String url = entry.getKey();
                Mapping mapping = entry.getValue();
                
                out.println("    <div>");
                out.println("        <p><span class='url'>URL: " + url + "</span></p>");
                out.println("        <div class='method'>");
                out.println("            <p>Méthodes:</p>");
                out.println("            <ul>");
                
                for (VerbAction verbAction : mapping.getVerbMethodes()) {
                    out.println("                <li><span class='verb " + verbAction.getVerbe().toLowerCase() + "'>" + 
                            verbAction.getVerbe().toUpperCase() + "</span>: " + verbAction.getMethode() + "</li>");
                }
                
                out.println("            </ul>");
                out.println("        </div>");
                out.println("    </div>");
            });
            
            out.println("</div>");
        });

        out.println("</body>");
        out.println("</html>");
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
        Mapping mapping = methodList.get(relativeURI);
        
        if (mapping == null) {
            // Si aucun mapping trouvé, renvoyer une erreur 404
            handleError404(request, response);
            return;
        }
        
        if (!isHttpMethodValid(mapping, request.getMethod())) {
             handleError(request, response, 
                HttpServletResponse.SC_BAD_REQUEST,
                "Méthode non autorisée",
                "HTTP method " + request.getMethod() + " is not allowed for this endpoint",
                null);
            return;
        }

        out.println("<p>Executing method:</p>");
        invokeMethod(mapping, out, request, response, formData);
    }

    public static Method findMethod(Class<?> clazz, String methodName) 
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

            System.out.println(e.getMessage() + " Suite de probleme" );
            e.printStackTrace();
            
            handleError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Erreur d'exécution",
                "Error invoking method: " + e.getMessage(),
                e);
                
        }
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
                FileUpload fileUpload = new FileUpload(fileName, pathDestinationFile, fileData);
                paramValues[i] = fileUpload;
            } 
            else {
                // Gérer les autres types de paramètres
                paramValues[i] = resolveParameterValue(parameters[i], param, modelParam, request);
            }
        }

        return paramValues;
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
                        FileUpload fileUpload = new FileUpload(fileName, pathDestinationFile, fileData);
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
        if (instance == null || field == null) {
            throw new ServletException("Instance ou champ null lors de la définition de la valeur");
        }
        try {
            field.setAccessible(true);
            field.set(instance, convertToParameterType(field.getType(), value));
        } catch (IllegalAccessException e) {
            throw new ServletException("Unable to set field value: " + field.getName(), e);
        } catch (IllegalArgumentException e) {
            throw new ServletException("Erreur de conversion pour le champ: " + field.getName() + " avec la valeur: " + value, e);
        }
    }

    private static Object convertToParameterType(Class<?> type, String value) {
        if (value == null || value.trim().isEmpty()) {
            return getDefaultParameterValue(type);
        }
        
        String trimmedValue = value.trim();
        
        try {
            // Types existants
            if (type == String.class) return trimmedValue;
            if (type == int.class || type == Integer.class) return Integer.parseInt(trimmedValue);
            if (type == long.class || type == Long.class) return Long.parseLong(trimmedValue);
            if (type == double.class || type == Double.class) return Double.parseDouble(trimmedValue);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(trimmedValue);
            
            // Types numériques étendus
            if (type == float.class || type == Float.class) return Float.parseFloat(trimmedValue);
            if (type == short.class || type == Short.class) return Short.parseShort(trimmedValue);
            if (type == byte.class || type == Byte.class) return Byte.parseByte(trimmedValue);
            
            // BigDecimal pour les calculs précis
            if (type == BigDecimal.class) {
                return new BigDecimal(trimmedValue);
            }
            
            // Gestion des dates Java 8+
            if (type == LocalDate.class) {
                return parseLocalDate(trimmedValue);
            }
            
            if (type == LocalDateTime.class) {
                return parseLocalDateTime(trimmedValue);
            }
            
            // Gestion des dates classiques
            if (type == Date.class) {
                return parseDate(trimmedValue);
            }
            
            // Gestion des énumérations
            if (type.isEnum()) {
                return parseEnum(type, trimmedValue);
            }
            
            throw new IllegalArgumentException("Type non supporté: " + type.getName());
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur numérique invalide pour le type " + type.getSimpleName() + ": " + trimmedValue, e);
        } catch (DateTimeParseException | ParseException e) {
            throw new IllegalArgumentException("Format de date invalide pour le type " + type.getSimpleName() + ": " + trimmedValue, e);
        }
    }


    private static Object getDefaultParameterValue(Class<?> type) {
        if (type.equals(String.class)) return null;
        if (type.equals(int.class) || type.equals(Integer.class)) return null;
        if (type.equals(long.class) || type.equals(Long.class)) return null;
        if (type.equals(double.class) || type.equals(Double.class)) return null;
        if (type.equals(float.class) || type.equals(Float.class)) return null;
        if (type.equals(short.class) || type.equals(Short.class)) return null;
        if (type.equals(byte.class) || type.equals(Byte.class)) return null;
        if (type.equals(boolean.class) || type.equals(Boolean.class)) return null;
        if (type.equals(BigDecimal.class)) return null;
        if (type.equals(LocalDate.class)) return null;
        if (type.equals(LocalDateTime.class)) return null;
        if (type.equals(Date.class)) return null;
        if (type.isEnum()) return null;
        return null;
    }

    private static LocalDate parseLocalDate(String value) {
        // Essayer plusieurs formats couramment utilisés
        String[] patterns = {
            "yyyy-MM-dd",      // Format HTML5 date input
            "dd/MM/yyyy",      // Format français
            "MM/dd/yyyy",      // Format américain
            "dd-MM-yyyy"       // Format avec tirets
        };
        
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // Continuer avec le format suivant
            }
        }
        
        throw new DateTimeParseException("Impossible de parser la date: " + value, value, 0);
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        // Essayer plusieurs formats couramment utilisés
        String[] patterns = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "yyyy-MM-ddTHH:mm:ss",  // Format ISO
            "yyyy-MM-ddTHH:mm"
        };
        
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // Continuer avec le format suivant
            }
        }
        
        throw new DateTimeParseException("Impossible de parser la date/heure: " + value, value, 0);
    }

    private static Date parseDate(String value) throws ParseException {
        // Essayer plusieurs formats couramment utilisés
        String[] patterns = {
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss"
        };
        
        for (String pattern : patterns) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                formatter.setLenient(false); // Mode strict
                return formatter.parse(value);
            } catch (ParseException e) {
                // Continuer avec le format suivant
            }
        }
        
        throw new ParseException("Impossible de parser la date: " + value, 0);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T parseEnum(Class<?> enumType, String value) {
        try {
            return Enum.valueOf((Class<T>) enumType, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Essayer aussi avec la casse exacte
            try {
                return Enum.valueOf((Class<T>) enumType, value);
            } catch (IllegalArgumentException e2) {
                throw new IllegalArgumentException("Valeur d'énumération invalide '" + value + "' pour le type " + enumType.getSimpleName());
            }
        }
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
        
        if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;
            if (modelView.isRedirect()) {
                // Gestion des messages flash avant redirection
                if (!modelView.getData().isEmpty()) {
                    HttpSession session = request.getSession();
                    session.setAttribute("flashData", modelView.getData());
                }
                // Effectuer une redirection HTTP
                response.sendRedirect(request.getContextPath() + "/" + modelView.getUrl());
                return;
            } else {
                // Forward comme avant
                handleModelView(modelView, request, response);
                return;
            }
        }

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
        // Récupérer les messages flash de la session
        HttpSession session = request.getSession();
        Map<String, Object> flashData = (Map<String, Object>) session.getAttribute("flashData");
        if (flashData != null) {
            modelView.getData().putAll(flashData);
            session.removeAttribute("flashData");
        }
        
        modelView.getData().forEach(request::setAttribute);
        request.getRequestDispatcher("/" + modelView.getUrl()).forward(request, response);
    }


    // Handle errors (forward to error page)
    public static void handleError(HttpServletRequest request, HttpServletResponse response, 
                                int statusCode, String errorTitle, String errorMessage, 
                                Throwable exception) throws ServletException, IOException {
        response.setStatus(statusCode);
        request.setAttribute("errorTitle", errorTitle);
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("exception", exception);
        request.setAttribute("stackTrace", getStackTrace(exception));
        request.setAttribute("requestDetails", getRequestDetails(request));
        
        // Vous pouvez créer un fichier error.jsp ou utiliser cette version HTML de base
        PrintWriter out = response.getWriter();
        displayErrorPage(out, statusCode, errorTitle, errorMessage, exception, request);
    }

    private static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private static Map<String, String> getRequestDetails(HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        details.put("Method", request.getMethod());
        details.put("URI", request.getRequestURI());
        details.put("Query String", request.getQueryString());
        details.put("Remote Address", request.getRemoteAddr());
        return details;
    }

    public static void displayErrorPage(PrintWriter out, int statusCode, String errorTitle, 
                                    String errorMessage, Throwable exception, 
                                    HttpServletRequest request) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <title>Erreur " + statusCode + " - " + errorTitle + "</title>");
        out.println("    <style>");
        out.println("        body { font-family: Arial, sans-serif; line-height: 1.6; margin: 0; padding: 20px; background-color: #f8f9fa; }");
        out.println("        .error-container { max-width: 1000px; margin: 0 auto; background: white; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }");
        out.println("        h1 { color: #dc3545; }");
        out.println("        .error-section { margin-bottom: 20px; padding: 15px; background: #f8f9fa; border-radius: 5px; }");
        out.println("        .error-section h2 { margin-top: 0; color: #6c757d; }");
        out.println("        pre { background: #2d2d2d; color: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto; }");
        out.println("        .request-details { display: grid; grid-template-columns: 150px 1fr; gap: 10px; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='error-container'>");
        out.println("        <h1>" + statusCode + " - " + errorTitle + "</h1>");
        out.println("        <p>" + errorMessage + "</p>");
        
        out.println("        <div class='error-section'>");
        out.println("            <h2>Détails de la requête</h2>");
        out.println("            <div class='request-details'>");
        out.println("                <div><strong>URL:</strong></div>");
        out.println("                <div>" + request.getRequestURL() + "</div>");
        out.println("                <div><strong>Méthode:</strong></div>");
        out.println("                <div>" + request.getMethod() + "</div>");
        out.println("                <div><strong>Adresse IP:</strong></div>");
        out.println("                <div>" + request.getRemoteAddr() + "</div>");
        out.println("            </div>");
        out.println("        </div>");
        
        if (exception != null) {
            out.println("        <div class='error-section'>");
            out.println("            <h2>Détails de l'erreur</h2>");
            out.println("            <p><strong>Type:</strong> " + exception.getClass().getName() + "</p>");
            out.println("            <p><strong>Message:</strong> " + exception.getMessage() + "</p>");
            out.println("        </div>");
            
            out.println("        <div class='error-section'>");
            out.println("            <h2>Stack Trace</h2>");
            out.println("            <pre>" + getStackTrace(exception) + "</pre>");
            out.println("        </div>");
        }
        
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }

    public static void logError(Throwable exception, HttpServletRequest request) {
        System.err.println("=== ERROR LOG ===");
        System.err.println("Timestamp: " + new Date());
        System.err.println("Request URL: " + request.getRequestURL());
        System.err.println("Method: " + request.getMethod());
        System.err.println("Error: " + exception.getClass().getName());
        System.err.println("Message: " + exception.getMessage());
        System.err.println("Stack Trace:");
        exception.printStackTrace();
        System.err.println("=== END ERROR LOG ===");
    }


    // Méthode pour gérer l'erreur 404
    public static void handleError404(HttpServletRequest request ,HttpServletResponse response) 
        throws ServletException, IOException 
    {
        handleError(request, response,
            HttpServletResponse.SC_NOT_FOUND,
            "Page non trouvée",
            "The requested URL was not found on this server",
            null);
    }

    public static String setVerbString( Method method ) {
        if (method.isAnnotationPresent(Get.class)) 
        {    return "get";      } 
        else if (method.isAnnotationPresent(Post.class)) 
        {    return "post";     }
        return "get";
    }

    public static HashMap<String, String> getFormParameters(HttpServletRequest request) {
        HashMap<String, String> formData = new HashMap<>();
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
            for (VerbAction verbAction : existingMapping.getVerbMethodes()) {
                if (verbAction.getVerbe().equalsIgnoreCase(verb)) {
                    return true; // Duplication trouvée
                }
            }
        }
        return false;
    }
}