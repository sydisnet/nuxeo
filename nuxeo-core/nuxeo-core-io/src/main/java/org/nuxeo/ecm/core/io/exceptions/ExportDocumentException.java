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
 * $Id: ExportDocumentException.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.exceptions;

/**
 * Import document exception.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ExportDocumentException extends RuntimeException {

    private static final long serialVersionUID = 2045308733731717902L;

    public ExportDocumentException() {
    }

    public ExportDocumentException(String message) {
        super(message);
    }

    public ExportDocumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportDocumentException(Throwable cause) {
        super(cause);
    }
}
