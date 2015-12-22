/*
 * $Id: LoadedOntology.java 1363 2010-03-26 15:58:57Z jdavid $
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

package fr.inrialpes.exmo.ontowrap;

import java.net.URI;
import java.util.Set;

import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLException;

import fr.inrialpes.exmo.ontowrap.util.FilteredSet;

public interface LoadedOntology<O> extends Ontology<O> {

    public Object getEntity( URI u ) throws OntowrapException;

    public URI getEntityURI( Object o ) throws OntowrapException;
    
    /**
     * returns the default name of an entity if specified.
     * otherwise, returns one of its names (e.g., "rdfs:label" property values).
     * Which name is returned is unspecified.
     * In case no such official name is given to the entity it is possible to 
     * use the entity URI to return its fragment identifier (after the '#') or 
     * last fragment (after the last "/" or just before) in this order.
     * Because of its low definiteness, it is not advised to use this primitive
     * and it is better to use other ones (getNames, getComments, getAnnotations).
     * @param o the entity
     * @return a label
     * @throws OntowrapException
     */
    public String getEntityName( Object o ) throws OntowrapException;

    /**
     * returns the default name of an entity in a language (attribute xml:lang)
     * if specified.
     * Which name is returned is unspecified.
     * otherwise, returns one of its names (e.g., "rdfs:label" property values)
     * otherwise returns the default name (getEntityName)
     * Because of its low definiteness, it is not advised to use this primitive
     * and it is better to use other ones (getNames, getComments, getAnnotations).
     * @param o the entity
     * @return a label
     * @throws OntowrapException
     */
    public String getEntityName( Object o, String lang ) throws OntowrapException;

    /**
     * returns all the names of an entity in a language if specified.
     * otherwise, returns null
     * @param o the entity
     * @param lang the code of the language ("en", "fr", "es", etc.) 
     * @return the default name
     * @throws OntowrapException
     */
    public Set<String> getEntityNames( Object o, String lang ) throws OntowrapException;
    /**
     * Returns all the names a given entity (e.g., rdfs:labels in OWL/RDFS).
     * @param o the entity
     * @return the set of labels
     * @throws OntowrapException
     */
    public Set<String> getEntityNames( Object o ) throws OntowrapException;
    
    /**
     * Returns the values ofof textual properties (e.g., "rdfs:comment", rdfs:label in RDFS/OWL) for a given entity and for a given natural language (attribute xml:lang).
     * @param o the entity
     * @param lang the code of the language ("en", "fr", "es", etc.) 
     * @return the set of comments
     * @throws OntowrapException
     */
    public Set<String> getEntityComments( Object o , String lang ) throws OntowrapException;
    
    /**
     * Returns all the values of textual properties (e.g., "rdfs:comment", rdfs:label in RDFS/OWL) for a given entity
     * @param o the entity
     * @return the set of comments
     * @throws OntowrapException
     */
    public Set<String> getEntityComments( Object o ) throws OntowrapException;
    
    /**
     * Returns all the values of the "owl:AnnotationProperty" property for a given entity. 
     * These annotations are those predefined in owl (owl:versionInfo, rdfs:label, rdfs:comment, rdfs:seeAlso and rdfs:isDefinedBy)
     * but also all other defined annotation properties which are subClass of "owl:AnnotationProperty"
     * @param o the entity
     * @return the set of annotation values
     * @throws OntowrapException
     */
    public Set<String> getEntityAnnotations( Object o ) throws OntowrapException;

    /**
     * Returns all the values of the "owl:AnnotationProperty" property for a given entity expressed in the required language. 
     * These annotations are those predefined in owl (owl:versionInfo, rdfs:label, rdfs:comment, rdfs:seeAlso and rdfs:isDefinedBy)
     * but also all other defined annotation properties which are subClass of "owl:AnnotationProperty"
     * @param o the entity
     * @param lang the code of the language ("en", "fr", "es", etc.) 
     * @return the set of annotation values
     * @throws OntowrapException
     */
    public Set<String> getEntityAnnotations( Object o, String lang ) throws OntowrapException;

    public boolean isEntity( Object o );
    public boolean isClass( Object o );
    public boolean isProperty( Object o );
    public boolean isDataProperty( Object o );
    public boolean isObjectProperty( Object o );
    public boolean isIndividual( Object o );

    /**
     * Returns all named entities having URI beginning with the ontology URI
     * @return the set of entities
     */
    public Set<? extends Object> getEntities();
    public Set<? extends Object> getClasses();
    public Set<? extends Object> getProperties();
    public Set<? extends Object> getObjectProperties();
    public Set<? extends Object> getDataProperties();
    public Set<? extends Object> getIndividuals();

    public int nbEntities();
    public int nbClasses();
    public int nbProperties();
    public int nbDataProperties();
    public int nbObjectProperties();
    public int nbIndividuals();

    public void unload();
}
