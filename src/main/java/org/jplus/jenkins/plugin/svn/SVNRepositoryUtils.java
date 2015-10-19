/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.svn;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.jplus.hyb.database.sqlite.SqliteUtil;
import org.jplus.hyb.log.Logger;
import org.jplus.hyb.log.LoggerManager;
import org.jplus.jenkins.plugin.listener.IRepositoryUtils;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 *
 * @author hyberbin
 */
public class SVNRepositoryUtils implements IRepositoryUtils {

    private static final Logger LOGGER = LoggerManager.getLogger(SVNRepositoryUtils.class);

    private String url = SqliteUtil.getProperty("svnUrl");//"http://192.168.0.22:6666/svn/DigitalCampus/trunk/codes";
    private String name = SqliteUtil.getProperty("svnUser");//"huangyingbing";
    private String password = SqliteUtil.getProperty("svnPass");//"hyb@163.com";
    public long lastVersion = 0l;
    private SVNRepository repository = null;

    public SVNRepositoryUtils(String url, String name, String password) {
        this.url = url;
        this.name = name;
        this.password = password;
    }

    /**
     * 获取SVN服务器连接
     *
     * @return
     */
    private SVNRepository getRepository() {
        if (repository == null) {
            LOGGER.debug("第一次打开SVN连接……");
            login();
        }
        try {
            repository.testConnection();
        } catch (SVNException ex) {
            LOGGER.debug("经测试SVN已经断开连接，尝试重新登录……");
            login();
            try {
                repository.testConnection();
            } catch (SVNException svne) {
                LOGGER.debug("无法连接SVN，系统退出……");
                System.exit(1);
            }
        }
        return repository;
    }

    /**
     * 登录SVN服务器.
     */
    private void login() {
        LOGGER.debug("登录SVN服务器");
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
        } catch (SVNException svne) {
            LOGGER.error("error while creating an SVNRepository for the location '{url}':{} ", url, svne.getMessage());
            LOGGER.error("登录SVN服务器失败自动退出");
            System.exit(1);
        }
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password.toCharArray());
        repository.setAuthenticationManager(authManager);
        LOGGER.debug("登录SVN服务器成功");
    }

    /**
     * 获取SVN最后版本.
     *
     * @return
     */
    @Override
    public Object getLastVersion() throws Exception {
        return getRepository().getLatestRevision();
    }

    public void update() {
    }

    /**
     * 获取有改动的文件路径
     *
     * @param startVersion
     * @return
     */
    public Collection<String> getChangedPaths(Object startVersion) {
        LOGGER.debug("获取有改动的文件路径");
        Collection<String> changedPaths = new HashSet();
        try {
            Collection logEntries = getRepository().log(new String[]{""}, null, (Long) startVersion, (Long) getLastVersion(), true, true);
            for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
                SVNLogEntry logEntry = (SVNLogEntry) entries.next();
                long v = logEntry.getRevision();
                lastVersion = v > lastVersion ? v : lastVersion;
                Collection<SVNLogEntryPath> values = logEntry.getChangedPaths().values();
                for (SVNLogEntryPath value : values) {
                    changedPaths.add(value.getPath());
                }
            }
        } catch (Exception svne) {
            LOGGER.error("获取更新日志失败！", svne);
        }
        return changedPaths;
    }

    static {
        LOGGER.debug("初始化SVN连接");
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }
}
