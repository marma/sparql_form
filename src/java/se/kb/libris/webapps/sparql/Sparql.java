/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kb.libris.webapps.sparql;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openrdf.query.*;
import org.openrdf.query.parser.*;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 *
 * @author marma
 */
public class Sparql extends HttpServlet {
    public static int N3 = 0x1;
    public static int SPARQL = 0x2;
    public static int RDFXML = 0x3;
    public static int TEXT = 0x4;
    public static int HTML = 0x5;
    public static int UNKNOWN = 0x6;
    public static int MAX_QUERY_TIME = 60;

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
        int format = getFormat(request);
        String query = request.getParameter("query");

        if (query != null) {
            QueryParser parser = new SPARQLParser();
            ParsedQuery q = null;

            try {
                q = parser.parseQuery(query, null);
            } catch (MalformedQueryException e) {
                doParseException(response, e);
                return;
            }

            if (q instanceof ParsedBooleanQuery) {
                doBoolean(response, query, format);
            } else if (q instanceof ParsedGraphQuery) {
                doGraph(response, query, format);
            } else if (q instanceof ParsedTupleQuery) {
                doTuple(response, query, format);
            }
        }
    } 

    private void doBoolean(HttpServletResponse response, String query, int format) {
        try {
            RepositoryConnection connection = getConnection();
            BooleanQuery booleanQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL, query);
            booleanQuery.setMaxQueryTime(MAX_QUERY_TIME);
            boolean result = booleanQuery.evaluate();

            response.setContentType("application/sparql-results+xml");
            response.setCharacterEncoding("UTF-8");

            
        } catch (QueryEvaluationException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedQueryException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void doGraph(HttpServletResponse response, String query, int format) {
        try {
            RepositoryConnection connection = getConnection();
        } catch (RepositoryException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void doParseException(HttpServletResponse response, MalformedQueryException e) throws IOException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        writer.println("4: " + e.getMessage());
        writer.close();
    }

    private void doTuple(HttpServletResponse response, String query, int format) {
        //SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);
        //TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
        //tupleQuery.evaluate(sparqlWriter);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private synchronized RepositoryConnection getConnection() throws RepositoryException {
        return repository.getConnection();
    }
    private int getFormat(HttpServletRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
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
