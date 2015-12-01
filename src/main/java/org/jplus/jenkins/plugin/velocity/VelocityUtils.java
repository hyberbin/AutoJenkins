/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.velocity;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 *
 * @author hyberbin
 */
public class VelocityUtils {

    protected final static VelocityEngine velocity = new VelocityEngine();

    static {
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        prop.put("input.encoding", "UTF-8");
        prop.put("output.encoding", "UTF-8");
        velocity.init(prop);
    }

    public static synchronized String getTemplate(String templateName, Map vars) {
        Template template = velocity.getTemplate("template/" + templateName);
        //取得velocity的上下文context
        VelocityContext context = new VelocityContext();
        for (Object key : vars.keySet()) {
            context.put(key.toString(), vars.get(key));
        }
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }
}
