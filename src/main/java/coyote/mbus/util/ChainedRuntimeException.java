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
package coyote.mbus.util;

import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * Exception class to allow Exception chaining. This helps indicate the origin
 * of an exception more clearly than simply passing on the original message.
 */
public class ChainedRuntimeException extends RuntimeException
{
  public static final long serialVersionUID = 1L;

  /** This holds the nested or "Chained" message */
  private Throwable nested = null;




  /**
   * Normal no-args constructor
   */
  public ChainedRuntimeException()
  {
    super();
  }




  /**
   * Normal constructor with a single message
   *
   * @param s The exception message
   */
  public ChainedRuntimeException( final String s )
  {
    super( s );
  }




  /**
   * Constructor with a single message and a nested exception
   *
   * @param message
   * @param newNested The nested item
   */
  public ChainedRuntimeException( final String message, final Throwable newNested )
  {
    super( message );

    nested = newNested;
  }




  /**
   * Constructor with no message and a nested exception
   *
   * @param newNested The nested exception
   */
  public ChainedRuntimeException( final Throwable newNested )
  {
    nested = newNested;
  }




  /**
   * Extend getMessage to return the nested message (if any) if we don't have one
   *
   * @return TODO Complete Documentation
   */
  public String getMessage()
  {
    final String message = super.getMessage();

    if( message == null )
    {
      if( nested == null )
      {
        return null;
      }
      else
      {
        return new String( "Nested exception message: " + nested.getMessage() );
      }
    }
    else
    {
      if( nested == null )
      {
        return message;
      }
      else
      {
        return new String( message + " - Nested exception message: " + nested.getMessage() );
      }
    }
  }




  /**
   * Extend printStackTrace to handle the nested exception correctly.
   */
  public void printStackTrace()
  {
    super.printStackTrace();

    if( nested != null )
    {
      nested.printStackTrace();
    }
  }




  /**
   * Extend printStackTrace to handle the nested Exception
   *
   * @param p The PrintStream to write the exception messages into
   */
  public void printStackTrace( final PrintStream p )
  {
    super.printStackTrace( p );

    if( nested != null )
    {
      nested.printStackTrace( p );
    }
  }




  /**
   * Extend printStackTrace to handle the nested Exception
   *
   * @param p The PrintWriter to write the exception messages into
   */
  public void printStackTrace( final PrintWriter p )
  {
    super.printStackTrace( p );

    if( nested != null )
    {
      nested.printStackTrace( p );
    }
  }
}
