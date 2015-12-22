/*
 * $Id: DistanceAlignment.java 1370 2010-03-26 22:20:30Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2010
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

package fr.inrialpes.exmo.align.impl; 

import java.lang.ClassNotFoundException;
import java.lang.ClassCastException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Comparator;
import java.util.Properties;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import fr.inrialpes.exmo.ontosim.util.HungarianAlgorithm;

/**
 *
 * @author J�r�me Euzenat
 * @version $Id: DistanceAlignment.java 1370 2010-03-26 22:20:30Z euzenat $ 
 */

public class DistanceAlignment extends ObjectAlignment implements AlignmentProcess {
    Similarity sim;

    /** Creation **/
    public DistanceAlignment() {}

    // JE: OntoRewr (LoadedOntology -> one step above)
    //public DistanceAlignment( LoadedOntology onto1, LoadedOntology onto2 ){
	// Init must now be triggered explicitely
	//    	init( onto1, onto2 );
    //};

    public void setSimilarity( Similarity s ) { sim = s; }
    public Similarity getSimilarity() { return sim; }

    public void addAlignDistanceCell( Object ob1, Object ob2, String relation, double measure) throws AlignmentException {
	addAlignCell( ob1, ob2, relation, 1.-measure );
    }
    public double getAlignedDistance1( Object ob ) throws AlignmentException {
	return (1. - getAlignedStrength1(ob));
    };
    public double getAlignedDistance2( Object ob ) throws AlignmentException {
	return (1. - getAlignedStrength2(ob));
    };

    /**
     * Process matching
     * - create distance data structures,
     * - compute distance or similarity
     * - extract alignment
     **/
    public void align( Alignment alignment, Properties params ) throws AlignmentException {
	loadInit( alignment );
	if (  params.getProperty("type") != null ) 
	    setType( params.getProperty("type") );
	// This is a 1:1 alignment in fact
	else setType("11");
	if ( sim == null )
	    throw new AlignmentException("DistanceAlignment: requires a similarity measure");

	sim.initialize( ontology1(), ontology2(), init );
	sim.compute( params );
	if ( params.getProperty("printMatrix") != null ) printDistanceMatrix(params);
	extract( getType(), params );
    }

    /**
     * Prints the distance matrix
     */
    public void printDistanceMatrix( Properties params ){
	System.out.println("\\documentclass{article}\n");
	System.out.println("\\usepackage{graphics}\n");
	System.out.println("\\begin{document}\n");
	System.out.println("\\begin{table}");
	sim.printClassSimilarityMatrix("tex");
	System.out.println("\\caption{Class distance with measure "+params.getProperty("stringFunction")+"}");
	System.out.println("\\end{table}");
	System.out.println();
	System.out.println("\\begin{table}");
	sim.printPropertySimilarityMatrix("tex");
	System.out.println("\\caption{Property distance with measure "+params.getProperty("stringFunction")+"}");
	System.out.println("\\end{table}");
	System.out.println();
	System.out.println("\\begin{table}");
	sim.printIndividualSimilarityMatrix("tex");
	System.out.println("\\caption{Individual distance with measure "+params.getProperty("stringFunction")+"}");
	System.out.println("\\end{table}");
	System.out.println("\n\\end{document}");
    }

    /**
     * Suppresses the distance matrix
     */
    public void cleanUp() {
	sim = null;
    }

    /**
     * Extract the alignment form the Similarity
     * There are theoretically 16 types of extractors composing the
     * characteristics
     * [q]estion mark = ?, one or zero relation
     * [s]tar = *, one, zero or many relations
     * [1] = 1, exactly one relation
     * [p]lus = +, one or many relations
     * for each place of the relation. Howerver, since it is not possible from a matrics to guarantee that one object will be in at least one relation, this is restricted to the four following types:
     * ?? (covering 11, 1? and ?1)
     * ** (covering ++, *+ and +*)
     * ?* (covering 1*, 1+ and ?+)
     * *? (covering +?, *1 and +1)
     */
    public Alignment extract(String type, Properties params) throws AlignmentException {
	double threshold = 0.;
	if (  params.getProperty("threshold") != null )
	    threshold = Double.parseDouble( params.getProperty("threshold") );

	//System.err.println("The type is "+type+" with length = "+type.length());
	if ( type.equals("?*") || type.equals("1*") || type.equals("?+") || type.equals("1+") ) return extractqs( threshold, params );
	else if ( type.equals("??") || type.equals("1?") || type.equals("?1") || type.equals("11") ) return extractqq( threshold, params );
	else if ( type.equals("*?") || type.equals("+?") || type.equals("*1") || type.equals("+1") ) return extractqs( threshold, params );
	else if ( type.equals("**") || type.equals("+*") || type.equals("*+") || type.equals("++") ) return extractqs( threshold, params );
	// The else should be an error message
	else throw new AlignmentException("Unknown alignment type: "+type);
    }

    // JE: It is now certainly possible to virtualise extraction has it has
    // been done for printing matrix in MatrixMeasure (todo)

    /**
     * Extract the alignment of a ?* type
     * Complexity: O(n^2)
     */
    @SuppressWarnings("unchecked") //ConcatenatedIterator
    public Alignment extractqs( double threshold, Properties params) {
      double max = 0.;
      boolean found = false;
      double val = 0;

      try {
	  // Extract for properties
	  ConcatenatedIterator pit1 = new 
	      ConcatenatedIterator(ontology1().getObjectProperties().iterator(),
				   ontology1().getDataProperties().iterator());
	  for( Object prop1 : pit1 ){
	      found = false; max = threshold; val = 0;
	      Object prop2 = null;
	      ConcatenatedIterator pit2 = new 
		  ConcatenatedIterator(ontology2().getObjectProperties().iterator(),
				       ontology2().getDataProperties().iterator());
	      for ( Object current : pit2 ){
		  val = 1 - sim.getPropertySimilarity(prop1,current);
		  if ( val > max) {
		      found = true; max = val; prop2 = current;
		  }
	      }
	      if ( found ) addAlignCell(prop1,prop2, "=", max);
	  }
	  // Extract for classes
	  for ( Object class1 : ontology1().getClasses() ) {
	      found = false; max = threshold; val = 0;
	      Object class2 = null;
	      for ( Object current : ontology2().getClasses() ) {
		  val = 1 - sim.getClassSimilarity(class1,current);
		  if (val > max) {
		      found = true; max = val; class2 = current;
		  }
	      }
	      if ( found ) addAlignCell(class1, class2, "=", max);
	  }
	  // Extract for individuals
	  if (  params.getProperty("noinst") == null ){
	      for ( Object ind1 : ontology1().getIndividuals() ) {
		  if ( ontology1().getEntityURI( ind1 ) != null ) {
		      found = false; max = threshold; val = 0;
		      Object ind2 = null;
		      for ( Object current : ontology2().getIndividuals() ) {
			  if ( ontology2().getEntityURI( current ) != null ) {
			      val = 1 - sim.getIndividualSimilarity( ind1, current );
			      if (val > max) {
				  found = true; max = val; ind2 = current;
			      }
			  }
		      }
		      if ( found ) addAlignCell(ind1,ind2, "=", max);
		  }
	      }
	  }
      } catch (OntowrapException owex) { owex.printStackTrace(); //}
      } catch (AlignmentException alex) { alex.printStackTrace(); }
      return((Alignment)this);
    }

    /**
     * Extract the alignment of a ?? type
     * 
     * exact algorithm using the Hungarian method
     */
    @SuppressWarnings("unchecked") //ConcatenatedIterator
    public Alignment extractqq( double threshold, Properties params) {
	try {
	    // A STRAIGHTFORWARD IMPLEMENTATION
	    // (redoing the matrix instead of getting it)
	    // For each kind of stuff (cl, pr, ind)
	    // Create a matrix
	    int nbclasses1 = ontology1().nbClasses();
	    int nbclasses2 = ontology2().nbClasses();
	    double[][] matrix = new double[nbclasses1][nbclasses2];
	    Object[] class1 = new Object[nbclasses1];
	    Object[] class2 = new Object[nbclasses2];
	    int i = 0;
	    for ( Object ob : ontology1().getClasses() ) {
		class1[i++] = ob;
	    }
	    int j = 0;
	    for ( Object ob : ontology2().getClasses() ) {
		class2[j++] = ob;
	    }
	    for( i = 0; i < nbclasses1; i++ ){
		for( j = 0; j < nbclasses2; j++ ){
		    matrix[i][j] = 1 - sim.getClassSimilarity(class1[i],class2[j]);
		}
	    }
	    // Pass it to the algorithm
	    int[][] result = HungarianAlgorithm.hgAlgorithm( matrix, "max" );
	    // Extract the result
	    for( i=0; i < result.length ; i++ ){
		// The matrix has been destroyed
		double val = 1 - sim.getClassSimilarity(class1[result[i][0]],class2[result[i][1]]);
		// JE: here using strict-> is a very good idea.
		// it means that alignments with 0. similarity
		// will be excluded from the best match. 
		if( val > threshold ){
		    addCell( new ObjectCell( (String)null, class1[result[i][0]], class2[result[i][1]], BasicRelation.createRelation("="), val ) );
		}
	    }
	} catch (AlignmentException alex) { alex.printStackTrace(); }
	// For properties
	try{
	    int nbprop1 = ontology1().nbProperties();
	    int nbprop2 = ontology2().nbProperties();
	    double[][] matrix = new double[nbprop1][nbprop2];
	    Object[] prop1 = new Object[nbprop1];
	    Object[] prop2 = new Object[nbprop2];
	    int i = 0;
	    ConcatenatedIterator pit1 = new 
		ConcatenatedIterator(ontology1().getObjectProperties().iterator(),
				     ontology1().getDataProperties().iterator());
	    for ( Object ob: pit1 ) prop1[i++] = ob;
	    int j = 0;
	    ConcatenatedIterator pit2 = new 
		ConcatenatedIterator(ontology2().getObjectProperties().iterator(),
				     ontology2().getDataProperties().iterator());
	    for ( Object ob: pit2 ) prop2[j++] = ob;
	    for( i = 0; i < nbprop1; i++ ){
		for( j = 0; j < nbprop2; j++ ){
		    matrix[i][j] = 1 - sim.getPropertySimilarity(prop1[i],prop2[j]);
		}
	    }
	    // Pass it to the algorithm
	    int[][] result = HungarianAlgorithm.hgAlgorithm( matrix, "max" );
	    // Extract the result
	    for( i=0; i < result.length ; i++ ){
		// The matrix has been destroyed
		double val = 1 - sim.getPropertySimilarity(prop1[result[i][0]],prop2[result[i][1]]);
		// JE: here using strict-> is a very good idea.
		// it means that alignments with 0. similarity
		// will be excluded from the best match. 
		if( val > threshold ){
		    addCell( new ObjectCell( (String)null, prop1[result[i][0]], prop2[result[i][1]], BasicRelation.createRelation("="), val ) );
		}
	    }
	} catch (AlignmentException alex) { alex.printStackTrace(); }
	// For individuals
	if (  params.getProperty("noinst") == null ){
	    try{
		// Create individual lists
		Object[] ind1 = new Object[ontology1().nbIndividuals()];
		Object[] ind2 = new Object[ontology2().nbIndividuals()];
		int nbind1 = 0;
		int nbind2 = 0;
		for( Object ob : ontology2().getIndividuals() ){
		    // We suppress anonymous individuals... this is not legitimate
		    if ( ontology2().getEntityURI( ob ) != null ) {
			ind2[nbind2++] = ob;
		    }
		}
		for( Object ob : ontology1().getIndividuals() ){
		    // We suppress anonymous individuals... this is not legitimate
		    if ( ontology1().getEntityURI( ob ) != null ) {
			ind1[nbind1++] = ob;
		    }
		}
		double[][] matrix = new double[nbind1][nbind2];
		int i, j;
		for( i=0; i < nbind1; i++ ){
		    for( j=0; j < nbind2; j++ ){
			matrix[i][j] = 1 - sim.getIndividualSimilarity(ind1[i],ind2[j]);
		    }
		}
		// Pass it to the algorithm
		int[][] result = HungarianAlgorithm.hgAlgorithm( matrix, "max" );
		// Extract the result
		for( i=0; i < result.length ; i++ ){
		    // The matrix has been destroyed
		    double val = 1 - sim.getIndividualSimilarity(ind1[result[i][0]],ind2[result[i][1]]);
		    // JE: here using strict-> is a very good idea.
		    // it means that alignments with 0. similarity
		    // will be excluded from the best match. 
		    if( val > threshold ){
			addCell( new ObjectCell( (String)null, ind1[result[i][0]], ind2[result[i][1]], BasicRelation.createRelation("="), val ) );
		    }
		}
	    } catch (AlignmentException alex) { alex.printStackTrace(); //}
	    } catch (OntowrapException owex) { owex.printStackTrace(); }
	}
	return((Alignment)this);
    }

    /**
     * Greedy algorithm:
     * 1) dump the part of the matrix distance above threshold in a sorted set
     * 2) traverse the sorted set and each time a correspondence involving two
     *    entities that have no correspondence is encountered, add it to the 
     *    alignment.
     * Complexity: O(n^2.logn)
     * Pitfall: no global optimality is warranted, nor stable marriage
     * for instance if there is the following matrix:
     * (a,a')=1., (a,b')=.9, (b,a')=.9, (b,b')=.1
     * This algorithm will select the first and last correspondances of
     * overall similarity 1.1, while the optimum is the second solution
     * with overall of 1.8.
     */
    @SuppressWarnings("unchecked") //ConcatenatedIterator
    public Alignment extractqqgreedy( double threshold, Properties params) {
	double val = 0;
	//TreeSet could be replaced by something else
	//The comparator must always tell that things are different!
	SortedSet<Cell> cellSet = new TreeSet<Cell>(
			    new Comparator<Cell>() {
				public int compare( Cell o1, Cell o2 )
				    throws ClassCastException{
				    try {
					//System.err.println(o1.getObject1()+" -- "+o1.getObject2()+" // "+o2.getObject1()+" -- "+o2.getObject2());
					if ( o1.getStrength() > o2.getStrength() ){
					    return -1;
					} else if ( o1.getStrength() < o2.getStrength() ){
					    return 1;
					} else if ( ontology1().getEntityName( o1.getObject1() ) == null
						    || ontology2().getEntityName( o2.getObject1() ) == null ) {
					    return -1;
					} else if ( ontology1().getEntityName( o1.getObject1()).compareTo( ontology2().getEntityName( o2.getObject1() ) ) > 0 ) {
					    return -1;
					} else if ( ontology1().getEntityName( o1.getObject1()).compareTo( ontology2().getEntityName( o2.getObject1() ) ) < 0 ) {
					    return 1;
					} else if ( ontology1().getEntityName( o1.getObject2() ) == null
						    || ontology2().getEntityName( o2.getObject2() ) == null ) {
					    return -1;
					} else if ( ontology1().getEntityName( o1.getObject2()).compareTo( ontology2().getEntityName( o2.getObject2() ) ) > 0 ) {
					    return -1;
					// Assume they have different names
					} else { return 1; }
				    } catch ( OntowrapException e) { 
					e.printStackTrace(); return 0;}
				}
			    }
			    );
      try {
	  // Get all the matrix above threshold in the SortedSet
	  // Plus a map from the objects to the cells
	  // O(n^2.log n)
	  // for classes
	  for ( Object ent1: ontology1().getClasses() ) {
	      for ( Object ent2: ontology2().getClasses() ) {
		  val = 1 - sim.getClassSimilarity( ent1, ent2 );
		  if ( val > threshold ){
		      cellSet.add( new ObjectCell( (String)null, ent1, ent2, BasicRelation.createRelation("="), val ) );
		  }
	      }
	  }
	  // for properties
	  ConcatenatedIterator pit1 = new 
	      ConcatenatedIterator(ontology1().getObjectProperties().iterator(),
				   ontology1().getDataProperties().iterator());
	  for ( Object ent1: pit1 ) {
	      ConcatenatedIterator pit2 = new 
		  ConcatenatedIterator(ontology2().getObjectProperties().iterator(),
					ontology2().getDataProperties().iterator());
	      for ( Object ent2: pit2 ) {
		  val = 1 - sim.getPropertySimilarity( ent1, ent2 );
		  if ( val > threshold ){
		      cellSet.add( new ObjectCell( (String)null, ent1, ent2, BasicRelation.createRelation("="), val ) );
		  }
	      }
	  }
	  // for individuals
	  if (  params.getProperty("noinst") == null ){
	      for( Object ent1: ontology1().getIndividuals() ) {
		  if ( ontology1().getEntityURI( ent1 ) != null ) {

		      for( Object ent2: ontology2().getIndividuals() ) {
			  if ( ontology2().getEntityURI( ent2 ) != null ) {
			      val = 1 - sim.getIndividualSimilarity( ent1, ent2 );
			      if ( val > threshold ){
				  cellSet.add( new ObjectCell( (String)null, ent1, ent2, BasicRelation.createRelation("="), val ) );
			      }
			  }
		      }
		  }
	      }
	  }

	  // O(n^2)
	  for( Cell cell : cellSet ){
	      Object ent1 = cell.getObject1();
	      Object ent2 = cell.getObject2();
	      if ( (getAlignCells1( ent1 ) == null) && (getAlignCells2( ent2 ) == null) ){
		  // The cell is directly added!
		  addCell( cell );
	      }
	  };

      } catch (AlignmentException alex) {
	  alex.printStackTrace();
      } catch (OntowrapException owex) {
	  owex.printStackTrace();
      };
      return((Alignment)this);
    }

}
