/*
 * Copyright (c) 2006 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial concept and implementation
 */
package coyote.mbus.network;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import coyote.mbus.util.ChainedException;


/**
 * The PacketException class models packet processing exception events
 */
public class PacketException extends ChainedException
{
  public static final long serialVersionUID = 1L;




  /**
   * 
   */
  public PacketException()
  {
    super();
  }




  /**
   * @param message
   */
  public PacketException( final String message )
  {
    super( message );
  }




  /**
   * @param message
   * @param newNested
   */
  public PacketException( final String message, final Throwable newNested )
  {
    super( message, newNested );
  }




  /**
   * @param newNested
   */
  public PacketException( final Throwable newNested )
  {
    super( newNested );
  }




  /**
   * Return the stack trace for the given throwable as a string.
   * 
   * <p>This will dump the entire stacktrace of the root exception.</p>
   *
   * @param t The Throwable object whose stack trace we want to render as a 
   *        string
   *
   * @return the stack trace as a string
   */
  public static String stackTrace( final Throwable t )
  {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    ChainedException.getRootException( t ).printStackTrace( new PrintWriter( out, true ) );
    return out.toString();
  }

}
