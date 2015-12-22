/*
 * $Id: RelationCoDomainRestriction.java 1311 2010-03-07 22:51:10Z euzenat $
 *
 * Copyright (C) 2006 Digital Enterprise Research Insitute (DERI) Innsbruck
 * Sourceforge version 1.4 - 2006
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
 * Represents a coDomainRestriction tag for RelationExpressions.
 * </p>
 * <p>
 * $Id: RelationCoDomainRestriction.java,v 1.3 2006/11/15 16:01:17 poettler_ric
 * Exp $
 * </p>
 * 
 * @author Richard Pöttler
 * @version $Revision: 1.4 $
 * @date $Date: 2010-03-07 20:40:05 +0100 (Sun, 07 Mar 2010) $
 * 
 */
public class RelationCoDomainRestriction extends RelationRestriction {

    protected ClassExpression codomain = null; 

    /**
     * Constructs a coDomainRestriction with the given restriction.
     * 
     * @param restriction
     *            the restriction for the domain
     * @throws NullPointerException
     *             if the restriction is null
     */
    public RelationCoDomainRestriction() {
	super();
    }
    
    /**
     * Constructs a coDomainRestriction with the given restriction.
     * 
     * @param res
     *            the restriction for the domain
     * @param target
     *            the target expression which should be restricted
     * @throws NullPointerException
     *             if the restriction is null
     */
    public RelationCoDomainRestriction(final ClassExpression cod) {
	super();
	codomain = cod;
    }

    public ClassExpression getCoDomain() {
	return codomain;
    }
    
}
