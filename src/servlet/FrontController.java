package servlet;

import java.io.*;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.ServletContext;

import controller.*;

public class FrontController extends HttpServlet {

    private String controllerPackage;
    private ControllerScanner scanner;
    private List<Class<?>> controllers;

    @Override
    public void init(ServletConfig config)
            throws ServletException {
        try {
            super.init(config);

            ServletContext context = config.getServletContext();
            controllerPackage = context.getInitParameter("base_package");

            this.scanner = new ControllerScanner();
            this.controllers = scanner.findControllers(controllerPackage);
        }

        catch (Exception e) {
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
        try {
            PrintWriter out = response.getWriter();
            String url = request.getRequestURI();

            out.println("<h1>" + " Hello World " + "</h1>");
            out.println("<h2>" + " You are here now: " + "</h2>");
            out.println("<h3> URL: " + url + " </h3>");

            for (Class<?> controller : this.controllers) {
                out.println("Found controller: " + controller.getName() + "<br>");
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}