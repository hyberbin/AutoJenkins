/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.jenkins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author hyberbin
 */
public class JobGroup {
    private final boolean linked;
    private final List<String> jobs=new ArrayList<String>();

    public JobGroup(boolean linked) {
        this.linked=linked;
    }

    public boolean isLinked() {
        return linked;
    }


    public List<String> getJobs() {
        return jobs;
    }

    public void addJob(String job) {
        this.jobs.add(job);
    }

    @Override
    public int hashCode() {
        int hash = jobs.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JobGroup other = (JobGroup) obj;
        for (String job : jobs) {
            if(!other.jobs.contains(job)){
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "JobGroup{" + "jobs=" + Arrays.toString(jobs.toArray()) + '}';
    }
    
    
}
