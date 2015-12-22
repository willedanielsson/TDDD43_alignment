/*
 * $Id: OWLAPI3OntologyFactory.java 1311 2010-03-07 22:51:10Z euzenat $
 *
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

package fr.inrialpes.exmo.ontowrap.owlapi30;

import java.net.URI;
import java.net.URISyntaxException;

import org.semanticweb.owlapi.apibinding.OWLManager;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.IRI;

import fr.inrialpes.exmo.ontowrap.OntowrapException;
import fr.inrialpes.exmo.ontowrap.OntologyCache;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.HeavyLoadedOntology;

public class OWLAPI3OntologyFactory extends OntologyFactory {

    private URI formalismUri = null;

    private String formalismId = "OWL2.0";

    private OWLOntologyManager manager;
    
    private static OntologyCache<OWLAPI3Ontology> cache = null;

    public OWLAPI3OntologyFactory() {
	cache = new OntologyCache<OWLAPI3Ontology>();
	try {
	    formalismUri = new URI("http://www.w3.org/2002/07/owl#");
	    manager = OWLManager.createOWLOntologyManager();
	} catch (URISyntaxException ex) { // should not happen
	    ex.printStackTrace();
	}
    }

    @Override
    public OWLAPI3Ontology newOntology( Object ontology ) throws OntowrapException {
	if ( ontology instanceof OWLOntology ) {
	    OWLAPI3Ontology onto = new OWLAPI3Ontology();
	    onto.setFormalism( formalismId );
	    onto.setFormURI( formalismUri );
	    onto.setOntology( (OWLOntology)ontology );
	    onto.setURI( ((OWLOntology)ontology).getOntologyID().getOntologyIRI().toURI() );
	    cache.recordOntology( onto.getURI(), onto );
	    cache.recordOntology( ((OWLOntology)ontology).getOntologyID().getOntologyIRI().toURI(), onto );
	    return onto;
	} else {
	    throw new OntowrapException( "Argument is not an OWLOntology: "+ontology );
	}
    }

    @Override
    public HeavyLoadedOntology loadOntology( URI uri ) throws OntowrapException {
	OWLAPI3Ontology onto = null;
	// Cache seems to be implemented in API 3.0 anyway
	// and it seems to not work well with this one
	onto = cache.getOntologyFromURI( uri );
	if ( onto != null ) return onto;
	onto = cache.getOntology( uri );
	if ( onto != null ) return onto;
	OWLOntology ontology;
	try {
	    // This below does not seem to work!
	    //ontology = manager.loadOntologyFromOntologyDocument( IRI.create( uri ) );
	    ontology = manager.loadOntology( IRI.create( uri ) );
	} catch ( OWLOntologyDocumentAlreadyExistsException oodaeex ) {
	    // This is a cache failure
	    throw new OntowrapException("Already loaded [cache failure] " + uri, oodaeex );
	} catch ( OWLOntologyCreationException oocex ) {
	    oocex.printStackTrace();
	    throw new OntowrapException("Cannot load " + uri, oocex );
	}
	onto = new OWLAPI3Ontology();
	onto.setFormalism( formalismId );
	onto.setFormURI( formalismUri );
	onto.setOntology( ontology );
	onto.setFile( uri );
	onto.setURI( ontology.getOntologyID().getOntologyIRI().toURI() );
	cache.recordOntology( uri, onto );
	return onto;
    }
    
    public OWLOntologyManager getManager() {
	return manager;
    }

    @Override
    public void clearCache() {
	
	cache.clear();
    }

}
