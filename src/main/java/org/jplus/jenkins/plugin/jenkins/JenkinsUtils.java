/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.jenkins;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import org.jplus.hyb.log.Logger;
import org.jplus.hyb.log.LoggerManager;
import org.jplus.jenkins.plugin.feiq.FeiqManger;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author hyberbin
 */
public class JenkinsUtils {

    protected final Logger LOGGER = LoggerManager.getLogger(JenkinsUtils.class);
    protected Connection connect = null;
    protected final String username;
    protected final String password;
    protected final String url;
    private final Timer timer = new Timer();
    protected final Queue<JobGroup> jobQueue = new LinkedList<JobGroup>();

    public JenkinsUtils(String username, String password, final String url) {
        this.username = username;
        this.password = password;
        this.url = url;
        LOGGER.info("准备登录，用户名：{},密码：{},url:{}", username, password, url);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (jobQueue.size() > 0) {
                    try {
                        JobGroup jobGroup = jobQueue.poll();
                        for (String job : jobGroup.getJobs()) {
                            boolean success = doBuildJob(job);
                            if (jobGroup.isLinked() && !success) {
                                sendMsg("由于构建模块出错,构建任务组退出!");
                                LOGGER.error("由于构建模块出错,构建任务组退出!");
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error("构建模块出错", ex);
                    }
                }
            }
        }, new Date(), 2000);//间隔两秒扫描任务队列
    }

    private synchronized boolean doBuildJob(String job) throws Exception {
        LOGGER.info("开始构建模块：{}", job);
        sendMsg("开始构建模块：{}", job);
        login();
        connect.url(url + "/job/" + job + "/build?delay=0sec");
        connect.followRedirects(false);
        connect.post();
        Thread.sleep(3000);
        while (getLastBuild(job).isBuilding()) {
            Thread.sleep(3000);
        }
        boolean success = getLastBuild(job).isSuccess();
        LOGGER.info("{}任务构建{}", job, success ? "成功" : "失败");
        sendMsg("{}任务构建{}", job, success ? "成功" : "失败");
        return success;
    }


    private synchronized void login() throws IOException {
        LOGGER.info("准备登录，用户名：{},密码：{}", username, password);
        connect = Jsoup.connect(url + "/j_acegi_security_check");
        connect.method(Connection.Method.POST);
        connect.data("j_username", username);
        connect.data("j_password", password);
        connect.data("from", "/");
        connect.data("Submit", "登录");
        connect.data("json", "{\"j_username\": \"" + username + "\", \"j_password\": \"" + password + "\", \"remember_me\": false, \"from\": \"/\"}");
        connect.post();

    }


    /**
     * 检查工作配置名是否存在
     *
     * @param moduleName
     * @return 存在返回false不存在返回true
     * @throws IOException
     */
    public synchronized boolean checkJobName(String moduleName) throws IOException {
        LOGGER.info("检查工作：{}是否已经存在", moduleName);
        login();
        connect.url(url + "/checkJobName?value=" + moduleName);
        Document get = connect.get();
        return get.text().isEmpty();
    }
    
    /**
     * 创建工作配置
     *
     * @param jobName 创建JOB需要的一些变量
     * @param configXml 配置文件内容
     * @throws IOException
     */
    public synchronized void createJob(String jobName,String configXml) throws IOException {
        LOGGER.info("准备创建工作：{}", jobName);
        sendMsg("准备创建工作：{}", jobName);
        login();
        connect.url(url + "/createItem?name=" + jobName);
        connect.method(Connection.Method.POST);
        connect.header("Content-Type", "application/xml");
        connect.setRequestBody(configXml);
        connect.execute();
    }

    /**
     * 如果工作配置不存在则创建
     *
      * @param jobName 创建JOB需要的一些变量
     * @param configXml 配置文件内容
     * @throws IOException
     */
    public synchronized void createJobIfNotExist(String jobName,String configXml) throws IOException {
        login();
        if (checkJobName(jobName)) {
            LOGGER.info("不存在工作：{},自动创建", jobName);
            sendMsg("不存在工作：{},自动创建", jobName);
            createJob(jobName,configXml);
        } else {
            LOGGER.info("已经存在工作：{},不创建", jobName);
            sendMsg("已经存在工作：{},不创建", jobName);
        }
    }


    public synchronized void addJob(String[] jobs) {
        JobGroup jobGroup = new JobGroup(jobs.length > 1);
        for (String job : jobs) {
            jobGroup.addJob(job);
        }
        if(jobQueue.contains(jobGroup)){
            jobQueue.remove(jobGroup);
        }
        jobQueue.add(jobGroup);
    }

    public synchronized String getJobLog(String jobName, int jobNumber, int startLine) throws IOException {
        login();
        connect.url(url + "/job/" + jobName + "/" + jobNumber + "/logText/progressiveHtml?start=" + startLine);
        Document get = connect.get();
        return get.body().html();
    }

    public synchronized JenkinsJobBean getLastBuild(String job) throws Exception {
        login();
        Connection conn = connect.url(url + "/job/" + job + "/lastBuild/api/json");
        conn.ignoreContentType(true);
        conn.method(Connection.Method.GET);
        String json = conn.execute().body();
        JSONObject jSONObject = new JSONObject(json);
        return new JenkinsJobBean(jSONObject.getBoolean("building"), "SUCCESS".equalsIgnoreCase(jSONObject.getString("result")));
    }

    public synchronized void buildByOrder(final String[] jobs) {
        LOGGER.info("构建任务组:", jobs);
        sendMsg("构建任务组:", jobs);
        addJob(jobs);
    }

    public Queue<JobGroup> getJobQueue() {
        return jobQueue;
    }

    protected synchronized static void sendMsg(String msg, Object... objects) {
        FeiqManger.INSTANCE.sendMsg(msg, objects);
    }

    public static void main(String[] args) throws IOException {
        JenkinsUtils jenkinsUtils = new JenkinsUtils("hyberbin", "hyb", "http://192.168.1.38:8080");
        String jobs = "framework data-framework data-domains data-modules data";
        String[] split = jobs.split(" ");
        jenkinsUtils.buildByOrder(split);
    }
}
