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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import coyote.util.ByteUtil;


/**
 * Simple Class to do some IP Address manipulation, including applying a subnet
 * mask to an address. 
 * 
 * <p>Sample Code 
 * <PRE>
 *  import net.bplg.network.IpAddress;
 *   ... 
 *   boolean bResult=false; 
 *   try { 
 *     ip=new IpAddress(cUserIPAddress); 
 *     ip.applyNetMask(oUser.NetMask); 
 *     bResult=ip.equals(oUser.AllowedNetwork);
 *   } catch (IpAddressException ipae) 
 *   { } 
 *   </PRE> </p>
 */
public class IpAddress implements Serializable
{
  private static final long serialVersionUID = 2863167649234642107L;

  protected final static int IP4_OCTETS = 4;
  protected final static int IP6_OCTETS = 16;

  public static final String IP4_LOOPBACK = "127.0.0.1";
  public static final String IP6_LOOPBACK = "0:0:0:0:0:0:0:1";

  protected short[] octets = new short[IpAddress.IP4_OCTETS];

  private InetAddress netAddress = null;
  private String dnsName = null;




  /**
   * Construct a new blank IP Address (0.0.0.0)
   */
  public IpAddress()
  {
  }




  /**
   * Construct a new IP Address object from an address string.
   *
   * <p>A common format for an IP Address is: 192.168.117.32, but this
   * constructor will allow truncated addresses in the form of 192.168 which
   * can be used to specify a network, as the remaining octets of the address
   * will be populated with zeros.</p>
   *
   * @param addr the string representation of the "dotted-quad" address
   *
   * @throws IpAddressException if there is a problem parsing the address string
   */
  public IpAddress( final String addr ) throws IpAddressException
  {
    if( addr == null )
    {
      throw new IpAddressException( "Null argument to constructor" );
    }

    octets = IpAddress.getOctets( addr.trim() );
  }




  /**
   * Construct a new IP Address object from an InetAddress object.
   *
   * @param addr the InetAddress to convert
   *
   * @throws IpAddressException if the InetAddress reference is null
   */
  public IpAddress( final InetAddress addr ) throws IpAddressException
  {
    if( addr != null )
    {
      final byte[] bytes = addr.getAddress();
      octets = new short[bytes.length];
      for( int i = 0; i < bytes.length; octets[i] = ByteUtil.fixByte( bytes[i++] ) )
      {
        ;
      }
    }
    else
    {
      throw new IpAddressException( "address reference was null" );
    }
  }




  public IpAddress( final byte[] data ) throws IpAddressException
  {
    if( data == null )
    {
      throw new IpAddressException( "Null argument in constructor" );
    }

    if( data.length == 0 )
    {
      throw new IpAddressException( "No data in constructor array argument" );
    }

    if( ( data.length % 2 ) != 0 )
    {
      throw new IpAddressException( "Odd number of bytes in constructor array argument" );
    }

    octets = new short[data.length];
    for( int i = 0; i < data.length; octets[i] = ByteUtil.fixByte( data[i++] ) )
    {
      ;
    }
  }




  /**
   * Copy an existing IP Address into a new object.
   *
   * @param master the IpAddress to use to create this object
   */
  public IpAddress( final IpAddress master )
  {
    final short[] aMaster = master.getOctets();
    octets = new short[aMaster.length];

    for( int i = 0; i < aMaster.length; i++ )
    {
      octets[i] = aMaster[i];
    }
  }




  /**
   * Clone this object.
   */
  public Object clone()
  {
    return new IpAddress( this );
  }




  /**
   * Constructor IpAddress
   *
   * @param octets
   */
  protected IpAddress( final short[] octets )
  {
    this.octets = octets;
  }




  /**
   * Method equals
   *
   * @param Octets
   *
   * @return TODO Complete Documentation
   */
  public boolean equals( final String Octets )
  {
    return this.toString().equals( Octets );
  }




  /**
   * Method equals
   *
   * @param addr
   *
   * @return TODO Complete Documentation
   */
  public boolean equals( final IpAddress addr )
  {
    if( addr == null )
    {
      return false;
    }

    final short[] addrOctets = addr.getOctets();
    boolean retval = ( addrOctets.length == octets.length );

    // If the lengths match
    if( retval )
    {
      // ...perform a byte-by-byte check
      for( int i = 0; retval && ( i < octets.length ); i++ )
      {
        retval &= ( octets[i] == addrOctets[i] );
      }
    }

    return retval;
  }




  /**
   * Return the IP Address as an array of Shorts.
   * @return TODO Complete Documentation
   */
  public short[] getOctets()
  {
    return octets;
  }




  /**
   * Return the dotted notation of the IP Address as a string.
   *
   * @return TODO Complete Documentation
   */
  public String toString()
  {
    final StringBuffer sb = new StringBuffer();

    for( int i = 0; i < octets.length; i++ )
    {
      sb.append( octets[i] );

      if( i < ( octets.length - 1 ) )
      {
        sb.append( '.' );
      }
    }

    return sb.toString();
  }




  /**
   * @return the address reversed. IP 1.2.3.4 is returned as 4.3.2.1 as in a DNS
   *         record entry.
   */
  public String toNSFormat()
  {
    final StringBuffer sb = new StringBuffer();

    for( int i = ( octets.length - 1 ); i > -1; i-- )
    {
      sb.append( octets[i] );

      if( i > ( 0 ) )
      {
        sb.append( '.' );
      }
    }

    return sb.toString();
  }




  public InetAddress toInetAddress()
  {
    if( netAddress == null )
    {
      netAddress = IpAddress.toInetAddress( this );
    }
    return netAddress;
  }




  public static InetAddress toInetAddress( final IpAddress addr )
  {
    if( addr != null )
    {
      try
      {
        return InetAddress.getByAddress( addr.getBytes() );
      }
      catch( final UnknownHostException e )
      {
      }
    }

    return null;
  }




  public byte[] getBytes()
  {
    final byte[] bytes = new byte[octets.length];

    for( int i = 0; i < octets.length; i++ )
    {
      bytes[i] = (byte)octets[i];
    }
    return bytes;
  }




  /**
   * Method dumpOctets
   *
   * @param bits
   *
   * @return TODO Complete Documentation
   */
  public static String dumpOctets( final short[] bits )
  {
    final byte[] bytes = new byte[bits.length];

    for( int i = 0; i < bits.length; i++ )
    {
      bytes[i] = (byte)bits[i];
    }

    return ByteUtil.dump( bytes );
  }




  /**
   * Method dump
   *
   * @return TODO Complete Documentation
   */
  public String dump()
  {
    return IpAddress.dumpOctets( getOctets() );
  }




  /**
   * Helper method to convert a string representation into a short[] array.
   *
   * @param addr
   *
   * @return TODO Complete Documentation
   *
   * @throws IpAddressException
   */
  public static short[] getOctets( final String addr ) throws IpAddressException
  {
    int octetCount = 0;
    int digit = 0;
    short[] temp = new short[IpAddress.IP4_OCTETS];

    if( addr.length() > 15 )
    {
      temp = new short[IpAddress.IP6_OCTETS];
    }

    // create a character array for parsing
    final char[] chars = addr.toCharArray();

    // create a StringBuffer to hold the character representation of the digits
    final StringBuffer buf = new StringBuffer();

    // goofy for loop to make sure we parse everything, delimiting on both the
    // '.' character and the end of the array
    for( int i = 0; i <= chars.length; i++ )
    {
      if( ( i == chars.length ) || ( chars[i] == '.' ) )
      {
        try
        {
          digit = Integer.parseInt( buf.toString() );
        }
        catch( final NumberFormatException nfe )
        {
          throw new IpAddressException( "Segment '" + buf.toString() + "' is not a number at position (" + i + ")" );
        }

        if( ( digit < 0 ) || ( digit > 255 ) )
        {
          throw new IpAddressException( "Segment '" + digit + "' out of range" );
        }

        temp[octetCount] = (short)digit;

        buf.setLength( 0 );

        octetCount++;

        // avoid over-runs
        if( i == chars.length )
        {
          break;
        }
      }
      else
      {
        buf.append( chars[i] );
      }

    }

    // If we did not parse anything...
    if( octetCount == 0 )
    {
      throw new IpAddressException( "Invalid address" );
    }
    else if( octetCount < temp.length )
    {
      // fillup the rest of the array
      for( int i = octetCount; i < temp.length; i++ )
      {
        temp[i] = 0;
      }
    }

    return temp;
  }




  /**
   * Apply a netmask to an address, leaving only the network portion of the
   * address.
   *
   * <p>For example: 192.168.100.195 netmask 255.255.255.224 returns
   * 192.168.100.192</p>
   *
   * @param mask
   *
   * @return TODO Complete Documentation
   *
   * @throws IpAddressException
   */
  public IpAddress applyNetMask( final String mask ) throws IpAddressException
  {
    return applyNetMask( new IpAddress( mask ) );
  }




  /**
   * Apply a netmask to an address, leaving only the network portion of the
   * address.
   *
   * <p><CODE>
   * IpAddress addr = new IpAddress("192.168.0.23");
   * addr.applyNetMask("255.255.255.0");
   * System.out.println(addr.toString());
   * // returns "192.168.0.0"
   * </CODE></p>
   *
   * @param netmask
   *
   * @return TODO Complete Documentation
   */
  public IpAddress applyNetMask( final IpAddress netmask )
  {
    short[] retval;
    retval = netmask.getOctets();

    for( int i = 0; i < octets.length; i++ )
    {
      octets[i] &= retval[i];
    }

    return this;
  }




  /**
   * Get the DNS name of this address. <p>The name will be calculated the first time this method is called and  its value will be cached for all future accesses.<p> <p>It is important to keep in mind that the return value will depend upon  this host name resolver. If this IP address is located in a local host  resolver file, then the return value will be whatever is specified in that  file and a DNS lookup may never take place.</p>
   * @return  The name to which this address resolves on this hosts resolver, or   null if the address is not valid or in DNS or the hosts resolver.
   */
  public String getDnsName()
  {
    if( dnsName == null )
    {
      final InetAddress ina = toInetAddress();
      if( ina != null )
      {
        dnsName = ina.getHostName().toLowerCase();
      }
    }
    return dnsName;
  }




  /**
   * @return Just the domain portion of this hosts name.
   */
  public String getDomain()
  {
    String hostname = getDnsName();

    if( hostname != null )
    {
      hostname = hostname.toLowerCase();

      final int indx = hostname.indexOf( '.' );

      if( indx > 0 )
      {
        final String retval = hostname.substring( indx + 1 );

        // we should have a DOMAIN.COM type string
        if( retval.indexOf( '.' ) > 0 )
        {
          return retval;
        }
        else
        {
          // No domain...how odd is that?
          return hostname;
        }

      } // if there are segments

    } // hostname !null

    return null;
  }




  /**
   * @return The hostname of this address without the domain portion.
   */
  public String getRelativeHostname()
  {
    String retval = null;

    final String hostname = getDnsName();

    if( hostname != null )
    {
      final int pt = hostname.indexOf( '.' );
      if( pt > 0 )
      {
        retval = hostname.substring( 0, pt );

        if( retval.length() < 3 )
        {
          try
          {
            // Uh-Oh, this may be a number, if it is return the whole name
            Integer.parseInt( retval );
            return hostname;
          }
          catch( final NumberFormatException e )
          {
          }

        } // len<3

      } // contains a '.'
      else
      {
        retval = hostname;
      }

    } //hostname !null

    return retval;
  }

}
