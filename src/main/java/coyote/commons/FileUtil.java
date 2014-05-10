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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * This is group of static functions to work with disk files.
 *
 * @author Stephan D. Cote' - Enterprise Architecture
 * @version $Revision$
 */
public final class FileUtil
{

  /** The path separator for the current platform. Defaults to ':' */
  public static final String PATH_SEPARATOR = System.getProperty( "path.separator", ":" );

  /** The file separator for the current platform. Defaults to '/' */
  public static final String FILE_SEPARATOR = System.getProperty( "file.separator", "/" );

  /** The absolute path to the home directory of the user running the VM. */
  public static final String HOME = System.getProperty( "user.home" );

  /** The home directory of the user running the VM. */
  public static final File HOME_DIR = new File( FileUtil.HOME );

  /** The file URI to the home directory of the user running the VM. */
  public static final URI HOME_DIR_URI = FileUtil.getFileURI( FileUtil.HOME_DIR );

  /** The absolute path to the current working directory of the VM. */
  public static final String CURRENT = System.getProperty( "user.dir" );

  /** The current working directory of the VM */
  public static final File CURRENT_DIR = new File( FileUtil.CURRENT );

  /** The file URI to the current working directory of the VM */
  public static final URI CURRENT_DIR_URI = FileUtil.getFileURI( FileUtil.CURRENT_DIR );

  /** Represents 1 Kilo Byte ( 1024 ). */
  public final static long ONE_KB = 1024L;

  /** Represents 1 Mega Byte ( 1024^2 ). */
  public final static long ONE_MB = FileUtil.ONE_KB * 1024L;

  /** Represents 1 Giga Byte ( 1024^3 ). */
  public final static long ONE_GB = FileUtil.ONE_MB * 1024L;

  /** Represents 1 Tera Byte ( 1024^4 ). */
  public final static long ONE_TB = FileUtil.ONE_GB * 1024L;

  /** Standard number formatter. */
  public static NumberFormat FILE_LENGTH_FORMAT = NumberFormat.getInstance();

  /** Standard byte formatter. */
  public static DecimalFormat byteFormat = new DecimalFormat( "0.00" );




  /**
   * Private constructor because everything is static
   */
  private FileUtil()
  {
  }




  /**
   * Tests the given string to make sure it is a fully-qualified file name to a
   * file which exists and can be read.
   *
   * @param filename
   *
   * @return TODO Complete Documentation
   */
  public static File validateFileName( final String filename )
  {
    if( filename != null )
    {
      final File tempfile = new File( filename );

      if( tempfile.isAbsolute() && tempfile.exists() && tempfile.canRead() )
      {
        return tempfile;
      }
      else
      {
        return null;
      }
    }

    return null;
  }




  /**
   * Checks to see if the given directory name exists and is a readable
   * directory.
   *
   * @param directory
   *
   * @return TODO Complete Documentation
   */
  public static File validateDirectory( final String directory )
  {
    if( directory != null )
    {
      final File tempfile = new File( directory );

      if( tempfile.exists() && tempfile.isDirectory() && tempfile.canRead() )
      {
        return tempfile;
      }
      else
      {
        return null;
      }
    }

    return null;
  }




  /**
   * Opens a file, reads it and returns the data as a string and closes the
   * file.
   *
   * @param fname - file to open
   *
   * @return String representing the file data
   */
  public static String fileToString( final String fname )
  {
    return FileUtil.fileToString( new File( fname ) );
  }




  /**
   * Opens a file, reads it and returns the data as a string and closes the
   * file.
   *
   * @param file - file to open
   *
   * @return String representing the file data
   */
  public static String fileToString( final File file )
  {
    try
    {
      final byte[] data = FileUtil.read( file );

      if( data != null )
      {
        // Attempt to return the string
        try
        {
          return new String( data, StringUtil.ISO8859_1 );
        }
        catch( final UnsupportedEncodingException uee )
        {
          // Send it back in default encoding
          return new String( data );
        }
      }
    }
    catch( final Exception ex )
    {
    }

    return null;
  }




  /**
   * Opens a file, writes out the given string then closes the file.
   *
   * @param text - string to write
   * @param fname - file to open
   *
   * @return boolean whether or not the operation worked
   */
  public static boolean stringToFile( final String text, final String fname )
  {
    if( fname != null )
    {
      final File file = new File( fname );

      try
      {
        FileUtil.write( file, text.getBytes( StringUtil.ISO8859_1 ) );

        return true;
      }
      catch( final Exception ex )
      {
      }
    }

    return false;
  }




  /**
   * CleanDirs is a utility method for cleaning up temporary directories, used
   * by various methods to hold & process files on the file system.
   *
   * <p>CleanDir takes a directory name as an argument, and checks for any
   * empty directories within that directory. If it finds any, it remove the
   * empty directory & checks again until no empty directories are found.</p>
   *
   * @param fileName
   *
   * @throws IOException
   */
  public static void cleanDirs( String fileName ) throws IOException
  {
    if( !fileName.endsWith( "/" ) )
    {
      fileName = fileName + "/";
    }

    final Vector contents = FileUtil.getDir( fileName );
    String oneItem = null;
    File oneFile = null;

    for( final Enumeration e = contents.elements(); e.hasMoreElements(); )
    {
      oneItem = (String)e.nextElement();
      oneFile = new File( fileName + oneItem );

      if( oneFile.isDirectory() )
      {
        // Try cleaning it
        FileUtil.cleanDirs( fileName + oneItem );

        // If it's now empty...
        if( FileUtil.getDir( fileName + oneItem ).size() == 0 )
        {
          oneFile = new File( fileName + oneItem );

          if( !oneFile.delete() )
          {
            System.err.println( "Unable to delete directory " + oneItem );
          }
        }
      }
    }
  }




  /**
   * Utility method to copy a file from one place to another & delete the
   * original.
   *
   * <p>If we fail, we throw IOException, also reporting what username this
   * process is running with to assist System Admins in setting appropriate
   * permissions.</p>
   *
   * @param sourceFile Source file pathname
   * @param destFile Destination file pathname
   * @throws IOException If the copy fails due to an I/O error
   */
  public static void copyFile( final String sourceFile, final String destFile ) throws IOException
  {
    FileUtil.copyFile( new File( sourceFile ), new File( destFile ) );
  }




  /**
   * Utility method to copy a file from one place to another & delete the
   * original.
   *
   * <p>If we fail, we throw IOException, also reporting what username this
   * process is running with to assist System Admins in setting appropriate
   * permissions.</p>
   *
   * @param sourceFile Source file pathname
   * @param destFile Destination file pathname
   * @throws IOException If the copy fails due to an I/O error
   */
  public static void copyFile( final File sourceFile, final File destFile ) throws IOException
  {
    int onechar = 0;

    if( sourceFile == null )
    {
      throw new IOException( "Source file is null - cannot copy." );
    }

    if( destFile == null )
    {
      throw new IOException( "Destination file is null - cannot copy." );
    }

    if( sourceFile.compareTo( destFile ) == 0 )
    {
      throw new IOException( "Cannot copy file '" + sourceFile + "' to itself" );
    }

    destFile.mkdirs();

    if( destFile.exists() && !destFile.delete() )
    {
      throw new IOException( "Unable to delete existing destination file '" + destFile + "'. Logged in as " + System.getProperty( "user.name" ) );
    }

    if( !sourceFile.exists() )
    {
      throw new IOException( "Source file '" + sourceFile + "' does not exist. Cannot copy. Logged in as " + System.getProperty( "user.name" ) );
    }

    final FileOutputStream fout = new FileOutputStream( destFile );
    final BufferedOutputStream bout = new BufferedOutputStream( fout );
    final FileInputStream fin = new FileInputStream( sourceFile );
    final BufferedInputStream bin = new BufferedInputStream( fin );
    onechar = bin.read();

    while( onechar != -1 )
    {
      bout.write( onechar );

      onechar = bin.read();
    }

    bout.flush();
    bin.close();
    fin.close();

    if( !destFile.exists() )
    {
      throw new IOException( "File copy failed: destination file '" + destFile + "' does not exist after copy." );
    }
    // The below test is commented out because it does not
    // appear to work correctly under Windows NT and Windows 2000
    // if (sourceFile.length() != destFile.length())
    // {
    // throw new IOException("File copy complete, but source file was " + sourceFile.length() + " bytes, destination now " + destFile.length() + " bytes.");
    // }
  }




  /**
   * Get the base filename of a file (e.g. no directory or extension)
   *
   * <p>Returns "readme" from "T:\projects\src\readme.txt".</p>
   *
   * <p>NOTE: This method uses the default file separator for the system.</p>
   *
   * @param fileName Original pathname to get the base name from.
   *
   * @return The base file name
   */
  public static String getBase( final String fileName )
  {
    String tempName1 = new String( "" );
    final StringTokenizer stk1 = new StringTokenizer( fileName, "/\\" );

    // Cruise through the string and eat up all the tokens before the last
    // directory delimiter
    while( stk1.hasMoreTokens() )
    {
      tempName1 = stk1.nextToken();
    }

    final StringTokenizer stk = new StringTokenizer( tempName1, "." );
    return stk.nextToken();
  }




  /**
   * Return a vector of the file/dir names in any give directory
   *
   * @param dirName
   *
   * @return TODO Complete Documentation
   *
   * @throws IOException If the given name is not a directory or if
   */
  public static Vector getDir( final String dirName ) throws IOException
  {
    final File dirFile = new File( dirName );

    if( !dirFile.isDirectory() )
    {
      throw new IOException( "'" + dirName + "' is not a directory." );
    }

    final String[] dir = dirFile.list();

    if( dir == null )
    {
      throw new IOException( "Null array reading directory of " + dirName );
    }

    final Vector fileList = new Vector( 1 );
    String oneFileName = null;

    for( int i = 0; i < dir.length; i++ )
    {
      oneFileName = dir[i].trim();

      fileList.addElement( oneFileName );
    }

    return fileList;
  }




  /**
   * Get the file extension (after the ".")
   *
   * @param fileName Original full file name
   *
   * @return String Extension name
   *
   * @throws IOException If unable to allocate the file to get the extension
   */
  public static String getExtension( final String fileName ) throws IOException
  {
    final String tempName = new File( fileName ).getName();
    final StringTokenizer stk = new StringTokenizer( tempName, "." );
    stk.nextToken();

    if( stk.hasMoreTokens() )
    {
      return stk.nextToken();
    }
    else
    {
      return new String( "" );
    }
  }




  /**
   * Open a file , creating it as necessary, and changing its modification time.
   *
   * @param file
   */
  public final static void touch( final File file )
  {
    try
    {
      FileUtil.append( file, new byte[0], false );
    }
    catch( final IOException ioe )
    {
    }
  }




  /**
   * Open a file , creating it as necessary, and changing its modification time.
   *
   * @param file
   * @param data
   * @param backup
   *
   * @throws IOException
   */
  public final static void append( final File file, final byte[] data, final boolean backup ) throws IOException
  {
    if( !file.exists() || ( file.exists() && file.canWrite() ) )
    {
      if( !file.exists() )
      {
        if( file.getParentFile() != null )
        {
          file.getParentFile().mkdirs();
        }
      }
      else if( backup )
      {
        FileUtil.createGenerationalBackup( file );
      }

      RandomAccessFile seeker = null;

      try
      {
        seeker = new RandomAccessFile( file, "rw" );

        seeker.seek( seeker.length() );
        seeker.write( data );
        file.setLastModified( System.currentTimeMillis() );
      }
      catch( final IOException ioe )
      {
        throw ioe;
      }
      finally
      {
        // Attempt to close the data input stream
        try
        {
          // If it is open, close it
          if( seeker != null )
          {
            seeker.close();
          }
        }
        catch( final Exception e )
        {
          // Nevermind
        }
        finally
        {
        }
      }
    }

  }




  /**
   * Strip the path and suffix of a file name
   *
   * @param file Name of a file  "/usr/local/dbase/test.DBF"
   *
   * @return filename "test"
   */
  public final static String stripPathAndExtension( final String file )
  {
    int begin = file.lastIndexOf( FileUtil.FILE_SEPARATOR );

    if( begin < 0 )
    {
      begin = 0;
    }
    else
    {
      begin++;
    }

    int end = file.lastIndexOf( "." );

    if( end < 0 )
    {
      end = file.length();
    }

    final String str = file.substring( begin, end );
    return str;
  }




  /**
   * Return the package portion of the given classname as a standard path
   * suitable for use in hierarchical storage systems.
   *
   * @param classname
   *
   * @return TODO Complete Documentation
   */
  public final static String getJavaPath( final String classname )
  {
    if( classname.endsWith( ".class" ) )
    {
      return "/" + StringUtil.getJavaPackage( classname.substring( 0, classname.lastIndexOf( '.' ) ) ).replace( '.', '/' );
    }

    return "/" + StringUtil.getJavaPackage( classname ).replace( '.', '/' );
  }




  /**
   * Returns the local class name of the given string without any package data
   * and with the ".class" appended to the result.
   *
   * @param classname
   *
   * @return TODO Complete Documentation
   */
  public final static String getJavaFile( final String classname )
  {
    if( classname.endsWith( ".class" ) )
    {
      return StringUtil.getLocalJavaName( classname.substring( 0, classname.lastIndexOf( '.' ) ) ) + ".class";
    }

    return StringUtil.getLocalJavaName( classname ) + ".class";
  }




  /**
   * Return a string representing the path of the given class name without any
   * extensions.
   *
   * <p>The returned string should represent a relative path to the base class
   * or source file with the simple adding of a &quot;.class&quot; or a
   * &quot;.java&quot; respectively.</p>
   *
   * @param classname The fully-qualified name of a class.
   *
   * @return A standard path structure to the base class, or the classname
   *         itself if no package information was found in the classname.
   */
  public final static String getJavaBasePath( final String classname )
  {
    String base = classname;

    // Get the main body of the class name (no extension)
    if( classname.endsWith( ".class" ) || classname.endsWith( ".java" ) || classname.endsWith( "." ) )
    {
      base = classname.substring( 0, classname.lastIndexOf( '.' ) );
    }

    // remove any leading dots
    if( base.charAt( 0 ) == '.' )
    {
      base = base.substring( 1 );
    }

    // replace dots with path separaters
    base = base.replace( '.', '/' );

    return base;
  }




  /**
   * Method getJavaClassFile
   *
   * @param classname
   *
   * @return TODO Complete Documentation
   */
  public final static File getJavaClassFile( final String classname )
  {
    return FileUtil.getJavaClassFile( FileUtil.CURRENT_DIR, classname );
  }




  /**
   * Method getJavaClassFile
   *
   * @param dir
   * @param classname
   *
   * @return TODO Complete Documentation
   */
  public final static File getJavaClassFile( final File dir, final String classname )
  {
    File retval = null;

    final String fil = FileUtil.getJavaBasePath( classname ) + ".class";

    // Make sure we have a parent directory
    if( ( dir != null ) && dir.isDirectory() )
    {
      retval = new File( dir, fil );
    }
    else
    {
      retval = new File( FileUtil.CURRENT_DIR, fil );
    }

    return retval;
  }




  /**
   * Method getJavaSourceFile
   *
   * @param classname
   *
   * @return TODO Complete Documentation
   */
  public final static File getJavaSourceFile( final String classname )
  {
    return FileUtil.getJavaSourceFile( FileUtil.CURRENT_DIR, classname );
  }




  /**
   * Method getJavaSourceFile
   *
   * @param dir
   * @param classname
   *
   * @return TODO Complete Documentation
   */
  public final static File getJavaSourceFile( final File dir, final String classname )
  {
    File retval = null;

    final String fil = FileUtil.getJavaBasePath( classname ) + ".java";

    // Make sure we have a parent directory
    if( ( dir != null ) && dir.isDirectory() )
    {
      retval = new File( dir, fil );
    }
    else
    {
      retval = new File( FileUtil.CURRENT_DIR, fil );
    }

    return retval;
  }




  /**
   * Get the path of a file name.
   *
   * <p>If the filename is relative, the result will be a relative path. If the
   * filename is absolute, the path returned will be absolute.</p>
   *
   * <p>The path separator for the current platform will be used as a path
   * delimiter of the returned path.</p>
   *
   * @param fileName Original pathname
   *
   * @return String Path portion of the pathname
   */
  public static String getPath( final String fileName )
  {
    final StringBuffer path = new StringBuffer();

    if( fileName.endsWith( "/" ) || fileName.endsWith( "\\" ) )
    {
      // Already appears to be a path
      path.append( fileName );
    }
    else
    {
      if( fileName.indexOf( ":" ) > 0 )
      {
        // looks like a DOS path with a Drive designator, do not specify root
      }
      else
      {
        if( ( fileName.indexOf( "\\" ) == 0 ) || ( fileName.indexOf( "/" ) == 0 ) )
        {
          // specify fully-qualified from root
          path.append( FileUtil.FILE_SEPARATOR );
        }
      }

      String token;
      final StringTokenizer stk = new StringTokenizer( fileName, "/\\" );

      while( stk.hasMoreTokens() )
      {
        token = stk.nextToken();

        if( stk.hasMoreTokens() && ( token.length() > 0 ) )
        {
          path.append( token );
          path.append( FileUtil.FILE_SEPARATOR );
        }
      }
    }

    return FileUtil.normalizeSlashes( path.toString() );
  }




  /**
   * Copy a file, then remove the original file
   *
   * @param sourceFile Original file name
   * @param destFile Destination file name
   *
   * @throws IOException If an I/O error occurs during the copy
   */
  public static void moveFile( final String sourceFile, final String destFile ) throws IOException
  {
    FileUtil.moveFile( new File( sourceFile ), new File( destFile ) );
  }




  /**
   * Copy a file, then remove the original file
   *
   * @param sourceFile Original file reference
   * @param destFile Destination file reference
   *
   * @throws IOException If an I/O error occurs during the copy
   */
  public static void moveFile( final File sourceFile, final File destFile ) throws IOException
  {
    if( !sourceFile.canRead() )
    {
      throw new IOException( "Cannot read source file '" + sourceFile + "'. Logged in as " + System.getProperty( "user.name" ) );
    }

    if( !sourceFile.canWrite() )
    {
      throw new IOException( "Cannot write to source file '" + sourceFile + "'. Logged in as " + System.getProperty( "user.name" ) + ". Cannot move without write permission to source file." );
    }

    if( sourceFile.compareTo( destFile ) == 0 )
    {
      // System.out.println( "Source and destination the same - no move required" );
      return;
    }

    FileUtil.copyFile( sourceFile, destFile );

    if( !sourceFile.delete() )
    {
      System.out.println( "Copy completed, but unable to delete source file '" + sourceFile + "'. Logged in as " + System.getProperty( "user.name" ) );
    }
  }




  /**
   * Take a prefix and a relative path and put the two together to make an
   * absolute path.
   *
   * @param prefix
   * @param originalPath
   *
   * @return TODO Complete Documentation
   */
  public static String makeAbsolutePath( String prefix, String originalPath )
  {
    StringUtil.assertNotBlank( originalPath, "Original path may not be blank here" );

    prefix = StringUtil.notNull( prefix );
    originalPath = originalPath.replace( '\\', '/' );
    prefix = prefix.replace( '\\', '/' );

    if( originalPath.startsWith( "/" ) )
    {
      return originalPath;
    }

    // Check for a drive specification for windows-type path
    if( originalPath.substring( 1, 2 ).equals( ":" ) )
    {
      return originalPath;
    }

    // Otherwise...Make sure the prefix ends with a "/"
    if( !prefix.endsWith( "/" ) )
    {
      prefix = prefix + "/";
    }

    // and put the two together
    return prefix + originalPath;
  }




  /**
   * This returns a URI for the given file.
   *
   * @param file from which to generate a URI
   *
   * @return the URI of the given file or null if a logic error occurred
   */
  public static URI getFileURI( final File file )
  {
    final StringBuffer buffer = new StringBuffer( "file://" );

    final char[] chars = file.getAbsolutePath().toCharArray();

    URI retval = null;

    if( chars != null )
    {
      if( chars.length > 1 )
      {
        // If there is a drive delimiter ':' in the second position, we assume
        // this is file is on a Windows system which does not return a leading /
        if( chars[1] == ':' )
        {
          buffer.append( "/" );
        }
      }

      for( int i = 0; i < chars.length; i++ )
      {
        final char c = chars[i];

        switch( c )
        {

          // Replace spaces
          case ' ':
            buffer.append( "%20" );
            continue;

            // Replace every Windows file separator
          case '\\':
            buffer.append( "/" );
            continue;

          default:
            buffer.append( c );
            continue;

        }
      }

      try
      {
        retval = new URI( buffer.toString() );
      }
      catch( final URISyntaxException e )
      {
        System.err.println( e.getMessage() );
      }
    }

    return retval;
  }




  /**
   * Scan the given directory for files containing the substrMatch
   * Small case extensions '.dbf' are recognized and returned as '.DBF'
   *
   * @param path eg "/usr/local/dbase"
   * @param suffix Case insensitive: eg ".DBF"
   *
   * @return TODO Complete Documentation
   */
  public final static Vector getAllFiles( final String path, final String suffix )
  {

    /**
     * Inner Class MyDir
     */
    class MyDir extends javax.swing.filechooser.FileSystemView
    {

      /**
       * Method createNewFolder
       *
       * @param containingDir
       *
       * @return TODO Complete Documentation
       */
      public File createNewFolder( final File containingDir )
      {
        return null;
      }




      /**
       * Method getRoots
       *
       * @return TODO Complete Documentation
       */
      public File[] getRoots()
      {
        return null;
      }




      /**
       * Method isHiddenFile
       *
       * @param f
       *
       * @return TODO Complete Documentation
       */
      public boolean isHiddenFile( final File f )
      {
        return false;
      }




      /**
       * Method isRoot
       *
       * @param f
       *
       * @return TODO Complete Documentation
       */
      public boolean isRoot( final File f )
      {
        return false;
      }
    }

    final MyDir view = new MyDir();
    final Vector vec = new Vector( 20 );
    final File dir = view.createFileObject( path );
    final File[] ff = view.getFiles( dir, false );
    String upperSuffix = null;

    if( suffix != null )
    {
      upperSuffix = suffix.toUpperCase();
    }

    for( int ii = 0; ii < ff.length; ii++ )
    {
      final String file = ff[ii].toString().toUpperCase();

      if( ( upperSuffix == null ) || file.endsWith( upperSuffix ) )
      {
        vec.addElement( ff[ii] );
      }
    }

    return vec;
  }




  /**
   * Delete a file
   *
   * @param fname
   *
   * @throws IOException
   * @throws NullPointerException
   */
  public final static void delFile( final String fname ) throws NullPointerException, IOException
  {
    final File f = new File( fname );

    // only delete a file that exists
    if( f.exists() )
    {
      // try the delete. If it fails, complain
      if( !f.delete() )
      {
        throw new IOException( "Could not delete file: '" + fname + "'" );
      }
    }
  }




  /**
   * Delete a file
   *
   * @param dataDir
   * @param fname
   *
   * @throws IOException
   * @throws NullPointerException
   */
  public final static void delFile( final String dataDir, final String fname ) throws NullPointerException, IOException
  {
    final File f = new File( dataDir + FileUtil.FILE_SEPARATOR + fname );

    // only delete a file that exists
    if( f.exists() )
    {
      // try the delete. If it fails, complain
      if( !f.delete() )
      {
        throw new IOException( "Could not delete file: " + dataDir + "/" + fname + "." );
      }
    }
  }




  /**
   * Performs a recursive delete of a directory and all its contents.
   *
   * @param dir
   *
   * @throws IOException
   */
  public static final void removeDir( final File dir ) throws IOException
  {
    final File[] list = dir.listFiles();

    if( null != list )
    {
      for( int ii = 0; ii < list.length; ii++ )
      {
        if( list[ii].isDirectory() )
        {
          FileUtil.removeDir( list[ii] );
        }
        else
        {
          if( !list[ii].delete() )
          {
            throw new IOException( "Unable to delete file " + list[ii].getAbsolutePath() );
          }
        }
      }
    }

    if( !dir.delete() && dir.exists() )
    {
      throw new IOException( "Unable to delete directory " + dir.getAbsolutePath() );
    }
  }




  /**
   * rename a file
   *
   * @param oldName
   * @param newName
   *
   * @return true if succeeded
   */
  public final static boolean renameFile( final String oldName, final String newName )
  {
    final File f_old = new File( oldName );
    final File f_new = new File( newName );
    final boolean ret = f_old.renameTo( f_new );
    return ret;
  }




  /**
   * Open a file and return a DataInputStream object
   *
   * @param fn
   *
   * @return DataInpuStream - stream to use for file data
   */
  public static DataInputStream openInputFile( final String fn )
  {
    FileInputStream fis = null;
    DataInputStream dis = null;
    BufferedInputStream bis = null;

    try
    {
      fis = new FileInputStream( fn );
    }
    catch( final IOException e )
    {
      return ( dis );
    }

    try
    {
      bis = new BufferedInputStream( fis );
      dis = new DataInputStream( bis );
    }
    catch( final Exception e )
    {
      try
      {
        fis.close();
      }
      catch( final IOException e1 )
      {
      }

      dis = null;

      return ( dis );
    }

    return ( dis );
  }




  /**
   * Method getFile
   *
   * @param filename
   *
   * @return TODO Complete Documentation
   */
  public static String getFile( final String filename )
  {
    final String tmp = new String( filename );
    tmp.replace( '\\', '/' );

    final int i = tmp.lastIndexOf( '/' );
    return ( i != -1 ) ? tmp.substring( i + 1 ) : tmp;
  }




  /**
   * GetFileSize - returns the file size
   *
   * @param filename - file to size
   *
   * @return length of file
   */
  public static long getFileSize( final String filename )
  {
    File f;

    try
    {
      f = new File( filename );

      if( f.exists() )
      {
        return f.length();
      }
    }
    catch( final Exception e )
    {
    }

    return -1;
  }




  /**
   * Clear a directory of all files
   *
   * @param dir The name of the directory to delete
   * @param clrdir Delete the directory after it has been cleared
   * @param clrsub Clear the subdirectories of this directory as well
   */
  public static void clearDir( final String dir, final boolean clrdir, final boolean clrsub )
  {
    FileUtil.clearDir( new File( dir ), clrdir, clrsub );
  }




  /**
   * Clear a directory of all files
   *
   * <p>Calling this with clrdir=false and clrsub=true will result in all files
   * in all the subdirectories of the given directory being deleted, but the
   * entire directory structure will remain on the file system.</p>
   *
   * <p>Calling this with clrdir=true and clrsub=true will result in all files
   * and all the subdirectories of the given directory being deleted as well as
   * the fiven directory.</p>
   *
   * <p>Calling this with clrdir=false and clrsub=false will result in all
   * files of the current directory being deleted with all the subdirectories
   * reamining untouched.</p>
   *
   * TODO Calling this with clrdir=true and clrsub=false will result in all files of the current directory being deleted with all the subdirectories being moved to the parent directory. The orphans of the current directory should not be lost in the clearing operation.
   *
   * @param dir The file reference to the directory to delete
   * @param clrdir Delete the directory after it has been cleared
   * @param clrsub Clear the subdirectories of this directory as well
   */
  public static void clearDir( final File dir, final boolean clrdir, final boolean clrsub )
  {
    if( !dir.isDirectory() )
    {
      return;
    }

    try
    {
      // Get a list of all the children in the current directory
      final String[] childlist = dir.list();

      // For each child in the directory
      for( int i = 0; i < childlist.length; i++ )
      {
        // Create a new file reference
        final File child = new File( dir, childlist[i] );

        // If it exists ( which is should )
        if( child.exists() )
        {
          // If the child is a file...
          if( child.isFile() )
          {
            // ...delete the file
            child.delete();
          }
          else
          {
            if( clrsub )
            {
              // .. otherwise recursively call this method to delete the
              // directory
              FileUtil.clearDir( child, clrdir, clrsub );
            }
            else
            {
              // Force the deletion of the children
              FileUtil.clearDir( child, clrdir, true );

              // We need to make sure that moveFile will move directories
              // before this call can be made:
              // moveFile(child,dir.getParentFile());
            }
          }
        }
      }

      // After all the contents are deleted, is we are to delete the directory
      if( clrdir )
      {
        // ...delete the directory itself
        dir.delete();
      }
    }
    catch( final Exception e )
    {
    }
  }




  /**
   * Method menu
   */
  private static void menu()
  {
    System.out.println( "1. copyFile" );
    System.out.println( "2. moveFile" );
    System.out.println( "3. getPath" );
    System.out.println( "4. getBase" );
    System.out.println( "5. getExtension" );
    System.out.println( "6. getDir" );
    System.out.println( "7. cleanDirs" );
    System.out.println( "8. makeAbsolutePath" );
    System.out.println( "0. quit" );
  }




  /**
   * Main method used for testing and command line operations
   *
   * @param args
   */
  public static void main( final String[] args )
  {
    System.out.println( "FileUtil Test" );

    String command = new String( "" );
    final BufferedReader ds = new BufferedReader( new InputStreamReader( System.in ) );

    try
    {
      while( !command.equals( "0" ) )
      {
        FileUtil.menu();
        System.out.print( "Command==>" );

        command = ds.readLine();

        System.out.println( "" );
        System.out.println( "Command:" + command );

        if( command.equals( "1" ) )
        {
          System.out.println( "copyFile" );
          System.out.print( "sourceFile:" );

          final String sourceFile = ds.readLine();
          System.out.print( "destFile:" );

          final String destFile = ds.readLine();
          FileUtil.copyFile( sourceFile, destFile );
          System.out.println( "Copy Complete\n\n" );
        }
        else
        {
          if( command.equals( "2" ) )
          {
            System.out.println( "moveFile" );
            System.out.print( "sourceFile:" );

            final String sourceFile = ds.readLine();
            System.out.print( "destFile:" );

            final String destFile = ds.readLine();
            FileUtil.moveFile( sourceFile, destFile );
            System.out.println( "Move Complete\n\n" );
          }
          else
          {
            if( command.equals( "3" ) )
            {
              System.out.println( "getPath" );
              System.out.print( "fileName:" );

              final String fileName = ds.readLine();
              System.out.println( "Path:'" + FileUtil.getPath( fileName ) + "'\n\n" );
            }
            else
            {
              if( command.equals( "4" ) )
              {
                System.out.println( "getBase" );
                System.out.print( "fileName:" );

                final String fileName = ds.readLine();
                System.out.println( "Base:'" + FileUtil.getBase( fileName ) + "'\n\n" );
              }
              else
              {
                if( command.equals( "5" ) )
                {
                  System.out.println( "getExtension" );
                  System.out.print( "fileName:" );

                  final String fileName = ds.readLine();
                  System.out.println( "Extension:'" + FileUtil.getExtension( fileName ) + "'\n\n" );
                }
                else
                {
                  if( command.equals( "6" ) )
                  {
                    System.out.println( "getDir" );
                    System.out.print( "dirName:" );

                    final String dirName = ds.readLine();
                    final Vector v = FileUtil.getDir( dirName );

                    for( final Enumeration e = v.elements(); e.hasMoreElements(); )
                    {
                      System.out.println( "Item:'" + (String)e.nextElement() + "'" );
                    }

                    System.out.println( "Directory Complete" );
                  }
                  else
                  {
                    if( command.equals( "7" ) )
                    {
                      System.out.println( "cleanDirs" );
                      System.out.println( "dirName:" );

                      final String dirName = ds.readLine();
                      FileUtil.cleanDirs( dirName );
                    }
                    else
                    {
                      if( command.equals( "8" ) )
                      {
                        System.out.println( "prefix:" );

                        final String prefix = ds.readLine();
                        System.out.println( "fileName:" );

                        final String fileName = ds.readLine();
                        System.out.println( "Converted name:" + FileUtil.makeAbsolutePath( prefix, fileName ) );
                      }
                      else
                      {
                        if( !command.equals( "0" ) )
                        {
                          System.out.println( "Unknown command:" + command );
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    catch( final Exception e )
    {
      e.printStackTrace( System.out );
    }
  }




  /**
   * Method backup
   *
   * @param file
   *
   * @throws IOException
   */
  public static void createGenerationalBackup( final File file ) throws IOException
  {
    int i = 0;
    for( i = 0; new File( file.getAbsolutePath() + "." + i ).exists(); i++ )
    {
      ;
    }

    FileUtil.copyFile( file.getAbsolutePath(), new File( file.getAbsolutePath() + "." + i ).getAbsolutePath() );
  }




  /**
   * Method write
   *
   * @param file
   * @param data
   *
   * @throws IOException
   */
  public static void write( final File file, final byte[] data ) throws IOException
  {
    FileUtil.write( file, data, false );
  }




  /**
   * Write the given data to the given file object creating it and it's parent
   * directiories as necessary.
   *
   * @param file The file reference to which the data will be written.
   * @param data The data to write to the file.
   * @param backup Flag indicating a generational backup of the data should be
   *        made before writing to the file.
   *
   * @throws IOException if there were problems with any of the operations
   *         involved with writing the data
   */
  public static void write( final File file, final byte[] data, final boolean backup ) throws IOException
  {
    if( file == null )
    {
      throw new IOException( "File reference was null" );
    }

    if( !file.exists() || ( file.exists() && file.canWrite() ) )
    {
      DataOutputStream dos = null;

      try
      {
        if( file.exists() && backup )
        {
          FileUtil.createGenerationalBackup( file );
        }

        // Make sure the parent directories are present
        if( file.getParent() != null )
        {
          file.getParentFile().mkdirs();
        }

        if( data.length > 0 )
        {
          // Create an output stream
          dos = new DataOutputStream( new FileOutputStream( file ) );

          // Write the data to it
          dos.write( data );

          // Flush the buffers
          dos.flush();
        }
        else
        {
          FileUtil.touch( file );
        }

      }
      catch( final EOFException eof )
      {
      }
      finally
      {
        // Attempt to close the data input stream
        try
        {
          // If it is open, close it
          if( dos != null )
          {
            dos.close();
          }
        }
        catch( final Exception e )
        {
          // Nevermind
        }
        finally
        {
        }
      }
    }
  }




  /**
   * Method makeDirectory
   *
   * @param file
   *
   * @throws IOException
   */
  public static void makeDirectory( final File file ) throws IOException
  {
    if( file == null )
    {
      throw new IOException( "File reference was null" );
    }

    if( !file.exists() || ( file.exists() && file.isFile() ) )
    {
      // Make sure the parent directories are present
      if( file.getParent() != null )
      {
        file.getParentFile().mkdirs();
      }

      if( !file.mkdir() )
      {
        throw new IOException( "Could not make directory" );
      }
    }
  }




  /**
   * Make a directory with the given name.
   *
   * <p>If the operation failed, a partial path may exist.</p>
   *
   * @param name any valid path name with slashes, back-slashes, relational
   * dots and whatever.
   *
   * @return The file reference to the directory created, null if the operation
   *         failed in some way.
   */
  public static File makeDirectory( final String name )
  {
    File retval = null;

    if( ( name != null ) && ( name.length() > 0 ) )
    {
      final File tempfile = new File( FileUtil.normalizePath( name ) );

      try
      {
        FileUtil.makeDirectory( tempfile );

        retval = tempfile;
      }
      catch( final Exception e )
      {
        retval = null;
      }
    }

    return retval;
  }




  /**
   * Read the entire file into memory as an array of bytes.
   *
   * @param file The file to read
   *
   * @return A byte array that contains the contents of the file.
   *
   * @throws IOException If problems occur.
   */
  public static byte[] read( final File file ) throws IOException
  {
    if( file == null )
    {
      throw new IOException( "File reference was null" );
    }

    if( file.exists() && file.canRead() )
    {
      DataInputStream dis = null;
      final byte[] bytes = new byte[new Long( file.length() ).intValue()];

      try
      {
        dis = new DataInputStream( new FileInputStream( file ) );

        dis.readFully( bytes );

        return bytes;
      }
      catch( final Exception ignore )
      {
      }
      finally
      {
        // Attempt to close the data input stream
        try
        {
          if( dis != null )
          {
            dis.close();
          }
        }
        catch( final Exception ignore )
        {
        }
      }
    }

    return null;
  }




  /**
   * Remove duplicate file separators, remove relation dots and correct all
   * file separators to those suitable for URI usage ('/').
   *
   * @param path The path to standardize
   *
   * @return The standardize path
   */
  public static String standardizePath( String path )
  {
    path = FileUtil.normalizeSlashes( path );
    path = FileUtil.removeRelations( path );
    path = path.replace( File.separatorChar, '/' );

    return path;
  }




  /**
   * &quot;normalize&quot; the given absolute path.
   *
   * <p>This includes:
   * <ul>
   *   <li>Uppercase the drive letter if there is one.</li>
   *   <li>Remove redundant slashes after the drive spec.</li>
   *   <li>resolve all ./, .\, ../ and ..\ sequences.</li>
   *   <li>DOS style paths that start with a drive letter will have
   *     \ as the separator.</li>
   * </ul>
   * Unlike <code>File#getCanonicalPath()</code> it specifically doesn't
   * resolve symbolic links.
   *
   * @param path the path to be normalized
   *
   * @return the normalized version of the path.
   */
  public static File normalize( String path )
  {
    final String orig = path;

    path = path.replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );

    // make sure we are dealing with an absolute path
    final int colon = path.indexOf( ":" );

    if( !path.startsWith( File.separator ) && ( colon == -1 ) )
    {
      final String msg = path + " is not an absolute path";
      throw new ChainedRuntimeException( msg );
    }

    final boolean dosWithDrive = false;
    String root = null;
    // Eliminate consecutive slashes after the drive spec
    if( path.length() == 1 )
    {
      root = File.separator;
      path = "";
    }
    else if( path.charAt( 1 ) == File.separatorChar )
    {
      // UNC drive
      root = File.separator + File.separator;
      path = path.substring( 2 );
    }
    else
    {
      root = File.separator;
      path = path.substring( 1 );
    }

    final Stack s = new Stack();
    s.push( root );

    final StringTokenizer tok = new StringTokenizer( path, File.separator );

    while( tok.hasMoreTokens() )
    {
      final String thisToken = tok.nextToken();

      if( ".".equals( thisToken ) )
      {
        continue;
      }
      else if( "..".equals( thisToken ) )
      {
        if( s.size() < 2 )
        {
          throw new ChainedRuntimeException( "Cannot resolve path " + orig );
        }
        else
        {
          s.pop();
        }
      }
      else
      {
        // plain component
        s.push( thisToken );
      }
    }

    final StringBuffer sb = new StringBuffer();

    for( int i = 0; i < s.size(); i++ )
    {
      if( i > 1 )
      {
        // not before the filesystem root and not after it, since root
        // already contains one
        sb.append( File.separatorChar );
      }

      sb.append( s.elementAt( i ) );
    }

    path = sb.toString();

    if( dosWithDrive )
    {
      path = path.replace( '/', '\\' );
    }

    return new File( path );
  }




  /**
   * Remove duplicate file separators, remove relation dots and correct all
   * non-platform specific file separators to those of the current platform.
   *
   * @param path The path to normalize
   *
   * @return The normalized path
   */
  public static String normalizePath( String path )
  {
    path = FileUtil.normalizeSlashes( path );
    path = FileUtil.removeRelations( path );

    return path;
  }




  /**
   * Replace all the file separator characters (either '/' or '\') with the
   * proper file serparator for this platform.
   *
   * @param path
   *
   * @return TODO Complete Documentation
   */
  public static String normalizeSlashes( String path )
  {
    if( path == null )
    {
      return null;
    }
    else
    {
      path = path.replace( '/', File.separatorChar );
      path = path.replace( '\\', File.separatorChar );

      return path;
    }
  }




  /**
   * Remove the current and parent directory relation references from the given
   * path string.
   *
   * <p>Takes a string like &quot;\home\work\bin\..\lib&quot; and returns a
   * path like &quot;\home\work\lib&quot;
   *
   * @param path The representative path with possible relational dot notation
   *
   * @return The representative path without the dots
   */
  public static String removeRelations( final String path )
  {
    if( path == null )
    {
      return null;
    }
    else if( path.length() == 0 )
    {
      return path;
    }
    else
    {
      // Break the path into tokens and skip any '.' tokens
      final StringTokenizer st = new StringTokenizer( path, "/\\" );
      final String[] tokens = new String[st.countTokens()];

      int i = 0;

      while( st.hasMoreTokens() )
      {
        final String token = st.nextToken();

        if( ( token != null ) && ( token.length() > 0 ) && !token.equals( "." ) )
        {
          // if there is a reference to the parent, then just move back to the
          // previous token in the list, which is this tokens parent
          if( token.equals( ".." ) )
          {
            if( i > 0 )
            {
              tokens[--i] = null;
            }
          }
          else
          {
            tokens[i++] = token;
          }
        }
      }

      // Start building the new path from the tokens
      final StringBuffer retval = new StringBuffer();

      // If the original path started with a file separator, then make sure the
      // return value starts the same way
      if( ( path.charAt( 0 ) == '/' ) || ( path.charAt( 0 ) == '\\' ) )
      {
        retval.append( File.separatorChar );
      }

      // For each token in the path
      if( tokens.length > 0 )
      {
        for( i = 0; i < tokens.length; i++ )
        {
          if( tokens[i] != null )
          {
            retval.append( tokens[i] );
          }

          // if there is another token on the list, use the platform-specific
          // file separator as a delimiter in the return value
          if( ( i + 1 < tokens.length ) && ( tokens[i + 1] != null ) )
          {
            retval.append( File.separatorChar );
          }
        }
      }

      if( ( path.charAt( path.length() - 1 ) == '/' ) || ( ( path.charAt( path.length() - 1 ) == '\\' ) && ( retval.charAt( retval.length() - 1 ) != File.separatorChar ) ) )
      {
        retval.append( File.separatorChar );
      }

      return retval.toString();
    }
  }




  /**
   * Method saveStreamToFile
   *
   * @param in
   * @param outFile
   *
   * @throws IOException
   */
  public static void saveStreamToFile( final InputStream in, final File outFile ) throws IOException
  {
    FileOutputStream out = null;

    try
    {
      out = new FileOutputStream( outFile );

      final byte[] buf = new byte[4096];
      int bytes_read;

      while( ( bytes_read = in.read( buf ) ) != -1 )
      {
        out.write( buf, 0, bytes_read );
      }
    }
    finally
    {
      if( in != null )
      {
        try
        {
          in.close();
        }
        catch( final IOException e )
        {
        }
      }

      if( out != null )
      {
        try
        {
          out.close();
        }
        catch( final IOException e )
        {
        }
      }
    }
  }




  /**
   * Formats the size as a most significant number of bytes.
   *
   * @param size
   *
   * @return TODO Complete Documentation
   */
  public static String formatSizeBytes( final double size )
  {
    final StringBuffer buf = new StringBuffer( 16 );
    String text;
    double divider;

    if( size < FileUtil.ONE_KB )
    {
      text = "bytes";
      divider = 1.0;
    }
    else if( size < FileUtil.ONE_MB )
    {
      text = "KB";
      divider = FileUtil.ONE_KB;
    }
    else if( size < FileUtil.ONE_GB )
    {
      text = "MB";
      divider = FileUtil.ONE_MB;
    }
    else if( size < FileUtil.ONE_TB )
    {
      text = "GB";
      divider = FileUtil.ONE_GB;
    }
    else
    {
      text = "TB";
      divider = FileUtil.ONE_TB;
    }

    final double d = ( (double)size ) / divider;
    FileUtil.byteFormat.format( d, buf, new FieldPosition( 0 ) ).append( ' ' ).append( text );

    return buf.toString();
  }




  /**
   * Method formatSizeBytes
   *
   * @param number
   *
   * @return TODO Complete Documentation
   */
  public static String formatSizeBytes( final Number number )
  {
    return FileUtil.formatSizeBytes( number.doubleValue() );
  }




  /**
   * Interpret the filename as a file relative to the given file -
   * unless the filename already represents an absolute filename.
   *
   * @param baseDir The "reference" file for relative paths. This instance must
   * be an absolute file and must not contain &quot;./&quot; or &quot;../&quot;
   * or &quot;\&quot; instead of &quot;/&quot;. If it is null, this call is
   * equivalent to <code>new java.io.File(filename)</code>.
   *
   * @param filename a file name
   *
   * @return an absolute file that doesn't contain &quot;./&quot; or
   * &quot;../&quot; sequences and uses the correct separator for
   * the current platform.
   */
  public static File resolveFile( final File baseDir, String filename )
  {
    // If file is
    filename = filename.replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );

    // deal with absolute files
    final int colon = filename.indexOf( ":" );
    if( filename.startsWith( File.separator ) || ( colon > -1 ) )
    {
      return FileUtil.normalize( filename );
    }

    if( baseDir == null )
    {
      return new File( filename );
    }

    File helpFile = new File( baseDir.getAbsolutePath() );
    final StringTokenizer tok = new StringTokenizer( filename, File.separator );

    while( tok.hasMoreTokens() )
    {
      final String part = tok.nextToken();

      if( part.equals( ".." ) )
      {
        helpFile = helpFile.getParentFile();

        if( helpFile == null )
        {
          final String msg = "The file or path you specified (" + filename + ") is invalid relative to " + baseDir.getPath();

          throw new ChainedRuntimeException( msg );
        }
      }
      else if( part.equals( "." ) )
      {
        // Do nothing here
      }
      else
      {
        helpFile = new File( helpFile, part );
      }
    }

    return new File( helpFile.getAbsolutePath() );
  }




  /**
   * Return the age of the given file or directory based on time last modified.
   *
   * <p>If the file is a directory, then the returned age will be the latest
   * modified time of all its childern. The reasoning is if a subdirectory has
   * a file that was last modified 10 seconds ago, then the parent directory
   * has been logically modified (if only through its path) the same 10 seconds
   * ago.</p>
   *
   * <p>There is no way to tell if a file was recently modified and then
   * deleted from the directory unless the underlying operating system records
   * the activity in the last modified attribute of the directory entry.</p>
   *
   * @param file The file to query.
   *
   * @return The epoch time in milliseconds the file, or one of its children was
   *         last accessed, or -1 if the file does not exist ir is null.
   */
  public static long getFileAge( final File file )
  {
    if( ( file != null ) && file.exists() )
    {
      if( file.isDirectory() )
      {
        long lastModified = file.lastModified();

        final String[] paths = file.list();

        if( paths != null )
        {
          for( int i = 0; i < paths.length; i++ )
          {
            final File fil = new File( file, paths[i] );
            final long age = FileUtil.getFileAge( fil );

            if( age > lastModified )
            {
              lastModified = age;
            }
          } // for each path in directory

        } // dir not empty

        return lastModified;
      } // is dir

      return file.lastModified();
    }

    return -1L;
  }




  public static String[] textToArray( final File file )
  {
    final ArrayList array = new ArrayList();
    FileInputStream fin = null;
    String line;
    try
    {
      fin = new FileInputStream( file );
      final BufferedReader myInput = new BufferedReader( new InputStreamReader( fin ) );
      while( ( line = myInput.readLine() ) != null )
      {
        array.add( line );
      }
    }
    catch( final Exception e )
    {
      e.printStackTrace();
    }
    finally
    {
      try
      {
        fin.close();
      }
      catch( final Exception e )
      {
      }
    }

    final String[] retval = new String[array.size()];
    for( int x = 0; x < retval.length; retval[x] = (String)array.get( x++ ) )
    {
      ;
    }

    return retval;
  }
}
