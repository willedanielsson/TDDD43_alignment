/*
 * $Id: PropertyTypeRestriction.java 1311 2010-03-07 22:51:10Z euzenat $
 *
 * Copyright (C) 2006 Digital Enterprise Research Insitute (DERI) Innsbruck
 * Sourceforge version 1.6 - 2006 -- then AttributeTypeCondition.java
 * Copyright (C) INRIA, 2009
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

/**
 * <p>
 * Represents a attributeTypeRestriction tag for a ClassExpressions.
 * </p>
 * <p>
 * Created on 24-Mar-2005 Committed by $Author: poettler_ric $
 * </p>
 * <p>
 * $Id: PropertyTypeRestriction.java 1311 2010-03-07 22:51:10Z euzenat $
 * </p>
 * 
 * @author Francois Scharffe
 * @author Adrian Mocan
 * @author Richard Pöttler
 * @version $Revision: 1.6 $ $Date: 2010-03-07 20:40:05 +0100 (Sun, 07 Mar 2010) $
 */
public class PropertyTypeRestriction extends PropertyRestriction implements Cloneable {

    // BEWARE THIS IS INCORRECTLY IMPLEMENTED AS VALUES INSTEAD OF TYPES
    Datatype type = null;

    /**
     * Constructs a attributeTypeRestriction with the given restriction.
     * 
     * @param attribute
     *            the attribute on which the restriction should be applied
     * @param restriction
     *            the restriction for the domain
     * @throws NullPointerException
     *             if the restriction is null
     */
    public PropertyTypeRestriction() {
	super();
    }

    /**
     * Constructs a attributeTypeRestriction with the given restriction.
     * 
     * @param attribute
     *            the attribute on which the restriction should be applied
     * @param restriction
     *            the restriction for the domain
     * @param target
     *            the target expression which should be restricted
     * @throws NullPointerException
     *             if the restriction is null
     */
    public PropertyTypeRestriction(final Datatype t) {
	super();
	type = t;
    }

    public Datatype getType() {
	return type;
    }
    public void setType( Datatype t ) {
	type = t;
    }

    /*
    public Object clone() {
	return super.clone();
    }
    */
}
