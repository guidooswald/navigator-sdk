// (c) Copyright 2015 Cloudera, Inc. All rights reserved.
package com.cloudera.nav.plugin.client.writer;

import com.cloudera.nav.plugin.client.PluginConfigurations;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.apache.commons.net.util.Base64;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * A factory that returns the appropriate MetadataWriter given a set of
 * configurations
 */
public class MetadataWriterFactory {

  public static final String HDFS = "hdfs";
  public static final String LOCAL = "file";
  public static final String HTTP = "http";

  /**
   * Return lower cased metadata parent uri scheme. If null then empty
   * string is returned (for easier string comparisons).
   *
   * @param config
   * @return
   */
  @VisibleForTesting
  static String getScheme(PluginConfigurations config) {
    URI uri = config.getMetadataParentUri();
    String scheme = uri.getScheme();
    return scheme == null ? "" : scheme.toLowerCase();
  }

  /**
   * Create a new metadata writer based on the given configurations.
   * The configurations should determine the file format and the underlying
   * storage mechanism.
   *
   * @param config
   * @return
   */
  public MetadataWriter newWriter(PluginConfigurations config) {
    String scheme = getScheme(config);
    if (scheme.equals(HDFS) ) {
      throw new UnsupportedOperationException();
    } else if (scheme.equals(LOCAL)) {
      throw new UnsupportedOperationException();
    } else {
      try {
        HttpURLConnection conn = createHttpStream(config);
        Writer dataWriter = null;
        dataWriter = new BufferedWriter(new OutputStreamWriter(
            conn.getOutputStream()));
        return new HttpJsonMetadataWriter(config, dataWriter, conn);
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
  }

  private HttpURLConnection createHttpStream(PluginConfigurations config)
      throws IOException {
    URL url = new URL(config.getMetadataParentUri().toASCIIString());
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    String userpass = config.getUsername() + ":" + config.getPassword();
    String basicAuth = "Basic " + new String(Base64.encodeBase64(
        userpass.getBytes()));
    conn.addRequestProperty("Authorization", basicAuth);
    conn.addRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);
    config.setProperty("conn", conn);
    return conn;
  }

  private OutputStream createLocalFileStream(PluginConfigurations config) {
    String fileName = getFilePath(config.getMetadataParentUri().getPath());
    File file = new File(fileName);
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    try {
      return new FileOutputStream(file);
    } catch (FileNotFoundException e) {
      throw Throwables.propagate(e);
    }
  }

  private OutputStream createHdfsStream(PluginConfigurations config) {
    try {
      FileSystem fs = FileSystem.get(config.getHadoopConfigurations());
      Path path = new Path(getFilePath(config.getMetadataParentUriString()));
      if (fs.exists(path)) {
        return fs.append(path);
      }
      // TODO block sizes, replication counts etc
      return fs.create(path);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private String getFilePath(String path) {
    // TODO file rotation
    if (!path.endsWith("/")) {
      path += "/";
    }
    return path + ".metadata";
  }

}
