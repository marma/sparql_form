/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.Normalizer.Form;
import java.util.Iterator;
import javax.servlet.ServletConfig;
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
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 *
 * @author marma
 */
public class Sparql extends HttpServlet {
    static Repository repository = null;

    @Override
    public void init() throws ServletException {
        try {
            File dataDir = new File(getServletConfig().getInitParameter("RepositoryDirectory"));
            repository = new SailRepository(new NativeStore(dataDir));
            repository.initialize();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            repository.shutDown();
        } catch (Exception e) {
            
        }

        super.destroy();
    }

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
        RepositoryConnection con = null;
        int max = (request.getParameter("max") != null && !request.getParameter("max").equals(""))? (Integer.parseInt(request.getParameter("max"))):100;

        try {
            if ((format != null && format.equalsIgnoreCase("html")) || query == null) {
                response.setContentType("text/html; charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
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
                    query = java.text.Normalizer.normalize(query, Form.NFD);
                    out.println("\n" + query);
                }

                out.println("</textarea>");
                out.println("      <br/>");
                out.println("      max: <select name=\"max\">");
                out.println("        <option value=\"100\">100</option");
                out.println("        <option value=\"500\">500</option");
                out.println("        <option value=\"1000\">1000</option");
                out.println("      </select><br/>");
                out.println("      format: <select name=\"format\">");
                out.println("        <option value=\"HTML\">HTML</option");
                out.println("        <option value=\"XML\">XML</option");
                out.println("      </select><br/>");
                out.println("      <input type=\"submit\">");
                out.println("    </form>");

                if (query != null) {
                    con = repository.getConnection();
                    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
                    TupleQueryResult result = tupleQuery.evaluate();

                    out.println("    <table border=\"1\">");
                    out.println("      <tr>");

                    for (String name: result.getBindingNames()) {
                        out.println("        <td>" + name + "</td>");
                    }

                    out.println("      </tr>");

                    int n=1;
                    while (result.hasNext() && n++ < max) {
                        BindingSet bs = result.next();

                        out.println("      <tr>");

                        for (String name: result.getBindingNames()) {
                            out.println("        <td>" + bs.getBinding(name).getValue() + "</td>");
                        }


                        /*Iterator<Binding> iter = bs.iterator();
                        while (iter.hasNext()) {
                            out.println("        <td>" + iter.next().getValue() + "</td>");
                        }*/

                        out.println("      </tr>");
                    }

                    out.println("    </table>");

                    con.close();
                }

                out.println("  </body>\n");
                out.println("</html>\n");
            } else {
                response.setContentType("text/xml; charset=UTF-8");
                OutputStream out = response.getOutputStream();
                con = repository.getConnection();
                SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);

                query = java.text.Normalizer.normalize(query, Form.NFC);
                TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
                tupleQuery.evaluate(sparqlWriter);
                out.close();
            }
        } catch (Throwable e) {
            throw new ServletException(e);
        } finally {
            try { con.close(); } catch (Exception e) {}
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
