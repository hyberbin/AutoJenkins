/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.jenkins;

import java.io.IOException;
import org.jplus.hyb.database.sqlite.SqliteUtil;
import org.jplus.hyb.log.LocalLogger;
import org.jplus.jenkins.plugin.feiq.FeiqManger;
import org.jplus.jenkins.plugin.listener.GitListener;
import org.jplus.util.NumberUtils;

/**
 *
 * @author hyberbin
 */
public class JenkinsConsole {
    private static String basePath;
    private static String JenkinsUser;
    private static String JenkinsPass;
    private static String JenkinsUrl;
    private static String[] Repositorys;
    private static String[] MainJobs=new String[]{};
    private static int ScanInterval=5000;
    private static int WaitInterval=5000;
    public static JenkinsUtils jenkinsGitUtils;
    
    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        setArgs(args);
        //testArg();
        LocalLogger.setLevel(LocalLogger.INFO);
        jenkinsGitUtils = new JenkinsUtils(JenkinsUser, JenkinsPass, JenkinsUrl);
        GitListener gitListener = new GitListener(jenkinsGitUtils);
        gitListener.setScanInterval(ScanInterval);
        gitListener.setWaitInterval(WaitInterval);
        for (String rep : Repositorys) {
            gitListener.addRepository(rep, basePath + rep+"/");
        }
        gitListener.setMainJobs(MainJobs);
        gitListener.start();
        FeiqManger.INSTANCE.start();
    }
    
    private static void setArgs(String[] args){
        for (String arg : args) {
            if(arg.startsWith("-path")){
                basePath=arg.split("=")[1];
            }else if(arg.startsWith("-Juser")){
                JenkinsUser=arg.split("=")[1];
            }else if(arg.startsWith("-Jpass")){
                JenkinsPass=arg.split("=")[1];
            }else if(arg.startsWith("-Jurl")){
                JenkinsUrl=arg.split("=")[1];
            }else if(arg.startsWith("-Repositorys")){
                Repositorys=arg.split("=")[1].split(",");
            }else if(arg.startsWith("-ScanInterval")){
                ScanInterval=NumberUtils.parseInt(arg.split("=")[1]);
            }else if(arg.startsWith("-WaitInterval")){
                WaitInterval=NumberUtils.parseInt(arg.split("=")[1]);
            }else if(arg.startsWith("-MainJobs")){
                MainJobs=arg.split("=")[1].split(",");
            }else if(arg.startsWith("-feiqUser")){
                FeiqManger.INSTANCE.addManagers(arg.split("=")[1].split(","));
            }
        }
    }
    private static void testArg(){
        String argLine="-path=/codes/platform/ -Juser=hyberbin -Jpass=hyb -Jurl=http://192.168.1.38:8080 -Repositorys=service-modules";
        setArgs(argLine.split(" "));
    }
    
    public static String[] getMainJobs(){
        return MainJobs;
    }
}
