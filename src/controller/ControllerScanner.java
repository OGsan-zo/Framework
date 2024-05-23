package controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import annotation.MyAnnotation;

public class ControllerScanner {

    public List<Class<?>> findControllers(String packageName) throws ClassNotFoundException, IOException {
        List<Class<?>> controllers = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            throw new IllegalStateException("ClassLoader is null");
        }

        String path = packageName.replace('.', '/');
        System.out.println("Looking for resources in path: " + path); // Debug

        Enumeration<URL> resources = classLoader.getResources(path);

        if (resources == null || !resources.hasMoreElements()) {
            System.out.println("No resources found for path: " + path); // Debug
        } else {
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                System.out.println("Found resource: " + resource.getFile()); // Debug
                controllers.addAll(findClasses(new File(resource.getFile()), packageName));
            }
        }

        return controllers;
    }

    private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            System.out.println("Directory does not exist: " + directory.getPath()); // Debug
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            System.out.println("No files found in directory: " + directory.getPath()); // Debug
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                Class<?> clazz = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                if (clazz.isAnnotationPresent(MyAnnotation.class)) {
                    classes.add(clazz);
                    System.out.println("Found annotated class: " + clazz.getName()); // Debug
                }
            }
        }
        return classes;
    }
}
