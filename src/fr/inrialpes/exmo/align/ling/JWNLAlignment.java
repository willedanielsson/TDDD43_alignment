/*
 * $Id: JWNLAlignment.java 1189 2010-01-03 17:57:13Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2005, 2007, 2009
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

package fr.inrialpes.exmo.align.ling; 

import fr.inrialpes.exmo.align.impl.DistanceAlignment;
import fr.inrialpes.exmo.align.impl.MatrixMeasure;

import fr.inrialpes.exmo.ontosim.string.JWNLDistances;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentException;

import java.util.Properties;
import java.net.URI;

/**
 * This Class uses JWNLDistances to align two ontologies.
 * @version $Id: JWNLAlignment.java 1189 2010-01-03 17:57:13Z euzenat $
 */

public class JWNLAlignment extends DistanceAlignment implements AlignmentProcess {
    final static String WNVERS = "3.0";

    protected class WordNetMatrixMeasure extends MatrixMeasure {
	protected JWNLDistances Dist = null;
	protected int method = 0;

	public WordNetMatrixMeasure() {
	    Dist = new JWNLDistances();
	}
	public void init() throws AlignmentException {
	    Dist.Initialize();
	}
	public void init( String wndict ) throws AlignmentException {
	    Dist.Initialize( wndict, WNVERS );
	}
	public void init( String wndict, String wnvers ) throws AlignmentException {
	    Dist.Initialize( wndict, wnvers );
	}
	public void init( String wndict, String wnvers, int simFunction ) throws AlignmentException {
	    Dist.Initialize( wndict, wnvers );
	    method = simFunction;
	}
	public double measure( Object o1, Object o2 ) throws Exception {
	    String s1 = ontology1().getEntityName( o1 );
	    String s2 = ontology2().getEntityName( o2 );
	    if ( s1 == null || s2 == null ) return 1.;
	    switch ( method ) {
	    case 0:
		return Dist.basicSynonymDistance( s1, s2 );
	    case 1:
		return 1. - Dist.cosynonymySimilarity( s1, s2 );
	    case 2:
		return 1. - Dist.basicSynonymySimilarity( s1, s2 );
	    default:
		return Dist.basicSynonymDistance( s1, s2 );
	    }
	}
	public double classMeasure( Object cl1, Object cl2 ) throws Exception {
	    return measure( cl1, cl2 );
	}
	public double propertyMeasure( Object pr1, Object pr2 ) throws Exception {
	    return measure( pr1, pr2 );
	}
	public double individualMeasure( Object id1, Object id2 ) throws Exception {
	    if ( debug > 4 ) System.err.println( "ID:"+id1+" -- "+id2);
	    return measure( id1, id2 );
	}
    }

    /** Creation **/
    public JWNLAlignment(){
	setSimilarity( new WordNetMatrixMeasure() );
	setType("**");
    };

    /** Processing **/
    public void align( Alignment alignment, Properties prop ) throws AlignmentException {
	int method = 0;
	loadInit( alignment );
	WordNetMatrixMeasure sim = (WordNetMatrixMeasure)getSimilarity();
	String wnvers = prop.getProperty("wnvers");
	if ( wnvers == null ) wnvers = WNVERS;
	String function = prop.getProperty("wnfunction");
	if ( function != null ) {
	    if ( function.equals("cosynonymySimilarity") ) method = 1;
	    else if ( function.equals("basicSynonymySimilarity") ) method = 2;
	}
	sim.init( prop.getProperty("wndict"), wnvers, method );
	sim.initialize( ontology1(), ontology2(), alignment );
	sim.compute( prop );
	if ( prop.getProperty("printMatrix") != null ) printDistanceMatrix( prop );
	extract( type, prop );
    }
}
	
	
	
	
	
	
			
