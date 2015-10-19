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
public class ModuleUpdateBean {

    private String moduleName;
    private String repositoryName;
    private Long time;

    public ModuleUpdateBean(String repositoryName,String moduleName, Long time) {
        this.repositoryName=repositoryName;
        this.moduleName = moduleName;
        this.time = time;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

}
