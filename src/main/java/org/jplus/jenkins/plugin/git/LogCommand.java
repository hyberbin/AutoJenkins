package org.jplus.jenkins.plugin.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Create a Log command that enables the follow option: git log --follow -- < path
 * >
 * User: OneWorld Example for usage: ArrayList<RevCommit> commits = new
 * LogFollowCommand(repo,"src/com/mycompany/myfile.java").call();
 */
public class LogCommand {

    private final Repository repository;
    private Git git;

    /**
     * Create a Log command that enables the follow option: git log --follow -- < path
     * >
     *
     * @param repository
     * @param path
     */
    public LogCommand(Repository repository) {
        this.repository = repository;
    }

    /**
     * Returns the result of a git log --follow -- < path >
     *
     * @return
     * @throws IOException
     * @throws MissingObjectException
     * @throws GitAPIException
     */
    public Collection<String> call(String until) throws IOException, MissingObjectException, GitAPIException {
        Set<String> changeList = new HashSet<String>();
        git = new Git(repository);
        org.eclipse.jgit.api.LogCommand logcmd = git.log();
        if (until != null) {
            ObjectId resolve = repository.resolve(until);
            logcmd.not(resolve);
        }
        Iterable<RevCommit> log = logcmd.call();
        for (RevCommit commit : log) {
            String renamedPath = getRenamedPath(commit);
            changeList.add(renamedPath);
        }
        return changeList;
    }

    /**
     * Checks for renames in history of a certain file. Returns null, if no
     * rename was found. Can take some seconds, especially if nothing is
     * found... Here might be some tweaking necessary or the LogFollowCommand
     * must be run in a thread.
     *
     * @param start
     * @return String or null
     * @throws IOException
     * @throws MissingObjectException
     * @throws GitAPIException
     */
    private String getRenamedPath(RevCommit start) throws IOException, MissingObjectException, GitAPIException {
        Iterable<RevCommit> allCommitsLater = git.log().add(start).call();
        for (RevCommit commit : allCommitsLater) {
            TreeWalk tw = new TreeWalk(repository);
            tw.addTree(commit.getTree());
            tw.addTree(start.getTree());
            tw.setRecursive(true);
            List<DiffEntry> files = DiffEntry.scan(tw);
            for (DiffEntry diffEntry : files) {
                if ((diffEntry.getChangeType() == DiffEntry.ChangeType.RENAME
                        || diffEntry.getChangeType() == DiffEntry.ChangeType.COPY
                        || diffEntry.getChangeType() == DiffEntry.ChangeType.MODIFY
                        || diffEntry.getChangeType() == DiffEntry.ChangeType.ADD
                        || diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE)) {
                    return diffEntry.getOldPath();
                }
            }
        }
        return null;
    }
}
