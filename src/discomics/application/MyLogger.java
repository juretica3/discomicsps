package discomics.application;

import java.io.IOException;
import java.util.logging.*;

/**
 * Created by Jure on 4.9.2016.
 */
public class MyLogger {

    private static Logger logger;
    private Formatter plainText;

    private MyLogger() throws IOException {
        //instance the logger
        logger = Logger.getLogger(MyLogger.class.getName());
        //instance the filehandler
        FileHandler fileHandler = new FileHandler("myLog.txt", true);
        //instance formatter, set formatting, and handler
        plainText = new SimpleFormatter();
        fileHandler.setFormatter(plainText);
        logger.addHandler(fileHandler);
    }

    private static Logger getLogger() {
        if (logger == null) {
            try {
                new MyLogger();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return logger;
    }

    public static void log(Level level, String msg) {
        //getLogger().log(level, msg);
        //System.out.println(level.toString() + ": " + msg);
    }

    public static void log(Level level, String msg, Throwable throwable) {
        //getLogger().log(level, msg, throwable);
        //System.out.println(level.toString() + ": " + msg);
    }

    public static void entering(String className, String methodName) {
        //getLogger().entering(className, methodName);
        //System.out.println("Entering class *" + className + "* method *" + methodName + "*");
    }

    public static void exiting(String className, String methodName) {
        //getLogger().exiting(className,methodName);
        //System.out.println("Exiting class *" + className + "* method *" + methodName + "*");
    }

}
