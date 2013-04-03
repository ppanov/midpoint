package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

@Component
public class ShadowCacheProvisioner extends ShadowCache{
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheProvisioner.class);
	
	@Override
	public <T extends ResourceObjectShadowType> String afterAddOnResource(PrismObject<T> shadow, ResourceType resource, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		
	shadow = shadowManager.createRepositoryShadow(shadow, resource);

	if (shadow == null) {
		parentResult
				.recordFatalError("Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
		throw new IllegalStateException(
				"Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
	}

	LOGGER.trace("Adding object with identifiers to the repository.");
	String oid = null;
	try {
		oid = getRepositoryService().addObject(shadow, null, parentResult);

	} catch (ObjectAlreadyExistsException ex) {
		// This should not happen. The OID is not supplied and it is
		// generated by the repo
		// If it happens, it must be a repo bug. Therefore it is safe to
		// convert to runtime exception
		parentResult.recordFatalError(
				"Couldn't add shadow object to the repository. Shadow object already exist. Reason: "
						+ ex.getMessage(), ex);
		throw new ObjectAlreadyExistsException(
				"Couldn't add shadow object to the repository. Shadow object already exist. Reason: "
						+ ex.getMessage(), ex);
	}
	shadow.setOid(oid);

	LOGGER.trace("Object added to the repository successfully.");

	parentResult.recordSuccess();

	return shadow.getOid();
	}

	@Override
	public <T extends ResourceObjectShadowType> void afterModifyOnResource(PrismObject<T> shadowType, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		Collection<? extends ItemDelta> shadowChanges = getShadowChanges(modifications);
		if (shadowChanges != null && !shadowChanges.isEmpty()) {
			LOGGER.trace(
					"Detected shadow changes. Start to modify shadow in the repository, applying modifications {}",
					DebugUtil.debugDump(shadowChanges));
			try {
				getRepositoryService().modifyObject(ResourceObjectShadowType.class, shadowType.getOid(), shadowChanges, parentResult);
				LOGGER.trace("Shadow changes processed successfully.");
			} catch (ObjectAlreadyExistsException ex) {
				throw new SystemException(ex);
			}
		}

		
	}
	
	@SuppressWarnings("rawtypes")
	private Collection<? extends ItemDelta> getShadowChanges(Collection<? extends ItemDelta> objectChange)
			throws SchemaException {

		Collection<ItemDelta> shadowChanges = new ArrayList<ItemDelta>();
		for (ItemDelta itemDelta : objectChange) {
			if (new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES).equals(itemDelta.getParentPath())
					|| SchemaConstants.PATH_PASSWORD.equals(itemDelta.getParentPath())
					|| SchemaConstants.PATH_ACTIVATION.equals(itemDelta.getParentPath())) {
				continue;
			} else {
				shadowChanges.add(itemDelta);
			}
		}
		return shadowChanges;
	}

	@Override
	public <T extends ResourceObjectShadowType> Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<T> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException {
		
		// TODO: error handling
		//do not merge deltas when complete postponed operation is set to false, because it can cause some unexpected behavior..
		if (options != null && options.getCompletePostponed() != null && !ProvisioningOperationOptions.isCompletePostponed(options)){
			return modifications;
		}
		
		ObjectDelta mergedDelta = mergeDeltas(shadow, modifications);

		if (mergedDelta != null) {
			modifications = mergedDelta.getModifications();
		}
		
		return modifications;
		
	}
	
	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T extends ResourceObjectShadowType> ObjectDelta mergeDeltas(PrismObject<T> shadow, Collection<? extends ItemDelta> modifications)
			throws SchemaException {
		T shadowType = shadow.asObjectable();
		if (shadowType.getObjectChange() != null) {

			ObjectDeltaType deltaType = shadowType.getObjectChange();
			Collection<? extends ItemDelta> pendingModifications = DeltaConvertor.toModifications(
					deltaType.getModification(), shadow.getDefinition());

			return ObjectDelta.summarize(ObjectDelta.createModifyDelta(shadow.getOid(), modifications,
					ResourceObjectShadowType.class, getPrismContext()), ObjectDelta.createModifyDelta(shadow.getOid(),
					pendingModifications, ResourceObjectShadowType.class, getPrismContext()));
		}
		return null;
	}

	
	
}
