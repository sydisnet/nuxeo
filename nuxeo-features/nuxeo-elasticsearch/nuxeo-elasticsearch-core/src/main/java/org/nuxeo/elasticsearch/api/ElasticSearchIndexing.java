/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tdelprat
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.elasticsearch.commands.IndexingCommand;

/**
 * Interface to process indexing of documents
 *
 * @since 5.9.3
 */
public interface ElasticSearchIndexing {

    /**
     * {true} if a command has already been submitted for indexing.
     *
     * @since 5.9.5
     */
    boolean isAlreadyScheduled(IndexingCommand cmd);

    /**
     * Ask to process the {@link IndexingCommand}. Recursive indexing is not taken in account.
     *
     * @since 7.1
     */
    void indexNonRecursive(IndexingCommand cmd) throws ClientException;

    /**
     * Ask to process a list of {@link IndexingCommand}. Commands list will be processed in bulk mode. Recursive
     * indexing is not taken in account.
     *
     * Indexation is done in the current thread.
     *
     * @since 7.1
     */
    void indexNonRecursive(List<IndexingCommand> cmds) throws ClientException;

    /**
     * Ask to process the {@link IndexingCommand}.
     *
     * @since 7.1
     */
    void index(IndexingCommand cmd);

    /**
     * Ask to process a list of {@link IndexingCommand}. Commands list will be processed in bulk mode.
     *
     * @since 7.1
     */
    void index(List<IndexingCommand> cmds);

    /**
     * Reindex documents matching the NXQL query,
     *
     * @since 7.1
     */
    void reindex(String repositoryName, String nxql);

}
