/*
 * Copyright (c) 2014 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial concept and initial implementation
 */
package coyote.commons;


/**
 * This is a null log appender - a log appender which does nothing.
 */
public class NullLogAppender implements LogAppender
{

  /**
   * @see coyote.commons.LogAppender#append(java.lang.String)
   */
  @Override
  public void append( String message )
  {
  }

  /**
   * @see coyote.commons.LogAppender#isEnabled()
   */
  @Override
  public boolean isEnabled()
  {
    return false;
  }

  /**
   * @see coyote.commons.LogAppender#setEnabled(boolean)
   */
  @Override
  public void setEnabled( boolean enabled )
  {
  }

 
}
