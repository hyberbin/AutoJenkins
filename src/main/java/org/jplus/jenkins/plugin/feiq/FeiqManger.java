/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.feiq;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jplus.jfeiq.feiq.FeiqServer;
import org.jplus.jfeiq.feiq.IPMSGData;
import org.jplus.util.ObjectHelper;

/**
 *
 * @author hyberbin
 */
public class FeiqManger {

    public static final FeiqManger INSTANCE = new FeiqManger();

    private final FeiqServer FEIQ_SERVER;

    private final Set<String> managers=new HashSet<String>();

    private FeiqManger() {
        FEIQ_SERVER = new FeiqServer();
        FEIQ_SERVER.setReceiveHandler(new ReceiveHandler(FEIQ_SERVER));
        FEIQ_SERVER.setServerName("Jenkins自动管理器");
    }
    
    public void start(){
        FEIQ_SERVER.start();
    }

    public Set<String> getManagers() {
        return managers;
    }

    public void addManagers(String... managers) {
        this.managers.addAll(Arrays.asList(managers));
        sendMsg("Jenkins自动管理器已经启动");
    }

    public void sendMsg(String msg,Object... objects) {
        if (ObjectHelper.isNotEmpty(managers)) {
            for (String manager : managers) {
                FEIQ_SERVER.sendMsg(new IPMSGData(IPMSGData.IPMSG_SENDMSG, format(msg,objects), manager));
            }
        }
    }

    /**
     * 将msg{},{},{}替换成msg{0},{1},{2}的形式
     *
     * @param message 要替换的内容
     * @param objects 替换对象
     * @return
     */
    private static String replace(StringBuilder message, Object... objects) {
        Integer n = 0;
        String res = message.toString();
        for (Object object : objects) {
            int indexOf = message.indexOf("{}");
            res = indexOf >= 0 ? message.insert(indexOf + 1, n++).toString() : message.toString();
        }
        return res;
    }

    /**
     * 格式化字符串
     *
     * @param message 要格式化的内容
     * @param objects 参数
     * @return
     */
    public static String format(String message, Object... objects) {
        StringBuilder builder = new StringBuilder(message);
        return MessageFormat.format(replace(builder, objects), objects);
    }
}
