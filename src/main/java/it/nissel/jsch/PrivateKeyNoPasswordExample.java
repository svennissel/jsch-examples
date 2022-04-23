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
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import java.io.File;

public class PrivateKeyNoPasswordExample {
  public static void main(String[] args) throws FileSystemException {
    String user = args[0]; // root
    String sshKey = args[1];// C:\private.ppk
    String url = args[2];// sftp://domain.com/path

    try (StandardFileSystemManager fm = new StandardFileSystemManager()) {
      fm.init();

      FileSystemOptions options = new FileSystemOptions();
      SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder.getInstance();
      builder.setStrictHostKeyChecking(options, "no");
      builder.setUserDirIsRoot(options, false);
      builder.setCompression(options, "zlib,none");
      builder.setIdentityRepositoryFactory(options, jsch -> {
        try {
          AgentConnector con = new PageantConnector();
          return new AgentIdentityRepository(con);
        } catch (AgentProxyException | RuntimeException e) {
          System.err.println("Unable to load PageantConnector: " + e.getMessage());
          return null;
        }
      });
      builder.setDisableDetectExecChannel(options, true); // see https://issues.apache.org/jira/browse/VFS-818

      SftpFileSystemConfigBuilder.getInstance().setIdentityProvider(options, new IdentityInfo(new File(sshKey)));

      DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(options, types -> {
        UserAuthenticationData userAuthenticationData = new UserAuthenticationData();
        userAuthenticationData.setData(UserAuthenticationData.USERNAME, user.toCharArray());
        userAuthenticationData.setData(new UserAuthenticationData.Type("sshKey"), sshKey.toCharArray());
        return userAuthenticationData;
      });


      FileObject fileObject = fm.resolveFile(url, options);

      for (FileObject child : fileObject.getChildren()) {
        System.out.println(child.getName());
      }
    }

  }
}