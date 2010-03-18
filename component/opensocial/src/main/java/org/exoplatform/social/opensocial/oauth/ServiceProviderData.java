package org.exoplatform.social.opensocial.oauth;

/**
 * An OAuth service provider is a web site that allows eXo to access its data,
 * provided that eXo identifies itself using the OAuth protocol.
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
public class ServiceProviderData {

  /**
   * name of the service provider, I.e the application whose data eXo will be accessing
   */
  private String name;
  
  /**
   * 
   */
  private String description;
  
  /**
   * The key assigned to eXo by this service provider. 
   * The format of this key is determined by the service provider
   */
  private String consumerKey;
  
  /**
   * The consumer secret assigned to eXo by the service provider. 
   * his secret is used to digitally sign all the requests from your application to the service provider.
   */
  private String sharedSecret;
  
  public ServiceProviderData(String name, String description, String consumerKey, String sharedSecret) {
    this.name = name;
    this.description = description;
    this.consumerKey = consumerKey;
    this.sharedSecret = sharedSecret;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getConsumerKey() {
    return consumerKey;
  }

  public void setConsumerKey(String consumerKey) {
    this.consumerKey = consumerKey;
  }

  public String getSharedSecret() {
    return sharedSecret;
  }

  public void setSharedSecret(String sharedSecret) {
    this.sharedSecret = sharedSecret;
  }
  
}
