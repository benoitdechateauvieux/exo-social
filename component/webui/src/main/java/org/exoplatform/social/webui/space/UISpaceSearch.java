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
package org.exoplatform.social.webui.space;

import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Searches space by space name that input by user.<br>
 *   - Search action is listened and information for search space is processed.<br>
 *   - After spaces is requested is returned, the search process is completed,
 *   - Search event is broadcasted to the form that added search form as child.<br>
 *
 * Author : hanhvi
 *          hanhvq@gmail.com
 * Oct 28, 2009
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "classpath:groovy/social/webui/space/UISpaceSearch.gtmpl",
  events = {
    @EventConfig(listeners = UISpaceSearch.AddSpaceActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UISpaceSearch.SearchActionListener.class)
  }
)
public class UISpaceSearch extends UIForm {
  /**
   * SPACE SEARCH.
   */
  public static final String SPACE_SEARCH = "SpaceSearch";

  /**
   * SEARCH.
   */
  public static final String SEARCH = "Search";

  /**
   * SEARCH ALL.
   */
  static final String ALL = "All";

  /**
   * DEFAULT SPACE NAME SEARCH.
   */
  public static final String DEFAULT_SPACE_NAME_SEARCH = "name or description";

  /**
   * INPUT PATTERN FOR CHECKING.
   */
  static final String RIGHT_INPUT_PATTERN = "^[\\p{L}][\\p{L}._\\- \\d]+$";
  
  /**
   * ADD PREFIX TO ENSURE ALWAY RIGHT THE PATTERN FOR CHECKING
   */
  static final String PREFIX_ADDED_FOR_CHECK = "PrefixAddedForCheck";
  
  private final String POPUP_ADD_SPACE = "UIPopupAddSpace";

  /**
   * The spaceService is used for SpaceService instance storage.
   */
  SpaceService spaceService = null;

  /**
   * The spaceList is used for search result storage.
   */
  private List<Space> spaceList = null;

  /**
   * The selectedChar is used for selected character storage when search by alphabet.
   */
  String selectedChar = null;

  /**
   * The spaceNameSearch is used for input space name storage.
   */
  String spaceNameSearch = null;

  /**
   * Contains all space name in individual context for auto suggesting.
   */
  List<String> spaceNameForAutoSuggest = null;

  /**
   * The flag notifies a new search when clicks search icon or presses enter.
   */
  private boolean isNewSearch;

  /**
   * Used stores type of relation with current user information.
   */
  String typeOfRelation = null;

  /**
   * URL of space that this UIComponent is used in member searching.
   */
  String spaceURL = null;

  /**
   * Gets input space name search input.
   *
   * @return Name of space.
   */
  public String getSpaceNameSearch() {
    return spaceNameSearch;
  }

  /**
   * Sets input space name search.
   *
   * @param spaceNameSearch A {@code String}
   */
  public void setSpaceNameSearch(String spaceNameSearch) {
    this.spaceNameSearch = spaceNameSearch;
  }

  /**
   * Gets type of relation with current user.
   */
  public String getTypeOfRelation() {
    return typeOfRelation;
  }

  /**
   * Sets type of relation with current user to variable.
   *
   * @param typeOfRelation <code>char</code>
   */
  public void setTypeOfRelation(String typeOfRelation) {
    this.typeOfRelation = typeOfRelation;
  }

  /**
   * Gets space url.
   */
  public String getSpaceURL() {
    return spaceURL;
  }

  /**
   * Sets space url.
   *
   * @param spaceURL <code>char</code>
   */
  public void setSpaceURL(String spaceURL) {
    this.spaceURL = spaceURL;
  }

  /**
   * Get current user name.
   *
   * @return
   */
  public String getCurrentUserName() {
    RequestContext context = RequestContext.getCurrentInstance();
    return context.getRemoteUser();
  }

  /**
   * Get current rest context name.
   *
   * @return
   */
  protected String getRestContextName() {
    return PortalContainer.getCurrentRestContextName();
  }

  /**
   * Get portal name.
   *
   * @return
   */
  protected String getPortalName() {
    return PortalContainer.getCurrentPortalContainerName();
  }

  /**
   * Gets space name for auto suggesting.
   *
   * @return List of space name.
   */
  public List<String> getSpaceNameForAutoSuggest() {
    return spaceNameForAutoSuggest;
  }

  /**
   * Sets space name for auto suggesting.
   *
   * @param spaceNameForAutoSuggest The list of space name. A {@code List}
   */
  public void setSpaceNameForAutoSuggest(List<String> spaceNameForAutoSuggest) {
    this.spaceNameForAutoSuggest = spaceNameForAutoSuggest;
  }

  /**
   * Sets result of searching to list.
   *
   * @param spaceList The list of space. A {@code List}
   */
  public void setSpaceList(List<Space> spaceList) {
    this.spaceList = spaceList;
  }

  /**
   * Gets list of searching.
   *
   * @return List of space.
   * @throws Exception
   */
  public List<Space> getSpaceList() throws Exception {
    return spaceList;
  }

  /**
   * Gets selected character.
   *
   * @return Character is selected.
   */
  public String getSelectedChar() {
    return selectedChar;
  }

  /**
   * Sets selected character.
   *
   * @param selectedChar A {@code String}
   */
  public void setSelectedChar(String selectedChar) {
    this.selectedChar = selectedChar;
  }

  /**
   * Initializes search form fields.
   *
   * @throws Exception
   */
  public UISpaceSearch() throws Exception {
    addUIFormInput(new UIFormStringInput(SPACE_SEARCH, null, DEFAULT_SPACE_NAME_SEARCH));
    UIPopupWindow uiPopup = createUIComponent(UIPopupWindow.class, null, POPUP_ADD_SPACE);
    uiPopup.setShow(false);
    uiPopup.setWindowSize(400, 0);
    addChild(uiPopup);
  }

  /**
   * Listens to search event is broadcasted from search form, then processes search condition and
   * set search result to the result variable.<br> - Gets space name from request.<br> - Searches
   * spaces that have name like input space name.<br> - Sets matched space into result list.<br>
   */
  static public class SearchActionListener extends EventListener<UISpaceSearch> {
    @Override
    public void execute(Event<UISpaceSearch> event) throws Exception {
      WebuiRequestContext ctx = event.getRequestContext();
      UISpaceSearch uiSpaceSearch = event.getSource();
      String charSearch = ctx.getRequestParameter(OBJECTID);
      ResourceBundle resApp = ctx.getApplicationResourceBundle();
      String defaultSpaceNameAndDesc = resApp.getString(uiSpaceSearch.getId() + ".label.DefaultSpaceNameAndDesc");
      String searchCondition = (((UIFormStringInput) uiSpaceSearch.getChildById(SPACE_SEARCH)).getValue());
      if ((searchCondition == null || searchCondition.equals(defaultSpaceNameAndDesc)) && charSearch == null) {
        uiSpaceSearch.setSelectedChar(null);
        uiSpaceSearch.setSpaceNameSearch(null);
        ctx.addUIComponentToUpdateByAjax(uiSpaceSearch);
      } else {
        if (searchCondition != null) {
          searchCondition = searchCondition.trim();
        }
        if (charSearch != null) {
          ((UIFormStringInput) uiSpaceSearch.getChildById(SPACE_SEARCH)).setValue(defaultSpaceNameAndDesc);
        }
        uiSpaceSearch.setSelectedChar(charSearch);
        uiSpaceSearch.setSpaceNameSearch(searchCondition);
        uiSpaceSearch.setNewSearch(true);

        Event<UIComponent> searchEvent = uiSpaceSearch.<UIComponent>getParent().createEvent(SEARCH, Event.Phase.DECODE, ctx);
        if (searchEvent != null) {
          searchEvent.broadcast();
        }
      }
    }
  }

  /**
   * This action is triggered when user clicks on AddSpace <br />
   * <p/>
   * UIAddSpaceForm will be displayed in a popup window
   */
  static public class AddSpaceActionListener extends EventListener<UISpaceSearch> {

    @Override
    public void execute(Event<UISpaceSearch> event) throws Exception {
      UISpaceSearch uiSpaceSearch = event.getSource();
      UIPopupWindow uiPopup = uiSpaceSearch.getChild(UIPopupWindow.class);
      UISpaceAddForm uiAddSpaceForm = uiSpaceSearch.createUIComponent(UISpaceAddForm.class,
              null,
              null);
      uiPopup.setUIComponent(uiAddSpaceForm);
      uiPopup.setWindowSize(500, 0);
      uiPopup.setShow(true);
    }

  }

  public boolean isNewSearch() {
    return isNewSearch;
  }

  public void setNewSearch(boolean isNewSearch) {
    this.isNewSearch = isNewSearch;
  }
}
