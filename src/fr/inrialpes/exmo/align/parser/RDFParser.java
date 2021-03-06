/*
 * $Id: RDFParser.java 1412 2010-03-31 13:57:07Z euzenat $
 *
 * Copyright (C) 2006 Digital Enterprise Research Insitute (DERI) Innsbruck
 * Sourceforge version 1.7 - 2008
 * Copyright (C) INRIA, 2008-2010
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

package fr.inrialpes.exmo.align.parser;

import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.BasicOntology;

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;

import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.EDOALRelation;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.Id;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.ClassExpression;
import fr.inrialpes.exmo.align.impl.edoal.ClassId;
import fr.inrialpes.exmo.align.impl.edoal.ClassConstruction;
import fr.inrialpes.exmo.align.impl.edoal.ClassRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassOccurenceRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PathExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;
import fr.inrialpes.exmo.align.impl.edoal.PropertyConstruction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationExpression;
import fr.inrialpes.exmo.align.impl.edoal.RelationId;
import fr.inrialpes.exmo.align.impl.edoal.RelationConstruction;
import fr.inrialpes.exmo.align.impl.edoal.RelationRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationCoDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.InstanceExpression;
import fr.inrialpes.exmo.align.impl.edoal.InstanceId;

import fr.inrialpes.exmo.align.impl.edoal.TransfService;
import fr.inrialpes.exmo.align.impl.edoal.ValueExpression;
import fr.inrialpes.exmo.align.impl.edoal.Value;
import fr.inrialpes.exmo.align.impl.edoal.Apply;
import fr.inrialpes.exmo.align.impl.edoal.Datatype;
import fr.inrialpes.exmo.align.impl.edoal.Comparator;
import fr.inrialpes.exmo.align.impl.edoal.Variable;

import fr.inrialpes.exmo.align.parser.SyntaxElement.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;

import java.util.logging.Logger;
import java.util.logging.Level;

// Yes we are relying on Jena for parsing RDF
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Container;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * <p>
 * Parser for the EDOAL syntax. The reader is Jena, input is an EDOALAlignment
 * The input document format shall be consistent with format document 
 * 
 * </p>
 * <p>
 * $Id: RDFParser.java 1412 2010-03-31 13:57:07Z euzenat $
 * </p>
 * 
 * @author Richard Pöttler
 * @version $Revision: 1.7 $
 * @date $Date: 2010-03-07 20:41:03 +0100 (Sun, 07 Mar 2010) $
 */
public class RDFParser {

    private static Logger logger = Logger.getLogger(RDFParser.class.toString());

    private static Model rDFModel;

    private int debug = 0;

    private boolean isPattern = false;

    private EDOALAlignment alignment;

    /** 
     * Creates an RDF Parser.
     */
    public RDFParser() {
	this(0);
    }

    /** 
     * Creates an RDF Parser.
     * @param debugMode The value of the debug mode
     */
    public RDFParser( int debugMode ) {
	debug = debugMode;
    }

    /**
     * Initialisation of the structures
     * This creates an RDF Model which contains all the syntactic elements.
     * This is to be called before parsing, i.e., before exploring RDF resources
     */
    public static void initSyntax() {
	if ( rDFModel == null ) {
	    rDFModel = ModelFactory.createDefaultModel();
	    // The register is always initialised?
	    for ( SyntaxElement el : SyntaxElement.values() ) {
		// JE2010 This is incorrect because there can be Resources (classes)
		// and/or Property ... They may all be resources...
		if ( el.isProperty == true ) {
		    el.resource = rDFModel.createProperty( el.id() );
		} else {
		    el.resource = rDFModel.createResource( el.id() );
		}
		//register.put( el.getName(), el );
	    }
	}
    }

    /**
     * Parse the input model. The model shall include one statement that include (?,RDF.type,Alignment)
     * @param align
     * @return the result EDOALAlignment
     * @throws AlignmentException if there is any exception, throw AlignmentException that include describe infomation
     * and a caused exception.
     */
    public EDOALAlignment parse( final Model rdfmodel ) throws AlignmentException {
	// Initialize the syntax description
	initSyntax();
	// Shut up logging handling
	com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler.silent = true;
	// Get the statement including alignment resource as rdf:type
	StmtIterator stmtIt = rdfmodel.listStatements(null, RDF.type,(Resource)SyntaxElement.getResource("Alignment"));
	// Take the first one if it exists
	if ( !stmtIt.hasNext() ) throw new AlignmentException("There is no alignment in the RDF document");
	Statement alignDoc = stmtIt.nextStatement();
	// Step from this statement
	alignment = parseAlignment( alignDoc.getSubject() );
	// Clean up memory
	rdfmodel.close(); // JE: I am not sure that I will not have trouble with initSyntax
	return alignment;
    }

    // Below is the plumbing:
    // Load the RDF under an RDFModel
    // Call the above parse: RDFModel -> EDOALAlignment

    public EDOALAlignment parse( final File file ) throws AlignmentException {
	try {
	    return parse( new FileInputStream( file ) );
	} catch ( FileNotFoundException fnfe ) {
	    throw new AlignmentException("RDFParser: There isn't such file: "+ file.getName(), fnfe);
	}
    }

    public EDOALAlignment parse( final Reader is ) throws AlignmentException {
	if (is == null) throw new AlignmentException("The reader must not be null");
	Model align = ModelFactory.createDefaultModel();
	align.read( is, null );
	return parse( align );
    }
    
    public EDOALAlignment parse( final InputStream is ) throws AlignmentException {
	if (is == null) throw new AlignmentException("The inputstream must not be null");
	Model align = ModelFactory.createDefaultModel();
	align.read( is, null );
	return parse( align );
    }

    public EDOALAlignment parse( final String uri ) throws AlignmentException {
	Model align = ModelFactory.createDefaultModel();
	align.read( uri );
	return parse( align );
    }

    // Below is the real work
    /**
     * Parses a mapping document. The resource passed to this method must be a
     * <code>&lt;Alignment&gt;</code> tag.
     * 
     * @param node    the alignment resource
     * @return the parsed mapping document
     * @throws AlignmentException
     */
    public EDOALAlignment parseAlignment( final Resource node ) throws AlignmentException {
	if (node == null) throw new NullPointerException("Alignment must not be null");
	try {
	    Ontology source = null;
	    Ontology target = null;
	    
	    // getting the id of the document
	    final URI id = getNodeId( node );
	    
	    alignment = new EDOALAlignment();
	    if ( id != null )
		alignment.setExtension( Namespace.ALIGNMENT.uri, Annotations.ID, id.toString() );
	    
	    StmtIterator stmtIt = node.listProperties((Property)SyntaxElement.MAPPING_SOURCE.resource );
	    if ( stmtIt.hasNext() ) {
		source = parseOntology( stmtIt.nextStatement().getResource() );
	    } else {
		throw new AlignmentException( "Missing ontology "+"onto1" );
	    }
	    stmtIt = node.listProperties((Property)SyntaxElement.MAPPING_TARGET.resource );
	    if ( stmtIt.hasNext() ) {
		target = parseOntology( stmtIt.nextStatement().getResource() );
	    } else {
		throw new AlignmentException( "Missing ontology "+"onto2" );
	    }
	    stmtIt = node.listProperties((Property)SyntaxElement.LEVEL.resource );
	    if ( stmtIt.hasNext() ) {
		final String level = stmtIt.nextStatement().getString();
		if ((level != null) && (!level.equals(""))) {
		    if ( level.startsWith("2EDOAL") ) {
			alignment.setLevel( level );
			if ( level.equals("2EDOALPattern") ) isPattern = true;
		    } else {
			throw new AlignmentException( "Cannot parse alignment of level "+level );
		    }
		}			    
	    } else {
		throw new AlignmentException( "Missing level " );
	    }
	    stmtIt = node.listProperties((Property)SyntaxElement.TYPE.resource );
	    if ( stmtIt.hasNext() ) {
		final String arity = stmtIt.nextStatement().getString();
		if ((arity != null) && (!arity.equals(""))) {
		    alignment.setType( arity );
		}
	    } else {
		throw new AlignmentException( "Missing type " );
	    }
	    
	    stmtIt = node.listProperties((Property)SyntaxElement.MAP.resource );
	    while (stmtIt.hasNext()) {
		Statement stmt = stmtIt.nextStatement();
		if ( debug > 0 ) System.err.println( "  ---------------> "+stmt );
		try { alignment.addAlignCell( parseCell( stmt.getResource() ) ); }
		catch ( AlignmentException ae ) {
		    System.err.println( "Error "+ae );
		    ae.printStackTrace();
		}
	    }

	    // Remaining resources...
	    // rdf:type must be forgotten
	    //else if ( !pred.equals( SyntaxElement.getResource("rdftype") ) ) { // Unknown is annotation
	    //	parseAnnotation( stmt, alignment );
	    //}

	    if ( source != null && target != null ) {
		alignment.init( source, target );
	    } else {
		throw new IllegalArgumentException("Missing ontology description");
	    }
	    return alignment;
	    
	} catch (AlignmentException e) {
	    throw e;
	} catch (Exception e) {
	    throw new AlignmentException("There is some error in parsing alignment: " + node.getLocalName(), e);
	}
    }

    /**
     * Parse an ontology node <code>&lt;onto1&gt;</code> or
     * <code>&lt;onto2&gt;</code> Node to an Ontology object. The node must
     * contain the <code>&lt;onto...&gt;</code> element.
     * 
     * @param node
     *            the ontology node
     * @return the Ontology object
     * @throws NullPointerException
     *             if the node is null
     */
    protected Ontology parseOntology(final Resource node) throws AlignmentException {
	if (node == null) {
	    throw new AlignmentException("The ontology node must not be null");
	}

	try {
	    Resource formu = node.getProperty((Property)SyntaxElement.FORMATT.resource).getResource();
	    final String formalismName = formu.getProperty((Property)SyntaxElement.NAME.resource).getString();
	    final String formalismUri = formu.getProperty((Property)SyntaxElement.URI.resource).getString();
	    final Statement location = node.getProperty((Property)SyntaxElement.LOCATION.resource);
	    Ontology onto = new BasicOntology();
	    onto.setURI( new URI( node.getURI() ) );
	    onto.setFormURI( new URI( formalismUri ) );
	    onto.setFormalism( formalismName );
	    if ( location != null ) onto.setFile( new URI( location.getString() ) );
	    return onto;
	} catch ( Exception e ) {
	    throw new AlignmentException("The ontology node isn't correct: "
					 + node.getLocalName(), e);
	}
    }

    /**
     * Parses a mapping rule. The parsed node must be a Cell resource including the mandatory Statement.
     * <code>&lt;Cell&gt;</code> tag.
     * 
     * @param node
     *            the <code>&lt;Cell&gt;</code> tag
     * @return the parsed rule
     * @exception AlignmentException
     */
    protected EDOALCell parseCell( final Resource node ) throws AlignmentException {
	if (node == null) {
	    throw new NullPointerException("The node must not be null");
	}
	try {
	    // Should be better to use Alignment API relation recognition
	    // determine the relation, the relation shall be Literal
	    final String relation = node.getProperty((Property)SyntaxElement.RULE_RELATION.resource).getString();
	    //Get the relation
	    final EDOALRelation type = new EDOALRelation( relation );
	    if (type == null) {	// I raise an error in this case anyway
		throw new IllegalArgumentException("Couln't parse the string \"" + relation
						   +"\" to a valid rule type");
	    }
	    
	    // parse the measure, the node shall be Literal and it's a number
	    final float m = node.getProperty((Property)SyntaxElement.MEASURE.resource).getFloat();
	    
	    // get the id
	    final String id = node.getURI();
	    
	    //parsing the entity1 and entity2 
	    Resource entity1 = node.getProperty((Property)SyntaxElement.ENTITY1.resource).getResource();
	    Resource entity2 = node.getProperty((Property)SyntaxElement.ENTITY2.resource).getResource();

	    // JE2010:
	    // Here it would be better to check if the entity has a type.
	    // If both have none, then we get a old-style correspondence for free
	    // If it has one, let's go parsing
	    // I also assume that the factory only do dispatch
	    
	    Expression s = parseExpression( entity1 );
	    Expression t = parseExpression( entity2 );
	    if ( debug > 0 ) {
		System.err.println(" s : "+s);	    
		System.err.println(" t : "+t);
	    }

	    return new EDOALCell( id, s, t, type, m );
	} catch (Exception e) {  //wrap other type exception
	    logger.log(java.util.logging.Level.SEVERE, "The cell isn't correct:" + node.getLocalName() + " "+e.getMessage());
	    throw new AlignmentException("Cannot parse correspondence " + node.getLocalName(), e);
	}
    }

    // Here given the type of expression, this can be grand dispatch
    protected Expression parseExpression( final Resource node ) throws AlignmentException {
	Expression result;
	Resource rdfType = node.getProperty( RDF.type ).getResource();
	if ( rdfType.equals( SyntaxElement.CLASS_EXPR.resource ) ||
	     rdfType.equals( SyntaxElement.OCCURENCE_COND.resource ) ||
	     rdfType.equals( SyntaxElement.DOMAIN_RESTRICTION.resource ) ||
	     rdfType.equals( SyntaxElement.TYPE_COND.resource ) ||
	     rdfType.equals( SyntaxElement.VALUE_COND.resource ) ) {
	    result = parseClass( node );
	} else if ( rdfType.equals( SyntaxElement.PROPERTY_EXPR.resource ) ||
		    rdfType.equals( SyntaxElement.PROPERTY_DOMAIN_COND.resource ) ||
		    rdfType.equals( SyntaxElement.PROPERTY_TYPE_COND.resource ) ||
		    rdfType.equals( SyntaxElement.PROPERTY_VALUE_COND.resource ) ) {
	    result = parseProperty( node );
	} else if ( rdfType.equals( SyntaxElement.RELATION_EXPR.resource ) ||
		    rdfType.equals( SyntaxElement.RELATION_DOMAIN_COND.resource ) || // JE 2010: no chance
		    rdfType.equals( SyntaxElement.RELATION_CODOMAIN_COND.resource ) ) {
	    result = parseRelation( node );
	} else if ( rdfType.equals( SyntaxElement.INSTANCE_EXPR.resource ) ) {
	    result = parseInstance( node );
	} else {
	    throw new AlignmentException("There is no parser for entity "+rdfType.getLocalName());
	}
	if ( isPattern ) {
	    StmtIterator stmtIt = node.listProperties( (Property)SyntaxElement.VAR.resource );
	    if ( stmtIt.hasNext() ) {
		final String varname = stmtIt.nextStatement().getString();
		final Variable var = alignment.recordVariable( varname, result );
		result.setVariable( var );
	    }
	}
	return result;
    }
    
    protected ClassExpression parseClass( final Resource node ) throws AlignmentException {
	if ( debug > 1 ) {
	    StmtIterator it = node.listProperties();
	    while ( it.hasNext() ) System.err.println( "   > "+it.next() );
	}
	Resource rdfType = node.getProperty(RDF.type).getResource();
	if ( rdfType.equals( SyntaxElement.CLASS_EXPR.resource ) ) {
	    URI id = getNodeId( node );
	    if ( id != null ) {
		return new ClassId( id );
	    } else {
		Statement stmt = null;
		Constructor op = null;
		// Using a List preserves the order... useful mostly for COMPOSE
		// Given the Jena encoding of Collection, LinkedList seems the best
		List<ClassExpression> clexpr = new LinkedList<ClassExpression>();
		if ( node.hasProperty( (Property)SyntaxElement.AND.resource ) ) {
		    op = SyntaxElement.AND.getOperator();
		    // listProperties would give them all
		    stmt = node.getProperty( (Property)SyntaxElement.AND.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.OR.resource ) ) { 
		    op = SyntaxElement.OR.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.OR.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.NOT.resource ) ) {
		    op = SyntaxElement.NOT.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.NOT.resource );
		} else {
		    if ( isPattern ) { // not necessarily with a variable (real patterns)
			return new ClassId();
		    } else {
			throw new AlignmentException( "Class statement must containt one constructor or Id : "+node );
		    }
		}
		Resource coll = stmt.getResource(); // MUSTCHECK
		if ( op == SyntaxElement.NOT.getOperator() ) {
		    clexpr.add( parseClass( coll ) );
		} else { // Jena encode these collections as first/rest statements
		    while ( !RDF.nil.getURI().equals( coll.getURI() ) ) { // THIS IS HORRIBLE
			clexpr.add( parseClass( coll.getProperty( RDF.first ).getResource() ) );
			coll = coll.getProperty( RDF.rest ).getResource(); // MUSTCHECK
		    }
		}
		return new ClassConstruction( op, clexpr );
	    }
	} else {
	    if ( !rdfType.equals( SyntaxElement.OCCURENCE_COND.resource ) &&
		 !rdfType.equals( SyntaxElement.DOMAIN_RESTRICTION.resource ) &&
		 !rdfType.equals( SyntaxElement.TYPE_COND.resource ) &&
		 !rdfType.equals( SyntaxElement.VALUE_COND.resource ) ) {
		throw new AlignmentException( "Bad class restriction type : "+rdfType );
	    }
	    PathExpression pe;
	    Comparator comp;
	    // Find onAttribute
	    Statement stmt = node.getProperty( (Property)SyntaxElement.ONPROPERTY.resource );
	    if ( stmt == null ) throw new AlignmentException( "Required edoal:onAttribute property" );
	    pe = parsePathExpression( stmt.getResource() ); // MUSTCHECK
	    if ( rdfType.equals( SyntaxElement.TYPE_COND.resource ) ) {
		// Check that pe is a Property / Relation
		// ==> different treatment
		// Datatype could also be defined as objets...? (like rdf:resource="")
		stmt = node.getProperty( (Property)SyntaxElement.DATATYPE.resource );
		if ( stmt == null ) throw new AlignmentException( "Required edoal:datatype property" );
		RDFNode nn = stmt.getObject();
		if ( nn.isLiteral() ) {
		    return new ClassTypeRestriction( pe, new Datatype( ((Literal)nn).getString() ) );
		} else {
		    throw new AlignmentException( "Bad edoal:datatype value" );
		}
	    } else if ( rdfType.equals( SyntaxElement.DOMAIN_RESTRICTION.resource ) ) {
		stmt = node.getProperty( (Property)SyntaxElement.TOCLASS.resource );
		if ( stmt == null ) throw new AlignmentException( "Required edoal:class property" );
		RDFNode nn = stmt.getObject();
		if ( nn.isResource() ) {
		    return new ClassDomainRestriction( pe,  parseClass( (Resource)nn ) );
		} else {
		    throw new AlignmentException( "Incorrect class expression "+nn );
		} 
	    } else {
		// Find comparator
		stmt = node.getProperty( (Property)SyntaxElement.COMPARATOR.resource );
		if ( stmt == null ) throw new AlignmentException( "Required edoal:comparator property" );
		URI id = getNodeId( stmt.getResource() );
		if ( id != null ) comp = new Comparator( id );
		else throw new AlignmentException("edoal:comparator requires an URI");
		if ( rdfType.equals( SyntaxElement.OCCURENCE_COND.resource ) ) {
		    stmt = node.getProperty( (Property)SyntaxElement.VALUE.resource );
		    if ( stmt == null ) throw new AlignmentException( "Required edoal:value property" );
		    RDFNode nn = stmt.getObject();
		    if ( nn.isLiteral() ) {
			return new ClassOccurenceRestriction( pe, comp, ((Literal)nn).getInt() );
		    } else {
			throw new AlignmentException( "Bad occurence specification : "+nn );
		    }
		} else if ( rdfType.equals( SyntaxElement.VALUE_COND.resource ) ) {
		    stmt = node.getProperty( (Property)SyntaxElement.VALUE.resource );
		    if ( stmt == null ) throw new AlignmentException( "Required edoal:value property" );
		    ValueExpression v = parseValue( stmt.getObject() );
		    return new ClassValueRestriction( pe, comp, v );
		}
	    }
	}
	return null;
    }

    // JE2010: Here is the problem again with DOMAIN (for instance)
    protected PathExpression parsePathExpression( final Resource node ) throws AlignmentException {
	Resource rdfType = node.getProperty(RDF.type).getResource();
	if ( rdfType.equals( SyntaxElement.PROPERTY_EXPR.resource ) ||
	     rdfType.equals( SyntaxElement.PROPERTY_DOMAIN_COND.resource ) ||
	     rdfType.equals( SyntaxElement.PROPERTY_TYPE_COND.resource ) ||
	     rdfType.equals( SyntaxElement.PROPERTY_VALUE_COND.resource ) ) {
	    return parseProperty( node );
	} else if ( rdfType.equals( SyntaxElement.RELATION_EXPR.resource ) ||
	     rdfType.equals( SyntaxElement.RELATION_CODOMAIN_COND.resource ) ||
	     rdfType.equals( SyntaxElement.RELATION_DOMAIN_COND.resource ) ) {
	    return parseRelation( node );
	} else throw new AlignmentException( "Cannot parse path expression ("+rdfType+"): "+node );
	
    }

    // rdf:parseType="Collection" is supposed to preserve the order ()
    // Jena indeed always preserves the order so this can be used
    protected PropertyExpression parseProperty( final Resource node ) throws AlignmentException {
	Resource rdfType = node.getProperty(RDF.type).getResource();
	Statement stmt = null;
	if ( rdfType.equals( SyntaxElement.PROPERTY_EXPR.resource ) ) {
	    URI id = getNodeId( node );
	    if ( id != null ) {
		return new PropertyId( id );
	    } else {
		Constructor op = null;
		List<PathExpression> clexpr = new LinkedList<PathExpression>();
		if ( node.hasProperty( (Property)SyntaxElement.AND.resource ) ) {
		    op = SyntaxElement.AND.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.AND.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.OR.resource ) ) { 
		    op = SyntaxElement.OR.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.OR.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.COMPOSE.resource ) ) { 
		    op = SyntaxElement.COMPOSE.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.COMPOSE.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.NOT.resource ) ) {
		    op = SyntaxElement.NOT.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.NOT.resource );
		} else {
		    if ( isPattern ) { // not necessarily with a variable (real patterns)
			return new PropertyId();
		    } else {
			throw new AlignmentException( "Property statement must containt one constructor or Id : "+node );
		    }
		}
		Resource coll = stmt.getResource(); // MUSTCHECK
		if ( op == SyntaxElement.NOT.getOperator() ) {
		    clexpr.add( parseProperty( coll ) );
		} else if ( op == SyntaxElement.COMPOSE.getOperator() ) {
		    while ( !RDF.nil.getURI().equals( coll.getURI() ) ) { // THIS IS HORRIBLE
			// In this present case, it is a series of Relations followed by a Property
			Resource newcoll = coll.getProperty( RDF.rest ).getResource(); // MUSTCHECK
			if ( !RDF.nil.getURI().equals( newcoll.getURI() ) ) {
			    clexpr.add( parseRelation( coll.getProperty( RDF.first ).getResource() ) );
			} else {
			    clexpr.add( parseProperty( coll.getProperty( RDF.first ).getResource() ) );
			}
			coll = newcoll;
		    }
		} else { // This is a first/rest statements
		    while ( !RDF.nil.getURI().equals( coll.getURI() ) ) { // THIS IS HORRIBLE
			clexpr.add( parseProperty( coll.getProperty( RDF.first ).getResource() ) );
			coll = coll.getProperty( RDF.rest ).getResource(); // MUSTCHECK
		    }
		}
		return new PropertyConstruction( op, clexpr );
	    }
	} else if ( rdfType.equals( SyntaxElement.PROPERTY_DOMAIN_COND.resource ) ) {
	    stmt = node.getProperty( (Property)SyntaxElement.TOCLASS.resource );
	    if ( stmt == null ) throw new AlignmentException( "Required edoal:toClass property" );
	    RDFNode nn = stmt.getObject();
	    if ( nn.isResource() ) {
		return new PropertyDomainRestriction( parseClass( (Resource)nn ) );
	    } else {
		throw new AlignmentException( "Incorrect class expression "+nn );
	    } 
	} else if ( rdfType.equals( SyntaxElement.PROPERTY_TYPE_COND.resource ) ) {
	    // Datatype could also be defined as objets...? (like rdf:resource="")
	    // Or classes? OF COURSE????
	    stmt = node.getProperty( (Property)SyntaxElement.DATATYPE.resource );
	    if ( stmt == null ) throw new AlignmentException( "Required edoal:datatype property" );
	    RDFNode nn = stmt.getObject();
	    if ( nn.isLiteral() ) {
		return new PropertyTypeRestriction( new Datatype( ((Literal)nn).getString() ) );
	    } else {
		throw new AlignmentException( "Bad edoal:datatype value" );
	    }
	} else if ( rdfType.equals( SyntaxElement.PROPERTY_VALUE_COND.resource ) ) {
	    // Find comparator
	    stmt = node.getProperty( (Property)SyntaxElement.COMPARATOR.resource );
	    if ( stmt == null ) throw new AlignmentException( "Required edoal:comparator property" );
	    URI id = getNodeId( stmt.getResource() );
	    if ( id == null ) throw new AlignmentException("edoal:comparator requires and URI");
	    Comparator comp = new Comparator( id );
	    stmt = node.getProperty( (Property)SyntaxElement.VALUE.resource );
	    if ( stmt == null ) throw new AlignmentException( "Required edoal:value property" );
	    ValueExpression v = parseValue( stmt.getObject() );
	    return new PropertyValueRestriction( comp, v );
	} else {
	    throw new AlignmentException("There is no pasrser for entity "+rdfType.getLocalName());
	}
    }

    protected RelationExpression parseRelation( final Resource node ) throws AlignmentException {
	Resource rdfType = node.getProperty(RDF.type).getResource();
	Statement stmt = null;
	if ( rdfType.equals( SyntaxElement.RELATION_EXPR.resource ) ) {
	    URI id = getNodeId( node );
	    if ( id != null ) {
		return new RelationId( id );
	    } else {
		Constructor op = null;
		// Remains a PathExpression (that this is a relation is checked by typing)
		List<PathExpression> clexpr = new LinkedList<PathExpression>();
		if ( node.hasProperty( (Property)SyntaxElement.AND.resource ) ) {
		    op = SyntaxElement.AND.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.AND.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.OR.resource ) ) { 
		    op = SyntaxElement.OR.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.OR.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.COMPOSE.resource ) ) { 
		    op = SyntaxElement.COMPOSE.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.COMPOSE.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.NOT.resource ) ) {
		    op = SyntaxElement.NOT.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.NOT.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.INVERSE.resource ) ) {
		    op = SyntaxElement.INVERSE.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.INVERSE.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.REFLEXIVE.resource ) ) {
		    op = SyntaxElement.REFLEXIVE.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.REFLEXIVE.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.SYMMETRIC.resource ) ) {
		    op = SyntaxElement.SYMMETRIC.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.SYMMETRIC.resource );
		} else if ( node.hasProperty( (Property)SyntaxElement.TRANSITIVE.resource ) ) {
		    op = SyntaxElement.TRANSITIVE.getOperator();
		    stmt = node.getProperty( (Property)SyntaxElement.TRANSITIVE.resource );
		} else {
		    if ( isPattern ) { // not necessarily with a variable (real patterns)
			return new RelationId();
		    } else {
			throw new AlignmentException( "Relation statement must containt one constructor or Id : "+node );
		    }
		}
		Resource coll = stmt.getResource(); // MUSTCHECK
		if ( op == SyntaxElement.NOT.getOperator() ||
		     op == SyntaxElement.INVERSE.getOperator() || 
		     op == SyntaxElement.REFLEXIVE.getOperator() || 
		     op == SyntaxElement.SYMMETRIC.getOperator() || 
		     op == SyntaxElement.TRANSITIVE.getOperator() ) {
		    clexpr.add( parseRelation( coll ) );
		} else { // This is a first/rest statements
		    while ( !RDF.nil.getURI().equals( coll.getURI() ) ) { // THIS IS HORRIBLE
			clexpr.add( parseRelation( coll.getProperty( RDF.first ).getResource() ) );
			coll = coll.getProperty( RDF.rest ).getResource(); // MUSTCHECK
		    }
		}
		return new RelationConstruction( op, clexpr );
	    }
	} else if ( rdfType.equals( SyntaxElement.RELATION_DOMAIN_COND.resource ) ) {
	    stmt = node.getProperty( (Property)SyntaxElement.TOCLASS.resource );
	    if ( stmt == null ) throw new AlignmentException( "Required edoal:toClass property" );
	    RDFNode nn = stmt.getObject();
	    if ( nn.isResource() ) {
		return new RelationDomainRestriction( parseClass( (Resource)nn ) );
	    } else {
		throw new AlignmentException( "Incorrect class expression "+nn );
	    } 
	} else if ( rdfType.equals( SyntaxElement.RELATION_CODOMAIN_COND.resource ) ) {
	    stmt = node.getProperty( (Property)SyntaxElement.TOCLASS.resource );
	    if ( stmt == null ) throw new AlignmentException( "Required edoal:toClass property" );
	    RDFNode nn = stmt.getObject();
	    if ( nn.isResource() ) {
		return new RelationCoDomainRestriction( parseClass( (Resource)nn ) );
	    } else {
		throw new AlignmentException( "Incorrect class expression "+nn );
	    } 
	} else {
	    throw new AlignmentException("There is no pasrser for entity "+rdfType.getLocalName());
	}
    }

    protected InstanceExpression parseInstance( final Resource node ) throws AlignmentException {
	Resource rdfType = node.getProperty(RDF.type).getResource();
	if ( rdfType.equals( SyntaxElement.INSTANCE_EXPR.resource ) ) {
	    URI id = getNodeId( node );
	    if ( id != null ) return new InstanceId( id );
	    else throw new AlignmentException("Cannot parse anonymous individual");
	} else if ( isPattern ) { // not necessarily with a variable (real patterns)
	    return new InstanceId();
	} else {
	    throw new AlignmentException("There is no pasrser for entity "+rdfType.getLocalName());
	}
    }

    protected ValueExpression parseValue( final RDFNode node ) throws AlignmentException {
	if ( node.isLiteral() ) { // should not appear anymore
	    return new Value( ((Literal)node).getString() );
	} else if ( node.isResource() ) {
	    Resource nodeType = ((Resource)node).getProperty(RDF.type).getResource();
	    if ( nodeType.equals( SyntaxElement.INSTANCE_EXPR.resource ) ) {
		return parseInstance( (Resource)node );
	    } else if ( nodeType.equals( SyntaxElement.LITERAL.resource ) ) {
		if ( ((Resource)node).hasProperty( (Property)SyntaxElement.STRING.resource ) ) {
		    if ( ((Resource)node).hasProperty( (Property)SyntaxElement.TYPE.resource ) ) {
			try {
			    return new Value( ((Resource)node).getProperty( (Property)SyntaxElement.STRING.resource ).getLiteral().getString(), new URI( ((Resource)node).getProperty( (Property)SyntaxElement.TYPE.resource ).getLiteral().getString() ) );
			} catch (URISyntaxException urisex) {
			    throw new AlignmentException( "Incorect URI for edoal:type : "+ ((Resource)node).getProperty( (Property)SyntaxElement.TYPE.resource ).getLiteral().getString() );
			}
		    } else {
			return new Value( ((Resource)node).getProperty( (Property)SyntaxElement.STRING.resource ).getLiteral().getString() );
		    }
		} else {
		    throw new AlignmentException( "edoal:Literal requires a edoal:value" );
		}
	    } else if ( nodeType.equals( SyntaxElement.APPLY.resource ) ) {
		// Get the operation
		URI op;
		if ( ((Resource)node).hasProperty( (Property)SyntaxElement.OPERATOR.resource ) ) {
		    String operation = ((Resource)node).getProperty( (Property)SyntaxElement.OPERATOR.resource ).getLiteral().getString();
		    try {
			op = new URI( operation ); 
		    } catch (URISyntaxException e) {
			throw new AlignmentException( "edoal:Apply incorrect operation URI : "+operation );
		    }
		} else {
		    throw new AlignmentException( "edoal:Apply requires an operation" );
		}
		// Get all arguments
		List<ValueExpression> valexpr = new LinkedList<ValueExpression>();
		if ( ((Resource)node).hasProperty( (Property)SyntaxElement.ARGUMENTS.resource ) ) {
		    Statement stmt = ((Resource)node).getProperty( (Property)SyntaxElement.ARGUMENTS.resource );
		    Resource coll = stmt.getResource(); // MUSTCHECK
		    while ( !RDF.nil.getURI().equals( coll.getURI() ) ) {
			valexpr.add( parseValue( coll.getProperty( RDF.first ).getResource() ) );
			coll = coll.getProperty( RDF.rest ).getResource();
		    }
		}
		return new Apply( op, valexpr );
	    } else { // Check that pe is a Path??
		return parsePathExpression( (Resource)node );
	    }
	} else {
	    throw new AlignmentException( "Bad edoal:value value" );
	}
    }

    protected URI getNodeId( final Resource node ) throws AlignmentException {
	final String idS = node.getURI();
	if ((idS != null) && (idS.length() > 0)) {
	    try {
		return new URI(idS);
	    } catch ( URISyntaxException usex ) {
		throw new AlignmentException( "Incorrect URI: "+idS );
	    }
	} else {
	    return null;
	}
    }

    /**
     * Parses a given annotaion in the the given node.
     * 
     * @param node
     *            which is the parent of the annotation node
     * @param e
     *            the tag which contains the annotation.
     * @return the parsed annotation, with the id set to the element and the
     *         value set to the text of the parsed node, or null, if nothing
     *         could be found
     * @throws NullPointerException
     *             if the node or the element is null
     */
    protected void parseAnnotation(final Statement stmt, EDOALAlignment al ) throws AlignmentException {
	try {
	    final String anno = stmt.getString();
	    if ((anno != null) && (anno.length() > 0)) {
		URI uri = new URI ( stmt.getPredicate().getURI() );
		String name = uri.getFragment();
		String prefix = uri.getScheme()+":"+uri.getSchemeSpecificPart();
		if ( name == null ) {
		    int pos = prefix.lastIndexOf('/');
		    name = prefix.substring( pos+1 );
		    prefix = prefix.substring( 0, pos+1 );
		} else { prefix += "#"; }
		// This will not work for stuff like dc:creator which has no fragment!
		al.setExtension( prefix, name, anno );
	    }
	} catch (Exception e1) {
	    // It would be better to silently ignore annotations
	    // Or report them in a bunch
	    throw new AlignmentException("The annotation is not correct", e1);
	}
    }
    

}
