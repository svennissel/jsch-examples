package it.nissel.jsch;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import java.io.File;
import java.util.Arrays;

public class PrivateKeyWithPasswordExample {
  public static void main(String[] args) throws FileSystemException {
    String url = args[0];// sftp://domain.com/path
    String user = args[1]; // username
    String sshKey = args[2];// C:\private.ppk
    String passwordKeyFile = args[3]; //Password to open private keyfile


    try (StandardFileSystemManager fm = new StandardFileSystemManager()) {
      fm.init();

      FileSystemOptions options = new FileSystemOptions();
      SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder.getInstance();
      builder.setStrictHostKeyChecking(options, "no");
      builder.setUserDirIsRoot(options, false);
      builder.setCompression(options, "zlib,none");
      builder.setDisableDetectExecChannel(options, true); // see https://issues.apache.org/jira/browse/VFS-818

      //Set private key with protected password
      SftpFileSystemConfigBuilder.getInstance().setIdentityProvider(options, new IdentityInfo(new File(sshKey), passwordKeyFile.getBytes()));

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