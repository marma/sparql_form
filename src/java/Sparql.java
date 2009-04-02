/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.openrdf.query.resultio.text.BooleanTextWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.http.HTTPRepository;

/**
 *
 * @author marma
 */
public class Sparql extends HttpServlet {
   
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String format = request.getParameter("format");
        String query = request.getParameter("query");
        String sesameServer = "http://ratat3.libris.kb.se:8080/openrdf-sesame";
        String repositoryID = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/"));

        try {
            if (format == null || format.equals("html")) {
                response.setContentType("text/html; charset=UTF-8");
                PrintWriter out = response.getWriter();

                out.println("<html>");
                out.println("  <body>");
                out.println("    <form>");
                out.print("      <textarea cols=\"80\" rows=\"20\" name=\"query\">");

                if (query == null) {
                    out.println("PREFIX dc:<http://purl.org/dc/elements/1.1/>");
                    out.println("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>");
                    out.println("PREFIX foaf:<http://xmlns.com/foaf/0.1/>");
                    out.println("PREFIX libris:<http://libris.kb.se/vocabulary/experimental#>");
                    out.println("PREFIX dbpedia:<http://dbpedia.org/property/>");
                    out.println("PREFIX owl:<http://www.w3.org/2002/07/owl#>");
                    out.println("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
                    out.println("PREFIX skos:<http://www.w3.org/2004/02/skos/core#>");
                } else {
                    out.println("\n" + query);
                }

                out.println("</textarea>");
                out.println("      <br/>");
                out.println("      <input type=\"submit\">");
                out.println("    </form>");

                if (query != null) {
                    Repository myRepository = new HTTPRepository(sesameServer, repositoryID);
                    myRepository.initialize();
                    RepositoryConnection con = myRepository.getConnection();
                    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
                    TupleQueryResult result = tupleQuery.evaluate();

                    out.println("    <table border=\"1\">");
                    out.println("      <tr>");

                    for (String name: result.getBindingNames()) {
                        out.println("        <td>" + name + "</td>");
                    }

                    out.println("      </tr>");

                    while (result.hasNext()) {
                        BindingSet bs = result.next();

                        out.println("      <tr>");

                        Iterator<Binding> iter = bs.iterator();
                        while (iter.hasNext()) {
                            out.println("        <td>" + iter.next().getValue() + "</td>");
                        }

                        out.println("      </tr>");
                    }

                    out.println("    </table>");
                }

                out.println("  </body>\n");
                out.println("</html>\n");

                out.close();
            } else if (format.equalsIgnoreCase("xml")) {
                response.setContentType("text/xml; charset=UTF-8");
                OutputStream out = response.getOutputStream();
                Repository myRepository = new HTTPRepository(sesameServer, repositoryID);
                myRepository.initialize();
                RepositoryConnection con = myRepository.getConnection();
                SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);

                TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
                tupleQuery.evaluate(sparqlWriter);
                out.close();
            }

/*


            SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);

            //String queryString = "SELECT * WHERE { ?s ?p 'Malmsten, Martin, 1974-' . }";
            String queryString = request.getParameter("query");
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            tupleQuery.evaluate(sparqlWriter);
 */
        } catch (Throwable e) {
            throw new ServletException(e);
        } finally {
        }
    } 

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
