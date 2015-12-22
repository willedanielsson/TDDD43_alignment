/*
 * $Id: Apply.java 1365 2010-03-26 21:46:46Z euzenat $
 *
 * Copyright (C) INRIA, 2010
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

package fr.inrialpes.exmo.align.impl.edoal;

import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import org.semanticweb.owl.align.Visitable;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;

import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor;

/**
 * <p>
 * Id to represent a simple valuestring.
 * </p>
 * <p>
 * $Id: Apply.java 1365 2010-03-26 21:46:46Z euzenat $
 * </p>
 * 
 * @author richi
 * @version $Revision: 1.2 $
 * @date $Date: 2010-03-07 20:40:05 +0100 (Sun, 07 Mar 2010) $
 */
public class Apply implements ValueExpression {

    /** Holds the operation to apply */
    private URI operation;

    private List<ValueExpression> arguments;

    /**
     * Constructs an object with the given value.
     * 
     * @param op
     *            the URI of the operation to apply.
     * @param args
     *            its list of argumenst
     * @throws NullPointerException
     *             if the value is {@code null}
     * @throws IllegalArgumentException
     *             if the value isn't longer than 0
     */
    public Apply( final URI op, final List<ValueExpression> args ) {
	if ( op == null) {
	    throw new NullPointerException("The operation must not be null");
	}
	operation = op;
	arguments = args;
    }

    public void accept(AlignmentVisitor visitor) throws AlignmentException {
	visitor.visit(this);
    }

    public void accept(TypeCheckingVisitor visitor) throws AlignmentException {
	visitor.visit(this);
    }

    public URI getOperation() {
	return operation;
    }

    public List<ValueExpression> getArguments() {
	return arguments;
    }

    public int hashCode() {
	return 5*operation.hashCode() + 13*arguments.hashCode();
    }

    public boolean equals( final Object o ) {
	if ( o == this ) return true;
	if ( !(o instanceof Apply) ) return false;
	Apply a = (Apply)o;
	return ( operation.equals(a.getOperation()) && 
		 arguments.equals(a.getArguments()) );
    }
    /*
    public Object clone() {
	return super.clone();
    }
    */

    /**
     * <p>
     * Returns a short description about this object. <b>The format of the
     * returned string is undocumentd and subject to change.</b>
     * </p>
     * <p>
     * An example return string could be: {@code 15}
     * </p>
     */
    //public String toString() {
    //	return value;
    //}
}
