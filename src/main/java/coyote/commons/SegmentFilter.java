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
package coyote.commons;

import java.lang.reflect.Array;


/**
 * Class used to match and filter class names, JMS topics and such.
 * 
 * @author  Stephan D. Cote' - Enterprise Architecture
 */
public class SegmentFilter
{

  /**
   * Break the given topic into an array of segments for easy comparison.
   *
   * @param topic The string representing the topic name to break into segments.
   *
   * @return An array of Strings containing each segment of the topic, or null
   *         if the topic is not a valid RV topic name.
   */
  public static String[] getSegments( final String topic )
  {
    if( topic == null )
    {
      return null;
    }

    // Length check
    final int len = topic.length();

    if( ( len == 0 ) || ( len > 250 ) )
    {
      throw new IllegalArgumentException( "Pattern length of '" + topic.length() + "' is illegal (1-250)" );
    }

    int fromIndex = 0;

    String[] retval = new String[0];

    do
    {
      final int dotPosition = topic.indexOf( '.', fromIndex );

      // no dot was found, the entire string is the first segment
      if( dotPosition < 0 )
      {
        // if this is the first time through...
        if( fromIndex == 0 )
        {
          // ...no dot was found so the whole topic is the (first) segment
          final int length = Array.getLength( retval );
          final String[] newarray = new String[length + 1];
          System.arraycopy( retval, 0, newarray, 0, length );
          Array.set( newarray, length, topic );
          retval = newarray;
        }
        else
        {
          // ...the rest of the string is the (last) segment
          final int length = Array.getLength( retval );
          final String[] newarray = new String[length + 1];
          System.arraycopy( retval, 0, newarray, 0, length );
          Array.set( newarray, length, topic.substring( fromIndex ) );
          retval = newarray;
        }

        break;
      }

      // Segments cannot be longer than 128 characters
      if( dotPosition - fromIndex > 127 )
      {
        throw new IllegalArgumentException( "Segment at position '" + fromIndex + "' is > 128 characters" );
      }

      // get the segment
      final String segment = topic.substring( fromIndex, dotPosition );

      // Make sure that if the segment is larger than one character, that it
      // does not contain the '>' character like "my.topic.ste>" or "my.su*"
      if( ( segment.length() > 1 ) && ( ( segment.indexOf( '>' ) > 1 ) || ( segment.indexOf( '*' ) > 1 ) ) )
      {
        throw new IllegalArgumentException( "Malformed wildcard in segment at position '" + fromIndex + "'" );
      }

      final int length = Array.getLength( retval );
      final String[] newarray = new String[length + 1];
      System.arraycopy( retval, 0, newarray, 0, length );
      Array.set( newarray, length, segment );
      retval = newarray;

      if( ( segment.length() == 1 ) && ( segment.charAt( 0 ) == '>' ) )
      {
        // nothing comes after the '>' wildcard
      }

      // set the from index to the point just past the location of the last dot
      fromIndex = dotPosition + 1;
    }
    while( true );

    return retval;
  }

  /**
   * The segments of the filter topic in order of their occurance without the
   * dots.
   */
  private String[] segments = null;




  /**
   * Constructor SegmentFilter
   *
   * @param pattern The topic pattern, with optional wildcards, to be used to
   *          compare against other topic names.
   */
  public SegmentFilter( final String pattern )
  {
    segments = SegmentFilter.getSegments( pattern );

    if( segments == null )
    {
      throw new IllegalArgumentException( "Subject pattern is not legal" );
    }

  }




  /**
   * Return the segments of the topic filter.
   * @return  The array of Strings that represent the filter topic passed to the  constructor.
   */
  public String[] getSegments()
  {
    return segments;
  }




  /**
   * See if the filter matches the given topic name.
   *
   * @param topic the topic name to check
   *
   * @return true if the filter matches the topic, false otherwise. False will
   *         also be returned if the given topic is not a valid RV topic name.
   */
  public boolean matches( final String topic )
  {
    if( ( topic != null ) && ( segments != null ) )
    {
      return matches( SegmentFilter.getSegments( topic ) );
    }

    return false;
  }




  /**
   * See if the filter segments match the topic segments
   *
   * @param subSegments the topic segments to match.
   *
   * @return True if the segments match our segments false otherwise.
   */
  public boolean matches( final String[] subSegments )
  {
    // if there are fewer text segments than filter segments, then all the
    // segments of the filter could not possibly be satisfied
    if( subSegments.length < segments.length )
    {
      return false;
    }

    if( ( ( segments != null ) && ( subSegments != null ) ) && ( subSegments.length >= segments.length ) )
    {
      for( int i = 0; i < segments.length; i++ )
      {
        if( segments[i].equals( ">" ) )
        {
          return true; // EarlyExit wildcard
        }

        if( !segments[i].equals( "*" ) )
        {
          if( !segments[i].equals( subSegments[i] ) )
          {
            return false; // no match
          }
        }
      } // for each segment
      // PASSED!
    }
    else
    {
      return false; // no match
    }

    // Made it all the way through, all segments match
    return true;
  }




  /**
   * Return the original filter string given in the constructor.
   *
   * @return the original filter string given in the constructor.
   */
  public String toString()
  {
    if( segments.length > 0 )
    {
      if( segments.length > 1 )
      {
        final StringBuffer retval = new StringBuffer();

        for( int i = 0; i < segments.length; i++ )
        {
          retval.append( segments[i] );

          if( i + 1 < segments.length )
          {
            retval.append( '.' );
          }
        }

        return retval.toString();
      }
      else
      {
        return segments[0];
      }
    }

    return super.toString();
  }
}
