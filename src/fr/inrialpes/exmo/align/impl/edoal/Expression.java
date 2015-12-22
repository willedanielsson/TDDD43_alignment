/*
 * $Id: Expression.java 1338 2010-03-18 21:33:00Z euzenat $
 *
 * Copyright (C) 2006 Digital Enterprise Research Insitute (DERI) Innsbruck
 * Sourceforge version 1.7 - 2007
 * Copyright (C) INRIA, 2009-2010
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owl.align.Visitable;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;

import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor;

/**
 * <p>
 * This class serves as the base for the four different expression types. These
 * types are namely <code>AttributeExpression</code>,
 * <code>ClassExpression</code>, <code>RelationExpression</code> and
 * <code>InstanceExpression</code>
 * </p>
 * <p>
 * The only fields stored in this class is the <code>ExpressionDefinition</code>
 * of this Expression and the set of condition associated with this expression.
 * </p>
 * <p>
 * To successfully subclass this class overwrite the <code>equals</code> and
 * <code>clone</code> methods. If new fields are introduced the
 * <code>toString</code> and <code>hashCode</code> methods must be
 * overwritten, too.
 * </p>
 * 
 * @author Francois Scharffe, Adrian Mocan
 * 
 * Created on 23-Mar-2005 Committed by $Author: adrianmocan $
 * 
 * $Source:
 * /cvsroot/mediation/mappingapi/src/fr.inrialpes.exmo.align.impl.edoal/Expression.java,v $,
 * @version $Revision: 1.7 $ $Date: 2010-03-07 20:40:05 +0100 (Sun, 07 Mar 2010) $
 */

public abstract class Expression implements Cloneable, Visitable {

    // should not be reasonable to have several variables
    // This would cost too much
    protected Variable variable;

    protected Expression() {}

    public void accept(AlignmentVisitor visitor) throws AlignmentException {
	visitor.visit(this);
    }

    public void accept(TypeCheckingVisitor visitor) throws AlignmentException {
	visitor.visit(this);
    }

    public Variable getVariable() { return variable; }
    public void setVariable( Variable v ) { variable = v; }

}
