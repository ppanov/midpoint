/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class FocusTypeAssignmentPopupTabPanel<F extends FocusType> extends AbstractAssignmentPopupTabPanel<F>{

    private static final long serialVersionUID = 1L;

    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";

    private static final String DOT_CLASS = FocusTypeAssignmentPopupTabPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(FocusTypeAssignmentPopupTabPanel.class);
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";

    public FocusTypeAssignmentPopupTabPanel(String id, ObjectTypes type){
        this(id, type, new ArrayList<>());
    }

    public FocusTypeAssignmentPopupTabPanel(String id, ObjectTypes type, List<F> selectedObjects){
        super(id, type, selectedObjects);
    }

    @Override
    protected void initParametersPanel(){
        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        add(relationContainer);

        DropDownChoicePanel<RelationTypes> relationSelector = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
                WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class), Model.of(RelationTypes.MEMBER),
                FocusTypeAssignmentPopupTabPanel.this, false);
        relationSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        relationSelector.setOutputMarkupId(true);
        relationSelector.setOutputMarkupPlaceholderTag(true);
        relationContainer.add(relationSelector);
    }

    @Override
    protected List<AssignmentType> getSelectedAssignmentsList(){
        List<AssignmentType> assignmentList = new ArrayList<>();

        List<FocusType> selectedObjects = getSelectedObjectsList();
        QName relation = getRelationValue();
        selectedObjects.forEach(selectedObject -> {
            assignmentList.add(ObjectTypeUtil.createAssignmentTo(selectedObject, relation));
        });
        return assignmentList;
    }

    public QName getRelationValue(){
        DropDownChoicePanel<RelationTypes> relationPanel = getRelationDropDown();
        RelationTypes relation = relationPanel.getModel().getObject();
        if (relation == null) {
            return SchemaConstants.ORG_DEFAULT;
        }
        return relation.getRelation();
    }

    private DropDownChoicePanel getRelationDropDown(){
        return (DropDownChoicePanel)get(ID_RELATION_CONTAINER).get(ID_RELATION);
    }

    @Override
    protected ObjectQuery addFilterToContentQuery(ObjectQuery query){
        LOGGER.debug("Loading roles which the current user has right to assign");
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ROLES);
        OperationResult result = task.getResult();
        ObjectFilter filter = null;
        try {
            ModelInteractionService mis = getPageBase().getModelInteractionService();
            RoleSelectionSpecification roleSpec =
                    mis.getAssignableRoleSpecification(SecurityUtils.getPrincipalUser().getUser().asPrismObject(), task, result);
            filter = roleSpec.getFilter();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load available roles", ex);
            result.recordFatalError("Couldn't load available roles", ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            getPageBase().showResult(result);
        }
        if (query == null){
            query = new ObjectQuery();
        }
        query.addFilter(filter);
        return query;
    }
}
