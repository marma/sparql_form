/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kb.libris.webapps.sparql;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openrdf.query.*;
import org.openrdf.query.parser.*;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 *
 * @author marma
 */
public class Sparql extends HttpServlet {
    public static String N3 = "N3";
    public static String SPARQL = "SPARQL";
    public static String RDFXML = "RDFXML";
    public static String TEXT = "TEXT";
    public static String HTML = "HTML";
    public static String JSON = "JSON";
    public static String UNKNOWN = "UNKNOWN";
    public static int MAX_QUERY_TIME = 60;
    static Repository repository = null;
    static String form = null;
    static String prefix = "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n" +
                    "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
                    "PREFIX libris:<http://libris.kb.se/vocabulary/experimental#>\n" +
                    "PREFIX dbpedia:<http://dbpedia.org/property/>\n" +
                    "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n";

    @Override
    public void init() throws ServletException {
        try {
            form = new Scanner(new File(getServletContext().getRealPath("form.html"))).useDelimiter("\\Z").next();

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
                String format = getFormat(request, HTML, SPARQL);
                doBoolean(response, query, format);
            } else if (q instanceof ParsedGraphQuery) {
                String format = getFormat(request, HTML, RDFXML, N3);
                doGraph(response, query, format);
            } else if (q instanceof ParsedTupleQuery) {
                String format = getFormat(request, HTML, SPARQL);
                doTuple(response, query, format);
            }
        } else {
            String format = getFormat(request, HTML);

            if (format.equals(HTML)) {
                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                PrintWriter writer = response.getWriter();
                writer.write(form.replace("<!-- QUERY -->", prefix));
                writer.close();
            } else {

            }
        }
    } 

    private void doBoolean(HttpServletResponse response, String query, String format) {
        RepositoryConnection connection = null;

        try {
            connection = getConnection();
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
        } finally {
            try { connection.close(); } catch (Exception e) {}
        }
    }

    private void doGraph(HttpServletResponse response, String query, String format) throws IOException {
        RepositoryConnection connection = null;
        OutputStream out = null;
        PrintWriter writer = null;

        try {
            connection = getConnection();
            GraphQuery graphQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
            graphQuery.setMaxQueryTime(MAX_QUERY_TIME);

            if (format.equals(N3)) {
                response.setContentType("text/rdf+n3");
                //response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                out = response.getOutputStream();
                N3Writer n3Writer = new N3Writer(out);
                graphQuery.evaluate(n3Writer);
                out.close();
            } else if (format.equals(RDFXML)) {
                response.setContentType("application/rdf+xml");
                //response.setContentType("text/xml");
                response.setCharacterEncoding("UTF-8");
                out = response.getOutputStream();
                RDFXMLPrettyWriter rdfWriter = new RDFXMLPrettyWriter(out);
                graphQuery.evaluate(rdfWriter);
            } else if (format.equals(HTML)) {
                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                StringWriter sw = new StringWriter();
                N3Writer n3Writer = new N3Writer(sw);
                graphQuery.evaluate(n3Writer);
                writer = response.getWriter();

                writer.write(form.replace("<!-- QUERY -->", query).replace("<!-- RESULT -->", "<pre>" + sw.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</pre>"));
            }

        } catch (QueryEvaluationException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RDFHandlerException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedQueryException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { connection.close(); } catch (Exception e) {}
            try { out.close(); } catch (Exception e) {}
            try { writer.close(); } catch (Exception e) {}
        }
    }

    private void doTuple(HttpServletResponse response, String query, String format) throws IOException {
        RepositoryConnection connection = null;
        OutputStream out = null;
        PrintWriter writer = null;

        try {
            connection = getConnection();
            TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            tupleQuery.setMaxQueryTime(MAX_QUERY_TIME);

            if (format.equals(SPARQL)) {
                response.setContentType("application/sparql-results+xml");
                response.setCharacterEncoding("UTF-8");
                out = response.getOutputStream();
                SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);
                tupleQuery.evaluate(sparqlWriter);
            } else if (format.equals(HTML)) {
                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                TupleQueryResult result = tupleQuery.evaluate();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);

                pw.println("    <table border=\"1\">");
                pw.println("      <tr>");

                for (String name: result.getBindingNames()) {
                    pw.println("        <td>" + name + "</td>");
                }

                pw.println("      </tr>");

                int n=1, max=1000;
                while (result.hasNext() && n++ < max) {
                    BindingSet bs = result.next();

                    pw.println("      <tr>");

                    for (String name: result.getBindingNames()) {
                        pw.print("        <td>");

                        if (bs.getBinding(name) != null) {
                            String value = bs.getBinding(name).getValue().toString();

                            /*if (value.startsWith("<")) {
                                String q = prefix + "select * where { " + value + " ?p ?o . }";
                                pw.print("<a href=\"?query=" + URLEncoder.encode(q, "UTF-8") + "#\">" + value + "</a>");
                            } else if (value.startsWith("\"")) {
                                pw.print(value);
                            } else {*/
                                pw.print(value);
                            //}
                        }

                        pw.println("        </td>");
                    }

                    pw.println("      </tr>");
                }

                pw.println("    </table>");
                pw.close();

                writer = response.getWriter();
                writer.print(form.replace("<!-- QUERY -->", query).replace("<!-- RESULT -->", sw.toString()));
            }
        } catch (QueryEvaluationException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TupleQueryResultHandlerException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedQueryException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            Logger.getLogger(Sparql.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { connection.close(); } catch (Exception e) {}
            try { out.close(); } catch (Exception e) {}
            try { writer.close(); } catch (Exception e) {}
        }

    }

    private void doParseException(HttpServletResponse response, MalformedQueryException e) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.println("4: " + e.getMessage());
        writer.close();
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    private synchronized RepositoryConnection getConnection() throws RepositoryException {
        return repository.getConnection();
    }
    private String getFormat(HttpServletRequest request, String ... formats) {
        if (request.getParameter("format") != null) {
            String format = request.getParameter("format");

            if (format.equals("N3"))
                return N3;
            else if (format.equals("HTML"))
                return HTML;
            else if (format.equals("RDFXML"))
                return RDFXML;
            else if (format.equals("TEXT"))
                return TEXT;
            else if (format.equals("SPARQL"))
                return SPARQL;
        } else {
            Set formatSet = new HashSet<String>();
            for (String f: formats)
                formatSet.add(f);
            
            String accept = (request.getHeader("Accept") != null)? request.getHeader("Accept"):"*/*";

            java.util.List<String> l = new java.util.LinkedList<String>();
            for (String s: accept.split(",")) {
                if (s.indexOf(";") != -1) {
                    String q = s.substring(s.indexOf(';')+1);
                    Double d = Double.parseDouble(q.substring(q.indexOf('=')+1).trim());

                    l.add(String.format("%02f", 1.0 - d) + " " + s.split(";")[0]);
                } else {
                    l.add("0.000000 " + s);
                }
            }

            java.util.Collections.sort(l);

            for (String s: l) {
                String ctype = s.split(" ")[1];

                if (ctype.equals("text/html") && formatSet.contains(HTML)) {
                    return HTML;
                } else if (ctype.equals("text/rdf+n3") && formatSet.contains(N3)) {
                    return N3;
                } else if (ctype.equals("application/rdf+xml") && formatSet.contains(RDFXML)) {
                    return RDFXML;
                } else if (ctype.equals("text/plain") && formatSet.contains(TEXT)) {
                    return TEXT;
                } else if (ctype.equals("application/sparql-results+xml") && formatSet.contains(SPARQL)) {
                    return SPARQL;
                } else if (ctype.equals("*/*")) {
                    return formats[0];
                }
            }
        }

        return HTML;
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
