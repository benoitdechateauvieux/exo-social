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

package org.exoplatform.social.core.storage.cache.model.data;

import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.cache.CachedListData;
import org.exoplatform.social.core.storage.cache.model.key.RelationshipKey;

import java.util.List;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class RelationshipListData extends CachedListData<Relationship, RelationshipKey, RelationshipData> {

  public RelationshipListData(final RelationshipKey next, final List<RelationshipData> relationshipDatas) {
    super(next, relationshipDatas);
  }
}