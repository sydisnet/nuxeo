/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: StatementImpl.java 20796 2007-06-19 09:52:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.relations.api.Blank;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidPredicateException;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidStatementException;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidSubjectException;

/**
 * Statement with subject, predicate and object.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
// TODO: make a statement handle metadata (as properties)
public class StatementImpl implements Statement {

    private static final long serialVersionUID = 1L;

    protected Subject subject;

    protected Resource predicate;

    protected Node object;

    protected Map<Resource, Node[]> properties = new HashMap<Resource, Node[]>();

    /**
     * Constructor for NULL statement.
     */
    public StatementImpl() {
    }

    /**
     * Constructor.
     *
     * @param subject Resource or Blank node
     * @param predicate Resource
     * @param object Resource, Blank or Literal node
     */
    public StatementImpl(Node subject, Node predicate, Node object) {
        boolean validSubject = true;
        try {
            setSubject(subject);
        } catch (InvalidSubjectException e) {
            validSubject = false;
        }
        boolean validPredicate = true;
        try {
            setPredicate(predicate);
        } catch (InvalidPredicateException e) {
            validPredicate = false;
        }
        if (!validPredicate && !validSubject) {
            throw new InvalidStatementException();
        } else if (!validSubject) {
            throw new InvalidSubjectException();
        } else if (!validPredicate) {
            throw new InvalidPredicateException();
        }
        this.object = object;
    }

    public Node getObject() {
        return object;
    }

    public void setObject(Node object) {
        this.object = object;
    }

    public Resource getPredicate() {
        return predicate;
    }

    public void setPredicate(Node predicate) {
        if (predicate != null && !predicate.isResource()) {
            throw new InvalidPredicateException();
        }
        this.predicate = (Resource) predicate;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Node subject) {
        if (subject != null) {
            if (subject.isBlank()) {
                this.subject = (Blank) subject;
            } else if (subject.isResource()) {
                this.subject = (Resource) subject;
            } else {
                throw new InvalidSubjectException();
            }
        }
    }

    public Map<Resource, Node[]> getProperties() {
        return properties;
    }

    public Map<String, Node[]> getStringProperties() {
        Map<String, Node[]> stringProps = new HashMap<String, Node[]>();
        for (Map.Entry<Resource, Node[]> property : properties.entrySet()) {
            stringProps.put(property.getKey().getUri(), property.getValue());
        }
        return stringProps;
    }

    public Node getProperty(Resource property) {
        // return first one found
        Node node = null;
        Node[] values = properties.get(property);
        if (values != null && values.length > 0) {
            node = values[0];
        }
        return node;
    }

    public Node[] getProperties(Resource property) {
        Node[] values = properties.get(property);
        return values;
    }

    public void setProperties(Map<Resource, Node[]> properties) {
        if (properties != null) {
            for (Map.Entry<Resource, Node[]> property : properties.entrySet()) {
                setProperties(property.getKey(), property.getValue());
            }
        } else {
            this.properties.clear();
        }
    }

    public void setProperty(Resource property, Node value) {
        if (property != null && value != null) {
            Node[] values = { value };
            properties.put(property, values);
        }
    }

    public void setProperties(Resource property, Node[] values) {
        if (property != null && values != null && values.length > 0) {
            properties.put(property, values);
        }
    }

    public void deleteProperties() {
        properties.clear();
    }

    public void deleteProperty(Resource property) {
        properties.remove(property);
    }

    public void deleteProperty(Resource property, Node value) {
        if (properties.containsKey(property)) {
            List<Node> valuesList = new ArrayList<Node>();
            valuesList.addAll(Arrays.asList(properties.get(property)));
            valuesList.remove(value);
            if (valuesList.isEmpty()) {
                properties.remove(property);
            } else {
                properties.put(property, valuesList.toArray(new Node[] {}));
            }
        }
    }

    public void deleteProperties(Resource property, Node[] values) {
        if (properties.containsKey(property) && values != null && values.length > 0) {
            List<Node> valuesList = new ArrayList<Node>();
            valuesList.addAll(Arrays.asList(properties.get(property)));
            valuesList.removeAll(Arrays.asList(values));
            if (valuesList.isEmpty()) {
                properties.remove(property);
            } else {
                properties.put(property, valuesList.toArray(new Node[] {}));
            }
        }
    }

    public void addProperties(Map<Resource, Node[]> properties) {
        if (properties != null) {
            for (Map.Entry<Resource, Node[]> property : properties.entrySet()) {
                addProperties(property.getKey(), property.getValue());
            }
        }
    }

    public void addProperty(Resource property, Node value) {
        if (property != null && value != null) {
            if (properties.containsKey(property)) {
                List<Node> valuesList = new ArrayList<Node>();
                valuesList.addAll(Arrays.asList(properties.get(property)));
                if (!valuesList.contains(value)) {
                    valuesList.add(value);
                    properties.put(property, valuesList.toArray(new Node[] {}));
                }
            } else {
                Node[] values = { value };
                properties.put(property, values);
            }
        }
    }

    public void addProperties(Resource property, Node[] values) {
        if (property != null && values != null && values.length > 0) {
            if (properties.containsKey(property)) {
                // add only missing nodes
                List<Node> valuesList = new ArrayList<Node>();
                valuesList.addAll(Arrays.asList(properties.get(property)));
                boolean changed = false;
                for (Node value : values) {
                    if (!valuesList.contains(value)) {
                        valuesList.add(value);
                        changed = true;
                    }
                }
                if (changed) {
                    properties.put(property, valuesList.toArray(new Node[] {}));
                }
            } else {
                properties.put(property, values);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s, %s)", getClass().getSimpleName(), subject, predicate, object);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StatementImpl)) {
            return false;
        }
        StatementImpl otherStatement = (StatementImpl) other;
        return subject.equals(otherStatement.subject) && predicate.equals(otherStatement.predicate)
                && object.equals(otherStatement.object);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 17 * result + subject.hashCode();
        result = 17 * result + predicate.hashCode();
        result = 17 * result + object.hashCode();
        return result;
    }

    public int compareTo(Statement o) {
        // dumb implementation, just used to compare statements lists
        return toString().compareTo(o.toString());
    }

    @Override
    public Object clone() {
        StatementImpl clone;
        try {
            clone = (StatementImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        Map<Resource, Node[]> clonedProperties = new HashMap<Resource, Node[]>();
        for (Map.Entry<Resource, Node[]> property : properties.entrySet()) {
            clonedProperties.put(property.getKey(), property.getValue().clone());
        }
        clone.properties = clonedProperties;
        return clone;
    }

}
