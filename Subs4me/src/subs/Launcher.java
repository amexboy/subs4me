package subs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import utils.PropertiesUtil;
import utils.TimeOutOptionPane;

/**
 * This class should handle launching the relevanat applications with their
 * correct params. Getsubs, Getsubs + HandleMultipulSubs, HandleMultipulSubs
 * 
 * @author ilan
 * 
 */
public class Launcher
{
    public static final String GET_SUBTITLE_PROPERTY = "getsubs";
    public static final String HANDLE_SUBTITLE_PROPERTY = "handlemultiplesubs";
    public static final String WAITFORIT_PROPERTY = "waitforit";
    public static final String WAITFORIT_DO_NOTHING_PROPERTY = "waitforitdonothing";
    
    public static final String DEFUALT_LAUNCH_PROPERTY = "default_launch";
    public static final String DEFUALT_LAUNCH_OPERATION = "/launch=";
    
    public Launcher()
    {
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args)
    {
        System.out.println("***** Starting Subs4Me launcher, edit the launch property in the subs4me.properties to decide what to do as default");
        Subs4me.initProperties();
        List<String> destinations = new LinkedList<String>();
        String def_launch = PropertiesUtil.getProperty(DEFUALT_LAUNCH_PROPERTY);
        //handle multi directories and different launchers
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.startsWith(DEFUALT_LAUNCH_OPERATION))
            {
                def_launch = arg.substring(DEFUALT_LAUNCH_OPERATION.length());
            }
            else
            {
                destinations.add(arg); 
            }
        }
        
        String sequence = PropertiesUtil.getProperty(def_launch);
        if (sequence == null)
            return;
        
        System.out.println("***** launching using " + def_launch + " configuration *******" );
        System.out.println("       to change launch config, either change default_launch property or use " + DEFUALT_LAUNCH_OPERATION + " param\n\n");
        String[] launch = sequence.split(",");
        for (int i = 0; i < launch.length; i++)
        {
            String prog = launch[i];
            String[] paramsSplit = prog.split(" ");
            ArrayList<String> params = new ArrayList<String>(
                    paramsSplit.length + 1);
            params.addAll(destinations);
            params.addAll(Arrays.asList(paramsSplit));
            params.remove(destinations.size());
            if (prog.toLowerCase().startsWith(GET_SUBTITLE_PROPERTY))
            {
                System.out.println(" ***** Launcher running getsubs *****\n");
                Subs4me.main(params.toArray(new String[params.size()]));
            }
            else if (prog.toLowerCase().equals(WAITFORIT_PROPERTY))
            {
                System.out.println("Launcher waiting");
                boolean touched = monitorKeyboard10Secs(true);
                if (touched)
                {
                    System.out.println("Launcher DONE - The END - FINITE - OK, you can go now - BYE BYE - NOOOO??????");
                    System.exit(0);
                }
            }
            else if (prog.toLowerCase().equals(WAITFORIT_DO_NOTHING_PROPERTY))
            {
                System.out.println("Launcher waiting");
                boolean touched = monitorKeyboard10Secs(false);
                if (!touched)
                {
                    System.out.println("Launcher DONE - The END - FINITE - OK, you can go now - BYE BYE - NOOOO??????");
                    System.exit(0);
                }
            } else if (prog.toLowerCase().startsWith(HANDLE_SUBTITLE_PROPERTY))
            {
                System.out.println("Launcher running HandleMultipleSubs");
                HandleMultipleSubs.main(params
                        .toArray(new String[params.size()]));
            }
        }
        System.out.println("Launcher DONE - THE END - FINITE - OK, you can go now - BYE BYE - NOOOO??????");
        System.exit(0);
    }

    /**
     * If okToStop is <b>true</b> the dialog means, we show a message instructing the user to click ok, 
     *  to exit and not do the next operation. 
     * If okToStop is <b>false</b> the dialog means, we show a message instructing the user to click ok, 
     *  to continue to the next operation.
     * @param okToStop true, false
     */
    private static boolean monitorKeyboard10Secs(boolean okToStop)
    {
        TimeOutOptionPane pane = new TimeOutOptionPane();
        String title = "";
        String message = "";
        String[] option = new String[1];  
        if (okToStop)
        {
            title = "Abort script????";
            message = "The script will continue in 5 seconds, press the cancel button to Abort\nClosing the dialog will NOT Abort";
            option[0] = "cancel";
        }
        else
        {
            title = "Continue script????";
            message = "The script will exit in 5 seconds, press the continue button to continue\nClosing the dialog will NOT Continue";   
            option[0] = "continue";
        }
        
        int ret = pane.showTimeoutDialog(null, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, option, "closing");
        switch (ret)
        {
            case 0:
                break;
            case 1:
                break;
        }
        
        return ret == 0;
    }
}
