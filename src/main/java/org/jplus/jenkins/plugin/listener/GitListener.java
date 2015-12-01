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
import org.jplus.jenkins.plugin.git.GITRepositoryUtils;
import org.jplus.jenkins.plugin.jenkins.JenkinsUtils;
import org.jplus.jenkins.plugin.velocity.VelocityUtils;
import org.jplus.util.IgnoreCaseMap;
import org.jplus.util.ObjectHelper;

/**
 *
 * @author hyberbin
 */
public class GitListener implements VersionListener {

    private static final Logger LOGGER = LoggerManager.getLogger(SvnListener.class);
    private static final Map<String, ModuleUpdateBean> MODULES_MAP = new HashMap<String, ModuleUpdateBean>();
    private int waitInterval = SqliteUtil.getLongProperty("waitInterval").intValue();
    private int scanInterval = SqliteUtil.getLongProperty("scanInterval").intValue();
    private final Map<String, IRepositoryUtils> repositoryUtils = new HashMap<String, IRepositoryUtils>();
    private final JenkinsUtils jenkinsUtils;
    
    private String[] mainJobs=new String[]{};

    private final List<Timer> scanGitTimer = new ArrayList<Timer>();
    private final Timer buildTimer = new Timer();
    private final Timer mainJobTimer = new Timer();

    public void addRepository(String name, String path) throws IOException {
        repositoryUtils.put(name, new GITRepositoryUtils(path));

    }

    public GitListener(JenkinsUtils jenkinsUtils) {
        this.jenkinsUtils = jenkinsUtils;
    }

    @Override
    public synchronized void doScanSvnLog(String name) {
        IRepositoryUtils repositoryUtil = repositoryUtils.get(name);
        try {
            //获取有改动的文件路径
            String startRevision = SqliteUtil.getProperty(name + "startRevision");
            if (ObjectHelper.isNullOrEmptyString(startRevision)) {
                startRevision = (String) repositoryUtil.getLastVersion();
                SqliteUtil.setProperty(name + "startRevision", startRevision);
                LOGGER.debug("{}之前没有版本号,重新获取为：{}",name, startRevision);
            }
            LOGGER.debug("{}最后版本为：{}",name, startRevision);
            repositoryUtil.update();
            String nowVersion=repositoryUtil.getLastVersion().toString();
            LOGGER.debug("{}现在版本为：{}",name, nowVersion);
            long time = new Date().getTime();
            if (!startRevision.equals(nowVersion)) {
                Collection<String> changedPaths = repositoryUtil.getChangedPaths(startRevision);
                LOGGER.debug("{}有{}个文件有改动",name, changedPaths.size());
                //更新数据库中代码最新版本
                SqliteUtil.setProperty(name + "startRevision", nowVersion);
                for (String changedPath : changedPaths) {
                    String moduleName = getModuleName(changedPath);
                    if("pom.xml".equals(moduleName)||"src".equals(moduleName)){//只有一级的工程
                        moduleName=name;
                    }
                    LOGGER.info("从改动路径:{}获取模块名为：{}",changedPath, moduleName);
                    if (moduleName != null) {
                        LOGGER.debug("准备构建模块：{}", moduleName);
                        MODULES_MAP.put(moduleName, new ModuleUpdateBean(name, moduleName, time));
                    }
                }
            } else {
                LOGGER.debug("{}没有更新版本",name);
            }

        } catch (Exception e) {
            LOGGER.error("监听出错!", e);
        }
    }

    @Override
    public synchronized void doBuild() {
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
                    map.put("gitName", get.getRepositoryName());
                    map.put("groupId", "com.gohighedu.platform." + get.getRepositoryName().replace("-", "."));
                    String template = VelocityUtils.getTemplate("Jenkins_job_pom.vm", map);
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
    public synchronized String getModuleName(String path) {
        if (!path.startsWith("/dev/")) {
            String[] split = path.split("/");
            return split[0];
        }
        return null;
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
            LOGGER.info("已经启动监听仓库:"+name);
        }
        buildTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                doBuild();
            }
        }, new Date(),scanInterval);
        LOGGER.info("已经启动监听构建任务");
        
        mainJobTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                jenkinsUtils.buildByOrder(mainJobs);
            }
        }, new Date(new Date().getTime()+1000*60*60*2),1000*60*60*2);
    }

    @Override
    public void stop() {
        for (Timer timer : scanGitTimer) {
            timer.cancel();
        }
    }
    
    public void setMainJobs(String[] jobs){
        mainJobs=jobs;
    }

    public void setWaitInterval(int waitInterval) {
        this.waitInterval = waitInterval;
    }

    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }

}
