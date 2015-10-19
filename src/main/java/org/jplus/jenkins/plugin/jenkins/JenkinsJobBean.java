/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.jenkins;

/**
 *
 * @author hyberbin
 */
public class JenkinsJobBean {

    private boolean building;
    private boolean success;

    public JenkinsJobBean(boolean building, boolean success) {
        this.building = building;
        this.success = success;
    }

    public boolean isBuilding() {
        return building;
    }

    public void setBuilding(boolean building) {
        this.building = building;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
