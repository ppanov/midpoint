/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;

import java.util.Collection;
import java.util.Set;

/**
 * Manages identity objects.
 * <p/>
 * Retrieves them and submits changes. It does not mean that the changes will be
 * applied immediately. There may be workflow, approval or any other business
 * logic.
 * <p/>
 * This is bad. The object is Java bean with java property names and the changes
 * are XML QNames. But the extenstion attributes will be XML anyway and the
 * Generic Objects as well. Beans are there only to simplify standard stuff. We
 * might get rid of them later. But now we just don't know.
 *
 * @author semancik
 */
public interface ObjectManager<T> {

    String CLASS_NAME = ObjectManager.class.getName() + ".";
    String LIST = CLASS_NAME + "list";
    String GET = CLASS_NAME + "get";
    String CREATE = CLASS_NAME + "create";
    String ADD = CLASS_NAME + "add";
    String SUBMIT = CLASS_NAME + "submit";
    String DELETE = CLASS_NAME + "delete";

    /**
     * List objects according to paging parameters.
     *
     * @return all objects from the repository.
     */
    Collection<T> list(PagingType paging);

    /**
     * List all objects.
     *
     * @return all objects from the repository.
     */
    Collection<T> list();

    /**
     * Lookup object by OID.
     *
     * @param oid OID of object to return
     * @return complete object retrieved from the repository
     */
    T get(String oid, PropertyReferenceListType resolve);

    /**
     * Create an empty object.
     * <p/>
     * TODO
     *
     * @return
     */
    T create();

    /**
     * Add new object to the repository.
     * <p/>
     * The OID property of the object may be null. In that case the OID will be
     * generated by the repository (recommended).
     *
     * @param newObject object to add
     * @return OID of stored object
     */
    String add(T newObject);

    /**
     * Modify the object.
     * <p/>
     * The method will change the object to a new state. It is not guaranteed
     * that the object will be in the state specified by changedObject parameter
     * when stored in the repository because many updates may be running in
     * parallel.
     * <p/>
     * TODO Should not be normally used.
     *
     * @param changedObject new state of the object
     * @return relative changes that will be applied to the object
     */
    Set<PropertyChange> submit(T changedObject, Task task, OperationResult parentResult);

    /**
     * Deletes object from the repository.
     *
     * @param oid
     */
    void delete(String oid);

    // TODO: search operations
    // TODO: optimistic locking
    // TODO: much, much better interface description
}
