/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.metamodel.specloader;

import org.apache.isis.core.metamodel.deployment.DeploymentCategory;

public enum IntrospectionMode {
    /**
     * Lazy (don't introspect members for most classes unless required), irrespective of the deployment mode.
     */
    LAZY{
        @Override
        public boolean isFullIntrospect(final DeploymentCategory category) {
            return false;
        }
    },
    /**
     * If production deployment mode, then full, otherwise lazy.
     */
    LAZY_UNLESS_PRODUCTION {
        @Override public boolean isFullIntrospect(final DeploymentCategory category) {
            return category.isProduction();
        }
    },
    /**
     * Full introspection, irrespective of deployment mode.
     */
    FULL {
        @Override
        public boolean isFullIntrospect(final DeploymentCategory category) {
            return true;
        }
    };

    public abstract boolean isFullIntrospect(final DeploymentCategory category);
}
