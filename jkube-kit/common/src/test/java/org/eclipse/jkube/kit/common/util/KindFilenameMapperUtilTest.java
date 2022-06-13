/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.common.util;

import org.eclipse.jkube.kit.common.SystemMock;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class KindFilenameMapperUtilTest {
    private static final String MAPPING_PROPERTIES = "Pod=pd, pod";

    private static final String VALID_TABLE = "cols=2*,options=\"header\"]" + System.lineSeparator()
            + "|===" + System.lineSeparator()
            + "|Kind" + System.lineSeparator()
            + "|Filename Type" + System.lineSeparator()
            + System.lineSeparator()
            + "|BuildConfig" + System.lineSeparator()
            + "a|`bc`, `buildconfig`" + System.lineSeparator()
            + System.lineSeparator()
            + "|ClusterRole" + System.lineSeparator()
            + "a|`cr`, `crole`, `clusterrole`" + System.lineSeparator()
            + System.lineSeparator()
            + "|ConfigMap" + System.lineSeparator()
            + "a|`cm`, `configmap`" + System.lineSeparator()
            + System.lineSeparator()
            + "|CronJob" + System.lineSeparator()
            + "a|`cj`, `cronjob`" + System.lineSeparator()
            + "|===";

    @Test
    public void shouldLoadMappings() {
        // given
        final AsciiDocParser asciiDocParser = new AsciiDocParser();
        final Map<String, List<String>> serializedMappings = asciiDocParser.serializeKindFilenameTable(new ByteArrayInputStream(VALID_TABLE.getBytes()));

        final PropertiesMappingParser propertiesMappingParser =  new PropertiesMappingParser();
        final Map<String, List<String>> propertiesMappings = propertiesMappingParser.parse(new ByteArrayInputStream(MAPPING_PROPERTIES.getBytes()));

        // when
        Map<String, List<String>> defaultMappings = KindFilenameMapperUtil.loadMappings();

        // then
        assertThat(defaultMappings)
                .containsAllEntriesOf(serializedMappings)
                .containsAllEntriesOf(propertiesMappings);
    }
}