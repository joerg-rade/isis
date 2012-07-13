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
package org.apache.isis.runtimes.dflt.objectstores.jdo.datanucleus;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.runtimes.dflt.objectstores.jdo.datanucleus.DataNucleusPersistenceMechanismInstaller;

public class DataNucleusPersistenceMechanismInstallerTest_getName {

    private DataNucleusPersistenceMechanismInstaller installer;

    @Before
    public void setUp() throws Exception {
        installer = new DataNucleusPersistenceMechanismInstaller();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void isSet() {
        assertThat(installer.getName(), is("datanucleus"));
    }

}
