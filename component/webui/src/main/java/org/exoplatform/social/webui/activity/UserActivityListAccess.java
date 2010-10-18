/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.webui.activity;

import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.Activity;
import org.exoplatform.social.core.activity.model.Util;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.profile.UIUserActivitiesDisplay;

/**
 * UserActivityListAccess
 * <p></p>
 *
 * @author Zuanoc
 * @copyright eXo SEA
 * @since Sep 7, 2010
 */
public class UserActivityListAccess implements ListAccess<Activity> {
  static private final Log LOG = ExoLogger.getLogger(UserActivityListAccess.class);

  private Identity ownerIdentity;
  private UIUserActivitiesDisplay.DisplayMode displayMode;
  private ActivityManager activityManager;

  /**
   * @param ownerIdentity
   * @param displayMode
   */
  public UserActivityListAccess(Identity ownerIdentity, UIUserActivitiesDisplay.DisplayMode displayMode) {
    //identityManager = (IdentityManager) PortalContainer.getComponent(IdentityManager.class);
    activityManager = (ActivityManager) PortalContainer.getComponent(ActivityManager.class);
    //spaceService = (SpaceService) PortalContainer.getComponent(SpaceService.class);

    this.ownerIdentity = ownerIdentity;
    this.displayMode = displayMode;
  }

  public int getSize() throws Exception {
    int size;
    if (displayMode == UIUserActivitiesDisplay.DisplayMode.MY_STATUS || displayMode == UIUserActivitiesDisplay.DisplayMode.OWNER_STATUS) {
      size = activityManager.getActivitiesCount(ownerIdentity);
    } else if (displayMode == UIUserActivitiesDisplay.DisplayMode.SPACES) {
      size = activityManager.getActivitiesOfUserSpaces(ownerIdentity).size();
    } else {
      size = activityManager.getActivitiesOfConnections(ownerIdentity).size();
    }

    return size;
  }

  public Activity[] load(int index, int length) throws Exception{
    List<Activity> activityList;
    if (displayMode == UIUserActivitiesDisplay.DisplayMode.MY_STATUS || displayMode == UIUserActivitiesDisplay.DisplayMode.OWNER_STATUS) {
      activityList = activityManager.getActivities(ownerIdentity, index, length);
    } else if (displayMode == UIUserActivitiesDisplay.DisplayMode.SPACES) {
      activityList = getActivitiesOfUserSpaces(index, length);
    } else {
      activityList = getActivitiesOfConnections(index, length);
    }

    return activityList.toArray(new Activity[activityList.size()]);
  }

  private List<Activity> getActivitiesOfConnections(int index, int length) throws Exception {
    List<Activity> activityList = activityManager.getActivitiesOfConnections(ownerIdentity);
    Collections.sort(activityList, Util.activityComparator());
    try {
      return activityList.subList(index, index + length);
    } catch (Exception e) {
      return activityList.subList(index, activityList.size() - 1);
    }

  }

  private List<Activity> getActivitiesOfUserSpaces(int index, int length) {
    List<Activity> activityList = activityManager.getActivitiesOfUserSpaces(ownerIdentity);
    Collections.sort(activityList, Util.activityComparator());
    try {
      return activityList.subList(index, index + length);
    } catch(Exception e) {
      return activityList.subList(index, activityList.size() - 1);
    }
  }

}
