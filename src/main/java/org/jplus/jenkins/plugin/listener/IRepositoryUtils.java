/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.listener;

import java.util.Collection;

/**
 *
 * @author hyberbin
 */
public interface IRepositoryUtils {
    /**
     * 获取最后版本.
     * @return
     * @throws java.lang.Exception
     */
     Object getLastVersion()throws Exception;
     
     /**
     * 获取有改动的文件路径
     * @param startVersion
     * @return
     */
    Collection<String> getChangedPaths(Object startVersion);
    /**
     * 更新代码
     */
    void update();
}
