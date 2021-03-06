/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheGCManager;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(ConvertFeature.class)
@LocalDeploy({ "org.nuxeo.ecm.core.convert:OSGI-INF/convert-service-config-enabled-gc.xml",
        "org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib3.xml" })
public class TestCGCache {

    @Inject
    ConversionService cs;

    @Test
    public void testCGTask() throws Exception {

        Converter cv = deployConverter();
        assertNotNull(cv);

        int cacheSize1 = ConversionCacheHolder.getNbCacheEntries();
        BlobHolder bh = getBlobHolder();
        BlobHolder result = cs.convert("identity", bh, null);
        assertNotNull(result);

        int cacheSize2 = ConversionCacheHolder.getNbCacheEntries();
        // check new cache entry was created
        assertEquals(1, cacheSize2 - cacheSize1);

        // wait for the GCThread to run
        int retryCount = 0;
        int noRuns = ConversionCacheGCManager.getGCRuns();
        while (ConversionCacheGCManager.getGCRuns() == noRuns && retryCount++ < 5) {
            Thread.sleep(1100);
        }
        assertTrue(ConversionCacheGCManager.getGCRuns() > 0);

        int cacheSize3 = ConversionCacheHolder.getNbCacheEntries();
        assertEquals(0, cacheSize3 - cacheSize1);
    }

    private Converter deployConverter() throws Exception {
        return ConversionServiceImpl.getConverter("identity");
    }

    private static BlobHolder getBlobHolder() throws IOException {
        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");
        assertNotNull(file);
        assertTrue(file.length() > 0);
        Blob blob = Blobs.createBlob(file, "application/msword", null, "hello.doc");
        return new SimpleBlobHolder(blob);
    }

}
