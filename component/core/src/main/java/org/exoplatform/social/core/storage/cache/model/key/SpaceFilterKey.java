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

package org.exoplatform.social.core.storage.cache.model.key;

import org.exoplatform.social.core.space.SpaceFilter;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class SpaceFilterKey implements CacheKey {

  private String userId;
  private char firstCharacterOfSpaceName;
  private String spaceNameSearchCondition;

  public SpaceFilterKey(String userId, SpaceFilter filter) {
    this.userId = userId;
    if (filter != null) {
      this.firstCharacterOfSpaceName = filter.getFirstCharacterOfSpaceName();
      this.spaceNameSearchCondition = filter.getSpaceNameSearchCondition();
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SpaceFilterKey)) {
      return false;
    }

    SpaceFilterKey that = (SpaceFilterKey) o;

    if (firstCharacterOfSpaceName != that.firstCharacterOfSpaceName) {
      return false;
    }
    if (spaceNameSearchCondition != null ? !spaceNameSearchCondition.equals(that.spaceNameSearchCondition) : that.spaceNameSearchCondition != null) {
      return false;
    }
    if (userId != null ? !userId.equals(that.userId) : that.userId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = userId != null ? userId.hashCode() : 0;
    result = 31 * result + (int) firstCharacterOfSpaceName;
    result = 31 * result + (spaceNameSearchCondition != null ? spaceNameSearchCondition.hashCode() : 0);
    return result;
  }

}