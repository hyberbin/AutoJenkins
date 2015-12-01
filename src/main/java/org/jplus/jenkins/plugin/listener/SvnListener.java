/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.jplus.hyb.database.sqlite.SqliteUtil;
import org.jplus.hyb.log.Logger;
import org.jplus.hyb.log.LoggerManager;
import org.jplus.jenkins.plugin.jenkins.JenkinsUtils;
import org.jplus.jenkins.plugin.svn.SVNRepositoryUtils;
import org.jplus.jenkins.plugin.velocity.VelocityUtils;
import org.jplus.util.IgnoreCaseMap;

/**
 *
 * @author hyberbin
 */
public class SvnListener implements VersionListener {

    private static final Logger LOGGER = LoggerManager.getLogger(SvnListener.class);
    private static final Map<String, ModuleUpdateBean> MODULES_MAP = new HashMap<String, ModuleUpdateBean>();
    private static final Long waitInterval = SqliteUtil.getLongProperty("waitInterval");
    private static final int scanInterval = SqliteUtil.getLongProperty("scanInterval").intValue();
    private final Map<String, IRepositoryUtils> repositoryUtils = new HashMap<String, IRepositoryUtils>();
    private final List<Timer> scanGitTimer = new ArrayList<Timer>();
    private final JenkinsUtils jenkinsUtils;

    public SvnListener(JenkinsUtils jenkinsUtils) {
        this.jenkinsUtils = jenkinsUtils;
    }

    public void addRepository(String repositoryName, String url, String name, String pass) throws IOException {
        repositoryUtils.put(repositoryName, new SVNRepositoryUtils(url, name, pass));
    }

    @Override
    public void start() {
        for (final String name : repositoryUtils.keySet()) {
            Timer timer = new Timer(name);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    doScanSvnLog(name);
                }
            }, new Date(), scanInterval);
            scanGitTimer.add(timer);
        }
    }

    @Override
    public void stop() {
        for (Timer timer : scanGitTimer) {
            timer.cancel();
        }
    }

    @Override
    public void doScanSvnLog(String name) {
        IRepositoryUtils repositoryUtil = repositoryUtils.get(name);
        try {
            //获取有改动的文件路径
            Long startRevision = SqliteUtil.getLongProperty("startRevision");
            Collection<String> changedPaths = repositoryUtil.getChangedPaths(startRevision);
            LOGGER.debug("SVN最后版本为：{}", repositoryUtil.getLastVersion());
            long time = new Date().getTime();
            if (!startRevision.equals(repositoryUtil.getLastVersion())) {
                //更新数据库中代码最新版本
                SqliteUtil.setProperty("startRevision", repositoryUtil.getLastVersion() + "");
                for (String changedPath : changedPaths) {
                    String moduleName = getModuleName(changedPath);
                    if (moduleName != null) {
                        LOGGER.debug("准备构建模块：{}", moduleName);
                        MODULES_MAP.put(moduleName, new ModuleUpdateBean(name, moduleName, time));
                    }
                }
            } else {
                LOGGER.debug("没有更新版本");
            }

        } catch (Exception e) {
            LOGGER.error("监听出错!", e);
        }
    }

    public void doBuild() {
        long time = new Date().getTime();
        Collection<String> buildList = new HashSet();
        for (String module : MODULES_MAP.keySet()) {
            ModuleUpdateBean get = MODULES_MAP.get(module);
            if (time - get.getTime() >= waitInterval) {
                LOGGER.debug("模块：{}上次构建时间为：{},当前时间为：{},等待时间为：{}结果参与构建", module, get.getTime(), time, waitInterval);
                try {
                    buildList.add(module);
                    Map map=new HashMap();
                    map.put("moduleName", module);
                    map.put("modulesPath", SqliteUtil.getProperty("svnUrl") + "/modules/");
                    String template = VelocityUtils.getTemplate("Jenkins_job_dc_module.vm", map);
                    jenkinsUtils.createJobIfNotExist(module,template);
                    jenkinsUtils.addJob(new String[]{module});
                } catch (Exception ex) {
                    LOGGER.error("创建Jenkins任务失败！", ex);
                }
            } else {
                LOGGER.debug("模块：{}上次构建时间为：{},当前时间为：{},等待时间为：{}结果不参与构建", module, get.getTime(), time, waitInterval);
            }
        }
        for (String build : buildList) {
            MODULES_MAP.remove(build);
        }
    }

    @Override
    public String getModuleName(String path) {
        if (path.startsWith("/trunk/codes/modules/dc-")) {
            return path.split("/")[4];
        }
        return null;
    }

}
