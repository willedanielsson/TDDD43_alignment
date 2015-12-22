/*
 * $Id: OWLAPIOntologyFactory.java 842 2008-09-19 20:41:29Z euzenat $
 *
 * Copyright (C) INRIA, 2008, 2010
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

package fr.inrialpes.exmo.ontowrap.owlapi10;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.util.OWLConnection;
import org.semanticweb.owl.util.OWLManager;

import fr.inrialpes.exmo.ontowrap.OntologyCache;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

public class OWLAPIOntologyFactory extends OntologyFactory {

    private static URI formalismUri = null;
    private static String formalismId = "OWL1.0";
    private static OntologyCache<OWLAPIOntology> cache = null;

    public OWLAPIOntologyFactory() {
	cache = new OntologyCache<OWLAPIOntology>();
	try {
	    formalismUri = new URI("http://www.w3.org/2002/07/owl#");
	} catch (URISyntaxException ex) { ex.printStackTrace(); } // should not happen
    };

    public void clearCache() {
	cache.clear();
    }

    public OWLAPIOntology newOntology( Object ontology ) throws OntowrapException {
	if ( ontology instanceof OWLOntology ) {
	    OWLAPIOntology onto = new OWLAPIOntology();
	    onto.setFormalism( formalismId );
	    onto.setFormURI( formalismUri );
	    onto.setOntology( (OWLOntology)ontology );
	    //onto.setFile( uri );// unknown
	    try {
		onto.setURI( ((OWLOntology)ontology).getLogicalURI() );
	    } catch (OWLException e) {
		// Better put in the OntowrapException of loaded
		e.printStackTrace();
	    }
	    //cache.recordOntology( uri, onto );
	    return onto;
	} else {
	    throw new OntowrapException( "Argument is not an OWLOntology: "+ontology );
	}
    }

    public OWLAPIOntology loadOntology( URI uri ) throws OntowrapException {
	OWLAPIOntology onto = null;
	onto = cache.getOntologyFromURI( uri );
	if ( onto != null ) return onto;
	onto = cache.getOntology( uri );
	if ( onto != null ) return onto;
	OWLConnection connection = null;
	Map<Object,Object> parameters = new HashMap<Object, Object>();
	parameters.put(OWLManager.OWL_CONNECTION,
		       "org.semanticweb.owl.impl.model.OWLConnectionImpl");
	try {
	    connection = OWLManager.getOWLConnection(parameters);
	    Level lev = Logger.getLogger("org.semanticweb.owl").getLevel();
	    Logger.getLogger("org.semanticweb.owl").setLevel(Level.ERROR);
	    OWLOntology ontology = connection.loadOntologyPhysical(uri);
	    Logger.getLogger("org.semanticweb.owl").setLevel(lev);
	    onto = new OWLAPIOntology();
	    // It may be possible to fill this as final in OWLAPIOntology...
	    onto.setFormalism( formalismId );
	    onto.setFormURI( formalismUri );
	    onto.setOntology( ontology );
	    onto.setFile( uri );
	    try {
		onto.setURI( ontology.getLogicalURI() );
	    } catch (OWLException e) {
		// Better put in the OntowrapException of loaded
		e.printStackTrace();
	    }
	    cache.recordOntology( uri, onto );
	    return onto;
	} catch (OWLException e) {
	    throw new OntowrapException("Cannot load "+uri, e );
	}
    }
}

