/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.webui.connections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.social.webui.profile.UIProfileUserSearch;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;

/**
 * Manages pending relation of all existing users. Manages actions
 * such as accept or deny invitation and search action.<br>
 *   - Get all users that have pending relation.<br>
 *   - Check the status of each user with current user then display the list.<br>
 *   - Listens to event: deny contact and search action.<br>
 *
 * Author : dang.tung
 *          tungcnw@gmail.com
 * Aug 25, 2009
 */
@ComponentConfig(
  template =  "classpath:groovy/social/webui/connections/UIPendingRelation.gtmpl",
  events = {
    @EventConfig(listeners = UIPendingRelation.IgnoreActionListener.class),
    @EventConfig(listeners = UIPendingRelation.SearchActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPendingRelation.LoadMorePeopleActionListener.class)
  }
)
public class UIPendingRelation extends UIContainer {
  private static final Log LOG = ExoLogger.getLogger(UIPendingRelation.class);
  
  /** Label for display invitation is revoked information */
  private static final String INVITATION_REVOKED_INFO = "UIPendingRelation.label.RevokedInfo";

  /** Pending Status information */
  private static final String PENDING_STATUS = "pending";
  
  /** Stores UIProfileUserSearch instance. */
  UIProfileUserSearch uiProfileUserSearch = null;

  /**
   * Default the number of relationships per page.
   * 
   * @since 1.2.0-Beta3
   */
  private static final int SENT_INVITATION_PER_PAGE = 45;
  
  private boolean loadAtEnd = false;
  private boolean hasUpdated = false;
  private int currentLoadIndex;
  private boolean enableLoadNext;
  private int loadingCapacity;
  private List<Identity> peopleList;
  private ListAccess<Identity> peopleListAccess;
  private int peopleNum;
  
  /**
   * Initializes components and add as child of form.<br>
   *
   * @throws Exception
   */
  public UIPendingRelation() throws Exception {
    uiProfileUserSearch = createUIComponent(UIProfileUserSearch.class, null, "UIProfileUserSearch");
    uiProfileUserSearch.setTypeOfRelation(PENDING_STATUS);
    uiProfileUserSearch.setHasPeopleTab(true);
    uiProfileUserSearch.setHasConnectionLink(false);
    addChild(uiProfileUserSearch);
    init();
  }
  
  /**
   * Inits at the first loading.
   * 
   * @since 1.2.2
   */
  public void init() {
    try {
      setHasUpdatedIdentity(false);
      setLoadAtEnd(false);
      enableLoadNext = false;
      currentLoadIndex = 0;
      loadingCapacity = SENT_INVITATION_PER_PAGE;
      peopleList = new ArrayList<Identity>();
      List<Identity> excludedIdentityList = new ArrayList<Identity>();
      excludedIdentityList.add(Utils.getViewerIdentity());
      uiProfileUserSearch.getProfileFilter().setExcludedIdentityList(excludedIdentityList);
      setPeopleList(loadPeople(currentLoadIndex, loadingCapacity));
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }
  
  /**
   * Sets loading capacity.
   * 
   * @param loadingCapacity
   */
  public void setLoadingCapacity(int loadingCapacity) {
    this.loadingCapacity = loadingCapacity;
  }

  /**
   * Gets flag to display LoadNext button or not.
   * 
   * @return the enableLoadNext
   * @since 1.2.2
   */
  public boolean isEnableLoadNext() {
    return enableLoadNext;
  }

  /**
   * Sets flag to display LoadNext button or not.
   * 
   * @param enableLoadNext the enableLoadNext to set
   * @since 1.2.2
   */
  public void setEnableLoadNext(boolean enableLoadNext) {
    this.enableLoadNext = enableLoadNext;
  }

  /**
   * Gets flags to clarify that load at the last element or not. 
   * 
   * @return the loadAtEnd
   * @since 1.2.2
   */
  public boolean isLoadAtEnd() {
    return loadAtEnd;
  }

  /**
   * Sets flags to clarify that load at the last element or not.
   * 
   * @param loadAtEnd the loadAtEnd to set
   * @since 1.2.2
   */
  public void setLoadAtEnd(boolean loadAtEnd) {
    this.loadAtEnd = loadAtEnd;
  }

  /**
   * Gets information that clarify one element is updated or not.
   * 
   * @return the hasUpdatedIdentity
   * @since 1.2.2
   */
  public boolean isHasUpdatedIdentity() {
    return hasUpdated;
  }

  /**
   * Sets information that clarify one element is updated or not.
   * 
   * @param hasUpdatedIdentity the hasUpdatedIdentity to set
   * @since 1.2.2
   */
  public void setHasUpdatedIdentity(boolean hasUpdatedIdentity) {
    this.hasUpdated = hasUpdatedIdentity;
  }

  /**
   * Gets list of all type of people.
   * 
   * @return the peopleList
   * @throws Exception 
   * @since 1.2.2
   */
  public List<Identity> getPeopleList() throws Exception {
    if (isHasUpdatedIdentity()) {
      setHasUpdatedIdentity(false);
      setPeopleList(loadPeople(0, this.peopleList.size()));
    }
    
    int realPeopleListSize = this.peopleList.size();

    setEnableLoadNext((realPeopleListSize >= SENT_INVITATION_PER_PAGE)
            && (realPeopleListSize < getPeopleNum()));
    
    return this.peopleList;
  }

  /**
   * Sets list of all type of people.
   * 
   * @param peopleList the peopleList to set
   * @since 1.2.2
   */
  public void setPeopleList(List<Identity> peopleList) {
    this.peopleList = peopleList;
  }
  
  /**
   * Gets number of people for displaying.
   * 
   * @return the peopleNum
   * @since 1.2.2
   */
  public int getPeopleNum() {
    return peopleNum;
  }

  /**
   * Sets number of people for displaying.
   * 
   * @param peopleNum the peopleNum to set
   * @since 1.2.2
   */
  public void setPeopleNum(int peopleNum) {
    this.peopleNum = peopleNum;
  }

  /**
   * Gets people with ListAccess type.
   * 
   * @return the peopleListAccess
   * @since 1.2.2
   */
  public ListAccess<Identity> getPeopleListAccess() {
    return peopleListAccess;
  }

  /**
   * Sets people with ListAccess type.
   * 
   * @param peopleListAccess the peopleListAccess to set
   * @since 1.2.2
   */
  public void setPeopleListAccess(ListAccess<Identity> peopleListAccess) {
    this.peopleListAccess = peopleListAccess;
  }

  /**
   * Loads more people.
   * 
   * @throws Exception
   * @since 1.2.2
   */
  public void loadNext() throws Exception {
    currentLoadIndex += loadingCapacity;
    if (currentLoadIndex <= getPeopleNum()) {
      List<Identity> currentPeopleList = new ArrayList<Identity>(this.peopleList);
      List<Identity> loadedPeople = new ArrayList<Identity>(Arrays.asList(getPeopleListAccess()
                    .load(currentLoadIndex, loadingCapacity)));
      currentPeopleList.addAll(loadedPeople);
      setPeopleList(currentPeopleList);
    }
  }
  
  /**
   * Loads people when searching.
   * 
   * @throws Exception
   * @since 1.2.2
   */
  public void loadSearch() throws Exception {
    currentLoadIndex = 0;
    setPeopleList(loadPeople(currentLoadIndex, loadingCapacity));
  }
  
  private List<Identity> loadPeople(int index, int length) throws Exception {

    Identity owner = Utils.getOwnerIdentity();

    ProfileFilter filter = uiProfileUserSearch.getProfileFilter();

    ListAccess<Identity> listAccess = Utils.getRelationshipManager().getOutgoingByFilter(owner, filter);
    Identity[] identities = listAccess.load(index, length);

    setPeopleNum(listAccess.getSize());
    setPeopleListAccess(listAccess);
    uiProfileUserSearch.setPeopleNum(listAccess.getSize());

    return Arrays.asList(identities);
    
  }
  
  /**
   * Listeners loading more people action.
   * 
   * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
   * @since Aug 18, 2011
   * @since 1.2.2
   */
  public static class LoadMorePeopleActionListener extends EventListener<UIPendingRelation> {
    public void execute(Event<UIPendingRelation> event) throws Exception {
      UIPendingRelation uiPendingRelation = event.getSource();
      if (uiPendingRelation.currentLoadIndex < uiPendingRelation.peopleNum) {
        uiPendingRelation.loadNext();
      } else {
        uiPendingRelation.setEnableLoadNext(false);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPendingRelation);
    }
  }
  
  /**
   * Listens to deny action then delete the invitation.<br>
   *   - Gets information of user is invited or made request.<br>
   *   - Checks the relation to confirm that have got pending relation.<br>
   *   - Removes the current pending relation and save the new relation.<br>
   *
   */
  public static class IgnoreActionListener extends EventListener<UIPendingRelation> {
    public void execute(Event<UIPendingRelation> event) throws Exception {
      UIPendingRelation uiPendingRelation = event.getSource();
      String userId = event.getRequestContext().getRequestParameter(OBJECTID);
      Identity invitedIdentity = Utils.getIdentityManager().getIdentity(userId, true);
      Identity invitingIdentity = Utils.getViewerIdentity();
      uiPendingRelation.setLoadAtEnd(false);
      Relationship relationship = Utils.getRelationshipManager().get(invitingIdentity, invitedIdentity);
      if (relationship ==null || relationship.getStatus() != Relationship.Type.PENDING) {
        UIApplication uiApplication = event.getRequestContext().getUIApplication();
        uiApplication.addMessage(new ApplicationMessage(INVITATION_REVOKED_INFO, null, ApplicationMessage.INFO));
        return;
      }
      
      uiPendingRelation.setHasUpdatedIdentity(true);
      Utils.getRelationshipManager().deny(invitedIdentity, invitingIdentity);
    }
  }

  /**
   * Listens event that broadcast from UIProfileUserSearch.
   * 
   * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
   * @since Aug 25, 2011
   */
  public static class SearchActionListener extends EventListener<UIPendingRelation> {
    @Override
    public void execute(Event<UIPendingRelation> event) throws Exception {
      UIPendingRelation uiPendingRelation = event.getSource();
      uiPendingRelation.loadSearch();
      uiPendingRelation.setLoadAtEnd(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPendingRelation);
    }
  }

  /**
   * Return true to accept user is viewing can edit.
   *
   * @return true if current user is current login user.
   */
  public boolean isEditable () {
    return Utils.isOwner();
  }
}
