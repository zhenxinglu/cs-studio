
/* 
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron, 
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND 
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR 
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. 
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, 
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION, 
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY 
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 *
 */

package org.csstudio.alarm.jms2ora;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.csstudio.alarm.jms2ora.database.OracleService;
import org.csstudio.alarm.jms2ora.util.MessageReceiver;
import org.csstudio.alarm.jms2ora.util.WaifFileHandler;
import de.desy.epics.singleton.EpicsSingleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <code>StoreMessages</code> gets all messages from the topics <b>ALARM and LOG</b> and stores them into the
 * database.
 * 
 * Steps:
 * 
 * 1. Read all message type and message properties from the database and store them into two hash tables
 * 2. Create a receiver and set the asynchronous message receiving.
 * 3. Wait for messages
 * 4. If a message is received, process it. If the processing fails, store the message in a file.
 * 
 * Message processing:
 * 
 * 1. Check for the TYPE and EVENTTIME properties
 *    Set the type to 'unknown', if the message does not contain the property TYPE.
 *    Set the property EVENTTIME to the current date, if the message does not contain it.
 * 2. Create a hash table with the following entries:
 *    Long   - The key (ID from MSG_PROPERTY_TYPE)
 *    String - The value (value from the received map message)
 * 3. Get the ID of the message type or set it to 'unknown'
 * 4. Create an entry in the table MESSAGE and return the ID for the entry
 * 5. With the ID from step 4 create the entries in the table MESSAGE_CONTENT
 * 6. If the last step fails, delete all created entries in MESSAGE and MESSAGE_CONTENT
 * 
 * @author  Markus Moeller
 * @version 1.2.2
 */

/*
 * TODO:    Auslagern von bestimmten Funktionen in eigenst�ndige Klassen
 *          - Die Properties der Datenbanktabellen
 */

public class StoreMessages implements MessageListener
{
    /** The object instance of this class */
    private static StoreMessages instance = null;
    
    /** Queue for received messages */
    private ConcurrentLinkedQueue<MapMessage> messages = new ConcurrentLinkedQueue<MapMessage>();
    
    /** Object for database handling */
    private OracleService oracle = null;
    
    /** Array of message receivers */
    private MessageReceiver[] receivers = null;
    
    /** Hashtable with message types. Key -> name, value -> database table id */
    private Hashtable<String, Long> msgType = null;
    
    /** Hashtable with message properties. Key -> name, value -> database table id  */
    private Hashtable<String, Long> msgProperty = null;

    /** Reads and holds the configuration stored in the confid file */
    private PropertiesConfiguration config = null;
    
    /** The logger */
    private Logger logger = null;
    
    /** Array with JMS server URLs */
    private String[] urlList = null;
    
    /** Array with topic names */
    private String[] topicList = null;
    
    /** Date object that indicates the last update of the quota record */
    private Date lastQuotaUpdate = null;
    
    /** Number of stored message files */
    private int fileNumber = 0;
    
    /** Indicates if the application was initialized or not */
    private boolean initialized = false;
    
    /** Indicates wether or not the application should shut down */
    private boolean running = true;

    private final String version = " 1.2.2";
    private final String build = " - BUILD 2008-04-04 13:34";
    private final String application = "Jms2Ora";
    private final String formatStd = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}";
    private final String formatTwoDigits = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{2}";
    private final String formatOneDigit = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1}";
    
    public final long RET_ERROR = -1;
    public static final int CONSOLE = 1;
    
    public final int PM_RETURN_OK = 0;
    public final int PM_RETURN_DISCARD = 1;
    public final int PM_ERROR_DB = 2;
    public final int PM_ERROR_JMS = 3;
    public final int PM_ERROR_GENERAL = 4;
    
    public final String[] infoText = { "Message is written into the database.",
                                       "Message was discarded.",
                                       "Database error",
                                       "JMS error",
                                       "General error"};
    
    /**
     * A nice private constructor...
     *
     */
    
    private StoreMessages()
    {
        int i;
        
        createLogger();

        checkObjectFolder();
        
        config = Jms2OraPlugin.getDefault().getConfiguration();
        
        oracle = new OracleService(logger, config.getString("oracle.user"), config.getString("oracle.password"));
        initialized = readMessageTypeAndProperties();
        
        lastQuotaUpdate = Calendar.getInstance().getTime();
        
        if(config.containsKey("provider.url") && config.containsKey("topic.names"))
        {
            urlList = config.getStringArray("provider.url");
            topicList = config.getStringArray("topic.names");
            
            for(i = 0;i < urlList.length;i++)
            {
                logger.info("[" + urlList[i] + "]");
            }
            
            for(i = 0;i < topicList.length;i++)
            {
                logger.info("[" + topicList[i] + "]");
            }
            
            receivers = new MessageReceiver[urlList.length];
            
            for(i = 0;i < urlList.length;i++)
            {
                try
                {
                    receivers[i] = new MessageReceiver("org.apache.activemq.jndi.ActiveMQInitialContextFactory", urlList[i], topicList);
                    
                    receivers[i].startListener(this, application);
                    
                    initialized = (initialized == true)? true : false;
                }
                catch(Exception e)
                {
                    logger.info("*** Exception *** : " + e.getMessage());
                    
                    initialized = false;
                }
            }
            
            initialized = (initialized == true) ? true : false;
        }
        else
        {
            initialized = false;
        }
        

    }
    
    public static synchronized StoreMessages getInstance()
    {
        if(instance == null)
        {
            instance = new StoreMessages();
        }
        
        return instance;
    }
    
    /**
     * <code>executeMe</code> is the main method of the class StoreMessages.
     *
     */
    
    public void executeMe()
    {
        MapMessage mapMessage  = null;
        int result;
        
        logger.info("Started" + application + version + build);
        
        while(running)
        {
            while(!messages.isEmpty() && running)
            {
                mapMessage = messages.poll();
                
                /*
                try
                {
                    logger.info("Received a map message: " + mapMessage.getJMSDestination().toString());
                }
                catch(JMSException e)
                {
                    logger.info(e.getMessage());
                }
                */
                
                result = processMessage(mapMessage);
                if((result != PM_RETURN_OK) && (result != PM_RETURN_DISCARD))
                {                    
                    // Store the message in a file, if it was not possible to write it to the DB.
                    writeMapMessageToFile(mapMessage);
                    
                    logger.warn(infoText[result] + ": Could not store the message in the database. Message is written on disk.");
                }
                else
                {
                    logger.info(infoText[result]);                        
                }
            }

            if(running)
            {
                logger.info("Waiting for messages...");
                
                synchronized(this)
                {
                    try
                    {
                        wait();                    
                    }
                    catch(InterruptedException ie)
                    {
                        logger.error("*** InterruptedException *** : executeMe() : wait() : " + ie.getMessage());
                    
                        running = false;
                    }               
                }
            }
        }
        
        // Process the remaining messages
        logger.info("Remaining messages: " + messages.size() + " -> Processing...");
        
        int writtenToDb = 0;
        int writtenToHd = 0;
        
        while(!messages.isEmpty())
        {
            mapMessage = messages.poll();
            result = processMessage(mapMessage);
            if((result != PM_RETURN_OK) && (result != PM_RETURN_DISCARD))
            {                    
                // Store the message in a file, if it was not possible to write it to the DB.
                writeMapMessageToFile(mapMessage);
                
                writtenToHd++;
            }
            else
            {
                writtenToDb++;
            }
            
            mapMessage = null;
        }
        
        logger.info("Remaining messages stored in the database: " + writtenToDb);
        logger.info("Remaining messages stored on disk:         " + writtenToHd);

        logger.info("executeMe() : ** DONE **");
    }

    public void onMessage(Message message)
    {
        if(message instanceof MapMessage)
        {
            messages.add((MapMessage)message);
        
            synchronized(this)
            {
                notify();
            }
        }
        else
        {
            logger.info("Received a non MapMessage object. Discarded...");
        }        
    }

    public int processMessage(MapMessage mmsg)
    {
        Hashtable<Long, String> msgContent  = null;
        Date currentDate = null;
        Enumeration<?> lst = null;
        String[] recordNames = null;
        String name = null;
        String type = null;
        String et = null;
        String temp = null;
        long typeId = 0;
        long msgId = 0;
        int result = PM_RETURN_OK;
        boolean reload = false;
        
        // ACHTUNG: Die Message ist f�r den Client READ-ONLY!!! Jeder Versuch in die Message zu schreiben
        //          l�st eine Exception aus.
                
        try
        {
            // Does the message contain the key TYPE?
            if(mmsg.itemExists("TYPE"))
            {
                // Get the value of the item TYPE
                type = mmsg.getString("TYPE").toLowerCase();                
            }
            else
            {
                // The message does not contain the item TYPE. We set it to UNKNOWN
                type = "unknown";                
            }
            
            // Discard messages with the type 'simulator'
            if(type.compareToIgnoreCase("simulator") == 0)
            {
                return PM_RETURN_DISCARD;
            }
            
            // Does the message contain the key EVENTTIME?
            if(mmsg.itemExists("EVENTTIME"))
            {
                // Yes. Get it
                et = mmsg.getString("EVENTTIME");
                
                // Check the date format
                temp = checkDateString(et);
                
                // If there is something wrong with the format...
                if(temp == null)
                {
                    logger.info("Property EVENTTIME contains invalid format: " + et);
                    
                    // ... create a new date string
                    et = getDateAndTimeString("yyyy-MM-dd HH:mm:ss.SSS");
                }
                else
                {
                    // ... otherwise 'temp' contains a valid date string
                    et = temp;
                }
            }
            else
            {
                // Get the current date and time
                // Format: 2006.07.26 12:49:12.345
                et = getDateAndTimeString("yyyy-MM-dd HH:mm:ss.SSS");
            }
            
            // Create a new hash table for the content of the message
            msgContent = new Hashtable<Long, String>();
            
            // Copy the type and the event time
            msgContent.put(msgProperty.get("TYPE"), type);
            msgContent.put(msgProperty.get("EVENTTIME"), et);
                        
            // Copy the content of the message into the hash table
            lst = mmsg.getMapNames();
            while(lst.hasMoreElements())
            {
                name = (String)lst.nextElement();
                
                // Do not copy the TYPE and EVENTTIME properties
                if((name.compareTo("TYPE") != 0) && (name.compareTo("EVENTTIME") != 0))
                {
                    // If we know the property
                    if(msgProperty.containsKey(name))
                    {
                        // Get the ID of the property and store it into the hash table
                        msgContent.put(msgProperty.get(name), mmsg.getString(name));
                    }
                    else
                    {
                        // Reload the tables if they are not reloaded
                        if(!reload)
                        {
                            readMessageTypeAndProperties();
                            reload = true;
                            
                            // Check again
                            if(msgProperty.containsKey(name))
                            {
                                msgContent.put(msgProperty.get(name), mmsg.getString(name));
                            }
                            else
                            {
                                msgContent.put(msgProperty.get("UNKNOWN"), "[" + name + "]:" + mmsg.getString(name));
                            }
                        }
                        else
                        {
                            msgContent.put(msgProperty.get("UNKNOWN"), "[" + name + "]:" + mmsg.getString(name));
                        }
                    }                    
                }
            }
        }
        catch(JMSException jmse)
        {
            msgContent = null;
            
            jmse.printStackTrace();
            
            return PM_ERROR_JMS;
        }        
         
        // Get the ID of the message type
        // If the type is not valid return the ID for 'unknown'
        typeId = getMessageTypeId(type);
        if(typeId == RET_ERROR)
        {
            logger.error("getMessageTypeId()");
            
            // Return value is -1, so there was an error
            return PM_ERROR_GENERAL;
        }
        
        // Create an entry in the table MESSAGE
        msgId = oracle.createMessageEntry(typeId, et);
        if(msgId == RET_ERROR)
        {
            logger.error("createMessageEntry()");
            
            return PM_ERROR_DB;
        }
        
        if(oracle.createMessageContentEntries(msgId, msgContent) == false)
        {
            logger.error("createMessageContentEntries()");
            
            oracle.deleteMessage(msgId);
            
            result = PM_ERROR_DB;
        }
        else
        {
            currentDate = Calendar.getInstance().getTime();
            
            // Wait min. 5 minutes for updating the EPICS record
            if((currentDate.getTime() - lastQuotaUpdate.getTime()) >= 300000)
            {
                if(config.containsKey("record.tablequota"))
                {
                    recordNames = config.getStringArray("record.tablequota");
                    for(int i = 0;i < recordNames.length;i++)
                    {
                        temp = createDatabaseNameFromRecord(recordNames[i]);
                        
                        if(temp.compareToIgnoreCase(config.getString("oracle.user")) != 0)
                        {
                            updateDBRecord(recordNames[i], temp, temp.toLowerCase());
                        }
                        else
                        {
                            updateDBRecord(recordNames[i]);
                        }
                    }
                }
                else
                {
                    logger.warn("No EPICS record name for the database quota is defined.");
                }
                
                // Always get the quota for user KRYKLOG
                // NOT USED: The MAX_BYTES value seemed always to be -1 (unlimited)
                // updateDBRecord("krykLog:UsedQuota_ai", "KRYKLOG", "kryklog");
                
                lastQuotaUpdate = currentDate;
                currentDate = null;
            }
            
            result = PM_RETURN_OK;
        }
        
        return result;
    }
    
    /**
     * Writes the new ORACLE table space quota to the record.
     *  
     *  @param name Record name
     */
    public void updateDBRecord(String name)
    {
        int quota = oracle.getUsedQuota();

        EpicsSingleton.getInstance().setValue(name, String.valueOf(quota));
        String record = EpicsSingleton.getInstance().get(name);
        logger.info(name + " quota: " + record);    
    }
    
    /**
     * Writes the new ORACLE table space quota to the record.
     * 
     * Remark: In fact, it is not the best place to update this record in Jms2Ora.
     * 
     *  @param recordName Record name
     *  @param dbUser Oracle user name
     *  @param dbPassword Oracle password name
     */
    public void updateDBRecord(String recordName, String dbUser, String dbPassword)
    {
        int quota = OracleService.getUsedQuota(logger, dbUser, dbPassword);

        EpicsSingleton.getInstance().setValue(recordName, String.valueOf(quota));
        String record = EpicsSingleton.getInstance().get(recordName);
        logger.info(recordName + " quota: " + record);    
    }

    public String createDatabaseNameFromRecord(String record)
    {
        String result = null;
        
        if(record.indexOf(':') != -1)
        {
            result = record.substring(0, record.indexOf(':')).toUpperCase();
        }
        
        return result;
    }
    
    /**
     * <code>writeMapMessageToFile</code> writes a map message object to disk.
     * 
     * @param mm - The MapMessage object which have to be stored on disk.
     */

    public void writeMapMessageToFile(MapMessage mm)
    {
        SimpleDateFormat    dfm         = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
        GregorianCalendar   cal         = null;
        FileOutputStream    fos         = null;
        ObjectOutputStream  oos         = null;
        String              fn          = null;

        cal = new GregorianCalendar();
        fn  = "waif_" + dfm.format(cal.getTime());                

        try
        {
            fos = new FileOutputStream(".\\nirvana\\" + fn + ".ser");
            oos = new ObjectOutputStream(fos);
            
            oos.writeObject(mm);            
        }
        catch(FileNotFoundException fnfe)
        {
            logger.error("FileNotFoundException : " + fnfe.getMessage());
        }
        catch(IOException ioe)
        {
            logger.error("IOException : " + ioe.getMessage());
        }
        finally
        {
            if(oos != null){try{oos.close();}catch(IOException ioe){}}
            if(fos != null){try{fos.close();}catch(IOException ioe){}}
            
            oos = null;
            fos = null;
        }
    }
    
    public long getMessageTypeId(String type)
    {
        long    result = 0;
        
        // Does the table contain the name of the type?
        if(msgType.containsKey(type))
        {
            // Yes, get it 
            result = msgType.get(type);
        }
        else // No, we have to read the tables again
        {
            // Read again the message types and properties
            if(readMessageTypeAndProperties())
            {
                // Do we now have a valid ID?
                if(msgType.containsKey(type))
                {
                    result = msgType.get(type);
                }
                else
                {
                    // No, so we set the type to unknown
                    result = msgType.get("unknown");
                }
            }
            else
            {
                result = -1;
            }
        }
        
        return result;
    }
    
    /**
     * <code>isInitialized</code>
     * 
     * @return true, if the initialization was successfull ; false, if it was not
     */
    
    public boolean isInitialized()
    {
        return initialized;
    }

    /**
     *  The method <code>readMessageTypeAndProperties()</code> fills two hash tables with the information from
     *  the database tables MSG_TYPE and MSG_PROPERTY_TYPE.
     *  
     *  @return true/false
     */
    
    private boolean readMessageTypeAndProperties()
    {
        ResultSet               rsType      = null;
        ResultSet               rsProperty  = null;
        
        // Delete old hash tables, if there are any
        msgType     = null;
        msgProperty = null;

        // Connect the database
        if(oracle.connect() == false)
        {
            return false;
        }

        // Execute the first query
        rsType      = oracle.executeSQLQuery("SELECT * from MSG_TYPE");
        
        // Execute the second query
        rsProperty  = oracle.executeSQLQuery("SELECT * from MSG_PROPERTY_TYPE");
        
        // Check the result sets 
        if((rsType == null) || (rsProperty == null))
        {           
            try
            {
                if(rsType != null)  rsType.close();
                rsType = null;
            }
            catch(SQLException sqle)
            {
                rsType = null;
            }
            
            try
            {
                if(rsProperty != null) rsProperty.close();
                rsProperty = null;
            }
            catch(SQLException sqle)
            {
                rsProperty = null;
            }
            
            return false;
        }
        
        msgType     = new Hashtable<String, Long>();
        msgProperty = new Hashtable<String, Long>();
        
        // Fill the hash tables with the received data of the tables
        try
        {
            while(rsType.next())
            {
                msgType.put(rsType.getString(2), rsType.getLong(1)); 
            }
            
            rsType.close();
            rsType = null;
            
            while(rsProperty.next())
            {
                msgProperty.put(rsProperty.getString(2), rsProperty.getLong(1));                 
            }

            rsProperty.close();
            rsProperty = null;
        }
        catch(SQLException sqle)
        {
            if(oracle.isConnected() == true)
            {
                oracle.close();
            }
            
            return false;            
        }
        
        // Close the database
        if(oracle.isConnected() == true)
        {
            oracle.close();
        }                

        return true;
    }

    public String getDateAndTimeString()
    {
        return getDateAndTimeString("[yyyy-MM-dd HH:mm:ss] ");
    }
    
    public String getDateAndTimeString(String frm)
    {
        SimpleDateFormat    sdf = new SimpleDateFormat(frm);
        GregorianCalendar   cal = new GregorianCalendar();
        
        return sdf.format(cal.getTime());
    }

    /**
     * Converts the time and date string using only one or two digits for the mili seconds
     * to a string using 3 digits for the mili seconds with leading zeros
     * 
     * @param dateString The date string that has to be converted
     * @param sourceFormat The format of the converted string
     * @param destinationFormat The format of the result string
     * @throws ParseException If the date string does not match the source format
     * @return The converted date and time string
     */
    public String convertDateString(String dateString, String sourceFormat, String destinationFormat) throws ParseException
    {
        SimpleDateFormat ssdf = new SimpleDateFormat(sourceFormat);
        SimpleDateFormat dsdf = new SimpleDateFormat(destinationFormat);
        String ds = null;
        
        try
        {
            Date date = ssdf.parse(dateString);
            
            ds = dsdf.format(date);
        }
        catch(ParseException pe)
        {
            throw new ParseException("String [" + dateString + "] is invalid: " + pe.getMessage(), pe.getErrorOffset());
        }
        
        return ds;
    }
    
    /**
     * Checks wether or not the date and time string uses the standard format yyyy-MM-dd HH:mm:ss.SSS
     * 
     * @param dateString
     * @return The date and time string that uses the standard format.
     */
    public String checkDateString(String dateString)
    {
        String r = null;
        
        if(dateString.matches(formatStd))
        {
            r = dateString;
        }
        else if(dateString.matches(formatTwoDigits))
        {
            try
            {
                r = convertDateString(dateString, "yyyy-MM-dd HH:mm:ss.SS", "yyyy-MM-dd HH:mm:ss.SSS");
            }
            catch(ParseException pe)
            {
                r = null;
            }

        }
        else if(dateString.matches(formatOneDigit))
        {
            try
            {
                r = convertDateString(dateString, "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd HH:mm:ss.SSS");
            }
            catch(ParseException pe)
            {
                r = null;
            }            
        }
        
        return r;
    }
    
    /**
     * The method returns the number of MapMessage objects which were serialized. Uses the {@link WaifFileHandler}
     * class.
     * 
     * @return Number of MapMessages objects which were written to the database because of errors during
     *         processing the objects.
     */
    
    public int getNumberOfWaifFiles()
    {
        WaifFileHandler waifFiles   = new WaifFileHandler();
        int             result      = 0;

        result = waifFiles.getNumberOfWaifFiles();
        
        waifFiles = null;
        
        return result;
    }
    
    /**
     * The method returns an array of String with the file names of all serialized MapMessage objects.
     * Uses the {@link WaifFileHandler} class.
     * 
     * @return Array of String with the file names.
     */
    
    public String[] getNameOfWaifFiles()
    {
        WaifFileHandler waifFiles   = new WaifFileHandler();
        String[]        result      = null;

        result = waifFiles.getWaifFileNames();
        
        waifFiles = null;
        
        return result;
    }

    public String[] getWaifFileContent()
    {
        MapMessage  msg     = null;
        String[]    result  = null;
        String      name    = null;
        
        WaifFileHandler waifFiles   = new WaifFileHandler();

        msg = waifFiles.getWaifFileContent(fileNumber);

        waifFiles = null;
        
        if(msg != null)
        {
            try
            {
                Enumeration<?> lst = msg. getMapNames();
             
                int count = 0;
                
                result = new String[32];
                
                while(lst.hasMoreElements())
                {
                    name = (String)lst.nextElement();
                    
                    result[count++] = name + "=" + msg.getString(name);
                }
            }
            catch (JMSException e)
            {
                result = null;
            }
        }
        
        return result;
    }
    
    /**
     * 
     * @return The number of files that could not be deleted.
     */
    
    public int deleteAllObjectFiles()
    {
        int result = 0;
        
        WaifFileHandler waifFiles   = new WaifFileHandler();

        result = waifFiles.deleteAllFiles();
        
        waifFiles = null;
        
        return result;
    }
    
    public int getFileNumber()
    {
        return fileNumber;
    }
    
    public void setFileNumber(int number)
    {
        fileNumber = number;
    }
    
    public int getNumberOfQueuedMessages()
    {
        if(messages != null)
        {
            return messages.size();
        }
        else
        {
            return 0;
        }
    }
    
    public void closeAllReceivers()
    {
        logger.info("closeAllReceivers(): Closing all receivers.");
        
        if(receivers != null)
        {
            for(int i = 0;i < receivers.length;i++)
            {
                receivers[i].stopListening();
            }
        }
    }
    
    public void shutdown()
    {
        running = false;
        
        logger.info("The application will shutdown now...");
        
        // Very important to close all receivers and connections!!!!!
        closeAllReceivers();
        
        synchronized(this)
        {
            notify();
        }
    }

    // Not used for the Jms2Ora headless eclipse application
    public boolean restart()
    {
        boolean result = false;
        
        running  = false;
        
        // Very important to close all receivers and connections!!!!!
        closeAllReceivers();
        
        ProcessBuilder pb = new ProcessBuilder("java.exe", "-Dcom.sun.management.jmxremote", "-Dcom.sun.management.jmxremote.port=8101", "-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.ssl=false", "-jar", "JMS2Ora.jar");

        pb.directory(new File(System.getProperty("user.dir")));
        
        try
        {
             pb.start();
             
             result = true;
        }
        catch(IOException ioe)
        {
            logger.info("ProcessRestart : restart() :\n   *** IOException *** : " + ioe.getMessage());
            
            result = false;
        }
        
        logger.info("Restarting me...");
        
        synchronized(this)
        {
            notify();
        }

        return result;
    }
    
    private void checkObjectFolder()
    {
        File folder = new File(".\\nirvana\\");
        
        if(!folder.exists())
        {
            boolean result = folder.mkdir();
            if(result)
            {
                logger.info("Folder 'nirvana' was created.");
            }
            else
            {
                logger.warn("Folder 'nirvana' was NOT created.");
            }
        }
    }
    
    private boolean createLogger()
    {
        boolean result = false;
        
        logger = Logger.getRootLogger();
        
        PatternLayout layout = new PatternLayout("%d{ISO8601} %-5p [%t] %c: %m%n");
        
        try
        {
            RollingFileAppender fileAppender = new RollingFileAppender(layout, "log/" + application + ".log", true);
            fileAppender.setMaxBackupIndex(100);
            fileAppender.setMaxFileSize("1024KB");
            logger.addAppender(fileAppender);
                        
            result = true;
        }
        catch(IOException ioe)
        {
            result = false;
        }
        
        return result;
    }
    
    public Logger getLogger()
    {
        return logger;
    }    
}
