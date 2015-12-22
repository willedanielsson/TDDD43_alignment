/*
 * $Id: AServProtocolManager.java 1374 2010-03-26 22:38:42Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2010
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package fr.inrialpes.exmo.align.service;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicParameters;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;

import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.LoadedOntology;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Evaluator;

import java.sql.SQLException;

import java.lang.ClassNotFoundException;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.IllegalAccessException;
import java.lang.NullPointerException;
import java.lang.UnsatisfiedLinkError;
import java.lang.ExceptionInInitializerError;
import java.lang.reflect.InvocationTargetException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.JarURLConnection;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes.Name;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

/**
 * This is the main class that control the behaviour of the Alignment Server
 * It is as independent from the OWL API as possible.
 * However, it is still necessary to test for the reachability of an ontology and moreover to resolve its URI for that of its source.
 * For these reasons we still need a parser of OWL files here.
 */

public class AServProtocolManager {

    CacheImpl alignmentCache = null;
    Properties commandLineParams = null;
    Set<String> renderers = null;
    Set<String> methods = null;
    Set<String> services = null;
    Set<String> evaluators = null;

    Hashtable<String,Directory> directories = null;

    // This should be stored somewhere
    int localId = 0; // surrogate of emitted messages
    String myId = null; // id of this alignment server

    /*********************************************************************
     * Initialization and constructor
     *********************************************************************/

    public AServProtocolManager ( Hashtable<String,Directory> dir ) {
	directories = dir;
    }

    public void init( DBService connection, Properties prop ) throws SQLException, AlignmentException {
	alignmentCache = new CacheImpl( connection );
	commandLineParams = prop;
	alignmentCache.init( prop );
	myId = "http://"+prop.getProperty("host")+":"+prop.getProperty("http");
	renderers = implementations( "org.semanticweb.owl.align.AlignmentVisitor" );
	methods = implementations( "org.semanticweb.owl.align.AlignmentProcess" );
	methods.remove("fr.inrialpes.exmo.align.impl.DistanceAlignment"); // this one is generic, but not abstract
	services = implementations( "fr.inrialpes.exmo.align.service.AlignmentServiceProfile" );
	evaluators = implementations( "org.semanticweb.owl.align.Evaluator" );
    }

    public void close() {
	try { alignmentCache.close(); }
	catch (SQLException sqle) { sqle.printStackTrace(); }
    }

    public void reset() {
	try {
	    alignmentCache.reset();
	} catch (SQLException sqle) { sqle.printStackTrace(); }
    }

    public void flush() {
	alignmentCache.flushCache();
    }

    public void shutdown() {
	try { 
	    alignmentCache.close();
	    System.exit(0);
	} catch (SQLException sqle) { sqle.printStackTrace(); }
    }

    private int newId() { return localId++; }

    /*********************************************************************
     * Extra administration primitives
     *********************************************************************/

    public Set<String> listmethods (){
	return methods;
    }

    public Set<String> listrenderers(){
	return renderers;
    }

    public Set<String> listservices(){
	return services;
    }

    public Set<String> listevaluators(){
	return evaluators;
    }

    /*
    public Enumeration alignments(){
	return alignmentCache.listAlignments();
    }
    */
    public Collection<Alignment> alignments() {
	return alignmentCache.alignments();
    }

    public String query( String query ){
	//return alignmentCache.query( query );
	return "Not available yet";
    }

    public String serverURL(){
	return myId;
    }

   /*********************************************************************
     * Basic protocol primitives
     *********************************************************************/

    // DONE
    // Implements: store (different from store below)
    public Message load( Message mess ) {
	boolean todiscard = false;
	Properties params = mess.getParameters();
	// load the alignment
	String name = params.getProperty("url");
	String file = null;
	if ( name == null || name.equals("") ){
	    file  = params.getProperty("filename");
	    if ( file != null && !file.equals("") ) name = "file://"+file;
	}
	//if ( debug > 0 ) System.err.println("Preparing for "+name);
	Alignment al = null;
	try {
	    //if (debug > 0) System.err.println(" Parsing alignment");
	    AlignmentParser aparser = new AlignmentParser(0);
	    al = aparser.parse( name );
	    //if (debug > 0) System.err.println(" Alignment parsed");
	} catch (Exception e) {
	    return new UnreachableAlignment(newId(),mess,myId,mess.getSender(),name,(Properties)null);
	}
	// We preserve the pretty tag within the loaded ontology
	String pretty = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY );
	if ( pretty == null ) pretty = params.getProperty("pretty");
	if ( pretty != null && !pretty.equals("") ) {
	    al.setExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY, pretty );
	}
	// register it
	String id = alignmentCache.recordNewAlignment( al, true );
	// if the file has been uploaded: discard it
	if ( al != null && al != null ) {
	    // try unlink
	}
	return new AlignmentId(newId(),mess,myId,mess.getSender(),id,(Properties)null,pretty);
    }

    // Implements: align
    @SuppressWarnings( "unchecked" )
    public Message align( Message mess ){
	Message result = null;
	Properties p = mess.getParameters();
	// These are added to the parameters wich are in the message
	//for ( String key : commandLineParams ) {
	// Unfortunately non iterable
	for ( Enumeration<String> e = (Enumeration<String>)commandLineParams.propertyNames(); e.hasMoreElements();) { //[W:unchecked]
	    String key = e.nextElement();
	    if ( p.getProperty( key ) == null ){
		p.setProperty( key , commandLineParams.getProperty( key ) );
	    }
	}
	// Do the fast part (retrieve)
	result = retrieveAlignment( mess );
	if ( result != null ) return result;
	String id = alignmentCache.generateAlignmentId();

	Aligner althread = new Aligner( mess, id );
	Thread th = new Thread(althread);
	// Do the slow part (align)
	if ( mess.getParameters().getProperty("async") != null ) {
	    th.start();
	    // Parameters are used
	    return new AlignmentId(newId(),mess,myId,mess.getSender(),id,mess.getParameters());
	} else {
	    th.start();
	    try{ th.join(); }
	    catch ( InterruptedException is ) {
		return new ErrorMsg(newId(),mess,myId,mess.getSender(),"Interrupted exception",(Properties)null);
	    };
	    return althread.getResult();
	}
    }

    /**
     * returns null if alignment not retrieved
     * Otherwise returns AlignmentId or an ErrorMsg
     */
    private Message retrieveAlignment( Message mess ){
	Properties params = mess.getParameters();
	String method = params.getProperty("method");
	// find and access o, o'
	URI uri1 = null;
	URI uri2 = null;
	Ontology onto1 = null;
	Ontology onto2 = null;
	try {
	    uri1 = new URI(params.getProperty("onto1"));
	    uri2 = new URI(params.getProperty("onto2"));
	} catch (Exception e) {
	    return new NonConformParameters(newId(),mess,myId,mess.getSender(),"nonconform/params/onto",(Properties)null);
	};
	Set<Alignment> alignments = alignmentCache.getAlignments( uri1, uri2 );
	if ( alignments != null && params.getProperty("force") == null ) {
	    for ( Alignment al: alignments ){
		if ( al.getExtension( Namespace.ALIGNMENT.uri, Annotations.METHOD ).equals(method) ) {
		    return new AlignmentId(newId(),mess,myId,mess.getSender(),
					   al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID ),(Properties)null,
					   al.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY ) );
		}
	    }
	}
	return (Message)null;
    }

    // DONE
    // Implements: query-aligned
    public Message existingAlignments( Message mess ){
	Properties params = mess.getParameters();
	// find and access o, o'
	String onto1 = params.getProperty("onto1");
	String onto2 = params.getProperty("onto2");
	URI uri1 = null;
	URI uri2 = null;
	Set<Alignment> alignments = new HashSet<Alignment>();
	try {
	    if( onto1 != null && !onto1.equals("") ) {
		uri1 = new URI( onto1 );
	    }
	    if ( onto2 != null && !onto2.equals("") ) {
		uri2 = new URI( onto2 );
	    }
	    alignments = alignmentCache.getAlignments( uri1, uri2 );
	} catch (Exception e) {
	    return new ErrorMsg(newId(),mess,myId,mess.getSender(),"MalformedURI problem",(Properties)null);
	}; //done below
	String msg = "";
	String prettys = "";
	for ( Alignment al : alignments ) {
	    msg += al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID )+" ";
	    prettys += al.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY )+ ":";
	}
	return new AlignmentIds(newId(),mess,myId,mess.getSender(),msg,(Properties)null,prettys);
    }

    // ABSOLUTELY NOT IMPLEMENTED
    // But look at existingAlignments
    // Implements: find
    // This may be useful when calling WATSON
    public Message find(Message mess){
    //\prul{search-success}{a - request ( find (O, T) ) \rightarrow S}{O' \Leftarrow Match(O,T)\\S - inform (O') \rightarrow a}{reachable(O)\wedge Match(O,T)\not=\emptyset}

    //\prul{search-void}{a - request ( find (O, T) ) \rightarrow S}{S - failure (nomatch) \rightarrow a}{reachable(O)\wedge Match(O,T)=\emptyset}

    //\prul{search-unreachable}{a - request ( find (O, T) ) \rightarrow S}{S - failure ( unreachable (O) ) \rightarrow a}{\neg reachable(O)}
	return new OntologyURI(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
    }

    // Implements: translate
    // This should be applied to many more kind of messages with different kind of translation
    public Message translate(Message mess){
	Properties params = mess.getParameters();
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment(newId(),mess,myId,mess.getSender(),id,(Properties)null);
	}
	// Translate the query
	try {
	    String translation = QueryMediator.rewriteSPARQLQuery( params.getProperty("query"), al );
	    return new TranslatedMessage(newId(),mess,myId,mess.getSender(),translation,(Properties)null);
	} catch (AlignmentException e) {
	    return new ErrorMsg(newId(),mess,myId,mess.getSender(),e.toString(),(Properties)null);
	}
    }

    // DONE
    // Implements: render
    public Message render( Message mess ){
	Properties params = mess.getParameters();
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment(newId(),mess,myId,mess.getSender(),id,(Properties)null);
	}
	// Render it
	String method = params.getProperty("method");
	AlignmentVisitor renderer = null;
	// Redirect the output in a String
	ByteArrayOutputStream result = new ByteArrayOutputStream(); 
	PrintWriter writer = null;
	try { 
	    writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( result, "UTF-8" )), true);
	    try {
		Object[] mparams = {(Object) writer };
		java.lang.reflect.Constructor[] rendererConstructors =
		    Class.forName(method).getConstructors();
		renderer =
		    (AlignmentVisitor) rendererConstructors[0].newInstance(mparams);
	    } catch (Exception ex) {
		// should return the message
		return new UnknownMethod(newId(),mess,myId,mess.getSender(),method,(Properties)null);
	    }
	    try {
		renderer.init( params );
		al.render( renderer );
	    } catch ( AlignmentException aex ) {
		al = ObjectAlignment.toObjectAlignment( (URIAlignment)al );
		al.render( renderer );
	    }
	    writer.flush();
	    writer.close();
	} catch (AlignmentException e) {
	    writer.flush();
	    writer.close();
	    return new UnknownMethod(newId(),mess,myId,mess.getSender(),method,(Properties)null);
	} catch (Exception e) { // These are exceptions related to I/O
	    writer.flush();
	    writer.close();
	    System.err.println(result.toString());
	    e.printStackTrace();
	}

	return new RenderedAlignment(newId(),mess,myId,mess.getSender(),result.toString(),(Properties)null);
    }

    /*********************************************************************
     * Extended protocol primitives
     *********************************************************************/

    // Implementation specific
    public Message store( Message mess ){
	String id = mess.getContent();
	Alignment al=null;
	 
	try {
	    try{
	    	al = alignmentCache.getAlignment( id );
	    } catch(Exception ex) {
	    	//System.err.println("Unknown Id in Store :=" + id );
	    	ex.printStackTrace();
	    }
	    // Be sure it is not already stored
	    if ( !alignmentCache.isAlignmentStored( al ) ) {

		alignmentCache.storeAlignment( id );
		 
		// Retrieve the alignment again
		al = alignmentCache.getAlignment( id );
		// for all directories...
		for ( Directory d : directories.values() ){
		    // Declare the alignment in the directory
		    try { d.register( al ); }
		    catch (AServException e) { e.printStackTrace(); }// ignore
		}
	    }
	    // register by them
	    // Could also be an AlreadyStoredAlignment error
	    return new AlignmentId(newId(),mess,myId,mess.getSender(),id,(Properties)null,
				   al.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY ));
	} catch (Exception e) {
	    return new UnknownAlignment(newId(),mess,myId,mess.getSender(),id,(Properties)null);
	}
    }

    /*
     * Returns only the metadata of an alignment and returns it in 
     * parameters
     */
    public Message metadata( Message mess ){
	// Retrieve the alignment
	String id = mess.getParameters().getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getMetadata( id );
	} catch (Exception e) {
	    return new UnknownAlignment(newId(),mess,myId,mess.getSender(),id,(Properties)null);
	}
	// JE: Other possibility is to render the metadata through XMLMetadataRendererVisitor into content...
	// Put all the local metadata in parameters
	Properties params = new Properties();
	params.setProperty( "file1", al.getFile1().toString() );
	params.setProperty( "file2", al.getFile2().toString() );
	params.setProperty( Namespace.ALIGNMENT.uri+"#level", al.getLevel() );
	params.setProperty( Namespace.ALIGNMENT.uri+"#type", al.getType() );
	for ( String[] ext : al.getExtensions() ){
	    params.setProperty( ext[0]+ext[1], ext[2] );
	}
	return new AlignmentMetadata(newId(),mess,myId,mess.getSender(),id,params);
    }

    /*********************************************************************
     * Extra alignment primitives
     *
     * All these primitives must create a new alignment and return its Id
     * There is no way an alignment server could modify an alignment
     *********************************************************************/

    public Message trim( Message mess ) {
	// Retrieve the alignment
	String id = mess.getParameters().getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment(newId(),mess,myId,mess.getSender(),id,(Properties)null);
	}
	// get the trim parameters
	String type = mess.getParameters().getProperty("type");
	if ( type == null ) type = "hard";
	double threshold = Double.parseDouble(mess.getParameters().getProperty("threshold"));
	al = (BasicAlignment)((BasicAlignment)al).clone();
	try { al.cut( type, threshold );}
	catch (AlignmentException e) {
	    return new ErrorMsg(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
	}
	String pretty = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY );
	if ( pretty != null ){
	    al.setExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY, pretty+"/trimmed "+threshold );
	};
	String newId = alignmentCache.recordNewAlignment( al, true );
	return new AlignmentId(newId(),mess,myId,mess.getSender(),newId,(Properties)null,
			       al.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY ));
    }

    public Message harden( Message mess ){
	return new AlignmentId(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
    }

    public Message inverse( Message mess ){
	Properties params = mess.getParameters();
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment(newId(),mess,myId,mess.getSender(),"unknown/Alignment/"+id,(Properties)null);
	}

	// Invert it
	try { al = al.inverse(); }
	catch (AlignmentException e) {
	    return new ErrorMsg(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
	}
	String pretty = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY );
	if ( pretty != null ){
	    al.setExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY, pretty+"/inverted" );
	};
	String newId = alignmentCache.recordNewAlignment( al, true );
	return new AlignmentId(newId(),mess,myId,mess.getSender(),newId,(Properties)null,
			       al.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY ));
    }

    public Message meet( Message mess ){
	// Retrieve alignments
	return new AlignmentId(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
    }

    public Message join( Message mess ){
	// Retrieve alignments
	return new AlignmentId(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
    }

    public Message compose( Message mess ){
	// Retrieve alignments
	return new AlignmentId(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
    }

    public Message eval( Message mess ){
	Properties params = mess.getParameters();
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment(newId(),mess,myId,mess.getSender(),"unknown/Alignment/"+id,(Properties)null);
	}
	// Retrieve the reference alignment
	String rid = params.getProperty("ref");
	Alignment ref = null;
	try {
	    ref = alignmentCache.getAlignment( rid );
	} catch (Exception e) {
	    return new UnknownAlignment(newId(),mess,myId,mess.getSender(),"unknown/Alignment/"+rid,(Properties)null);
	}
	// Set the comparison method
	String classname = params.getProperty("method");
	if ( classname == null ) classname = "fr.inrialpes.exmo.align.impl.eval.PRecEvaluator";
	Evaluator eval = null;
	try {
	    Object [] mparams = {(Object)ref, (Object)al};
	    Class<?> oClass = Class.forName("org.semanticweb.owl.align.Alignment");
	    Class[] cparams = { oClass, oClass };
	    Class<?> evaluatorClass =  Class.forName(classname);
	    java.lang.reflect.Constructor evaluatorConstructor = evaluatorClass.getConstructor(cparams);
	    eval = (Evaluator)evaluatorConstructor.newInstance(mparams);
	} catch (Exception ex) {
	    return new ErrorMsg(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
	}
	// Compare it
	try { eval.eval(params); }
	catch (AlignmentException e) {
	    return new ErrorMsg(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
	}
	// Return it, not easy
	StringWriter sw = new StringWriter();
	try {
	    eval.write( new PrintWriter( sw ) );
	} catch (IOException ioex) {}; // never occurs
	// Should not be alignment evaluation results...
	return new EvaluationId(newId(),mess,myId,mess.getSender(),sw.toString(),(Properties)null);
    }

    /**
     * Store evaluation result from its URI
     */
    public Message storeEval( Message mess ){
	return new ErrorMsg(newId(),mess,myId,mess.getSender(),"Not yet implemented",(Properties)null);
    }

    /**
     * Evaluate a track: a set of results
     */
    // It is also possible to try a groupeval ~> with a zipfile containing results
    //            ~~> But it is more difficult to know where is the reference (non public)
    // There should also be options for selecting the result display
    //            ~~> PRGraph (but this may be a Evaluator)
    //            ~~> Triangle
    //            ~~> Cross
    public Message groupEval( Message mess ){
	return new ErrorMsg(newId(),mess,myId,mess.getSender(),"Not yet implemented",(Properties)null);
    }

    /**
     * Store the result
     */
    public Message storeGroupEval( Message mess ){
	return new ErrorMsg(newId(),mess,myId,mess.getSender(),"Not yet implemented",(Properties)null);
    }

    /**
     * Retrieve the results (all registered result) of a particular test
     */
    public Message getResults( Message mess ){
	return new ErrorMsg(newId(),mess,myId,mess.getSender(),"Not yet implemented",(Properties)null);
    }

    public boolean storedAlignment( Message mess ) {
	// Retrieve the alignment
	String id = mess.getParameters().getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return false;
	}
	if ( al.getExtension(CacheImpl.SVCNS, CacheImpl.STORED) != null && al.getExtension(CacheImpl.SVCNS, CacheImpl.STORED) != "" ) {
	    return true;
	} else {
	    return false;
	}
    }

    /*********************************************************************
     * Network of alignment server implementation
     *********************************************************************/

    /**
     * Ideal network implementation protocol:
     *
     * - publication (to some directory)
     * registerID
     * publishServices
     * unregisterID
     * (publishRenderer)
     * (publishMethods) : can be retrieved through the classical interface.
     *  requires a direcory
     *
     * - subscribe style
     * subscribe() : ask to receive new metadata
     * notify( metadata ) : send new metadata to subscriber
     * unsubscribe() :
     * update( metadata ) : update some modification
     *   requires to store the subscribers
     *
     * - query style: this is the classical protocol that can be done through WSDL
     * getMetadata()
     * getAlignment()
     *   requires to store the node that can be 
     */

    // Implements: reply-with
    public Message replywith(Message mess){

    //\prul{redirect}{a - request ( q(x)~reply-with:~i) \rightarrow S}{
    //Q \Leftarrow Q\cup\{\langle a, i, !i', q(x), S'\rangle\}\		\
    //S - request( q( R(x) )~reply-with:~i')\rightarrow S'}{S'\in C(q)}
	return new Message(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
    }

    // Implements: reply-to
    public Message replyto(Message mess){

    //\prul{handle-return}{S' - inform ( y~reply-to:~i') \rightarrow S}{
    //Q \Leftarrow Q-\{\langle a, i, i', _, S'\rangle\}\		\
    //S - inform( R^{-1}(y)~reply-to:~i)\rightarrow a}{\langle a, i, i', _, S'\rangle \in Q, \neg surr(y)}

    //\prul{handle-return}{S' - inform ( y~reply-to:~i') \rightarrow S}{
    //Q \Leftarrow Q-\{\langle a, i, i', _, S'\rangle\}\	\
    //R \Leftarrow R\cup\{\langle a, !y', y, S'\rangle\}\		\
    //S - inform( R^{-1}(y)~reply-to:~i)\rightarrow a}{\langle a, i, i', _, S'\rangle \in Q, surr(y)}
	return new Message(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
    }

    // Implements: failure
    public Message failure(Message mess){

    //\prul{failure-return}{S' - failure ( y~reply-to:~i') \rightarrow S}{
    //Q \Leftarrow Q-\{\langle a, i, i', _, S'\rangle\}\		\
    //S - failure( R^{-1}(y)~reply-to:~i)\rightarrow a}{\langle a, i, i', _, S'\rangle \in Q}
	return new Message(newId(),mess,myId,mess.getSender(),"dummy//",(Properties)null);
    }

    /*********************************************************************
     * Utilities: reaching and loading ontologies
     *********************************************************************/

    public LoadedOntology reachable( URI uri ){
	try { 
	    OntologyFactory factory = OntologyFactory.getFactory();
	    return factory.loadOntology( uri );
	} catch (Exception e) { return null; }
    }

    /*********************************************************************
     * Utilities: Finding the implementation of an interface
     *********************************************************************/

    public static void implementations( Class tosubclass, Set<String> list, boolean debug ){
	Set<String> visited = new HashSet<String>();
	String classPath = System.getProperty("java.class.path",".");
	// Hack: this is not necessary
	//classPath = classPath.substring(0,classPath.lastIndexOf(File.pathSeparatorChar));
	if ( debug ) System.err.println(classPath);
	StringTokenizer tk = new StringTokenizer(classPath,File.pathSeparator);
	classPath = "";
	while ( tk != null && tk.hasMoreTokens() ){
	    StringTokenizer tk2 = tk;
	    tk = null;
	    // Iterate on Classpath
	    while ( tk2.hasMoreTokens() ) {
		try {
		    File file = new File( tk2.nextToken() );
		    if ( file.isDirectory() ) {
			//System.err.println("DIR "+file);
			String subs[] = file.list();
			for( int index = 0 ; index < subs.length ; index ++ ){
			    if ( debug ) System.err.println("    "+subs[index]);
			    // IF class
			    if ( subs[index].endsWith(".class") ) {
				String classname = subs[index].substring(0,subs[index].length()-6);
				if (classname.startsWith(File.separator)) 
				    classname = classname.substring(1);
				classname = classname.replace(File.separatorChar,'.');
				if ( implementsInterface( classname, tosubclass, debug ) ) {
				    list.add( classname );
				}
			    }
			}
		    } else if ( file.toString().endsWith(".jar") &&
				!visited.contains( file.toString() ) &&
				file.exists() ) {
			if ( debug ) System.err.println("JAR "+file);
			visited.add( file.toString() );
			JarFile jar = null;
			try {
			    jar = new JarFile( file );
			    exploreJar( list, visited, tosubclass, jar, debug );
			    // Iterate on needed Jarfiles
			    // JE(caveat): this deals naively with Jar files,
			    // in particular it does not deal with section'ed MANISFESTs
			    Attributes mainAttributes = jar.getManifest().getMainAttributes();
			    String path = mainAttributes.getValue( Name.CLASS_PATH );
			    if ( debug ) System.err.println("  >CP> "+path);
			    if ( path != null && !path.equals("") ) {
				// JE: Not sure where to find the other Jars:
				// in the path or at the local place?
				//classPath += File.pathSeparator+file.getParent()+File.separator + path.replaceAll("[ \t]+",File.pathSeparator+file.getParent()+File.separator);
				// This replaces the replaceAll which is not tolerant on Windows in having "\" as a separator
				// Is there a way to make it iterable???
				for( StringTokenizer token = new StringTokenizer(path," \t"); token.hasMoreTokens(); )
				    classPath += File.pathSeparator+file.getParent()+File.separator+token.nextToken();
			    }
			} catch (NullPointerException nullexp) { //Raised by JarFile
			    System.err.println("Warning "+file+" unavailable");
			}
		    }
		} catch( IOException e ) {
		    continue;
		}
	    }
	    if ( !classPath.equals("") ) {
		tk =  new StringTokenizer(classPath,File.pathSeparator);
		classPath = "";
	    }
	}
    }
    
    public static void exploreJar( Set<String> list, Set<String> visited, Class tosubclass, JarFile jar, boolean debug ) {
	Enumeration enumeration = jar.entries();
	while( enumeration != null && enumeration.hasMoreElements() ){
	    JarEntry entry = (JarEntry)enumeration.nextElement();
	    String entryName = entry.toString();
	    if ( debug ) System.err.println("    "+entryName);
	    int len = entryName.length()-6;
	    if( len > 0 && entryName.substring(len).compareTo(".class") == 0) {
		entryName = entryName.substring(0,len);
		// Beware, in a Jarfile the separator is always "/"
		// and it would not be dependent on the current system anyway.
		//entryName = entryName.replaceAll(File.separator,".");
		entryName = entryName.replaceAll("/",".");
		if ( implementsInterface( entryName, tosubclass, debug ) ) {
			    list.add( entryName );
		}
	    } else if( entryName.endsWith(".jar") &&
		       !visited.contains( entryName ) ) { // a jar in a jar
		if ( debug ) System.err.println("JAR "+entryName);
		visited.add( entryName );
		//System.err.println(  "jarEntry is a jarfile="+je.getName() );
		try {
		    InputStream jarSt = jar.getInputStream( (ZipEntry)entry );
		    File f = File.createTempFile( "aservTmpFile"+visited.size(), "jar" );
		    OutputStream out = new FileOutputStream( f );
		    byte buf[]=new byte[1024];
		    int len1 ;
		    while( (len1 = jarSt.read(buf))>0 )
			out.write(buf,0,len1);
		    out.close();
		    jarSt.close();
		    JarFile inJar = new JarFile( f );
		    exploreJar( list, visited, tosubclass, inJar, debug );
		    f.delete();
		} catch (IOException ioex) {
		    System.err.println( "Cannot read embedded jar: "+ioex );
		}
	    } 
	}
    }

    public static boolean implementsInterface( String classname, Class tosubclass, boolean debug ) {
	try {
	    if ( classname.equals("org.apache.xalan.extensions.ExtensionHandlerGeneral") ) throw new ClassNotFoundException( "Stupid JAVA/Xalan bug" );
	    // JE: Here there is a bug that is that it is not possible
	    // to have ALL interfaces with this function!!!
	    // This is really stupid but that's life
	    // So it is compulsory that AlignmentProcess be declared 
	    // as implemented
	    Class cl = Class.forName(classname);
	    // It is possible to suppress here abstract classes by:
	    if ( java.lang.reflect.Modifier.isAbstract( cl.getModifiers() ) ) return false;
	    Class[] interfaces = cl.getInterfaces();
	    for ( int i=interfaces.length-1; i >= 0  ; i-- ){
		if ( interfaces[i] == tosubclass ) {
		    if ( debug ) System.err.println(" -j-> "+classname);
		    return true;
		}
		if ( debug ) System.err.println("       I> "+interfaces[i] );
	    }
	    // Not one of our classes
	} catch ( NoClassDefFoundError ncdex ) {
	} catch (ClassNotFoundException cnfex) {
	} catch (UnsatisfiedLinkError ule) {
	    if ( debug ) System.err.println("   ******** "+classname);
	}
	return false;
    }

    /**
     * Display all the classes inheriting or implementing a given
     * interface in the currently loaded packages.
     * @param interfaceName the name of the interface to implement
     */
    public static Set<String> implementations( String interfaceName ) {
	Set<String> list = new HashSet<String>();
	try {
	    Class toclass = Class.forName(interfaceName);
	    //Package [] pcks = Package.getPackages();
	    //for (int i=0;i<pcks.length;i++) {
		//System.err.println(interfaceName+ ">> "+pcks[i].getName() );
		//implementations( pcks[i].getName(), toclass, list );
		//}
	    implementations( toclass, list, false );
	} catch (ClassNotFoundException ex) {
	    System.err.println("Class "+interfaceName+" not found!");
	}
	return list;
    }

    protected class Aligner implements Runnable {
	private Message mess = null;
	private Message result = null;
	private String id = null;

	public Aligner( Message m, String id ) {
	    mess = m;
	    this.id = id;
	}

	public Message getResult() {
	    return result;
	}

	public void run() {
	    Properties params = mess.getParameters();
	    String method = params.getProperty("method");
	    // find and access o, o'
	    URI uri1 = null;
	    URI uri2 = null;

	    try {
		uri1 = new URI(params.getProperty("onto1"));
		uri2 = new URI(params.getProperty("onto2"));
	    } catch (Exception e) {
		result = new NonConformParameters(newId(),mess,myId,mess.getSender(),"nonconform/params/onto",(Properties)null);
		return;
	    };

	    // find initial alignment
	    Alignment init = null;
	    if ( params.getProperty("init") != null && !params.getProperty("init").equals("") ) {
		try {
		    //if (debug > 0) System.err.println(" Retrieving init");
		    try {
			init = alignmentCache.getAlignment( params.getProperty("init") );
		} catch (Exception e) {
			result = new UnknownAlignment(newId(),mess,myId,mess.getSender(),params.getProperty("init"),(Properties)null);
			return;
		    }
		} catch (Exception e) {
		    result = new UnknownAlignment(newId(),mess,myId,mess.getSender(),params.getProperty("init"),(Properties)null);
		    return;
		}
	    }
	    
	    // Create alignment object
	    try {
		Object[] mparams = {};
		if ( method == null )
		    method = "fr.inrialpes.exmo.align.impl.method.StringDistAlignment";
		Class<?> alignmentClass = Class.forName(method);
		Class[] cparams = {};
		java.lang.reflect.Constructor alignmentConstructor = alignmentClass.getConstructor(cparams);
		AlignmentProcess aresult = (AlignmentProcess)alignmentConstructor.newInstance(mparams);
		try {
		    aresult.init( uri1, uri2 );
		    long time = System.currentTimeMillis();
		    aresult.align( init, params ); // add opts
		    long newTime = System.currentTimeMillis();
		    aresult.setExtension( Namespace.ALIGNMENT.uri, Annotations.TIME, Long.toString(newTime - time) );
		    aresult.setExtension( Namespace.ALIGNMENT.uri, Annotations.TIME, Long.toString(newTime - time) );
		    String pretty = params.getProperty( "pretty" );
		    if ( pretty != null && !pretty.equals("") )
			aresult.setExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY, pretty );
		} catch (AlignmentException e) {
		    // The unreachability test has already been done
		    // JE 15/1/2009: commented the unreachability test
		    if ( reachable( uri1 ) == null ){
			result = new UnreachableOntology(newId(),mess,myId,mess.getSender(),params.getProperty("onto1"),(Properties)null);
		    } else if ( reachable( uri2 ) == null ){
			result = new UnreachableOntology(newId(),mess,myId,mess.getSender(),params.getProperty("onto2"),(Properties)null);
		    } else {
			result = new NonConformParameters(newId(),mess,myId,mess.getSender(),"nonconform/params/"+e.getMessage(),(Properties)null);
		    }
		    return;
		}
		// ask to store A'
		alignmentCache.recordNewAlignment( id, aresult, true );
		result = new AlignmentId(newId(),mess,myId,mess.getSender(),id,(Properties)null,
			       aresult.getExtension( Namespace.ALIGNMENT.uri, Annotations.PRETTY ));
	    } catch (ClassNotFoundException e) {
		result = new RunTimeError(newId(),mess,myId,mess.getSender(),"Class not found: "+method,(Properties)null);
	    } catch (NoSuchMethodException e) {
		result = new RunTimeError(newId(),mess,myId,mess.getSender(),"No such method: "+method+"(Object, Object)",(Properties)null);
	    } catch (InstantiationException e) {
		result = new RunTimeError(newId(),mess,myId,mess.getSender(),"Instantiation",(Properties)null);
	    } catch (IllegalAccessException e) {
		result = new RunTimeError(newId(),mess,myId,mess.getSender(),"Cannot access",(Properties)null);
	    } catch (InvocationTargetException e) {
		result = new RunTimeError(newId(),mess,myId,mess.getSender(),"Invocation target",(Properties)null);
	    } catch (AlignmentException e) {
		result = new NonConformParameters(newId(),mess,myId,mess.getSender(),"nonconform/params/",(Properties)null);
	    } catch (Exception e) {
		result = new RunTimeError(newId(),mess,myId,mess.getSender(),"Unexpected exception :"+e,(Properties)null);
	    }
	}
    }

    
}
