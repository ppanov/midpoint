/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;

/**
 * @author semancik
 *
 */
public class DeltaConvertor {
	
	public static final QName PATH_ELEMENT_NAME = new QName(PrismConstants.NS_TYPES, "path");

	public static <T extends Objectable> ObjectDelta<T> createObjectDelta(ObjectModificationType objectModification,
			Class<T> type, PrismContext prismContext) throws SchemaException {
		PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		if (objectDefinition == null) {
			throw new SchemaException("No object definition for class "+type);
		}
        return createObjectDelta(objectModification, objectDefinition);
    }

    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(ObjectModificationType objectModification,
            PrismObjectDefinition<T> objDef) throws SchemaException {
        ObjectDelta<T> objectDelta = new ObjectDelta<T>(objDef.getCompileTimeClass(), ChangeType.MODIFY);
        objectDelta.setOid(objectModification.getOid());

        for (ItemDeltaType propMod : objectModification.getModification()) {
            ItemDelta itemDelta = createItemDelta(propMod, objDef);
            objectDelta.addModification(itemDelta);
        }

        return objectDelta;
    }
    
    public static <T extends Objectable> Collection<? extends ItemDelta> toModifications(ObjectModificationType objectModification,
			Class<T> type, PrismContext prismContext) throws SchemaException {
		PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		if (objectDefinition == null) {
			throw new SchemaException("No object definition for class "+type);
		}
        return toModifications(objectModification, objectDefinition);
    }
    
    public static <T extends Objectable> Collection<? extends ItemDelta> toModifications(ObjectModificationType objectModification, 
    		PrismObjectDefinition<T> objDef) throws SchemaException {
    	return toModifications(objectModification.getModification(), objDef);
    }
    	
    public static <T extends Objectable> Collection<? extends ItemDelta> toModifications(Collection<ItemDeltaType> itemDeltaTypes, 
    		PrismObjectDefinition<T> objDef) throws SchemaException {
    	Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
    	for (ItemDeltaType propMod : itemDeltaTypes) {
            ItemDelta itemDelta = createItemDelta(propMod, objDef);
            modifications.add(itemDelta);
        }
    	return modifications;
	}
    
    /**
     * Converts this delta to ObjectModificationType (XML).
     */
    public static <T extends Objectable> ObjectModificationType toObjectModificationType(ObjectDelta<T> delta) throws SchemaException {
        if (delta.getChangeType() != ChangeType.MODIFY) {
            throw new IllegalStateException("Cannot produce ObjectModificationType from delta of type " + delta.getChangeType());
        }
        ObjectModificationType modType = new ObjectModificationType();
        modType.setOid(delta.getOid());
        List<ItemDeltaType> propModTypes = modType.getModification();
        for (ItemDelta propDelta : delta.getModifications()) {
            Collection<ItemDeltaType> propPropModTypes;
            try {
                propPropModTypes = toPropertyModificationTypes(propDelta);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " in " + delta.toString(), e);
            }
            propModTypes.addAll(propPropModTypes);
        }
        return modType;
    }
    
    /**
     * Creates delta from PropertyModificationType (XML). The values inside the PropertyModificationType are converted to java.
     * That's the reason this method needs schema and objectType (to locate the appropriate definitions).
     */
    public static ItemDelta createItemDelta(ItemDeltaType propMod,
            Class<? extends Objectable> objectType, PrismContext prismContext) throws SchemaException {
        PrismObjectDefinition<? extends Objectable> objectDefinition = prismContext.getSchemaRegistry().
        		findObjectDefinitionByCompileTimeClass(objectType);
        return createItemDelta(propMod, objectDefinition);
    }

    public static ItemDelta createItemDelta(ItemDeltaType propMod, PrismContainerDefinition pcDef) throws
            SchemaException {
    	XPathHolder xpath = new XPathHolder(propMod.getPath());
        PropertyPath parentPath = xpath.toPropertyPath();
        if (propMod.getValue() == null) {
            throw new IllegalArgumentException("No value in item delta (path: " + parentPath + ") while creating a property delta");
        }
        PrismContainerDefinition<?> containingPcd = pcDef.findContainerDefinition(parentPath);
        if (containingPcd == null) {
            throw new SchemaException("No container definition for " + parentPath + " (while creating delta for " + pcDef + ")");
        }
        Collection<? extends Item<?>> items = pcDef.getPrismContext().getPrismDomProcessor().
        						parseContainerItems(containingPcd, propMod.getValue().getAny());
        if (items.size() > 1) {
            throw new SchemaException("Expected presence of a single item (path " + propMod.getPath() + ") in a object modification, but found " + items.size() + " instead");
        }
        if (items.size() < 1) {
            throw new SchemaException("Expected presence of a value (path " + propMod.getPath() + ") in a object modification, but found nothing");
        }
        Item<?> item = items.iterator().next();
        ItemDelta itemDelta = item.createDelta(parentPath.subPath(item.getName()));
        if (propMod.getModificationType() == ModificationTypeType.ADD) {
        	itemDelta.addValuesToAdd(item.getValues());
        } else if (propMod.getModificationType() == ModificationTypeType.DELETE) {
        	itemDelta.addValuesToDelete(item.getValues());
        } else if (propMod.getModificationType() == ModificationTypeType.REPLACE) {
        	itemDelta.setValuesToReplace(item.getValues());
        }

        return itemDelta;
    }

    /**
     * Converts this delta to PropertyModificationType (XML).
     */
    public static Collection<ItemDeltaType> toPropertyModificationTypes(ItemDelta delta) throws SchemaException {
    	delta.checkConsistence();
        Collection<ItemDeltaType> mods = new ArrayList<ItemDeltaType>();
        XPathHolder xpath = new XPathHolder(delta.getParentPath());
        Document document = DOMUtil.getDocument();
        Element xpathElement = xpath.toElement(PATH_ELEMENT_NAME, document);
        if (delta.getValuesToReplace() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(xpathElement);
            mod.setModificationType(ModificationTypeType.REPLACE);
            try {
                addModValues(delta, mod, delta.getValuesToReplace(), document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getName(), e);
            }
            mods.add(mod);
        }
        if (delta.getValuesToAdd() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(xpathElement);
            mod.setModificationType(ModificationTypeType.ADD);
            try {
                addModValues(delta, mod, delta.getValuesToAdd(), document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getName(), e);
            }
            mods.add(mod);
        }
        if (delta.getValuesToDelete() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(xpathElement);
            mod.setModificationType(ModificationTypeType.DELETE);
            try {
                addModValues(delta, mod, delta.getValuesToDelete(), document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getName(), e);
            }
            mods.add(mod);
        }
        return mods;
    }

    private static void addModValues(ItemDelta delta, ItemDeltaType mod, Collection<PrismPropertyValue<Object>> values,
            Document document) throws SchemaException {
    	ItemDeltaType.Value modValue = new ItemDeltaType.Value();
        mod.setValue(modValue);
        for (PrismPropertyValue<Object> value : values) {
        	Object realValue = value.getValue();
        	Object xmlValue = realValue;
        	if (XmlTypeConverter.canConvert(realValue.getClass())) {
        		// Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
        		xmlValue = XmlTypeConverter.toXsdElement(realValue, delta.getName(), document, true);
        	}
            modValue.getAny().add(xmlValue);
        }
    }

}
