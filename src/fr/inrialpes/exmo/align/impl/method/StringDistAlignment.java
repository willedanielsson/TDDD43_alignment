/*
 * $Id: StringDistAlignment.java 1275 2010-02-18 00:26:15Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2010
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

package fr.inrialpes.exmo.align.impl.method; 

import java.net.URI;
import java.util.Properties;
import java.lang.reflect.Method;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.align.impl.DistanceAlignment;
import fr.inrialpes.exmo.align.impl.MatrixMeasure;

/**
 * Represents an OWL ontology alignment. An ontology comprises a number of
 * collections. Each ontology has a number of classes, properties and
 * individuals, along with a number of axioms asserting information
 * about those objects.
 *
 * An improvement of that class is that, since it is based on names only,
 * it can match freely property names with class names...
 *
 * @author J�r�me Euzenat
 * @version $Id: StringDistAlignment.java 1275 2010-02-18 00:26:15Z euzenat $ 
 */

public class StringDistAlignment extends DistanceAlignment implements AlignmentProcess {
    
    Method dissimilarity = null;
    String methodName = "equalDistance";

    /** Creation **/
    public StringDistAlignment() {
	setSimilarity( new MatrixMeasure() {
		// The method that we might need to change, emailed Valentina for
		// confirmation
		public double measure( Object o1, Object o2 ) throws Exception {
		    String s1 = ontology1().getEntityName( o1 );
		    String s2 = ontology2().getEntityName( o2 );
		    // Unnamed entity = max distance
		    if ( s1 == null || s2 == null ) 
		    	return 1.;
		    Object[] params = { s1.toLowerCase(), s2.toLowerCase() };
		    if ( debug > 4 ) 
			System.err.println( "OB:"+s1+" ++ "+s2+" ==> "+dissimilarity.invoke( null, params ));
		    return ((Double)dissimilarity.invoke( null, params )).doubleValue();
		}
		public double classMeasure( Object cl1, Object cl2 ) throws Exception {
		    return measure( cl1, cl2 );
		}
		public double propertyMeasure( Object pr1, Object pr2 ) throws Exception{
		    return measure( pr1, pr2 );
		}
		public double individualMeasure( Object id1, Object id2 ) throws Exception{
		    return measure( id1, id2 );
		}
	    } );
	setType("**");
    }

    /* Processing */
    public void align( Alignment alignment, Properties params ) throws AlignmentException {
	// Get function from params
	String f = params.getProperty("stringFunction");
	try {
	    if ( f != null ) methodName = f.trim();
	    Class sClass = Class.forName("java.lang.String");
	    Class[] mParams = { sClass, sClass };
	    dissimilarity = Class.forName("fr.inrialpes.exmo.ontosim.string.StringDistances").getMethod( methodName, mParams );
	} catch (ClassNotFoundException e) {
	    e.printStackTrace(); // never happens
	} catch (NoSuchMethodException e) {
	    throw new AlignmentException( "Unknown method for StringDistAlignment : "+params.getProperty("stringFunction"), e );
	}

	// JE2010: Strange: why does it is not equivalent to call
	// super.align( alignment, params )
	// Load initial alignment
	loadInit( alignment );

	// Initialize matrix
	getSimilarity().initialize( ontology1(), ontology2(), alignment );

	// Compute similarity/dissimilarity
	getSimilarity().compute( params );

	// Print matrix if asked
	if ( params.getProperty("printMatrix") != null ) printDistanceMatrix( params );

	// Extract alignment
	extract( type, params );
    }

}
