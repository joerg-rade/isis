/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.core.unittestsupport.bidir;

import java.util.SortedSet;

import org.apache.isis.unittestsupport.bidir.Instantiator;

/**
 * To ensure that different ({@link Comparable}) {@link ChildDomainObject}s
 * are not equivalent when placed into the {@link ParentDomainObject#getChildren() children} (a {@link SortedSet}.)
 */
public class InstantiatorForChildDomainObject implements Instantiator {

    private int i;

    @Override
    public Object instantiate() {
        final ChildDomainObject cdo = new ChildDomainObject();
        cdo.setIndex(++i);
        return cdo;
    }

}
