/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jplus.jenkins.plugin.git;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.jplus.hyb.log.LoggerManager;
import org.jplus.jenkins.plugin.listener.IRepositoryUtils;
import org.jplus.jenkins.plugin.listener.SvnListener;

/**
 *
 * @author hyberbin
 */
public class GITRepositoryUtils implements IRepositoryUtils {
    private final org.jplus.hyb.log.Logger LOGGER = LoggerManager.getLogger(SvnListener.class);

    private final Repository repo;
    private final Git git;
    private final String repositoryPath;

    public GITRepositoryUtils(String repositoryPath) throws IOException {
        this.repositoryPath=repositoryPath;
        git = Git.open(new File(repositoryPath));
        this.repo = git.getRepository();
    }

    @Override
    public Object getLastVersion() throws Exception {
        return repo.resolve("HEAD").getName();
    }

    @Override
    public Collection<String> getChangedPaths(Object startVersion) {
        try {
            update();
            LogCommand logCommand = new LogCommand(repo);
            Collection<String> call = logCommand.call((String) startVersion);
            return call;
        } catch (IOException ex) {
            Logger.getLogger(GITRepositoryUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GitAPIException ex) {
            Logger.getLogger(GITRepositoryUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void update() {
        try {
            LOGGER.debug("update git:"+repositoryPath);
            CheckoutCommand checkout = git.checkout();
            ObjectId resolve = repo.resolve("master");
            if(resolve==null){
                checkout.setCreateBranch(true);
            }
            checkout.setForce(true);
            checkout.setStage(CheckoutCommand.Stage.THEIRS);
            checkout.setName("master");
            checkout.call();
            ResetCommand reset = git.reset();
            reset.setMode(ResetCommand.ResetType.HARD);
            reset.setRef("HEAD");
            reset.call();
            PullCommand pull = git.pull();
            pull.setRebase(true);
            pull.setRemote("origin");
            pull.setRemoteBranchName("master");
            pull.setStrategy(MergeStrategy.THEIRS);
            pull.call();
        } catch (Exception ex) {
            Logger.getLogger(GITRepositoryUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
