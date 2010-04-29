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
package org.exoplatform.social.webui;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.social.space.Space;
import org.exoplatform.social.space.SpaceService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
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
  template = "system:/groovy/social/webui/component/UISpaceSearch.gtmpl",
  events = {
    @EventConfig(listeners = UISpaceSearch.SearchActionListener.class) 
  }
)
public class UISpaceSearch extends UIForm {
  /** SPACE SEARCH. */
  final public static String SPACE_SEARCH = "SpaceSearch";
  
  /** SPACE DESCRIPTION SEARCH. */
  final public static String SPACE_DESC_SEARCH = "SpaceDescSearch";
  
  /** SEARCH. */
  final public static String SEARCH = "Search";
  
  /** SEARCH ALL. */
  final static String ALL = "All";
  
  /** DEFAULT SPACE NAME SEARCH. */
  final public static String DEFAULT_SPACE_NAME_SEARCH = "Space name";
  
  /** DEFAULT SPACE NAME SEARCH. */
  final public static String DEFAULT_SPACE_DESC_SEARCH = "Description";
  
  /** INPUT PATTERN FOR CHECKING. */
  final static String RIGHT_INPUT_PATTERN = "^[\\p{L}][\\p{L}._\\- \\d]+$";
  
  /** ADD PREFIX TO ENSURE ALWAY RIGHT THE PATTERN FOR CHECKING */
  final static String PREFIX_ADDED_FOR_CHECK = "PrefixAddedForCheck";
  
  /** The spaceService is used for SpaceService instance storage. */ 
  SpaceService spaceService = null;
  
  /** The spaceList is used for search result storage. */
  private List<Space> spaceList = null;
  
  /** The selectedChar is used for selected character storage when search by alphabet. */
  String selectedChar = null;
  
  /** The spaceNameSearch is used for input space name storage. */
  String spaceNameSearch = null;
  
  /** Contains all space name in individual context for auto suggesting. */
  List<String> spaceNameForAutoSuggest = null;
  
  /**
   * Gets input space name search input.
   * 
   * @return Name of space.
   */
  public String getSpaceNameSearch() { return spaceNameSearch;}
  
  /**
   * Sets input space name search.
   * 
   * @param spaceNameSearch
   *        A {@code String}
   */
  public void setSpaceNameSearch(String spaceNameSearch) { 
    this.spaceNameSearch = spaceNameSearch;
  }
  
  /**
   * Gets space name for auto suggesting.
   * 
   * @return List of space name.
   */
  public List<String> getSpaceNameForAutoSuggest() { return spaceNameForAutoSuggest;}

  /**
   * Sets space name for auto suggesting.
   * 
   * @param spaceNameForAutoSuggest The list of space name.
   *        A {@code List}
   */
  public void setSpaceNameForAutoSuggest(List<String> spaceNameForAutoSuggest) { 
    this.spaceNameForAutoSuggest = spaceNameForAutoSuggest;
  }

  /**
   * Sets result of searching to list.
   * 
   * @param spaceList The list of space.
   *        A {@code List}
   */
  public void setSpaceList(List<Space> spaceList) { this.spaceList = spaceList;}
  
  /**
   * Gets list of searching.
   * 
   * @return List of space.
   * @throws Exception 
   */
  public List<Space> getSpaceList() throws Exception { return spaceList;}
  
  /**
   * Gets selected character.
   * 
   * @return Character is selected.
   */
  public String getSelectedChar() { return selectedChar;}

  /**
   * Sets selected character.
   * 
   * @param selectedChar
   *        A {@code String}
   */
  public void setSelectedChar(String selectedChar) { this.selectedChar = selectedChar;}
  
  /**
   * Initializes search form fields.
   * 
   * @throws Exception
   */
  public UISpaceSearch() throws Exception { 
    addUIFormInput(new UIFormStringInput(SPACE_SEARCH, null, DEFAULT_SPACE_NAME_SEARCH));
    addUIFormInput(new UIFormStringInput(SPACE_DESC_SEARCH, null, DEFAULT_SPACE_DESC_SEARCH));
  }
  
  /**
   * Listens to search event is broadcasted from search form, then processes search condition
   * and set search result to the result variable.<br>
   *    - Gets space name from request.<br>
   *    - Searches spaces that have name like input space name.<br>
   *    - Sets matched space into result list.<br> 
   */
  static public class SearchActionListener extends EventListener<UISpaceSearch> {
    @Override
    public void execute(Event<UISpaceSearch> event) throws Exception {
      WebuiRequestContext ctx = event.getRequestContext();
      UISpaceSearch uiSpaceSearch = event.getSource();
      String charSearch = ctx.getRequestParameter(OBJECTID);
      SpaceService spaceService = uiSpaceSearch.getSpaceService();
      ResourceBundle resApp = ctx.getApplicationResourceBundle();
      String defaultSpaceName = resApp.getString(uiSpaceSearch.getId() + ".label.SpaceName");
      String spaceName = (((UIFormStringInput)uiSpaceSearch.getChildById(SPACE_SEARCH)).getValue());
      String defaultSpaceDesc = resApp.getString(uiSpaceSearch.getId() + ".label.Description");
      String spaceDesc = (((UIFormStringInput)uiSpaceSearch.getChildById(SPACE_DESC_SEARCH)).getValue());
      if (spaceName != null) spaceName = spaceName.trim();
      if (spaceDesc != null) spaceDesc = spaceDesc.trim();
      spaceDesc = ((spaceDesc == null) || (spaceDesc.length() == 0) || spaceDesc.equals(defaultSpaceDesc)) ? null : spaceDesc;
      
      spaceName = ((spaceName == null) || (spaceName.length() == 0) || spaceName.equals(defaultSpaceName)) ? "*" : spaceName;
      spaceName = (charSearch != null) ? charSearch : spaceName;
      spaceName = ((charSearch != null) && ALL.equals(charSearch)) ? "" : spaceName;
      
      if (charSearch != null) {
    	  ((UIFormStringInput)uiSpaceSearch.getChildById(SPACE_SEARCH)).setValue(defaultSpaceName);
    	  ((UIFormStringInput)uiSpaceSearch.getChildById(SPACE_DESC_SEARCH)).setValue(defaultSpaceDesc);
      }
      uiSpaceSearch.setSelectedChar(charSearch);
         
      if (charSearch == null) { // is not searching by first character
        if (!isValidInput(spaceName)) {
          uiSpaceSearch.setSpaceList(new ArrayList<Space>());
        } else {
          spaceName = (spaceName.charAt(0) != '*') ? "*" + spaceName : spaceName;
          spaceName = (spaceName.charAt(spaceName.length()-1) != '*') ? spaceName += "*" : spaceName;
          spaceName = (spaceName.indexOf("*") >= 0) ? spaceName.replace("*", ".*") : spaceName;
          spaceName = (spaceName.indexOf("%") >= 0) ? spaceName.replace("%", ".*") : spaceName;
          List<Space> spacesSearchByName = spaceService.getSpacesByName(spaceName, false);
          List<Space> spaceSearchResult = new ArrayList<Space>();
          if (spaceDesc == null) {
        	  uiSpaceSearch.setSpaceList(spacesSearchByName);
          } else {
        	  spaceSearchResult = filterSpacesByDesc(spacesSearchByName, spaceDesc);
        	  uiSpaceSearch.setSpaceList(spaceSearchResult);
          }
        }
      } else { // is searching by alphabet
        List<Space> spaceSearchResult = spaceService.getSpacesByName(spaceName, true );
        uiSpaceSearch.setSpaceList(spaceSearchResult);  
      }
      
      uiSpaceSearch.setSpaceNameSearch(spaceName);
      
      Event<UIComponent> searchEvent = uiSpaceSearch.<UIComponent>getParent().createEvent(SEARCH, Event.Phase.DECODE, ctx);
      if (searchEvent != null) {
        searchEvent.broadcast();
      }
    }
    
    private List<Space> filterSpacesByDesc(List<Space> spacesSearchByName, String spaceDesc) {
    	List<Space> spaces = new ArrayList<Space>();
    	String[] spaceDescriptions = spaceDesc.split("\u0020"); // split by space
    	String exactSearchStr = getExactSearchStr(spaceDesc);
    	String description = null;
    	String descPart = null;
    	if (exactSearchStr != null) {
    		for (Space space : spacesSearchByName) {
    			description = space.getDescription();
    			if (description.contains(exactSearchStr) && !spaces.contains(space)) {
    				spaces.add(space);
    			}
    		}
    		
    		return spaces;
    	}
    	
    	for (Space space : spacesSearchByName) {
    		description = space.getDescription();
    		for (int idx = 0; idx < spaceDescriptions.length; idx++) {
    			descPart = spaceDescriptions[idx];
    			if (description.contains(descPart) && !spaces.contains(space)) spaces.add(space);
    		}
    	}
    	
		return spaces;
	}
    
    private String getExactSearchStr(String spaceDesc) {
    	int firstIndexOfDQuoter = spaceDesc.indexOf('\u0022'); // index of double quoter
    	int lastIndexOfDQuoter = spaceDesc.lastIndexOf('\u0022'); // index of double quoter
    	int firstIndexOfQuoter = spaceDesc.indexOf("\u0027"); // index of single quoter
    	int lastIndexOfQuoter = spaceDesc.lastIndexOf("\u0027"); // index of single quoter
    	
    	if ((firstIndexOfDQuoter != -1) && (lastIndexOfDQuoter != -1) && 
    			(firstIndexOfDQuoter < lastIndexOfDQuoter)) {
    		return spaceDesc.substring(firstIndexOfDQuoter + 1, lastIndexOfDQuoter - 2);
    	}
    	
    	if ((firstIndexOfQuoter != -1) && (lastIndexOfQuoter != -1) && 
    			(firstIndexOfQuoter < lastIndexOfQuoter)) {
    		return spaceDesc.substring(firstIndexOfQuoter + 1, lastIndexOfQuoter - 2);
    	}
    	
    	return null;
    }
	/**
     * Checks input values follow regular expression.
     * 
     * @param input
     *        A {@code String}
     *        
     * @return true if user input a right string for space searching else return false.
     */
    private boolean isValidInput(String input) {
      // Eliminate '*' and '%' character in string for checking
      String spacenameForCheck = input.replace("*", "");
      spacenameForCheck = spacenameForCheck.replace("%", "");
      // Make sure string for checking is started by alphabet character
      spacenameForCheck =  PREFIX_ADDED_FOR_CHECK + spacenameForCheck;
      if (!spacenameForCheck.matches(RIGHT_INPUT_PATTERN)) return false;
    
      return true;
    }
  }
  
  /**
   * Gets an instance of SpaceService. If the instance is still existed then return
   * else it is get from container.
   * 
   * @return an instance of SpaceService.
   */
  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = getApplicationComponent(SpaceService.class);
    }
    return spaceService;
  }
  
}
