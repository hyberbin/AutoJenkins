/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.listener;

/**
 *
 * @author hyberbin
 */
public interface VersionListener {

    void doScanSvnLog(String name);
    
    void doBuild();

    String getModuleName(String path);

    void start();

    void stop();
    
}
