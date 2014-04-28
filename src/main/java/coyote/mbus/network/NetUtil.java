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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;

import coyote.util.ByteUtil;


/**
 * Class NetUtil
 */
public class NetUtil
{
  private static String[] dnsServerList = null;
  private static boolean dnsProbedFlag;

  private static InetAddress localAddress = null;
  private static String cachedLocalHostName = null;




  /**
   * Method addressToHex
   *
   * @param address
   *
   * @return TODO Complete Documentation
   */
  public static String addressToHex( final InetAddress address )
  {
    if( address != null )
    {
      return ByteUtil.bytesToHex( address.getAddress() );
    }

    return null;
  }




  /**
   * Method decodeAddress
   *
   * @param data
   *
   * @return TODO Complete Documentation
   */
  public static InetAddress decodeAddress( final byte[] data )
  {
    if( data.length < 4 )
    {
      throw new IllegalArgumentException( "data is too short" );
    }

    final String address = ( data[0] & 0xFF ) + "." + ( data[1] & 0xFF ) + "." + ( data[2] & 0xFF ) + "." + ( data[3] & 0xFF );

    try
    {
      return InetAddress.getByName( address );
    }
    catch( final UnknownHostException uhe )
    {
      throw new IllegalArgumentException( "data '" + address + "' is not a valid address" );
    }
  }




  /**
   * Determine the name servers on a Windows 95 box by calling 'winipcfg'
   */
  private static void find95()
  {
    try
    {
      final Process p = Runtime.getRuntime().exec( "winipcfg /all /batch winipcfg.out" );

      p.waitFor();

      final File f = new File( "winipcfg.out" );

      NetUtil.findWin( new FileInputStream( f ) );
      f.delete();
    }
    catch( final Exception _ex )
    {
      return;
    }
  }




  /**
   * Check to see if the system properties have a comma-separated list of DNS
   * server names for us to use.
   */
  private static void findDnsProperty()
  {
    Vector v = null;
    final String prop = System.getProperty( "dns.server" );

    if( prop != null )
    {
      String s;

      for( final StringTokenizer st = new StringTokenizer( prop, "," ); st.hasMoreTokens(); v.addElement( s ) )
      {
        s = st.nextToken();

        if( v == null )
        {
          v = new Vector();
        }
      }

      if( v != null )
      {
        NetUtil.dnsServerList = new String[v.size()];

        for( int i = 0; i < v.size(); i++ )
        {
          NetUtil.dnsServerList[i] = (String)v.elementAt( i );
        }
      }
    }
  }




  /**
   * Call the 'ipconfig" utility on Windows NT, XP and 200X operating systems.
   */
  private static void findNT()
  {
    try
    {
      final Process p = Runtime.getRuntime().exec( "ipconfig /all" );

      NetUtil.findWin( p.getInputStream() );
      p.destroy();
    }
    catch( final Exception _ex )
    {
      return;
    }
  }




  /**
   * Parse through the resolver configuration file on a Unix platform and locate
   * the name server entries.
   */
  private static void findUnix()
  {
    InputStream in = null;

    try
    {
      in = new FileInputStream( "/etc/resolv.conf" );
    }
    catch( final FileNotFoundException _ex )
    {
      return;
    }

    final InputStreamReader isr = new InputStreamReader( in );
    final BufferedReader br = new BufferedReader( isr );
    Vector vserver = null;

    try
    {
      String line;

      while( ( line = br.readLine() ) != null )
      {
        if( line.startsWith( "nameserver" ) )
        {
          if( vserver == null )
          {
            vserver = new Vector();
          }

          final StringTokenizer st = new StringTokenizer( line );

          st.nextToken();
          vserver.addElement( st.nextToken() );
        }
      }

      br.close();
    }
    catch( final IOException _ex )
    {
    }

    if( ( NetUtil.dnsServerList == null ) && ( vserver != null ) )
    {
      NetUtil.dnsServerList = new String[vserver.size()];

      for( int i = 0; i < vserver.size(); i++ )
      {
        NetUtil.dnsServerList[i] = (String)vserver.elementAt( i );
      }
    }
  }




  /**
   * Parse through the output of Windows IP configuration stats and locate the
   * DNS Server entries
   *
   * @param in
   */
  private static void findWin( final InputStream in )
  {
    final BufferedReader br = new BufferedReader( new InputStreamReader( in ) );

    try
    {
      Vector vserver = null;
      String line = null;
      boolean readingServers = false;

      while( ( line = br.readLine() ) != null )
      {
        // ignore empty lines
        if( line.length() == 0 )
        {
          continue;
        }

        final StringTokenizer st = new StringTokenizer( line );

        if( !st.hasMoreTokens() )
        {
          readingServers = false;
        }
        else
        {
          String s = st.nextToken();

          if( line.indexOf( ":" ) != -1 )
          {
            readingServers = false;
          }

          if( readingServers || ( line.indexOf( "DNS Servers" ) != -1 ) )
          {
            while( st.hasMoreTokens() )
            {
              s = st.nextToken();
            }

            if( !s.equals( ":" ) )
            {
              if( vserver == null )
              {
                vserver = new Vector();
              }

              vserver.addElement( s );

              readingServers = true;
            }
          }
        }
      }

      if( ( NetUtil.dnsServerList == null ) && ( vserver != null ) )
      {
        NetUtil.dnsServerList = new String[vserver.size()];

        for( int i = 0; i < vserver.size(); i++ )
        {
          NetUtil.dnsServerList[i] = (String)vserver.elementAt( i );
        }
      }
    }
    catch( final IOException _ex )
    {
    }
    finally
    {
      try
      {
        br.close();
      }
      catch( final IOException _ex )
      {
      }
    }
  }




  /**
   * Return a InetAddress that is suitable for use as a broadcast address.
   *
   * <p>Take a mask in the form of "255.255.111.0" and apply it to the given
   * address to calculate the broadcast address for the given subnet mask.</p>
   *
   * @param addr InetAddress representing a node in a subnet.
   * @param mask Valid dotted-quad netmask.
   *
   * @return an InetAddress capable of being used as a broadcast address in the
   *         given nodes subnet.
   */
  public static InetAddress getBroadcastAddress( final InetAddress addr, final String mask )
  {
    if( mask != null )
    {
      try
      {
        final IpNetwork network = new IpNetwork( addr.getHostAddress(), mask );
        final IpAddress adr = network.getBroadcastAddress();
        return InetAddress.getByName( adr.toString() );
      }
      catch( final Exception e )
      {
        // just return the address
      }
    }

    return addr;
  }




  /**
   * Return a InetAddress that is suitable for use as a broadcast address.
   *
   * <p>Take a mask in the form of "255.255.111.0" and apply it to the given
   * address to calculate the broadcast address for the given subnet mask.</p>
   *
   * @param addr InetAddress representing a node in a subnet.
   * @param mask Valid dotted-quad netmask.
   *
   * @return an InetAddress capable of being used as a broadcast address in the
   *         given nodes subnet.
   */
  public static InetAddress getBroadcastAddress( final String addr, final String mask )
  {
    InetAddress node = null;

    if( mask != null )
    {
      try
      {
        node = InetAddress.getByName( addr );

        final IpNetwork network = new IpNetwork( addr, mask );
        final IpAddress adr = network.getBroadcastAddress();
        return InetAddress.getByName( adr.toString() );
      }
      catch( final Exception ignore )
      {
        // just return the node address
        try
        {
          node = InetAddress.getByName( "255.255.255.255" );
        }
        catch( final Exception e )
        {
          // should always work
        }
      }
    }

    return node;
  }




  /**
   * Return a InetAddress that is suitable for use as a broadcast address.
   *
   * <p>Take a mask in the form of "255.255.111.0" and apply it to the given
   * address to calculate the broadcast address for the given subnet mask.</p>
   *
   * @param addr InetAddress representing a node in a subnet.
   * @param mask Valid dotted-quad netmask.
   *
   * @return an InetAddress capable of being used as a broadcast address in the
   *         given nodes subnet.
   */
  public static String getBroadcastAddressString( final String addr, final String mask )
  {
    if( mask != null )
    {
      try
      {
        // final InetAddress node = InetAddress.getByName( addr );
        final IpNetwork network = new IpNetwork( addr, mask );
        return network.getBroadcastAddress().toString();
      }
      catch( final Exception ignore )
      {
        // just return the node address
      }
    }

    return "255.255.255.255";
  }




  /**
   * @return the primary DNS server name for this platform, or null if no server
   *         could be found.
   */
  public static String getDnsServer()
  {
    NetUtil.probeDnsConfig();

    final String[] array = NetUtil.dnsServerList;

    if( array == null )
    {
      return null;
    }
    else
    {
      return array[0];
    }
  }




  /**
   * @return an array of strings containing the names of DNS servers discovered
   *         for this platform
   */
  public static String[] getDnsServers()
  {
    NetUtil.probeDnsConfig();

    return NetUtil.dnsServerList;
  }




  /**
   * Method getDomain
   *
   * @param addr
   *
   * @return TODO Complete Documentation
   */
  public static String getDomain( final InetAddress addr )
  {
    if( addr != null )
    {
      final String hostname = NetUtil.getQualifiedHostName( addr );

      if( hostname != null )
      {
        final int indx = hostname.indexOf( '.' );

        if( indx > 0 )
        {
          final String retval = hostname.substring( indx + 1 );

          if( retval.indexOf( '.' ) > 0 )
          {
            return retval;
          }
          else
          {
            return hostname;
          }
        }
      }
    }

    return null;
  }




  /**
   * Get the address of the host.
   *
   * <p><b>NOTE</b>: This may take a long time as it will perform a DNS lookup
   * which can take several seconds!</p>
   *
   *
   * @param uri
   *
   * @return The InetAddress of the host specified in the URI. Will return null
   *         if DNS lookup fails, if the URI reference is null or if no host is
   *         specified in the URI.
   */
  public static InetAddress getHostAddress( final URI uri )
  {
    if( uri != null )
    {
      final String host = uri.getHost();

      if( host != null )
      {
        try
        {
          return InetAddress.getByName( host );
        }
        catch( final Exception exception )
        {
        }
      }
    }

    return null;
  }




  /**
   * Get the IP Address by which the rest of the world knows us. <p>This is useful in helping insure that we don't accidently start binding to or otherwise using the local loopback address.</p> <p>This requires some type of IP address resolver to be installed, like DNS, NIS or at least hostname lookup.</p>
   * @return  The InetAddress representing the host on the network and NOT the  loopback address.
   */
  public static InetAddress getLocalAddress()
  {
    // If we already looked this up, use the cached result to save time
    if( NetUtil.localAddress != null )
    {
      return NetUtil.localAddress;
    }

    // No cached result, figure it out and cache it for later
    InetAddress addr = null;

    // Make sure we get the IP Address by which the rest of the world knows us
    // or at least, our host's default network interface
    try
    {
      // This helps insure that we do not get localhost (127.0.0.1)
      addr = InetAddress.getByName( InetAddress.getLocalHost().getHostName() );
    }
    catch( final UnknownHostException e )
    {
      // Aaaaww Phooey! DNS is not working or we are not in it.
      addr = null;
    }

    // If it looks like a unique address, return it, otherwise try again
    if( ( addr != null ) && !addr.getHostAddress().equals( "127.0.0.1" ) && !addr.getHostAddress().equals( "0.0.0.0" ) )
    {
      NetUtil.localAddress = addr;

      return addr;
    }

    // Try it the way it's supposed to work
    try
    {
      addr = InetAddress.getLocalHost();
    }
    catch( final Exception ex )
    {
      addr = null;
    }

    NetUtil.localAddress = addr;

    return addr;
  }




  /**
   * Get the IP Address by which the rest of the world knows us as a string.
   *
   * <p>This is useful in helping insure that we don't accidently start binding
   * to or otherwise using the local loopback address.</p>
   *
   * <p>This requires some type of IP address resolver to be installed, like
   * DNS, NIS or at least hostname lookup.</p>
   *
   * @return The InetAddress representing the host on the network and NOT the
   *         loopback address.
   */
  public static String getLocalAddressString()
  {
    try
    {
      return NetUtil.getLocalAddress().getHostAddress();
    }
    catch( final RuntimeException e )
    {
    }

    return null;
  }




  /**
   * Return a InetAddress that is suitable for use as a broadcast address.
   *
   * <p>Take a mask in the form of "255.255.111.0" and apply it to the local
   * address to calculate the broadcast address for the given subnet mask.</p>
   *
   * @param mask Valid dotted-quad netmask.
   *
   * @return an InetAddress capable of being used as a broadcast address
   */
  public static InetAddress getLocalBroadcast( final String mask )
  {
    final InetAddress retval = NetUtil.getLocalAddress();

    if( retval != null )
    {
      return NetUtil.getBroadcastAddress( retval, mask );
    }

    return retval;
  }




  /**
   * Return a InetAddress that is suitable for use as a broadcast address.
   *
   * <p>Take a mask in the form of "255.255.111.0" and apply it to the local
   * address to calculate the broadcast address for the given subnet mask.</p>
   *
   * @param mask Valid dotted-quad netmask.
   *
   * @return TODO Complete Documentation
   */
  public static String getLocalBroadcastString( final String mask )
  {
    final String retval = "255.255.255.255";

    if( mask != null )
    {
      try
      {
        return NetUtil.getLocalBroadcast( mask ).getHostAddress();
      }
      catch( final Exception ignore )
      {
        // just return the address
      }
    }

    return retval;
  }




  /**
   * Method getLocalDomain
   *
   * @return TODO Complete Documentation
   */
  public static String getLocalDomain()
  {
    return NetUtil.getDomain( NetUtil.getLocalAddress() );
  }




  /**
   * @return the FQDN of the local host or null if the lookup failed for any
   *         reason.
   */
  public static String getLocalQualifiedHostName()
  {
    // Use the cached version of the hostname to save DNS lookups
    if( NetUtil.cachedLocalHostName != null )
    {
      return NetUtil.cachedLocalHostName;
    }

    NetUtil.cachedLocalHostName = NetUtil.getQualifiedHostName( NetUtil.getLocalAddress() );

    return NetUtil.cachedLocalHostName;
  }




  /**
   * Method getLocalRelativeHostName
   *
   * @return TODO Complete Documentation
   */
  public static String getLocalRelativeHostName()
  {
    return NetUtil.getRelativeHostName( NetUtil.getLocalAddress() );
  }




  /**
   * Return a port number that can be used to create a socket on the given
   * address starting with port 1.
   *
   * @param address
   *
   * @return TODO Complete Documentation
   */
  public static int getNextAvailablePort( final InetAddress address )
  {
    return NetUtil.getNextAvailablePort( address, 1 );
  }




  /**
   * Return a port number that can be used to create a socket on the given
   * address starting with the given port.
   *
   * <p>If the given port can be used to create a server socket (TCP) then that
   * port number will be used, otherwise, the port number will be incremented
   * and tested until a free port is found.</p>
   *
   * <p>This is not thread-safe nor fool-proof. A valid value can be returned,
   * yet when a call is made to open a socket at that port, another thread may
   * have already opened a socket on that port. A better way would be to use
   * the <code>getNextServerSocket(address,port)</code> method if it desired to
   * obtain the next available server.</p>
   *
   * @param address
   * @param port
   *
   * @return TODO Complete Documentation
   */
  public static int getNextAvailablePort( final InetAddress address, final int port )
  {
    final ServerSocket socket = NetUtil.getNextServerSocket( address, port, 0 );
    int retval = -1;

    if( socket != null )
    {
      // Get the port as a return value
      retval = socket.getLocalPort();

      // Close the un-needed socket
      try
      {
        socket.close();
      }
      catch( final IOException e )
      {
        // Ignore it
      }
    }

    return retval;
  }




  // ///////////////////////////////////////////////////////////////////////////
  // / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / /
  //
  // -- New methods
  //
  // / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / /
  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Return a port number that can be used to create a socket with the given
   * port on the local address.
   *
   * @param port
   *
   * @return TODO Complete Documentation
   */
  public static int getNextAvailablePort( final int port )
  {
    return NetUtil.getNextAvailablePort( null, port );
  }




  /**
   * Return a port number that can be used to create a datagram socket on the
   * given address starting with port 1.
   *
   * @param address
   *
   * @return TODO Complete Documentation
   */
  public static int getNextAvailableUdpPort( final InetAddress address )
  {
    return NetUtil.getNextAvailableUdpPort( address, 1 );
  }




  /**
   * Method getNextAvailableUdpPort
   *
   * @param address
   * @param port
   *
   * @return TODO Complete Documentation
   */
  public static int getNextAvailableUdpPort( final InetAddress address, final int port )
  {
    final DatagramSocket dgramsocket = NetUtil.getNextDatagramSocket( address, port, false );

    int retval = -1;

    if( dgramsocket != null )
    {
      // Get the port as a return value
      retval = dgramsocket.getLocalPort();

      // Close the un-needed socket
      dgramsocket.close();
    }

    return retval;
  }




  /**
   * Return a port number that can be used to create a datagram socket on the
   * given address starting with the given port.
   *
   * <p>If the given port can be used to create a datagram socket (UDP) then
   * that port number will be used, otherwise, the port number will be
   * incremented and tested until a free port is found.</p>
   *
   * <p>This is not thread-safe nor fool-proof. A valid value can be returned,
   * yet when a call is made to open a socket at that port, another thread may
   * have already opened a socket on that port. A better way would be to use
   * the <code>getNextDatagramSocket(address,port)</code> method if it desired
   * to obtain the next available datagram server.</p>
   *
   * @param address
   * @param port
   * @param reuse allow reuse of a socket via the SO_REUSEADDR socket option
   *
   * @return TODO Complete Documentation
   */
  public static int getNextAvailableUdpPort( final InetAddress address, final int port, final boolean reuse )
  {
    final DatagramSocket dgramsocket = NetUtil.getNextDatagramSocket( address, port, reuse );

    int retval = -1;

    if( dgramsocket != null )
    {
      // Get the port as a return value
      retval = dgramsocket.getLocalPort();

      // Close the un-needed socket
      dgramsocket.close();
    }

    return retval;
  }




  /**
   * Return a port number that can be used to create a datagram socket with the
   * given port on the local address.
   *
   * @param port
   *
   * @return TODO Complete Documentation
   */
  public static int getNextAvailableUdpPort( final int port )
  {
    return NetUtil.getNextAvailableUdpPort( null, port );
  }




  /**
   * Return a UDP server socket on the given address and port, incrementing the
   * port until a server socket can be opened.
   *
   * @param address
   * @param port
   * @param reuse
   *
   * @return TODO Complete Documentation
   */
  public static DatagramSocket getNextDatagramSocket( InetAddress address, final int port, final boolean reuse )
  {
    int i = port;
    DatagramSocket dgramsocket = null;

    // If no address was given, then try to determine our local address so we
    // can use our main address instead of 127.0.0.1 which may be chosen by the
    // VM if it is not specified in the DatagramSocket constructor
    if( address == null )
    {
      address = NetUtil.getLocalAddress();
    }

    while( NetUtil.validatePort( i ) != 0 )
    {
      try
      {
        if( address == null )
        {
          dgramsocket = new DatagramSocket( i );

          dgramsocket.setReuseAddress( reuse );
        }
        else
        {
          dgramsocket = new DatagramSocket( i, address );

          dgramsocket.setReuseAddress( reuse );
        }

        if( dgramsocket != null )
        {
          return dgramsocket;
        }

      }
      catch( final IOException e )
      {
        i++;
      }
    }

    return null;
  }




  /**
   * Return a TCP server socket on the given address and port, incrementing the
   * port until a server socket can be opened.
   *
   * @param address
   * @param port
   * @param backlog
   *
   * @return TODO Complete Documentation
   */
  public static ServerSocket getNextServerSocket( InetAddress address, final int port, final int backlog )
  {
    int i = port;
    ServerSocket socket = null;

    // If no address was given, then try to determine our local address so we
    // can use our main address instead of 127.0.0.1 which may be chosen by the
    // VM if it is not specified in the ServerSocket constructor
    if( address == null )
    {
      address = NetUtil.getLocalAddress();
    }

    while( NetUtil.validatePort( i ) != 0 )
    {
      try
      {
        if( address == null )
        {
          socket = new ServerSocket( i, backlog );
        }
        else
        {
          socket = new ServerSocket( i, backlog, address );
        }

        if( socket != null )
        {
          return socket;
        }

      }
      catch( final IOException e )
      {
        i++;
      }
    }

    return null;
  }




  /**
   * Use the underlying getCanonicalHostName as used in Java 1.4, but return
   * null if the value is the numerical address (dotted-quad) representation of
   * the address.
   *
   * @param addr The IP address to lookup.
   *
   * @return The Canonical Host Name; null if the FQDN could not be determined
   *         or if the return value was the dotted-quad representation of the
   *         host address.
   */
  public static String getQualifiedHostName( final InetAddress addr )
  {
    String name = null;

    try
    {
      name = addr.getCanonicalHostName();

      if( name != null )
      {
        // Check for a return value of and address instead of a name
        if( Character.isDigit( name.charAt( 0 ) ) )
        {
          // Looks like an address, return null;
          return null;
        }

        // normalize the case
        name = name.toLowerCase();
      }
    }
    catch( final Exception ex )
    {
    }

    return name;
  }




  /**
   * Attempted to return the FQDN of the given string representation of the IP
   * address.
   *
   * @param addr String representing a valid IP address.
   *
   * @return the FQDN of the addresses represented by the given string, or null
   *         if the FQDN could not be determined.
   */
  public static String getQualifiedHostName( final String addr )
  {
    try
    {
      final InetAddress address = InetAddress.getByName( addr );

      if( address != null )
      {
        return NetUtil.getQualifiedHostName( address );
      }
    }
    catch( final Exception ex )
    {
    }

    return null;
  }




  /**
   * Return the hostname of the given IP address in lowercase.
   *
   * @param addr
   *
   * @return TODO Complete Documentation
   */
  public static String getRelativeHostName( final InetAddress addr )
  {
    return addr.getHostName().toLowerCase();
  }




  /**
   * Method getRelativeHostName
   *
   * @param addr
   *
   * @return TODO Complete Documentation
   */
  public static String getRelativeHostName( final String addr )
  {
    try
    {
      final InetAddress address = InetAddress.getByName( addr );

      if( address != null )
      {
        return NetUtil.getRelativeHostName( address );
      }
    }
    catch( final Exception ex )
    {
    }

    return null;
  }




  /**
   * Method hexToAddress
   *
   * @param hex
   *
   * @return TODO Complete Documentation
   */
  public static InetAddress hexToAddress( final String hex )
  {
    if( ( hex != null ) && ( hex.length() > 7 ) )
    {
      try
      {
        // This will take longer as a DNS lookup will be performed as part of
        // the InetAddress.getByName method.
        return InetAddress.getByName( NetUtil.decodeAddress( ByteUtil.hexToBytes( hex.substring( 0, 8 ) ) ).getHostName() );
      }
      catch( final Exception ex )
      {
      }
    }

    return null;
  }




  /**
   * Method hexToAddressString
   *
   * @param hex
   *
   * @return TODO Complete Documentation
   */
  public static String hexToAddressString( final String hex )
  {
    if( ( hex != null ) && ( hex.length() > 7 ) )
    {
      return new String( Integer.parseInt( hex.substring( 0, 2 ), 16 ) + "." + Integer.parseInt( hex.substring( 2, 4 ), 16 ) + "." + Integer.parseInt( hex.substring( 4, 6 ), 16 ) + "." + Integer.parseInt( hex.substring( 6, 8 ), 16 ) );
    }

    return null;
  }




  /**
   * Method hostPortToHex
   *
   * @param host
   * @param port
   *
   * @return TODO Complete Documentation
   */
  public static String hostPortToHex( final InetAddress host, final int port )
  {
    if( host != null )
    {
      if( ( port > -1 ) && ( port < 65536 ) )
      {
        return new String( ByteUtil.bytesToHex( host.getAddress() ) + ByteUtil.bytesToHex( ByteUtil.renderUnsignedShort( port ) ) );
      }
      else
      {
        return new String( ByteUtil.bytesToHex( host.getAddress() ) + ByteUtil.bytesToHex( ByteUtil.renderUnsignedShort( 0 ) ) );
      }
    }

    return null;
  }




  /**
   * Method hostPortToHex
   *
   * @param port
   *
   * @return TODO Complete Documentation
   */
  public static String hostPortToHex( final int port )
  {
    return NetUtil.hostPortToHex( NetUtil.getLocalAddress(), port );
  }




  /**
   * Method hostPortToHex
   *
   * @param uri
   *
   * @return TODO Complete Documentation
   */
  public static String hostPortToHex( final URI uri )
  {
    if( uri != null )
    {
      return NetUtil.hostPortToHex( NetUtil.getHostAddress( uri ), uri.getPort() );
    }

    return null;
  }




  /**
   * Method hostToHex
   *
   * @param host
   *
   * @return TODO Complete Documentation
   */
  public static String hostToHex( final InetAddress host )
  {
    if( host != null )
    {
      return new String( ByteUtil.bytesToHex( host.getAddress() ) );
    }

    return null;
  }




  /**
   * Determines if the given string represents an integer between 0 and 65535
   * inclusive.
   *
   * @param port The string representing a number
   *
   * @return True if the string represents a valid port number, false otherwise
   */
  public static boolean isValidPort( final String port )
  {
    return ( NetUtil.validatePort( port ) != 0 );
  }




  /**
   * Probe the platform configuration for DNS server data.
   */
  private static synchronized void probeDnsConfig()
  {
    if( NetUtil.dnsProbedFlag )
    {
      return;
    }

    NetUtil.dnsProbedFlag = true;

    NetUtil.findDnsProperty();

    if( NetUtil.dnsServerList == null )
    {
      final String OS = System.getProperty( "os.name" );

      if( OS.indexOf( "Windows" ) != -1 )
      {
        if( ( OS.indexOf( "NT" ) != -1 ) || ( OS.indexOf( "200" ) != -1 ) || ( OS.indexOf( "XP" ) != -1 ) )
        {
          NetUtil.findNT();
        }
        else
        {
          NetUtil.find95();
        }
      }
      else
      {
        NetUtil.findUnix();
      }
    }
  }




  /**
   *
   * @param address
   *
   * @return TODO Complete Documentation
   */
  public static InetAddress resolveAddress( final String address )
  {
    try
    {
      return InetAddress.getByName( address );
    }
    catch( final UnknownHostException e )
    {
      // System.err.println( "NetUtil.resolveAddress(String) Could not resolve \"" + address + "\":\n" );
    }

    return null;
  }




  /**
   * Set the cached Fully-qualified hostname to avoid additional DNS lookups.
   * @param name  the name to use as the local host name.
   */
  public static void setCachedLocalHostName( final String name )
  {
    NetUtil.cachedLocalHostName = name;
  }




  /**
   * Checks the address as being capable of being used as a ServerSocket by
   * binding a socket to that port.
   *
   * @param address String representation of an IP address.
   *
   * @return The InetAddress if it can be used for a ServerSocket, null
   *         otherwise.
   */
  public static InetAddress validateBindAddress( final InetAddress address )
  {
    try
    {
      final ServerSocket socket = new ServerSocket( 0, 0, address );
      socket.close();

      return address;
    }
    catch( final IOException e )
    {
    }

    return null;
  }




  /**
   * Checks the string address as representing an InetAddress capable of being
   * used as a ServerSocket by binding a socket and returning the InetAddress it
   * represents.
   *
   * @param address String representation of an IP address.
   *
   * @return The InetAddress of the string if it can be used for a ServerSocket,
   *         null otherwise.
   */
  public static InetAddress validateBindAddress( final String address )
  {
    final InetAddress temp = NetUtil.resolveAddress( address );
    return NetUtil.validateBindAddress( temp );
  }




  /**
   *
   * @param port
   *
   * @return TODO Complete Documentation
   */
  public static int validatePort( final int port )
  {
    if( ( port < 0 ) || ( port > 0xFFFF ) )
    {
      return 0;
    }
    else
    {
      return port;
    }
  }




  /**
   *
   * @param port
   *
   * @return TODO Complete Documentation
   */
  public static int validatePort( final String port )
  {
    try
    {
      final int temp = Integer.parseInt( port );
      return NetUtil.validatePort( temp );
    }
    catch( final NumberFormatException nfe )
    {
      // System.err.println( "NetUtil.validatePort(String) Could not convert \"" + port + "\" into an integer:\n" );
    }

    return 0;
  }




  /**
   * Just test to see if the given string can be parse into a URI.
   *
   * @param uri
   *
   * @return TODO Complete Documentation
   */
  public static URI validateURI( final String uri )
  {
    try
    {
      // We could try to open it, but even if it succeeds now, it may fail later
      // or if it fails right now, it may succeede later. So what is the use?
      return new URI( uri );
    }
    catch( final Exception mfue )
    {
      // System.err.println( "NetUtil.validateURI(String) The URI \"" + uri + "\" is not valid:\n" );
    }

    return null;
  }




  /**
   * Private constructor because everything is static
   */
  private NetUtil()
  {
  }

}
