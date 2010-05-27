/*
 *      Copyright (c) 2004-2009 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */

package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Code taken from YAMJ: http://code.google.com/p/moviejukebox/
 * @author altman.matthew
 */
public class PropertiesUtil {

    private static final String PROPERTIES_CHARSET = "UTF-8";
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Properties props = new Properties();
    public static final String PROPERTIES = "subs4me.properties";

    public static boolean setPropertiesStreamName(String streamName) {
        logger.fine("Using properties file " + streamName);
        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(streamName);

        try {
            if (propertiesStream == null) {
                propertiesStream = new FileInputStream(streamName);
            }

            Reader reader = new InputStreamReader(propertiesStream, PROPERTIES_CHARSET);
            props.load(reader);

        } catch (IOException error) {
            // Output a warning if moviejukebox.properties isn't found. Otherwise it's an error
            if (streamName.contains("subs4me.properties")) {
                logger.warning("Warning (non-fatal): User properties file: subs4me.properties, not found.");
            } else {
                logger.severe("Failed loading file " + streamName + ": Please check your configuration. The properties file should be in the classpath.\n" +
                		"using default properties");
            }
//            return false;
        }
        return true;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(
                key, defaultValue);
    }

    // Issue 309
    public static Set<Entry<Object, Object>> getEntrySet() {
        // Issue 728
        // Shamelessly adapted from: http://stackoverflow.com/questions/54295/how-to-write-java-util-properties-to-xml-with-sorted-keys
        return new TreeMap<Object, Object>(props).entrySet();
    }
    
    public static void setProperty(String key, String value) {
        props.setProperty(key, value);
    }
    
    public static void updatePropertyToDisk(String key, String value) 
    {
        File f = new File("./" + PROPERTIES);
        if (!f.exists())
        {
            try
            {
                f.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
//        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream("./" + PROPERTIES);
        Reader reader;
        try
        {
            reader = new FileReader(f);
            Properties tempProps = new Properties();
            tempProps.load(reader);
            tempProps.setProperty(key, value);
            tempProps.store(new FileOutputStream("./" + PROPERTIES), null);
            reader.close();
        }
        catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
