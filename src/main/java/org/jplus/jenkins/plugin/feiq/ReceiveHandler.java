/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.feiq;

import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jplus.jenkins.plugin.jenkins.JenkinsConsole;
import org.jplus.jenkins.plugin.jenkins.JobGroup;
import org.jplus.jfeiq.feiq.FeiqServer;
import org.jplus.jfeiq.feiq.IPMSGData;
import org.jplus.jfeiq.handler.SimpleReceiveHandler;
import org.jplus.util.ObjectHelper;

/**
 *
 * @author hyberbin
 */
public class ReceiveHandler extends SimpleReceiveHandler {

    private static final Logger log = Logger.getLogger(ReceiveHandler.class.getName());
    private final FeiqServer server;

    public ReceiveHandler(FeiqServer server) {
        super(server);
        this.server = server;
    }

    @Override
    public void dealWith(IPMSGData data) {
        log.log(Level.INFO, "准备处理消息:{0}", data.getAdditionalSection());
        if ((data.getCommandNo() & IPMSGData.IPMSG_SENDCHECKOPT) == IPMSGData.IPMSG_SENDCHECKOPT) {//当别人给我发消息并且需要回执的时候
            IPMSGData ipmsgData = new IPMSGData(IPMSGData.IPMSG_RECVMSG, server.getServerName(), data.getIp());
            getServer().sendMsg(ipmsgData);//告诉他我我已经收到了你的信息了
        } else if (data.getCommandNo() == 0 || data.getCommandNo() == 1) {//当有人问我在不在线的时候
            IPMSGData ipmsgData = new IPMSGData(IPMSGData.IPMSG_ANSENTRY, server.getServerName(), data.getIp());
            getServer().sendMsg(ipmsgData);//告诉他我在线
        }
        if (!ObjectHelper.isNullOrEmptyString(data.getAdditionalSection())) {
            callBack(data);
        }
    }

    public void callBack(IPMSGData data) {
        Set<String> managers = FeiqManger.INSTANCE.getManagers();
        String msg = data.getAdditionalSection().trim().toLowerCase();
        if ("+".equals(msg)) {
            if (!managers.contains(data.getIp())) {
                managers.add(data.getIp());
                getServer().sendMsg(new IPMSGData(IPMSGData.IPMSG_SENDMSG, "已经将您添加到管理员", data.getIp()));
                log.log(Level.INFO, "已经将{0}添加到管理员", data.getIp());
            } else {
                getServer().sendMsg(new IPMSGData(IPMSGData.IPMSG_SENDMSG, "您已经是管理员", data.getIp()));
            }
        } else if ("-".equals(msg)) {
            managers.remove(data.getIp());
            getServer().sendMsg(new IPMSGData(IPMSGData.IPMSG_SENDMSG, "已经将您从管理员移除", data.getIp()));
            log.log(Level.INFO, "已经将{0}从管理员移除", data.getIp());
        } else if ("deploy-weblogic".equals(msg)) {
            JenkinsConsole.jenkinsGitUtils.addJob(new String[]{"testhyb"});
        } else if ("jobs".equals(msg)) {
            StringBuilder jobq = new StringBuilder();
            Queue<JobGroup> jobQueue = JenkinsConsole.jenkinsGitUtils.getJobQueue();
            for (JobGroup que : jobQueue) {
                jobq.append(que.toString()).append("\n");
            }
            getServer().sendMsg(new IPMSGData(IPMSGData.IPMSG_SENDMSG, jobq.toString(), data.getIp()));
        } else if (msg.startsWith("build@")) {
            JenkinsConsole.jenkinsGitUtils.addJob(new String[]{msg.split("@")[1]});
        }else if (msg.equals("buildmain")) {
            JenkinsConsole.jenkinsGitUtils.buildByOrder(JenkinsConsole.getMainJobs());
        } else if(data.getCommandNo()==32||data.getCommandNo()==288){
            StringBuilder message = new StringBuilder();
            message.append("回复以下指令与飞秋互动\n");
            message.append("+     将自己设为消息接收者\n");
            message.append("-     不接收消息\n");
            message.append("jobs  查看当前构建的队列\n");
            message.append("build@job名  构建job名的job  例如 build@data,build@service-modules-educational-courseset\n");
            message.append("buildmain  按顺序构建主要任务\n");
            getServer().sendMsg(new IPMSGData(IPMSGData.IPMSG_SENDMSG, message.toString(), data.getIp()));
        }
    }
}
