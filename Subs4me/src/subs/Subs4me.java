package subs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import subs.providers.OpenSubs;
import subs.providers.Sratim;
import subs.providers.Subscene;
import subs.providers.Torec;
import utils.FileStruct;
import utils.PropertiesUtil;

public class Subs4me
{
    public static final String SRT_EXISTS = "/c";
    public static final String VERSION = "1.0";
    public static final String RECURSIVE_SEARCH = "/r";
    public static final String FULL_DOWNLOAD = "/all";
    public static final String PROVIDERS = "/p";
    public static final String GET_MOVIE_PIC = "/i";
    public static final String GRT_MOVIE_PIC_FORCED = "/if";
    public static final String DO_NOT_USE_OPENSUBS_FOR_FILE_REALIZATION = "/n";
    public static final String DO_WORK_EXT = ".run_HandleMultiplesubs";
    
    public static final String PROVIDERS_PROPERTY = "get_subs_providers";
    public static final String SUBS_CHECK_ALL_PROPERTY = "get_subs_check_exists";
    public static final String SUBS_RECURSIVE_PROPERTY = "get_subs_recursive";
    public static final String SUBS_GET_ALL_PROPERTY = "get_subs_all";
    public static final String SUBS_DOWNLOAD_PICTURE_PROPERTY = "get_subs_download_movie_picture";
    public static final String SUBS_DOWNLOAD_PICTURE_FORCE_PROPERTY = "get_subs_download_movie_picture_force";
    
    public LinkedList<String> oneSubsFound = new LinkedList<String>();
    public LinkedList<String> moreThanOneSubs = new LinkedList<String>();
    public LinkedList<String> noSubs = new LinkedList<String>();
    
    String srcDir = new String();
    // private String _group = "";
    public static boolean checkSrtExists = false;
    private static boolean recursive = false;
//    private static boolean intense = false;
    private static boolean fullDownload = false;
    private static boolean getMoviePic = false;
    private static boolean getMoviePicForce = false;
    
    public static String propertiesName = "./subs4me.properties";
    
    private static LinkedList<Provider> _availableProviders = new LinkedList<Provider>();
    
    public static LinkedList<Provider> _providers = null;
    
    private static Subs4me instance = new Subs4me();
    public static boolean noUseOpen;
    
    public static Subs4me getInstance()
    {
        return instance;
    }
    
    public Subs4me()
    {
//        System.out.println("Locale = " + Locale.getDefault());
//        Locale.setDefault(new Locale("en", "US"));
//        System.out.println("New Locale = " + Locale.getDefault());
    }

    /**
     * need to be recursive to process sub directories
     * 
     * @param src
     */
    public void startProcessingFiles(String src)
    {
        String[] sources = findFilesInDir(src);
        if (sources == null)
        {
            // this is a file and not a directory
            File fi = new File(src);
            doWork(fi);
        } else
        {
            for (int j = 0; j < sources.length; j++)
            {
                File fi = new File(src + File.separator + sources[j]);
                if (fi.isDirectory())
                {
                    startProcessingFiles(fi.getPath());
                } else
                {
                    doWork(fi);
                }
            }
        }
    }
    
    /**
     * iterate over providers to get the subs
     * @param fi file to work on
     * @return Providre values to help print the list of files to which we found subtitles
     */
    private void doWork(File fi)
    {
        int ret = Provider.not_found;
        FileStruct fs = new FileStruct(fi);
        for (Iterator iterator = _providers.iterator(); iterator.hasNext();)
        {
            Provider p = (Provider) iterator.next();
            try
            {
                int retTemp = p.doWork(fs);
                if (retTemp > ret)
                {
                    ret = retTemp;
                }
                if (retTemp == Provider.perfect)
                {
                    cleanup();
                }
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        if (ret == Provider.perfect)
        {
            oneSubsFound.add(fs.getSrcDir() + File.separator + fs.getFullFileName());
        }
        else if (ret == Provider.not_perfect)
        {
            moreThanOneSubs.add(fs.getSrcDir() + File.separator + fs.getFullFileName());
        }
        else if (ret == Provider.not_found)
        {
            noSubs.add(fs.getSrcDir() + File.separator + fs.getFullFileName());
        }
//        return ret;
    }
    
    private void cleanup(File f)
    {
        FileStruct fs = new FileStruct(f, false);
        String[] files = findFilesTocleanupInDir(fs);
        for (int i = 0; i < files.length; i++)
        {
            String delName = files[i];
            File del = new File(f.getParent(), delName);
            del.delete();
        }
    }
    
    private String[] findFilesTocleanupInDir(final FileStruct fs)
    {
        File dir = new File(fs.getFile().getParent());
        if (dir.isDirectory())
        {
            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    if (name.equals(fs.getNameNoExt() + Subs4me.DO_WORK_EXT))
                    {
                        return true;
                    }
                    String n = fs.getNameNoExt() + ".srt";
                    if (name.startsWith(n) 
                            && name.length() > n.length())
                    {
                        return true;
                    }
                    return false;
                }
            };
            return dir.list(filter);
        } else
        {
            return null;
        }
    }
    
    private void cleanup()
    {
        // TODO Auto-generated method stub
        
    }

    private String[] findFilesInDir(String src)
    {
        File dir = new File(src);
        if (dir.isDirectory())
        {
            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    if (isRecursive() && new File(dir, name).isDirectory())
                    {
                        return true;
                    }
                    //NO SAMPLE FILES!!!!
                    if (name.indexOf("-sample") > -1)
                    {
                        return false;
                        
                    }
                    if (name.endsWith("mkv") || name.endsWith("avi"))
                    {
                        if (checkSrtExists)
                        {
                            Pattern p1 = Pattern.compile(".*([.].*$)");
                            Matcher m1 = p1.matcher(name);
                            File srt = null;
                            if (m1.find())
                            {
                                String ext = m1.group(1);
                                srt = new File(dir, name.substring(0, name
                                        .length()
                                        - ext.length())
                                        + ".srt");
                            }
                            if (srt.exists())
                            {
                                return false;
                            }
                        }
                        return true;
                    }
                    return false;
                }
            };
            return dir.list(filter);
        } else
        {
            return null;
        }
    }

    public static boolean isRecursive()
    {
        return recursive;
    }

    public static void setRecursive(boolean recursive)
    {
        Subs4me.recursive = recursive;
    }
    
    private void initProviders(LinkedList<String> provNames)
    {
        Torec.getInstance();
        Sratim.getInstance();
        new OpenSubs();
        new Subscene();
        
        _providers = new LinkedList<Provider>();
        if (provNames == null)
        {
            _providers.add(getProvider("opensubs"));
            _providers.add(getProvider("sratim"));
            _providers.add(getProvider("torec"));
        }
        else
        {
            for (Iterator iterator = provNames.iterator(); iterator.hasNext();)
            {
                String p = (String) iterator.next();
                Provider prov = getProvider(p);
                if (prov != null)
                {
                    if (prov.getName().equals(Sratim.getInstance().getName()))
                    {
                        if (!Sratim.getInstance().loadSratimCookie())
                        {
                            Login login = new Login();
                            if (!login.isLoginOk())
                            {
                                System.exit(-1);
                            }
                        }
                    }
                    _providers.add(prov);
                }
            }
        }
    }
    
    private Provider getProvider(String name)
    {
        for (Iterator iterator2 = _availableProviders.iterator(); iterator2.hasNext();)
        {
            Provider availProv = (Provider) iterator2.next();
            if (availProv.getName().toLowerCase().equals(name.toLowerCase()))
            {
                return availProv;
            }
        }
        return null;
    }
    
    public static boolean isFullDownload()
    {
        return fullDownload;
    }

    public static void main(String[] args)
    {
//        args = new String[]{};
        if (args.length < 1)
        {
            exitShowMessage();
        }
        
        File f = new File(args[0]);
        if (!f.exists())
        {
            exitShowMessage();
        }
        
        Subs4me as = Subs4me.getInstance();
        
     // Load the sub4me-default.properties file
        if (!PropertiesUtil.setPropertiesStreamName("./properties/subs4me-default.properties")) {
            return;
        }

        // Load the user properties file "moviejukebox.properties"
        // No need to abort if we don't find this file
        // Must be read before the skin, because this may contain an override skin
        PropertiesUtil.setPropertiesStreamName(propertiesName);
        LinkedList<String> providers = null;
        
        providers = initProperties();
        
        for (int i = 1; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.equals(SRT_EXISTS))
            {
                checkSrtExists = true;
            } else if (arg.equals(RECURSIVE_SEARCH))
            {
                setRecursive(true);
            }
            else if (arg.equals(FULL_DOWNLOAD))
            {
                fullDownload = true;
            }
            else if (arg.startsWith(PROVIDERS))
            {
                providers = parseProviderNames(arg);
            }
            else if (arg.startsWith(DO_NOT_USE_OPENSUBS_FOR_FILE_REALIZATION))
            {
                noUseOpen = true;
            }
            else if (arg.startsWith(GET_MOVIE_PIC))
            {
                getMoviePic = true;
            }
            else if (arg.startsWith(GRT_MOVIE_PIC_FORCED))
            {
                getMoviePicForce = true;
            }
        }
        as.initProviders(providers);
        StringBuilder sb = new StringBuilder();
        for (Iterator iterator = _providers.iterator(); iterator.hasNext();)
        {
            Provider p = (Provider) iterator.next();
            sb.append(p.getName() + ",");
        }
        System.out.println("Subs4me version " + VERSION);
        System.out.println("        selected providers are:" + sb.toString());
        System.out.println("        check recursively = " + isRecursive());
        System.out.println("        do not check if srt exists = " + checkSrtExists);
        System.out.println("        download everything = " + isFullDownload());
        System.out.println("        check movie name using opensubs first = " + !noUseOpen);
        System.out.println("        get movie picture = " + getMoviePic + ", (forced = " + getMoviePicForce + ")");
        as.startProcessingFiles(args[0]);
        
        if (as.oneSubsFound.size() > 0)
        {
            StringBuilder sbExact = new StringBuilder("\n********************** found exact matches **********************************************");
            sbExact.append("\nExact matches were found for:");
            for (Iterator iterator = as.oneSubsFound.iterator(); iterator.hasNext();)
            {
                String src = (String) iterator.next();
                sbExact.append("\n");
                sbExact.append(src);
            }
            System.out.println(sbExact.toString());
        }
        
        if (as.moreThanOneSubs.size() > 0)
        {
            StringBuilder sbExact = new StringBuilder("\n************** found inexact matches, run HandleMultipleSubs ****************************");
            sbExact.append("\nPlease run HandleMultipleSubs on:");
            for (Iterator iterator = as.moreThanOneSubs.iterator(); iterator.hasNext();)
            {
                String src = (String) iterator.next();
                sbExact.append("\n");
                sbExact.append(src);
            }
            System.out.println(sbExact.toString());
        }
        if (as.noSubs.size() > 0)
        {
            StringBuilder sbExact = new StringBuilder("\n********************* no subs were found ************************************************");
            sbExact.append("\nNo subs were found for:");
            for (Iterator iterator = as.noSubs.iterator(); iterator.hasNext();)
            {
                String src = (String) iterator.next();
                sbExact.append("\n");
                sbExact.append(src);
            }
            System.out.println(sbExact.toString());
        }
        if (as.moreThanOneSubs.size() > 0 || as.oneSubsFound.size() > 0)
        {
            System.out.println("*****************************************************************************************");
        }
        System.out.println("******* Thanks for using subs4me, hope you enjoy the results *******");
    }
    
    private static LinkedList<String> initProperties()
    {
        LinkedList<String> providers = null;
        
        String pp = PropertiesUtil.getProperty(PROVIDERS_PROPERTY, "opensubs,sratim,torec");
        if (pp != null)
        {
            providers = parseProviderNames(pp);
        }
        
        String subCheck = PropertiesUtil.getProperty(SUBS_CHECK_ALL_PROPERTY, "true");
        if (subCheck != null)
        {
            checkSrtExists = subCheck.equalsIgnoreCase("true");
        }
        
        String subRecursive = PropertiesUtil.getProperty(SUBS_RECURSIVE_PROPERTY, "true");
        if (subRecursive != null)
        {
            setRecursive(subRecursive.equalsIgnoreCase("true"));
        }
        
        String subGetAll = PropertiesUtil.getProperty(SUBS_GET_ALL_PROPERTY, "true");
        if (subGetAll != null)
        {
            fullDownload = subGetAll.equalsIgnoreCase("true");
        }
        
        String getPicture = PropertiesUtil.getProperty(SUBS_DOWNLOAD_PICTURE_PROPERTY, "true");
        if (getPicture != null)
        {
            getMoviePic = getPicture.equalsIgnoreCase("true");
        }
        
        String getPictureForce = PropertiesUtil.getProperty(SUBS_DOWNLOAD_PICTURE_FORCE_PROPERTY, "true");
        if (getPictureForce != null)
        {
            getMoviePicForce = getPictureForce.equalsIgnoreCase("true");
        }
        return providers;
    }

    private static LinkedList<String> parseProviderNames(String providers)
    {
        if (providers == null || providers.isEmpty())
        {
            return null;
        }
        
        LinkedList<String> ret = new LinkedList<String>();
        String[] pros = null;
        if (providers.startsWith(PROVIDERS))
        {
            pros = providers.substring(PROVIDERS.length()+1).split(",");
        }
        else
        {
            pros = providers.split(",");
        }
        for (int j = 0; j < pros.length; j++)
        {
            String p = pros[j];
            ret.add(p);
        }
        
        return ret;
    }

    /**
     * 
     */
    public static void exitShowMessage()
    {
        StringBuffer sb = new StringBuffer("Usage: subs4me \"[file]\" | \"[directory]\" [/params]");
        sb.append("\nVersion ");
        sb.append(VERSION);
        sb.append("\n");
        sb.append("Example:\n");
        sb.append("\tsubs4me \"C:\\movies\" /r /all \n\n");
        sb.append("Params:\n");
        sb.append("  c: If an srt file exists do not try to get the subtitels for this file\n");
        sb.append("  r: Recurse over all the files in all the directories\n");
        sb.append("  p: select providers, /p=torec,opensubs will select these two providers\n     (order is important), default is opensubs,sratim,torec \n");
        sb.append("     Currently supporting: torec, opensubs, sratim, subscene\n");
        sb.append("  all: Download all the subtitles for this title and unzip with the above schema\n");
        sb.append("  n: do not use opensubs to validate actual movie name (use google only)\n");
        sb.append("  i: get the image file for the movie (only if it does not exist)\n");
        sb.append("  if: get the image file for the movie (refresh current)\n");
        sb.append("\nCreated by ilank\nEnjoy...");
        System.out.println(sb.toString());
        System.exit(-1);
    }
    
    public static void registerProvider(Provider provider)
    {
        if (_availableProviders.contains(provider))
        {
            return;
        }
        _availableProviders.add(provider);
    }
    
    public static boolean shouldGetPic()
    {
        return getMoviePic;
    }
    
    public static boolean shouldForceGetPic()
    {
        return getMoviePicForce;
    }
}
