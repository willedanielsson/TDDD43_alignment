/*
 * $Id: OntologyURI.java 1189 2010-01-03 17:57:13Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2009
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package fr.inrialpes.exmo.align.service;

import java.util.Properties;

/**
 * Contains the messages that should be sent according to the protocol
 */

public class OntologyURI extends Success {

    public OntologyURI ( int surr, Message rep, String from, String to, String cont, Properties param ) {
	super( surr, rep, from, to, cont, param );
    }
    public String HTMLString(){
	return "Ontology URI: "+content;
    }
    public String RESTString(){
	return "<uri>"+content+"</uri>";
    }

}
