package servlet;

import java.io.IOException;
import java.io.PrintWriter;

public class FrontController extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<HTML>");
        out.println("<HEAD><TITLE>Hello Framework</TITLE></HEAD>");
        out.println("<BODY>");
        out.println("<BIG>Hello World</BIG>");
        out.println("</BODY></HTML>");

        out.println(processRequest(req, res));
    }

    protected String processRequest(HttpServletRequest request, HttpServletResponse response) {
        // Récupérer l'URL de la requête
        String url = request.getRequestURI().toString();
        System.out.println("URL entrée par l'utilisateur : " + url);

        return url;
    }
}