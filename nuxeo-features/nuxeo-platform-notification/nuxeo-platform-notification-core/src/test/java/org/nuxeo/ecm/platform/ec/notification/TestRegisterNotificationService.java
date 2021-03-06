/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestRegisterPlacefulService.java 13110 2007-03-01 17:25:47Z rspivak $
 */
package org.nuxeo.ecm.platform.ec.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public class TestRegisterNotificationService extends NXRuntimeTestCase {

    private static final String BUNDLE_TEST_NAME = "org.nuxeo.ecm.platform.notification.core.tests";

    NotificationService notificationService;

    NotificationRegistry notificationRegistry;

    EmailHelper mailHelper = new EmailHelper();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        File propertiesFile = FileUtils.getResourceFileFromContext("notifications.properties");
        InputStream notificationsProperties = new FileInputStream(propertiesFile);
        ((OSGiRuntimeService) runtime).loadProperties(notificationsProperties);

        deployContrib("org.nuxeo.ecm.platform.notification.core", "OSGI-INF/NotificationService.xml");
    }

    @Test
    public void testRegistration() throws Exception {
        deployContrib(BUNDLE_TEST_NAME, "notification-contrib.xml");
        List<Notification> notifications = getService().getNotificationsForEvents("testEvent");

        assertEquals(1, notifications.size());

        Notification notif = notifications.get(0);
        assertEquals("email", notif.getChannel());
        assertEquals(false, notif.getAutoSubscribed());
        assertEquals("section", notif.getAvailableIn());
        // assertEquals(true, notif.getEnabled());
        assertEquals("Test Notification Label", notif.getLabel());
        assertEquals("Test Notification Subject", notif.getSubject());
        assertEquals("Test Notification Subject Template", notif.getSubjectTemplate());
        assertEquals("test-template", notif.getTemplate());
        assertEquals("NotificationContext['exp1']", notif.getTemplateExpr());

        Map<String, Serializable> infos = new HashMap<String, Serializable>();
        infos.put("exp1", "myDynamicTemplate");
        String template = mailHelper.evaluateMvelExpresssion(notif.getTemplateExpr(), infos);
        assertEquals("myDynamicTemplate", template);

        notifications = getRegistry().getNotificationsForSubscriptions("section");
        assertEquals(1, notifications.size());

        URL newModifTemplate = NotificationService.getTemplateURL("test-template");
        assertTrue(newModifTemplate.getFile().endsWith("templates/test-template.ftl"));

    }

    @Test
    public void testRegistrationDisabled() throws Exception {
        deployContrib(BUNDLE_TEST_NAME, "notification-contrib-disabled.xml");
        List<Notification> notifications = getService().getNotificationsForEvents("testEvent");

        assertEquals(0, notifications.size());
    }

    @Test
    public void testRegistrationOverrideWithDisabled() throws Exception {
        deployContrib(BUNDLE_TEST_NAME, "notification-contrib.xml");
        List<Notification> notifications = getService().getNotificationsForEvents("testEvent");

        assertEquals(1, notifications.size());
        deployContrib(BUNDLE_TEST_NAME, "notification-contrib-disabled.xml");
        notifications = getService().getNotificationsForEvents("testEvent");

        assertEquals(0, notifications.size());
    }

    @Test
    public void testRegistrationOverride() throws Exception {
        deployContrib(BUNDLE_TEST_NAME, "notification-contrib.xml");
        deployContrib(BUNDLE_TEST_NAME, "notification-contrib-overridden.xml");

        List<Notification> notifications = getService().getNotificationsForEvents("testEvent");
        assertEquals(0, notifications.size());

        notifications = getService().getNotificationsForEvents("testEvent-ov");
        assertEquals(1, notifications.size());

        Notification notif = notifications.get(0);
        assertEquals("email-ov", notif.getChannel());
        assertEquals(true, notif.getAutoSubscribed());
        assertEquals("folder", notif.getAvailableIn());
        // assertEquals(true, notif.getEnabled());
        assertEquals("Test Notification Label-ov", notif.getLabel());
        assertEquals("Test Notification Subject-ov", notif.getSubject());
        assertEquals("Test Notification Subject Template-ov", notif.getSubjectTemplate());
        assertEquals("test-template-ov", notif.getTemplate());
        assertEquals("NotificationContext['exp1-ov']", notif.getTemplateExpr());

        notifications = getRegistry().getNotificationsForSubscriptions("section");
        assertEquals(0, notifications.size());

        notifications = getRegistry().getNotificationsForSubscriptions("folder");
        assertEquals(0, notifications.size());

        URL newModifTemplate = NotificationService.getTemplateURL("test-template");
        assertTrue(newModifTemplate.getFile().endsWith("templates/test-template-ov.ftl"));
    }

    @Test
    public void testExpandVarsInGeneralSettings() throws Exception {
        deployContrib(BUNDLE_TEST_NAME, "notification-contrib.xml");

        assertEquals("http://localhost:8080/nuxeo/", getService().getServerUrlPrefix());
        assertEquals("[Nuxeo5]", getService().getEMailSubjectPrefix());

        // this one should not be expanded
        assertEquals("java:/Mail", getService().getMailSessionJndiName());

        deployContrib(BUNDLE_TEST_NAME, "notification-contrib-overridden.xml");

        assertEquals("http://testServerPrefix/nuxeo", getService().getServerUrlPrefix());
        assertEquals("testSubjectPrefix", getService().getEMailSubjectPrefix());

        // this one should not be expanded
        assertEquals("${not.existing.property}", getService().getMailSessionJndiName());
    }

    @Test
    public void testVetoRegistration() throws Exception {
        deployContrib(BUNDLE_TEST_NAME, "notification-veto-contrib.xml");
        deployContrib(BUNDLE_TEST_NAME, "notification-veto-contrib-overridden.xml");

        Collection<NotificationListenerVeto> vetos = getService().getNotificationVetos();
        assertEquals(2, vetos.size());
        assertEquals("org.nuxeo.ecm.platform.ec.notification.veto.NotificationVeto1",
                getService().getNotificationListenerVetoRegistry().getVeto("veto1").getClass().getCanonicalName());
        assertEquals("org.nuxeo.ecm.platform.ec.notification.veto.NotificationVeto20",
                getService().getNotificationListenerVetoRegistry().getVeto("veto2").getClass().getCanonicalName());

    }

    public NotificationService getService() {
        if (notificationService == null) {
            notificationService = (NotificationService) Framework.getLocalService(NotificationManager.class);
        }
        return notificationService;
    }

    public NotificationRegistry getRegistry() {
        if (notificationRegistry == null) {
            notificationRegistry = getService().getNotificationRegistry();
        }
        return notificationRegistry;
    }

}
