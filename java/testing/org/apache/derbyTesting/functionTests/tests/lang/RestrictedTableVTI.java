/*

   Derby - Class org.apache.derbyTesting.functionTests.tests.lang.RestrictedTableVTI

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package org.apache.derbyTesting.functionTests.tests.lang;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.derby.vti.VTITemplate;
import org.apache.derby.vti.RestrictedVTI;
import org.apache.derby.vti.Restriction;

/**
 * <p>
 * This class contains a table function which can be used to read data
 * from a Derby table.
 * </p>
 */
public	class   RestrictedTableVTI extends VTITemplate implements  RestrictedVTI
{
    ////////////////////////////////////////////////////////////////////////
    //
    //	CONSTANTS
    //
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    //
    //	STATE
    //
    ////////////////////////////////////////////////////////////////////////

    private String  _schemaName;
    private String  _tableName;
    private Connection _connection;

    private String[]    _columnNames;
    private Restriction _restriction;

    // this maps table function columns (0-based) to table column numbers (1-based) in
    // the actual query
    private int[]               _columnNumberMap;
    private PreparedStatement   _preparedStatement;
    private ResultSet           _resultSet;

    private static  String  _lastQuery;

    ////////////////////////////////////////////////////////////////////////
    //
    //	CONSTRUCTOR
    //
    ////////////////////////////////////////////////////////////////////////

    protected  RestrictedTableVTI
        (
         String schemaName,
         String tableName
         )
        throws Exception
    {
        _schemaName = schemaName;
        _tableName = tableName;
        _connection = getDerbyConnection();
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //	TABLE FUNCTIONS
    //
    ////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Table function to read a table in Derby.
     * </p>
     */
    public  static  RestrictedTableVTI readTable
        (
         String schemaName,
         String tableName
         )
        throws Exception
    {
        return new RestrictedTableVTI( schemaName, tableName );
    }

    /**
     * <p>
     * Scalar function to retrieve the last query generated by this machinery.
     * </p>
     */
    public  static  String  getLastQuery() { return _lastQuery; }
    

    ////////////////////////////////////////////////////////////////////////
    //
    //	ResultSet BEHAVIOR
    //
    ////////////////////////////////////////////////////////////////////////

    public  void    close() throws SQLException
    {
        if ( !isClosed() )
        {
            _schemaName = null;
            _tableName = null;
            _connection = null;
            _columnNames = null;
            _restriction = null;
            _columnNumberMap = null;

            if ( _resultSet != null ) { _resultSet.close(); }
            if ( _preparedStatement != null ) { _preparedStatement.close(); }

            _resultSet = null;
            _preparedStatement = null;
        }
    }

    public  boolean next()  throws SQLException
    {
        if ( !isClosed() && (_resultSet == null) )
        {
            _preparedStatement = prepareStatement( _connection, makeQuery() );
            _resultSet = _preparedStatement.executeQuery();
        }

        return _resultSet.next();
    }

    public boolean isClosed() { return (_connection == null); }

    public  boolean wasNull()   throws SQLException
    { return _resultSet.wasNull(); }

    public  ResultSetMetaData   getMetaData()   throws SQLException
    { return _resultSet.getMetaData(); }

    public  InputStream 	getAsciiStream(int i) throws SQLException
    { return _resultSet.getAsciiStream( mapColumnNumber( i ) ); }
    
    public  BigDecimal 	getBigDecimal(int i) throws SQLException
    { return _resultSet.getBigDecimal( mapColumnNumber( i ) ); }
    
    public  BigDecimal 	getBigDecimal(int i, int scale) throws SQLException
    { return _resultSet.getBigDecimal( mapColumnNumber( i ), scale ); }
    
    public  InputStream 	getBinaryStream(int i)  throws SQLException
    { return _resultSet.getBinaryStream( mapColumnNumber( i ) ); }
    
    public  Blob 	getBlob(int i)  throws SQLException
    { return _resultSet.getBlob( mapColumnNumber( i ) ); }
    
    public  boolean 	getBoolean(int i) throws SQLException
    { return _resultSet.getBoolean( mapColumnNumber( i ) ); }
    
    public  byte 	getByte(int i)    throws SQLException
    { return _resultSet.getByte( mapColumnNumber( i ) ); }
    
    public  byte[] 	getBytes(int i) throws SQLException
    { return _resultSet.getBytes( mapColumnNumber( i ) ); }
    
    public  Reader 	getCharacterStream(int i) throws SQLException
    { return _resultSet.getCharacterStream( mapColumnNumber( i ) ); }

    public  Clob 	getClob(int i)  throws SQLException
    { return _resultSet.getClob( mapColumnNumber( i ) ); }

    public  Date 	getDate(int i)  throws SQLException
    { return _resultSet.getDate( mapColumnNumber( i ) ); }

    public  Date 	getDate(int i, Calendar cal)    throws SQLException
    { return _resultSet.getDate( mapColumnNumber( i ), cal ); }

    public  double 	getDouble(int i)    throws SQLException
    { return _resultSet.getDouble( mapColumnNumber( i ) ); }

    public  float 	getFloat(int i) throws SQLException
    { return _resultSet.getFloat( mapColumnNumber( i ) ); }

    public  int 	getInt(int i)   throws SQLException
    { return _resultSet.getInt( mapColumnNumber( i ) ); }

    public  long 	getLong(int i)  throws SQLException
    { return _resultSet.getLong( mapColumnNumber( i ) ); }

    public  Object 	getObject(int i)    throws SQLException
    { return _resultSet.getObject( mapColumnNumber( i ) ); }

    public  short 	getShort(int i) throws SQLException
    { return _resultSet.getShort( mapColumnNumber( i ) ); }

    public  String 	getString(int i)    throws SQLException
    { return _resultSet.getString( mapColumnNumber( i ) ); }

    public  Time 	getTime(int i)  throws SQLException
    { return _resultSet.getTime( mapColumnNumber( i ) ); }

    public  Time 	getTime(int i, Calendar cal)    throws SQLException
    { return _resultSet.getTime( mapColumnNumber( i ), cal ); }

    public  Timestamp 	getTimestamp(int i) throws SQLException
    { return _resultSet.getTimestamp( mapColumnNumber( i ) ); }

    public  Timestamp 	getTimestamp(int i, Calendar cal)   throws SQLException
    { return _resultSet.getTimestamp( mapColumnNumber( i ), cal ); }

    ////////////////////////////////////////////////////////////////////////
    //
    //	RestrictedVTI BEHAVIOR
    //
    ////////////////////////////////////////////////////////////////////////

    public  void    initScan
        ( String[] columnNames, Restriction restriction )
        throws SQLException
    {
        _columnNames = columnNames;
        _restriction = restriction;

        int columnCount = _columnNames.length;

        _columnNumberMap = new int[ columnCount ];
        int foreignColumnID = 1;
        for ( int i = 0; i < columnCount; i++ )
        {
            if ( columnNames[ i ] != null ) { _columnNumberMap[ i ] = foreignColumnID++; }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //	Connection MANAGEMENT
    //
    ////////////////////////////////////////////////////////////////////////

    private static  Connection  getDerbyConnection() throws SQLException
    {
        return DriverManager.getConnection( "jdbc:default:connection" );
    }



    ////////////////////////////////////////////////////////////////////////
    //
    //	QUERY FACTORY
    //
    ////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Build the query which will be sent to the nested connection.
     * </p>
     */
    private String  makeQuery()
    {
        StringBuffer   buffer = new StringBuffer();

        buffer.append( "select " );

        int possibleCount = _columnNames.length;
        int actualCount = 0;
        for ( int i = 0; i < possibleCount; i++ )
        {
            String  rawName = _columnNames[ i ];
            if ( rawName == null ) { continue; }

            if ( actualCount > 0 ) { buffer.append( ", " ); }
            actualCount++;
            
            buffer.append( doubleQuote( rawName ) );
        }

        buffer.append( "\nfrom " );
        buffer.append( doubleQuote( _schemaName ) );
        buffer.append( '.' );
        buffer.append( doubleQuote( _tableName ) );

        if ( _restriction != null )
        {
            String  clause = _restriction.toSQL();

            if (clause != null)
            {
                clause = clause.trim();
                if ( clause.length() != 0 )
                {
                    buffer.append( "\nwhere " + clause );
                }
            }
        }

        _lastQuery = buffer.toString();

        return _lastQuery;
    }

    private static  String  doubleQuote( String text )  { return '"' + text + '"'; }
    private static  String  singleQuote( String text )  { return '\'' + text + '\''; }

    private static  PreparedStatement   prepareStatement
        ( Connection conn, String text )
        throws SQLException
    {
        return conn.prepareStatement( text );
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //	UTILITY METHODS
    //
    ////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Map a 1-based Derby column number to a 1-based column number in the
     * query.
     * </p>
     */
    private int mapColumnNumber( int derbyNumber )
    {
        return _columnNumberMap[ derbyNumber - 1 ];
    }
}
