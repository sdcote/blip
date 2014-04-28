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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;


/**
 * Exception class to allow Exception chaining. This helps indicate the origin
 * of an exception more clearly than simply passing on the original message.
 *
 * <p>As of Java 1.4, the Throwable class supports nested or 'cause' exception
 * nesting. This code no longer maintains it's own reference to the nested
 * exception, it uses and expands upon the 1.4 functionality.</p>
 *
 * @author Stephan D. Cote' - Enterprise Architecture
 * @version $Revision$
 */
public class ChainedException extends Exception
{
  public static final long serialVersionUID = 1L;




  /**
   * Normal no-arg constructor
   */
  public ChainedException()
  {
    super();
  }




  /**
   * Normal constructor with a single message
   *
   * @param message The exception message
   */
  public ChainedException( final String message )
  {
    super( message );
  }




  /**
   * Constructor with a single message and a nested exception
   *
   * @param message The exception message
   * @param newNested The nested item
   */
  public ChainedException( final String message, final Throwable newNested )
  {
    super( message );

    super.initCause( newNested );
  }




  /**
   * Constructor with no message and a nested exception
   *
   * @param newNested The nested exception
   */
  public ChainedException( final Throwable newNested )
  {
    super.initCause( newNested );
  }




  /**
   * Return just our local message, don't display the nested messages as is the
   * default output of the <tt>getMessage()</tt> method.
   *
   * <p>If we don't have one, a null value will be returned.</p>
   *
   * @return Our local message only.
   */
  public String getLocalMessage()
  {
    return super.getMessage();
  }




  /**
   * Return the stack trace of this exception as a string.
   *
   * @return The stack trace of this exception.
   */
  public String stackTrace()
  {
    return ChainedException.stackTrace( this );
  }




  /**
   * Return the stack trace for the given throwable as a string.
   * 
   * <p>This will dump the entire stacktrace of the root exception
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




  /**
   * Return the nested or 'cause' message.
   *
   * <p>Return the message of the nested exception or the message of this
   * exception if there is no nested exception.</p>
   *
   * @return The message of the core exception.
   */
  public String getCauseMessage()
  {
    if( getCause() != null )
    {
      if( getCause() instanceof ChainedException )
      {
        return ( (ChainedException)getCause() ).getLocalMessage();
      }
      else
      {
        return getCause().getMessage();
      }
    }
    else
    {
      return super.getMessage();
    }
  }




  /**
   * Return the root cause message.
   *
   * <p>Return the message of the inner-most exception that is wrapped by this
   * and any nested exceptions threrein. This will delegate the event call to
   * the first exception in the chain and return its value.</p>
   *
   * @return The message of the core exception.
   */
  public static String getRootMessage( final Throwable t )
  {
    return ChainedException.getRootException( t ).getMessage();
  }




  public String getRootMessage()
  {
    return ChainedException.getRootMessage( this );
  }




  /**
   * Return the root cause exception.
   *
   * <p>Return the inner-most exception that is wrapped by this and any nested
   * exceptions threrein. This will delegate the event call to the first
   * exception in the chain and return its reference.</p>
   *
   * @return The message of the core exception.
   */
  public static Throwable getRootException( final Throwable t )
  {
    if( t.getCause() != null )
    {
      return ChainedException.getRootException( t.getCause() );
    }
    else
    {
      return t;
    }
  }




  public Throwable getRootException()
  {
    return ChainedException.getRootException( this );
  }




  /**
   * Dump the exception and its message as a String with the root class, method
   * and line number.
   *
   * @return String suitable for logging.
   */
  public String toString()
  {
    return ChainedException.toString( this );
  }




  /**
   * Dump the exception and its message as a String with the root class, method
   * and line number.
   *
   * @return String suitable for logging.
   */
  public static String toString( final Throwable t )
  {
    final StringBuffer buffer = new StringBuffer();
    final Throwable root = ChainedException.getRootException( t );
    final StackTraceElement[] stack = root.getStackTrace();
    final StackTraceElement elem = stack[0];

    buffer.append( StringUtil.getLocalJavaName( t.getClass().getName() ) );
    buffer.append( ": '" );

    if( t.getMessage() == null )
    {
      buffer.append( "" );
    }
    else
    {
      buffer.append( t.getMessage() );
    }

    if( t.getCause() != null )
    {
      buffer.append( "' caused by " );
      buffer.append( StringUtil.getLocalJavaName( root.getClass().getName() ) );
      buffer.append( " exception thrown from " );
    }
    else
    {
      buffer.append( "' at " );
    }

    buffer.append( elem.getClassName() );
    buffer.append( "." );
    buffer.append( elem.getMethodName() );
    buffer.append( "(" );

    if( elem.getLineNumber() < 0 )
    {
      buffer.append( "Native Method" );
    }
    else
    {
      buffer.append( elem.getFileName() );
      buffer.append( ":" );
      buffer.append( elem.getLineNumber() );
    }

    buffer.append( ")" );

    return buffer.toString();
  }
}
