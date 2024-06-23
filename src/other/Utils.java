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

import annotation.Get;
import annotation.Param;

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
        out.println("<hr><h2>Form Data</h2>");
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
            Object result = executeControllerMethod(mapping, request);
            processMethodResult(result, out, request, response);
        } catch (Exception e) {
            out.println("<p>Error invoking method: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }

    public static Object executeControllerMethod(Mapping mapping, HttpServletRequest request) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException ,ServletException{
        Class<?> cls = Class.forName(mapping.getClassName());
        Method method = findMethodWithRequestParams(cls, mapping.getMethodName());
        Object obj = cls.getConstructor().newInstance();
        Object[] params = getMethodParams(method, request);
        return method.invoke(obj, params);
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

        if (param != null) {
            String paramName = param.name();
            String paramValue = request.getParameter(paramName);
            paramValues[i] = convertToParameterType(parameters[i].getType(), paramValue);
            
        } else if (modelParam != null) {
            Class<?> paramType = parameters[i].getType();
            Object paramInstance;
                try {
                    paramInstance = paramType.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new ServletException("Unable to instantiate parameter: " + paramType.getName(), e);
                }
                populateModelFields(paramInstance, request , modelParam.name());
                paramValues[i] = paramInstance;
            } else {
                throw new ServletException("Cannot find param or modelParam annotation for parameter: " + parameters[i].getName());
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
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        // Add more type conversions as needed
        throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
    }
    
    public static void processMethodResult(Object result, PrintWriter out, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (result instanceof String) {
            out.println("<p>Result: " + result + "</p>");
        } else if (result instanceof ModelView) {
            handleModelView((ModelView) result, request, response);
        } else {
            throw new ServletException("Unsupported return type: " + result.getClass().getName());
        }
        out.println("<p>Method executed successfully.</p>");
    }

    public static void handleModelView(ModelView modelView, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = modelView.getUrl();
        if (url == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "La vue specifiee est introuvable");
            return;
        }
        modelView.getData().forEach(request::setAttribute);
        RequestDispatcher dispatcher = request.getRequestDispatcher(url);
        dispatcher.forward(request, response);
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
}
