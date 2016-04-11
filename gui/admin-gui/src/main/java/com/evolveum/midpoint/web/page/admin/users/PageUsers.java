/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserListItemDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UsersStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/users", action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_URL,
                label = "PageUsers.auth.users.label",
                description = "PageUsers.auth.users.description")})
public class PageUsers extends PageAdminUsers {

    private static final Trace LOGGER = TraceManager.getTrace(PageUsers.class);
    private static final String DOT_CLASS = PageUsers.class.getName() + ".";
    private static final String OPERATION_DELETE_USERS = DOT_CLASS + "deleteUsers";
    private static final String OPERATION_DELETE_USER = DOT_CLASS + "deleteUser";
    private static final String OPERATION_DISABLE_USERS = DOT_CLASS + "disableUsers";
    private static final String OPERATION_DISABLE_USER = DOT_CLASS + "disableUser";
    private static final String OPERATION_ENABLE_USERS = DOT_CLASS + "enableUsers";
    private static final String OPERATION_ENABLE_USER = DOT_CLASS + "enableUser";
    private static final String OPERATION_RECONCILE_USERS = DOT_CLASS + "reconcileUsers";
    private static final String OPERATION_RECONCILE_USER = DOT_CLASS + "reconcileUser";
    private static final String OPERATION_UNLOCK_USERS = DOT_CLASS + "unlockUsers";
    private static final String OPERATION_UNLOCK_USER = DOT_CLASS + "unlockUser";
    private static final String DIALOG_CONFIRM_DELETE = "confirmDeletePopup";

    private static final String ID_EXECUTE_OPTIONS = "executeOptions";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";
    private static final String ID_SEARCH = "search";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_TABLE_HEADER = "tableHeader";

    private UserType singleDelete;
    private LoadableModel<Search> searchModel;
    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel;

    public PageUsers() {
        this(true, null, null);
    }
    
    public PageUsers(boolean clearPagingInSession) {
        this(clearPagingInSession, null, null);
    }
    

    public PageUsers(boolean clearPagingInSession, final UsersDto.SearchType type, final String text) {
//        searchModel = new LoadableModel<Search>(false) {
//
//            @Override
//            public Search load() {
//                UsersStorage storage = getSessionStorage().getUsers();
//                Search search = storage.getUsersSearch();
//                if (search == null) {
//                    search = SearchFactory.createSearch(UserType.class, getPrismContext(), true);
//                }
//
//                return search;
//            }
//        };

        executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(false) {

            @Override
            protected ExecuteChangeOptionsDto load() {
                return new ExecuteChangeOptionsDto();
            }
        };

        initLayout();
    }

    public PageUsers(UsersDto.SearchType type, String text) {
        this(true, type, text);
    }


    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        add(new ConfirmationDialog(DIALOG_CONFIRM_DELETE,
                createStringResource("pageUsers.dialog.title.confirmDelete"), createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteConfirmedPerformed(target);
            }
        });

        initTable(mainForm);
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if (singleDelete == null) {
                    return createStringResource("pageUsers.message.deleteUserConfirm",
                            getTable().getSelectedObjects().size()).getString();
                } else {
                    return createStringResource("pageUsers.message.deleteUserConfirmSingle",
                            singleDelete.getName()).getString();
                }
            }
        };
    }

    private List<IColumn<SelectableBean<UserType>, String>> initColumns() {
        List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<IColumn<SelectableBean<UserType>, String>>();

        columns.add(new CheckBoxHeaderColumn());
//        columns.add(new IconColumn<UserListItemDto>(null) {
//
//            @Override
//            protected IModel<String> createIconModel(final IModel<UserListItemDto> rowModel) {
//                return new AbstractReadOnlyModel<String>() {
//
//                    @Override
//                    public String getObject() {
//                        return rowModel.getObject().getIcon();
//                    }
//                };
//            }
//
//            @Override
//            protected IModel<String> createTitleModel(final IModel<UserListItemDto> rowModel) {
//                return new AbstractReadOnlyModel<String>() {
//
//                    @Override
//                    public String getObject() {
//                        String key = rowModel.getObject().getIconTitle();
//                        if (key == null) {
//                            return null;
//                        }
//                        return createStringResource(key).getString();
//                    }
//                };
//            }
//        });

//        IColumn column = new LinkColumn<UserListItemDto>(createStringResource("ObjectType.name"),
//                UserType.F_NAME.getLocalPart(), UserListItemDto.F_NAME) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target, IModel<UserListItemDto> rowModel) {
//                userDetailsPerformed(target, rowModel.getObject().getOid());
//            }
//        };
//        columns.add(column);

        IColumn<SelectableBean<UserType>, String> column = new PropertyColumn(createStringResource("UserType.givenName"),
                UserType.F_GIVEN_NAME.getLocalPart(), SelectableBean.F_VALUE + ".givenName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.familyName"),
                UserType.F_FAMILY_NAME.getLocalPart(), SelectableBean.F_VALUE + ".familyName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.fullName"),
                UserType.F_FULL_NAME.getLocalPart(), SelectableBean.F_VALUE + ".fullName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.emailAddress"), null, SelectableBean.F_VALUE + ".emailAddress");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<UserType>, String>(createStringResource("pageUsers.accounts")) {
        	
        	@Override
        	public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem, String componentId,
        			IModel<SelectableBean<UserType>> model) {
        		cellItem.add(new Label(componentId, model.getObject().getValue().getLinkRef().size()));
        	}
		};
        
//        column = new PropertyColumn(createStringResource("pageUsers.accounts"), null, UserListItemDto.F_ACCOUNT_COUNT);
//        columns.add(column);

        column = new InlineMenuHeaderColumn(initInlineMenu());
        columns.add(column);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.enable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, true, null);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.disable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, false, null);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.reconcile"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        reconcilePerformed(target, null);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.unlock"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        unlockPerformed(target, null);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem());

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.delete"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deletePerformed(target, null);
                    }
                }));

        return headerMenuItems;
    }

    private void initTable(Form mainForm) {
//        List<IColumn<SelectableBean<UserType>, String>> columns = initColumns();

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        options.add(SelectorOptions.create(UserType.F_LINK_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        options.add(SelectorOptions.create(UserType.F_ASSIGNMENT,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        MainObjectListPanel<UserType> userListPanel = new MainObjectListPanel<UserType>(ID_TABLE, UserType.class, options, this) {
		
			@Override
			protected List<IColumn<SelectableBean<UserType>, String>> createColumns() {
				return PageUsers.this.initColumns();
			}
			
			@Override
			protected void objectDetailsPerformed(AjaxRequestTarget target, UserType object) {
				// TODO Auto-generated method stub
				userDetailsPerformed(target, object.getOid());
			}
		};
		mainForm.add(userListPanel);
        
//        ObjectDataProvider<UserListItemDto, UserType> provider =
//                new ObjectDataProvider<UserListItemDto, UserType>(PageUsers.this, UserType.class) {
//
//                    @Override
//                    protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
//                        UsersStorage storage = getSessionStorage().getUsers();
//                        storage.setPaging(paging);
//                    }
//
//                    @Override
//                    public UserListItemDto createDataObjectWrapper(PrismObject<UserType> obj) {
//                        return createRowDto(obj);
//                    }
//                };
//
//        Search search = searchModel.getObject();
//        ObjectQuery query = search.createObjectQuery(getPrismContext());
//        provider.setQuery(query);
//
//        
//        provider.setOptions(options);
//
//        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns,
//                UserProfileStorage.TableId.PAGE_USERS_PANEL,
//                (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_USERS_PANEL)) {
//
//            @Override
//            protected WebMarkupContainer createHeader(String headerId) {
//                return new SearchFragment(headerId, ID_TABLE_HEADER, PageUsers.this, searchModel, executeOptionsModel);
//            }
//        };
//
//        table.setOutputMarkupId(true);
//
//        UsersStorage storage = getSessionStorage().getUsers();
//        table.setCurrentPage(storage.getPaging());
//
//        mainForm.add(table);
    }

//    private UserListItemDto createRowDto(PrismObject<UserType> obj) {
//        UserType user = obj.asObjectable();
//
//        UserListItemDto dto = new UserListItemDto(user.getOid(),
//                WebComponentUtil.getOrigStringFromPoly(user.getName()),
//                WebComponentUtil.getOrigStringFromPoly(user.getGivenName()),
//                WebComponentUtil.getOrigStringFromPoly(user.getFamilyName()),
//                WebComponentUtil.getOrigStringFromPoly(user.getFullName()),
//                user.getEmailAddress());
//
//        dto.setAccountCount(createAccountCount(obj));
//        dto.setCredentials(obj.findContainer(UserType.F_CREDENTIALS));
//        dto.setIcon(WebComponentUtil.createUserIcon(obj));
//        dto.setIconTitle(WebComponentUtil.createUserIconTitle(obj));
//
//        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.enable"),
//                new ColumnMenuAction<SelectableBean<UserType>>() {
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        SelectableBean<UserType> rowDto = getRowModel().getObject();
//                        updateActivationPerformed(target, true, rowDto.getValue());
//                    }
//                }));
//
//        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.disable"),
//                new ColumnMenuAction<SelectableBean<UserType>>() {
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                    	SelectableBean<UserType> rowDto = getRowModel().getObject();
//                        updateActivationPerformed(target, false, rowDto.getValue());
//                    }
//                }));
//
//        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.reconcile"),
//                new ColumnMenuAction<SelectableBean<UserType>>() {
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                    	SelectableBean<UserType> rowDto = getRowModel().getObject();
//                        reconcilePerformed(target, rowDto.getValue());
//                    }
//                }));
//
//        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.unlock"),
//                new ColumnMenuAction<SelectableBean<UserType>>() {
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                    	SelectableBean<UserType> rowDto = getRowModel().getObject();
//                        unlockPerformed(target, rowDto.getValue());
//                    }
//                }));
//
//        dto.getMenuItems().add(new InlineMenuItem());
//
//        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.delete"),
//                new ColumnMenuAction<SelectableBean<UserType>>() {
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                    	SelectableBean<UserType> rowDto = getRowModel().getObject();
//                        deletePerformed(target, rowDto.getValue());
//                    }
//                }));
//
//
//        return dto;
//    }

//    private int createAccountCount(PrismObject<UserType> object) {
//        PrismReference accountRef = object.findReference(UserType.F_LINK_REF);
//        return accountRef != null ? accountRef.size() : 0;
//    }

    private void userDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        getSessionStorage().setPreviousPageInstance(new PageUsers(false));
        setResponsePage(PageUser.class, parameters);
    }

    private MainObjectListPanel<UserType> getTable() {
        return (MainObjectListPanel<UserType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

//    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
//        target.add(getFeedbackPanel());
//
//        BoxedTablePanel panel = getTable();
//        DataTable table = panel.getDataTable();
//        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
//        provider.setQuery(query);
//
////        UsersStorage storage = getSessionStorage().getUsers();
////        storage.setUsersSearch(searchModel.getObject());
////        storage.setUsersPaging(null);
//        panel.setCurrentPage(null);
//
//        target.add(panel);
//    }

    private void deletePerformed(AjaxRequestTarget target, UserType selectedUser) {
        singleDelete = selectedUser;
        List<UserType> users = isAnythingSelected(target, selectedUser);
        if (users.isEmpty()) {
            return;
        }

        ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target) {
        List<UserType> users = new ArrayList<UserType>();

        if (singleDelete == null) {
            users = isAnythingSelected(target, null);
        } else {
            users.add(singleDelete);
        }

        if (users.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_USERS);
        for (UserType user : users) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_USER);
            try {
                Task task = createSimpleTask(OPERATION_DELETE_USER);

                ObjectDelta delta = new ObjectDelta(UserType.class, ChangeType.DELETE, getPrismContext());
                delta.setOid(user.getOid());

                ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
                ModelExecuteOptions options = executeOptions.createOptions();
                LOGGER.debug("Using options {}.", new Object[]{executeOptions});
                getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), options, task, subResult);
                subResult.computeStatus();
            } catch (Exception ex) {
                subResult.recomputeStatus();
                subResult.recordFatalError("Couldn't delete user.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't delete user", ex);
            }
        }
        result.computeStatusComposite();

//        ObjectDataProvider<UserListItemDto, UserType> provider = (ObjectDataProvider) getTable().getDataTable()
//                .getDataProvider();
//        provider.clearCache();
        getTable().clearCache();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

//    public static String toShortString(UserListItemDto object) {
//        if (object == null) {
//            return "null";
//        }
//        StringBuilder builder = new StringBuilder();
//        builder.append(ObjectTypeUtil.getShortTypeName(UserType.class));
//        builder.append(": ");
//        builder.append(object.getName());
//        builder.append(" (OID:");
//        builder.append(object.getOid());
//        builder.append(")");
//
//        return builder.toString();
//    }

    private void unlockPerformed(AjaxRequestTarget target, UserType selectedUser) {
        List<UserType> users = isAnythingSelected(target, selectedUser);
        if (users.isEmpty()) {
            return;
        }
        OperationResult result = new OperationResult(OPERATION_UNLOCK_USERS);
        for (UserType user : users) {
//            String userShortString = toShortString(user);
            OperationResult opResult = result.createSubresult(getString(OPERATION_UNLOCK_USER, user));
            try {
                Task task = createSimpleTask(OPERATION_UNLOCK_USER + user);
                // TODO skip the operation if the user has no password credentials specified (otherwise this would create almost-empty password container)
                ObjectDelta delta = ObjectDelta.createModificationReplaceProperty(UserType.class, user.getOid(),
                        new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS), getPrismContext(), 0);
                Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil.createDeltaCollection(delta);
                getModelService().executeChanges(deltas, null, task, opResult);
                opResult.computeStatusIfUnknown();
            } catch (Exception ex) {
                opResult.recomputeStatus();
                opResult.recordFatalError("Couldn't unlock user " + user + ".", ex);
                LoggingUtils.logException(LOGGER, "Couldn't unlock user " + user + ".", ex);
            }
        }

        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    private void reconcilePerformed(AjaxRequestTarget target, UserType selectedUser) {
        List<UserType> users = isAnythingSelected(target, selectedUser);
        if (users.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RECONCILE_USERS);
        for (UserType user : users) {
//            String userShortString = toShortString(user);
            OperationResult opResult = result.createSubresult(getString(OPERATION_RECONCILE_USER, user));
            try {
                Task task = createSimpleTask(OPERATION_RECONCILE_USER + user);
                ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, user.getOid(), getPrismContext());
                Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil.createDeltaCollection(delta);
                getModelService().executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, opResult);
                opResult.computeStatusIfUnknown();
            } catch (Exception ex) {
                opResult.recomputeStatus();
                opResult.recordFatalError("Couldn't reconcile user " + user + ".", ex);
                LoggingUtils.logException(LOGGER, "Couldn't reconcile user " + user + ".", ex);
            }
        }

        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    /**
     * This method check selection in table. If selectedUser != null than it returns only this user.
     */
    private List<UserType> isAnythingSelected(AjaxRequestTarget target, UserType selectedUser) {
        List<UserType> users;
        if (selectedUser != null) {
            users = new ArrayList<>();
            users.add(selectedUser);
        } else {
        	users = getTable().getSelectedObjects();
//            users = WebComponentUtil.getSelectedData(getTable());
            if (users.isEmpty()) {
                warn(getString("pageUsers.message.nothingSelected"));
                target.add(getFeedbackPanel());
            }
        }

        return users;
    }

    /**
     * This method updates user activation. If userOid parameter is not null, than it updates only that user,
     * otherwise it checks table for selected users.
     */
    private void updateActivationPerformed(AjaxRequestTarget target, boolean enabling, UserType selectedUser) {
        List<UserType> users = isAnythingSelected(target, selectedUser);
        if (users.isEmpty()) {
            return;
        }

        String operation = enabling ? OPERATION_ENABLE_USERS : OPERATION_DISABLE_USERS;
        OperationResult result = new OperationResult(operation);
        for (UserType user : users) {
            operation = enabling ? OPERATION_ENABLE_USER : OPERATION_DISABLE_USER;
            OperationResult subResult = result.createSubresult(operation);
            try {
                Task task = createSimpleTask(operation);

                ObjectDelta objectDelta = WebModelServiceUtils.createActivationAdminStatusDelta(UserType.class, user.getOid(),
                        enabling, getPrismContext());

                ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
                ModelExecuteOptions options = executeOptions.createOptions();
                LOGGER.debug("Using options {}.", new Object[]{executeOptions});
                getModelService().executeChanges(WebComponentUtil.createDeltaCollection(objectDelta), options, task,
                        subResult);
                subResult.recordSuccess();
            } catch (Exception ex) {
                subResult.recomputeStatus();
                if (enabling) {
                    subResult.recordFatalError("Couldn't enable user.", ex);
                    LoggingUtils.logException(LOGGER, "Couldn't enable user", ex);
                } else {
                    subResult.recordFatalError("Couldn't disable user.", ex);
                    LoggingUtils.logException(LOGGER, "Couldn't disable user", ex);
                }
            }
        }
        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }
    
//    private static class SearchFragment extends Fragment {
//
//        public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
//                              IModel<Search> model, IModel<ExecuteChangeOptionsDto> executeOptionsModel) {
//            super(id, markupId, markupProvider, model);
//
//            initLayout(executeOptionsModel);
//        }
//
//        private void initLayout(IModel<ExecuteChangeOptionsDto> executeOptionsModel) {
//            final Form searchForm = new Form(ID_SEARCH_FORM);
//            add(searchForm);
//            searchForm.setOutputMarkupId(true);
//
//            SearchPanel search = new SearchPanel(ID_SEARCH, (IModel) getDefaultModel()) {
//
//                @Override
//                public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
//                    PageUsers page = (PageUsers) getPage();
//                    page.searchPerformed(query, target);
//                }
//            };
//            searchForm.add(search);
//
//            add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, false, false, false));
//        }
//    }
}
