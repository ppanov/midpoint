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

package com.evolveum.midpoint.model.impl.expr;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.UndefinedFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFilterExpression extends AbstractInternalModelIntegrationTest {

	private static final String TEST_DIR = "src/test/resources/expr";

	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	private TaskManager taskManager;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void test100EvaluateExpressionToUndefinedFilter() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToUndefinedFilter";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult result = new OperationResult(TestFilterExpression.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);
		
		ObjectFilter filter = evaluateExpressionAssertFilterEmployeeType("expression-employeeType-undefined-filter.xml", 
				null, UndefinedFilter.class, task, result);
		
		executeFilter(filter, 5, task, result);
	}
	
	@Test
	public void test110EvaluateExpressionToNoneFilter() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToNoneFilter";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult result = new OperationResult(TestFilterExpression.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);
		
		ObjectFilter filter = evaluateExpressionAssertFilterEmployeeType("expression-employeeType-none-filter.xml", 
				null, NoneFilter.class, task, result);
		
		executeFilter(filter, 0, task, result);
	}

	@Test
	public void test120EvaluateExpressionToAllFilter() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToAllFilter";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult result = new OperationResult(TestFilterExpression.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);
		
		ObjectFilter filter = evaluateExpressionAssertFilterEmployeeType("expression-employeeType-all-filter.xml", 
				null, AllFilter.class, task, result);
		
		executeFilter(filter, 5, task, result);
	}

	@Test
	public void test130EvaluateExpressionToError() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToError";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult result = new OperationResult(TestFilterExpression.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);
		
		try {
			evaluateExpressionAssertFilterEmployeeType("expression-employeeType-error.xml", 
					null, NoneFilter.class, task, result);
			AssertJUnit.fail("Unexpected success");
		} catch (ExpressionEvaluationException e) {
			// this is expected
			assertTrue("Unexpected exception message: "+e.getMessage(), e.getMessage().contains("evaluated to no value"));
		}
	}

	
	@Test
	public void test140EvaluateExpressionToEmptyFilter() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToEmptyFilter";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult result = new OperationResult(TestFilterExpression.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);
		
		ObjectFilter filter = evaluateExpressionAssertFilterEmployeeType("expression-employeeType-empty-filter.xml", 
				null, EqualFilter.class, task, result);
		
		EqualFilter equalFilter = (EqualFilter) filter;
		AssertJUnit.assertNull("Expected NO values in filter, but found " + equalFilter.getValues(), equalFilter.getValues());
		
		executeFilter(filter, 4, task, result);
	}

	@Test
	public void test150EvaluateExpressionDefaultsNull() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionDefaults";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult result = new OperationResult(TestFilterExpression.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);
		
		
		ObjectFilter filter = evaluateExpressionAssertFilterEmployeeType("expression-employeeType-filter-defaults.xml", 
				null, EqualFilter.class, task, result);

		EqualFilter equalFilter = (EqualFilter) filter;
		AssertJUnit.assertNull("Expected NO values in filter, but found " + equalFilter.getValues(), equalFilter.getValues());
		
		executeFilter(filter, 4, task, result);
	}
	
	@Test
	public void test152EvaluateExpressionDefaultsCaptain() throws Exception {
		final String TEST_NAME = "test152EvaluateExpressionDefaultsCaptain";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult result = new OperationResult(TestFilterExpression.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);
		
		ObjectFilter filter = evaluateExpressionAssertFilterEmployeeType("expression-employeeType-filter-defaults.xml", 
				"CAPTAIN", EqualFilter.class, task, result);

		EqualFilter equalFilter = (EqualFilter) filter;
		PrismAsserts.assertValues("Wrong values in filter", equalFilter.getValues(), "CAPTAIN");
		
		executeFilter(filter, 1, task, result);
	}
	
	private ObjectFilter evaluateExpressionAssertFilterEmployeeType(String filename, 
			String employeeTypeInput, Class<? extends ObjectFilter> expectedType,
			Task task, OperationResult result) throws SchemaException, IOException, ObjectNotFoundException, ExpressionEvaluationException {
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		SearchFilterType filterType = PrismTestUtil.parseAtomicValue(new File(TEST_DIR, filename), SearchFilterType.COMPLEX_TYPE);

		ObjectFilter filter = QueryJaxbConvertor.createObjectFilter(UserType.class, filterType, prismContext);

		Map<QName, Object> params = new HashMap<>();
		PrismPropertyValue<String> pval = null;
		if (employeeTypeInput != null) {
			pval = new PrismPropertyValue<String>(employeeTypeInput);
		}
		params.put(UserType.F_EMPLOYEE_TYPE, pval);

		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinitions(params);

		// WHEN
		ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(filter, variables, expressionFactory, prismContext,
				"evaluating filter with null value not allowed", task, result);

		// THEN
		AssertJUnit.assertTrue("Expression should be evaluated to "+expectedType+", but was "+evaluatedFilter, expectedType.isAssignableFrom(evaluatedFilter.getClass()));
		
		return evaluatedFilter;
	}
	
	private void executeFilter(ObjectFilter filter, int expectedNumberOfResults, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		SearchResultList<PrismObject<UserType>> objects = modelService.searchObjects(UserType.class, query, null, task, result);
		display("Found objects", objects);
		assertEquals("Wrong number of results (found: "+objects+")", expectedNumberOfResults, objects.size());
	}

}
