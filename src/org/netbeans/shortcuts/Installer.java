package org.netbeans.shortcuts;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        WindowManager.getDefault().invokeWhenUIReady(new ActionInstaller());
    }

    @Override
    public void uninstalled() {
        WindowManager.getDefault().invokeWhenUIReady(new ActionUninstaller());
    }

    private static final class ActionInstaller extends FileTypeHandler {

        @Override
        protected void handleFileType(FileObject fileType) {
            try {
                final FileObject actionsFolder = FileUtil.createFolder(fileType, "Actions");

                final FileObject newAction = actionsFolder.getFileObject("org-netbeans-shortcuts-ShortcutAction.shadow");
                if (null == newAction) {
                    final FileObject action = actionsFolder.createData("org-netbeans-shortcuts-ShortcutAction.shadow");
                    action.setAttribute("originalFile", "Actions/File/org-netbeans-shortcuts-ShortcutAction.instance");
                    action.setAttribute("position", 2950);
                }
                final FileObject separatorBefore = actionsFolder.getFileObject("org-netbeans-shortcuts-ShortcutAction-separatorBefore.instance");
                if (null == separatorBefore) {
                    final FileObject action = actionsFolder.createData("org-netbeans-shortcuts-ShortcutAction-separatorBefore.instance");
                    action.setAttribute("instanceClass", "javax.swing.JSeparator");
                    action.setAttribute("position", 2930);
                }
                final FileObject separatorafter = actionsFolder.getFileObject("org-netbeans-shortcuts-ShortcutAction-separatorAfter.instance");
                if (null == separatorafter) {
                    final FileObject action = actionsFolder.createData("org-netbeans-shortcuts-ShortcutAction-separatorAfter.instance");
                    action.setAttribute("instanceClass", "javax.swing.JSeparator");
                    action.setAttribute("position", 2970);
                }

            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
    }

    private static final class ActionUninstaller extends FileTypeHandler {

        @Override
        protected void handleFileType(FileObject fileType) {
            try {
                final FileObject actionsFolder = FileUtil.createFolder(fileType, "Actions");
                final FileObject newAction = actionsFolder.getFileObject("org-netbeans-shortcuts-ShortcutAction.shadow");
                if (null != newAction) {
                    newAction.delete();
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
    }

    private static abstract class FileTypeHandler implements Runnable {

        @Override
        public void run() {
            final FileObject loaders = FileUtil.getConfigFile("Loaders");
            final FileObject[] categories = loaders.getChildren();
            for (FileObject category : categories) {
                final FileObject[] fileTypes = category.getChildren();
                for (FileObject fileType : fileTypes) {
                    handleFileType(fileType);
                }
            }
        }

        protected abstract void handleFileType(FileObject fileType);
    }
}
