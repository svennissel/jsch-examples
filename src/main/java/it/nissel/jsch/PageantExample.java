package it.nissel.jsch;

import com.jcraft.jsch.AgentConnector;
import com.jcraft.jsch.AgentIdentityRepository;
import com.jcraft.jsch.AgentProxyException;
import com.jcraft.jsch.PageantConnector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import java.util.Arrays;

public class PageantExample {
  public static void main(String[] args) throws FileSystemException {
    String user = args[0]; // username
    String url = args[1];// sftp://domain.com/path

    try (StandardFileSystemManager fm = new StandardFileSystemManager()) {
      fm.init();

      FileSystemOptions options = new FileSystemOptions();
      SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder.getInstance();
      builder.setStrictHostKeyChecking(options, "no");
      builder.setUserDirIsRoot(options, false);
      builder.setCompression(options, "zlib,none");
      builder.setDisableDetectExecChannel(options, true); // see https://issues.apache.org/jira/browse/VFS-818

      //Add Support for Pageant. Only set if you only want to use pageant
      builder.setIdentityRepositoryFactory(options, jsch -> {
        try {
          AgentConnector con = new PageantConnector();
          return new AgentIdentityRepository(con);
        } catch (AgentProxyException | RuntimeException e) {
          System.err.println("Unable to load PageantConnector");
          e.printStackTrace();
          return null;
        }
      });

      DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(options, types -> {
        UserAuthenticationData userAuthenticationData = new UserAuthenticationData();
        if (Arrays.asList(types).contains(UserAuthenticationData.USERNAME)) {
          userAuthenticationData.setData(UserAuthenticationData.USERNAME, user.toCharArray());
        }
        return userAuthenticationData;
      });


      FileObject fileObject = fm.resolveFile(url, options);

      for (FileObject child : fileObject.getChildren()) {
        System.out.println(child.getName());
      }
    }

  }
}