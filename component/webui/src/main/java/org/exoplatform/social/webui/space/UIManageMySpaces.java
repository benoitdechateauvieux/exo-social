/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.social.webui.space;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * UIManageMySpaces.java <br />
 * Manage all user's spaces, user can edit, delete, leave space.
 * User can create new space here. <br />
 *
 * Created by The eXo Platform SAS
 * @author hoatle <hoatlevan at gmail dot com>
 * @since Jun 29, 2009
 */
@ComponentConfig(
  template="classpath:groovy/social/webui/space/UIManageMySpaces.gtmpl",
  events = {
    @EventConfig(listeners = UIManageMySpaces.EditSpaceActionListener.class),
    @EventConfig(listeners = UIManageMySpaces.DeleteSpaceActionListener.class,
                 confirm = "UIManageMySpaces.msg.confirm_space_delete"),
    @EventConfig(listeners = UIManageMySpaces.LeaveSpaceActionListener.class)
  }
)
public class UIManageMySpaces extends UIContainer {
  private static final String MSG_WARNING_LEAVE_SPACE = "UIManageMySpaces.msg.warning_leave_space";
  private static final String MSG_ERROR_LEAVE_SPACE = "UIManageMySpaces.msg.error_leave_space";
  private static final String MSG_ERROR_DELETE_SPACE = "UIManageMySpaces.msg.error_delete_space";
  private static final Integer LEADER = 1, MEMBER = 2;
  private static final String SPACE_DELETED_INFO = "UIManageMySpaces.msg.DeletedInfo";
  private static final String MEMBERSHIP_REMOVED_INFO = "UIManageMySpaces.msg.MemberShipRemovedInfo";
  private static final String NAVIGATION_REMOVED_INFO = "UIManageMySpaces.msg.NavigationRemovedInfo";
  private static final String CONFIRMED_STATUS = "confirmed";
  
  /**
   * SEARCH ALL.
   */
  private static final String SEARCH_ALL = "All";

  /**
   * The first page.
   */
  private static final int FIRST_PAGE = 1;


  private UIPageIterator iterator;
  private final Integer SPACES_PER_PAGE = 4;
  private final String ITERATOR_ID = "UIIteratorMySpaces";
  private SpaceService spaceService = null;
  private String userId = null;
  private List<UserNavigation> navigations;
  private UserNavigation selectedNavigation;
  private List<Space> spaces; // for search result
  private UISpaceSearch uiSpaceSearch = null;

  /**
   * Constructor for initialize UIPopupWindow for adding new space popup.
   *
   * @throws Exception
   */
  public UIManageMySpaces() throws Exception {
    uiSpaceSearch = createUIComponent(UISpaceSearch.class, null, "UISpaceSearch");
    uiSpaceSearch.setTypeOfRelation(CONFIRMED_STATUS);
    addChild(uiSpaceSearch);
    iterator = addChild(UIPageIterator.class, null, ITERATOR_ID);
  }

  /**
   * Gets uiPageIterator.
   *
   * @return uiPageIterator
   */
  public UIPageIterator getMySpacesUIPageIterator() {
    return iterator;
  }

  /**
   * Gets all user's spaces.
   *
   * @return user spaces
   * @throws Exception
   */
  public List<Space> getAllUserSpaces() throws Exception {
    SpaceService spaceService = getSpaceService();
    String userId = getUserId();
    List<Space> userSpaces = spaceService.getAccessibleSpaces(userId);
    //reload navigation BUG #SOC-555
//    SpaceUtils.reloadNavigation();
    return SpaceUtils.getOrderedSpaces(userSpaces);
  }

  /**
   * Gets selected navigation.
   *
   * @return page navigation
   */
  public UserNavigation getSelectedNavigation() {
    return selectedNavigation;
  }

  /**
   * Sets selected navigation.
   *
   * @param navigation
   */
  public void setSelectedNavigation(UserNavigation navigation) {
    selectedNavigation = navigation;
  }

  /**
   * Gets paginated spaces in which user is member or leader.
   *
   * @return paginated spaces list
   * @throws Exception
   */
  public List<Space> getUserSpaces() throws Exception {
    uiSpaceSearch.setSpaceNameForAutoSuggest(getAllMySpaceNames());
    return getDisplayMySpace(iterator);
  }

  /**
   * Gets role of the user in a specific space for displaying in template.
   *
   * @param spaceId
   * @return UIManageMySpaces.LEADER if the remote user is the space's leader <br />
   *         UIManageMySpaces.MEMBER if the remote user is the space's member
   * @throws SpaceException
   */
  public int getRole(String spaceId) throws SpaceException {
    SpaceService spaceService = getSpaceService();
    Space space = spaceService.getSpaceById(spaceId);
    String userId = getUserId();
    if (spaceService.hasSettingPermission(space, userId)) {
      return LEADER;
    }
    return MEMBER;
  }

  /**
   * Checks in case root has membership with current space.
   *
   * @param spaceId
   * @return true or false
   * @throws SpaceException
   */
  public boolean hasMembership(String spaceId) throws SpaceException {
    SpaceService spaceService = getSpaceService();
    String userId = getUserId();
    Space space = spaceService.getSpaceById(spaceId);
    return spaceService.isMember(space, userId);
  }

  /**
   * Sets space list.
   *
   * @param spaces
   */
  public void setSpaces(List<Space> spaces) {
    this.spaces = spaces;
  }

  /**
   * Gets space list.
   *
   * @return space list
   */
  public List<Space> getSpaces() {
    return spaces;
  }

  /**
   * Gets image source url.
   *
   * @param space
   * @return image source url
   * @throws Exception
   */
  public String getImageSource(Space space) throws Exception {
    return space.getAvatarUrl();
  }


  /**
   * This action is triggered when user click on EditSpace Currently, when user click on EditSpace,
   * they will be redirected to /xxx/SpaceSettingPortlet When user click on editSpace, the user is
   * redirected to SpaceSettingPortlet.
   */
  static public class EditSpaceActionListener extends EventListener<UIManageMySpaces> {

    @Override
    public void execute(Event<UIManageMySpaces> event) throws Exception {
      UIManageMySpaces uiMySpaces = event.getSource();
      WebuiRequestContext ctx = event.getRequestContext();
      UIApplication uiApp = ctx.getUIApplication();
      SpaceService spaceService = uiMySpaces.getSpaceService();
      Space space = spaceService.getSpaceById(ctx.getRequestParameter(OBJECTID));
      if (space == null) {
        uiApp.addMessage(new ApplicationMessage("UIManageMySpaces.msg.warning_space_not_available",
                                                null, ApplicationMessage.WARNING));
      }
      OrganizationService organizationService = SpaceUtils.getOrganizationService();
      Group group = organizationService.getGroupHandler().findGroupById(space.getGroupId());
      if (group == null) {
        uiApp.addMessage(new ApplicationMessage("UIManageMySpaces.msg.group_unable_to_retrieve",
                                                null, ApplicationMessage.ERROR));
        return;
      } else {
        String spaceUrl = Util.getPortalRequestContext().getPortalURI();
        String spaceSettingUri = uiMySpaces.getNodeUri(space, "SpaceSettingPortlet");
        String spaceSettingUrl = spaceUrl + spaceSettingUri;
        PortalRequestContext prContext = Util.getPortalRequestContext();
        prContext.setResponseComplete(true);
        prContext.getResponse().sendRedirect(spaceSettingUrl);
      }
    }
  }

  /**
   * This action trigger when user click on back button from UINavigationManagement.
   *
   * @author hoatle
   */
  static public class BackActionListener extends EventListener<UIPageNodeForm> {

    @Override
    public void execute(Event<UIPageNodeForm> event) throws Exception {
      UIPageNodeForm uiPageNode = event.getSource();
      UserNavigation contextNavigation = uiPageNode.getContextPageNavigation();
      UIManageMySpaces uiMySpaces = uiPageNode.getAncestorOfType(UIManageMySpaces.class);
      UIPopupWindow uiPopup = uiMySpaces.getChild(UIPopupWindow.class);
      UISpaceNavigationManagement navigationManager = uiMySpaces.createUIComponent(UISpaceNavigationManagement.class, null, null);
      navigationManager.setOwner(contextNavigation.getKey().getName());
      navigationManager.setOwnerType(contextNavigation.getKey().getTypeName());
      UISpaceNavigationNodeSelector selector = navigationManager.getChild(UISpaceNavigationNodeSelector.class);
      selector.setEdittedNavigation(contextNavigation);
      selector.initTreeData();
      uiPopup.setUIComponent(navigationManager);
      uiPopup.setWindowSize(400, 400);
      uiPopup.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMySpaces);
    }

  }

  /**
   * This action is triggered when user click on DeleteSpace a prompt popup is display for
   * confirmation, if yes delete that space; otherwise, do nothing.
   */
  static public class DeleteSpaceActionListener extends EventListener<UIManageMySpaces> {

    @Override
    public void execute(Event<UIManageMySpaces> event) throws Exception {
      UIManageMySpaces uiMySpaces = event.getSource();
      SpaceService spaceService = uiMySpaces.getSpaceService();
      WebuiRequestContext ctx = event.getRequestContext();
      UIApplication uiApp = ctx.getUIApplication();
      String spaceId = ctx.getRequestParameter(OBJECTID);
      Space space = spaceService.getSpaceById(spaceId);
      String userId = uiMySpaces.getUserId();

      if (space == null) {
        uiApp.addMessage(new ApplicationMessage(SPACE_DELETED_INFO, null, ApplicationMessage.INFO));
        return;
      }

      if (!spaceService.isMember(space, userId) && !spaceService.hasSettingPermission(space, userId)) {
        uiApp.addMessage(new ApplicationMessage(MEMBERSHIP_REMOVED_INFO, null, ApplicationMessage.INFO));
        return;
      }

      spaceService.deleteSpace(space);
      SpaceUtils.updateWorkingWorkSpace();
    }

  }

  /**
   * This action is triggered when user click on LeaveSpace <br /> The leaving space will remove
   * that user in the space. <br /> If that user is the only leader -> can't not leave that space
   * <br />
   */
  static public class LeaveSpaceActionListener extends EventListener<UIManageMySpaces> {
    public void execute(Event<UIManageMySpaces> event) throws Exception {
      UIManageMySpaces uiMySpaces = event.getSource();
      SpaceService spaceService = uiMySpaces.getSpaceService();
      WebuiRequestContext ctx = event.getRequestContext();
      UIApplication uiApp = ctx.getUIApplication();
      String spaceId = ctx.getRequestParameter(OBJECTID);
      String userId = uiMySpaces.getUserId();
      Space space = spaceService.getSpaceById(spaceId);

      if (space == null) {
        uiApp.addMessage(new ApplicationMessage(SPACE_DELETED_INFO, null, ApplicationMessage.INFO));
        return;
      }

      if (!spaceService.isMember(space, userId) && !spaceService.hasSettingPermission(space, userId)) {
        uiApp.addMessage(new ApplicationMessage(MEMBERSHIP_REMOVED_INFO, null, ApplicationMessage.INFO));
        return;
      }

      if (spaceService.isOnlyManager(space, userId)) {
        uiApp.addMessage(new ApplicationMessage(MSG_WARNING_LEAVE_SPACE, null, ApplicationMessage.WARNING));
        return;
      }

      spaceService.removeMember(space, userId);
      spaceService.setManager(space, userId, false);
      SpaceUtils.updateWorkingWorkSpace();
    }
  }

  /**
   * Gets spaceService.
   *
   * @return spaceService
   * @see SpaceService
   */
  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = getApplicationComponent(SpaceService.class);
    }
    return spaceService;
  }

  /**
   * Gets remote user Id.
   *
   * @return remote userId
   */
  private String getUserId() {
    if (userId == null) {
      userId = Util.getPortalRequestContext().getRemoteUser();
    }
    return userId;
  }

  /**
   * Gets display my space list.
   *
   * @param spaces_
   * @param pageIterator_
   * @return display my space list
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private List<Space> getDisplayMySpace(UIPageIterator pageIterator_) throws Exception {
    int currentPage = pageIterator_.getCurrentPage();
    String selectedChar = this.uiSpaceSearch.getSelectedChar();
    String spaceNameSearch = this.uiSpaceSearch.getSpaceNameSearch();
    LazyPageList<Space> pageList = null;
    if ((selectedChar == null && spaceNameSearch == null) || (selectedChar != null && selectedChar.equals(SEARCH_ALL))) {
      pageList = new LazyPageList<Space>(spaceService.getAccessibleSpacesWithListAccess(userId), SPACES_PER_PAGE);
    } else {
      SpaceFilter spaceFilter = null;
      if (selectedChar != null) {
        spaceFilter = new SpaceFilter(selectedChar.charAt(0));
      } else {
        spaceFilter = new SpaceFilter(spaceNameSearch);
      }
      pageList = new LazyPageList<Space>(spaceService.getAccessibleSpacesByFilter(userId, spaceFilter), SPACES_PER_PAGE);
    }
    pageIterator_.setPageList(pageList);
    int availablePage = pageIterator_.getAvailablePage();
    if (this.uiSpaceSearch.isNewSearch()) {
      pageIterator_.setCurrentPage(FIRST_PAGE);
    } else if (currentPage > availablePage) {
      pageIterator_.setCurrentPage(availablePage);
    } else {
      pageIterator_.setCurrentPage(currentPage);
    }
    this.uiSpaceSearch.setNewSearch(false);
    return pageIterator_.getCurrentPageData();
  }

  /**
   * Gets all my space names.
   *
   * @return my space names
   * @throws Exception
   */
  private List<String> getAllMySpaceNames() throws Exception {
    List<Space> allSpaces = getAllUserSpaces();
    List<String> allSpacesNames = new ArrayList<String>();
    for (Space space : allSpaces) {
      allSpacesNames.add(space.getDisplayName());
    }
    return allSpacesNames;
  }

  /**
   * Gets node's name base on application name.
   *
   * @param space
   * @param appId
   * @throws SpaceException
   */
  private String getNodeUri(Space space, String appId) throws SpaceException {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    DataStorage dataStorage = (DataStorage) container.getComponentInstanceOfType(DataStorage.class);
    try {
      String groupId = space.getGroupId();
      UserNavigation nav = SpaceUtils.getGroupNavigation(groupId);
      // return in case group navigation was removed by portal SOC-548
      if (nav == null) {
        return null;
      }
      UserNode homeNode = SpaceUtils.getHomeNodeWithChildren(nav, space.getUrl());
      if (homeNode == null) {
        throw new Exception("homeNode is null!");
      }
      String nodeName = SpaceUtils.getAppNodeName(space, appId);
      UserNode childNode = homeNode.getChild(nodeName);
      //bug from portal, gets by nodeUri instead
      if (childNode != null) {
        return childNode.getURI();
      } else {
        Collection<UserNode> pageNodes = homeNode.getChildren();
        for (UserNode pageNode : pageNodes) {
          String pageReference = pageNode.getPageRef();
          if (pageReference.contains(nodeName)) {
            return pageNode.getURI();
          }
        }
      }
      return null;
    } catch (Exception e) {
      throw new SpaceException(SpaceException.Code.UNABLE_TO_REMOVE_APPLICATION, e);
    }
  }
}

