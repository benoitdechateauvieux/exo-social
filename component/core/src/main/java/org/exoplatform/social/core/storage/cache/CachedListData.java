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

package org.exoplatform.social.core.storage.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class CachedListData<T, K extends CacheKey<T>, D extends CacheData<T>> implements CacheData<List<T>> {

  private final List<D> list;

  private final K next;

  public CachedListData(final K next, final List<D> list) {
    this.list = Collections.unmodifiableList(list);
    this.next = next;
  }

  public List<T> build() {
    List<T> ts = new ArrayList<T>();

    for (D d : list) {
      ts.add(d.build());
    }

    return ts;
  }

  public K getNext() {
    return next;
  }
}