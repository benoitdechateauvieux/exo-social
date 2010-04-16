package org.exoplatform.social.benches;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.benches.util.LoremIpsum4J;
import org.exoplatform.social.benches.util.NameGenerator;
import org.exoplatform.social.core.activitystream.ActivityManager;
import org.exoplatform.social.core.activitystream.model.Activity;
import org.exoplatform.social.core.identity.IdentityManager;
import org.exoplatform.social.core.identity.impl.organization.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.model.GlobalId;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.relationship.Relationship;
import org.exoplatform.social.core.relationship.RelationshipManager;
import org.exoplatform.social.space.SpaceService;

/**
 * Injects variable amounts of social key data.
 * 
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice
 *         Lamarque</a>
 * @version $Revision$
 */
public class DataInjector {

  private static Log          LOG = ExoLogger.getLogger(DataInjector.class);

  private ActivityManager     activityManager;

  private IdentityManager     identityManager;

  private RelationshipManager relationshipManager;

  private SpaceService        spaceService;

  private OrganizationService orgnizationservice;

  private UserHandler         userHandler;

  private AtomicInteger       userCount;
  private AtomicInteger       relationshipCount;
  private AtomicInteger       activityCount;
  private AtomicInteger       spaceCount;
  
  private NameGenerator nameGenerator;

  public DataInjector(ActivityManager activityManager,
                      IdentityManager identityManager,
                      RelationshipManager relationshipManager,
                      SpaceService spaceService,
                      OrganizationService organizationService) {
    this.activityManager = activityManager;
    this.identityManager = identityManager;
    this.relationshipManager = relationshipManager;
    this.spaceService = spaceService;
    this.orgnizationservice = organizationService;
    userHandler = orgnizationservice.getUserHandler();
    userCount = new AtomicInteger(0);
    relationshipCount = new AtomicInteger(0);
    activityCount = new AtomicInteger(0);
    spaceCount = new AtomicInteger(0);
    nameGenerator = new NameGenerator();
  }

  /**
   * Generate a variable amount of people
   * 
   * @param count
   * @return identities created
   */
  public Collection<Identity> generatePeople(long count) {
    Collection<Identity> identities = new ArrayList<Identity>();
    for (int i = 0; i < count; i++) {
      Identity identity = generateUser();
      if (identity != null) {
        identities.add(identity);
      }
    }
    return identities;
  }

  public Collection<Relationship> generateRelations(long count) {
    Collection<Relationship> relationships = new ArrayList<Relationship>();
    for (int i = 0; i < count; i++) {
      Relationship relationship = generateRelationship();
      if (relationship != null) {
        relationships.add(relationship);
      }
    }
    return relationships;
  }
  
  
  public Collection<Activity> generateActivities(long count) {
    Collection<Activity> activities = new ArrayList<Activity>();
    for (int i = 0; i < count; i++) {
      Activity activity = generateActivity();
      if (activity != null) {
        activities.add(activity);
      }
    }
    return activities;
  }

  private Activity generateActivity() {
    Identity id1 = selectRandomUser(null);
    Activity activity = null;
    if (id1 != null) {

      try {
        int idx = activityCount.getAndIncrement();
        activity = generateRandomActvity();
        activity.setExternalId("benches:"+ idx);
        activityManager.saveActivity(id1, activity);
        LOG.info("created activity " + activity.getExternalId());
      } catch (Exception e) {
        LOG.error("failed to save activity for " + id1 + ": "  + e.getMessage());
      }

    }
    return activity;    
  }

  private Activity generateRandomActvity() {
    Activity activity = new Activity();
    LoremIpsum4J lorem = new LoremIpsum4J();
    activity.setBody(lorem.getWords(10));
    activity.setTitle(lorem.getParagraphs());
    return activity;
  }

  private Relationship generateRelationship() {
    Identity[] pple = selectUnrelatedUsers();
    Relationship relationship = null;
    if (pple != null) {

      try {
        int idx = relationshipCount.getAndIncrement();
        relationship = relationshipManager.create(pple[0], pple[1]);
        relationshipManager.confirm(relationship);
      } catch (Exception e) {
        LOG.error("failed to create relation between " + pple[0] + " and " + pple[1] + ": "
            + e.getMessage());
      }
      LOG.info("created relation " + relationship + ".");
    }
    return relationship;
  }

  private Identity[] selectUnrelatedUsers() {

    Identity id1 = selectRandomUser(null);
    Identity id2 = selectRandomUser(id1);

    if (id1 != null && id2 != null) {
      return new Identity[] { id1, id2 };
    } else {
      return null;
    }

  }

  private Identity selectRandomUser(Identity except) {

    Identity identity = null;
    int limit = 10;
    String username = null;
    while (identity == null) {
      try {
        username = username(getRandomUserIndex());
        String id = GlobalId.create(OrganizationIdentityProvider.NAME, username).toString();
        identity = identityManager.getIdentity(id);
        if (except != null && except.getId().equals(identity.getId())) {
          identity = null; // continue
        }
      } catch (Exception e) {
        LOG.warn("failed to get identity for " + username + ": " + e.getMessage());
      }
      if (--limit == 0) {
        LOG.warn("failed to select a random user after 10 attempts. Make sure enough identities are populated.");
        break;
      }
    }
    return identity;
  }

  private int getRandomUserIndex() {
    Random rnd = new Random();
    return rnd.nextInt(userCount.get());
  }

  /**
   * Generate a new user
   * 
   * @return
   */
  private Identity generateUser() {
    User user = generateOrgUser();
    Identity identity = generateSocialIdentity(user);
    return identity;
  }

  /**
   * Generate or get an identity for the given user
   * 
   * @param user
   * @return
   */
  private Identity generateSocialIdentity(User user) {
    Identity identity = null;
    if (user != null) {
      String username = null;
      try {
        username = user.getUserName();
        identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username);
      } catch (Exception e) {
        LOG.error("Failed to generate social identity for " + username + ": " + e.getMessage());
      }
    }
    return identity;
  }

  /**
   * generate a new or user with name as bench.userXXX where XXX is an internal
   * counter. The method checks if the user exists and will attempt to find a
   * new name by incrementing the counter
   * 
   * @return
   */
  private User generateOrgUser() {
    User user = null;
    boolean avail = false;
    while (!avail) {
      int idx = userCount.getAndIncrement();
      String username = username(idx);

      LOG.info("creating org user : " + username);
      try {

        user = userHandler.findUserByName(username);
      } catch (Exception e) {
        LOG.warn("failed to check existence of  " + username + ": " + e.getMessage());
      }
      if (user != null) {
        LOG.info(username + " already exists, skipping");
      } else {
        try {
          avail = true;
          user = userHandler.createUserInstance(username);
          initRandomUser(user, username);
          userHandler.createUser(user, true);
        } catch (Exception e) {
          LOG.warn("failed to create user " + username + ": " + e.getMessage());
          return null;
        }
      }
    }
    return user;
  }

  void initRandomUser(User user, String username) {
    user.setEmail(username + "@exoplatform.int");
    user.setFirstName(nameGenerator.compose(3));
    user.setLastName(nameGenerator.compose(4));
    user.setPassword("exo");
  }

  private String username(int idx) {
    return "bench.user" + (idx);
  }

}