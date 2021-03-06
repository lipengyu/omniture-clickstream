package com.github.bartekdobija.omniture.loader;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * S3 backed {@link DataLoader} implementation.
 * @since 0.1
 */
public class S3DataLoader implements DataLoader {

  private static final String PROXY_HOST_KEY = "http.proxyHost";
  private static final String PROXY_PORT_KEY = "http.proxyPort";

  private String source;
  private InputStream is;

  private String proxyHost;
  private int proxyPort;

  AWSCredentialsProviderChain credentials;
  ClientConfiguration awsConf;

  public S3DataLoader(String url) {
    this(url,
         System.getProperty(PROXY_HOST_KEY),
         System.getProperty(PROXY_PORT_KEY)
    );
  }

  public S3DataLoader(
      String url,
      String proxyHost,
      String proxyPort) {

    source = url;
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort == null ? -1 : Integer.parseInt(proxyPort);

    credentials = new AWSCredentialsProviderChain(
        new SystemPropertiesCredentialsProvider(),
        new EnvironmentVariableCredentialsProvider(),
        new InstanceProfileCredentialsProvider()
    );
  }

  @Override
  public InputStream stream() throws DataLoaderException {

    if (source == null) {
      throw new DataLoaderException("data source cannot be empty");
    }

    awsConf = new ClientConfiguration();
    awsConf.setProxyHost(proxyHost);
    awsConf.setProxyPort(proxyPort);

    AmazonS3Client s3 = new AmazonS3Client(credentials, awsConf);

    URI uri;
    try {
      uri = new URI(source);
    } catch (URISyntaxException e) {
      throw new DataLoaderException(e);
    }

    String bucket = uri.getHost();
    String key = uri.getPath().substring(1);
    is = s3.getObject(new GetObjectRequest(bucket, key)).getObjectContent();

    return is;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public void close() {
    if(is != null) try { is.close(); } catch (IOException e) {}
  }

  public void setSource(String source) {
    this.source = source;
  }
}
