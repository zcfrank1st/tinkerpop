/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.tinkerpop.gremlin.java.translator;

import org.apache.tinkerpop.gremlin.LoadGraphWith;
import org.apache.tinkerpop.gremlin.process.traversal.CoreTraversalTest;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalInterruptionComputerTest;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalInterruptionTest;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.PageRankTest;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.ProgramTest;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.ElementIdStrategyProcessTest;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.EventStrategyProcessTest;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.PartitionStrategyProcessTest;
import org.apache.tinkerpop.gremlin.process.traversal.util.JavaTranslator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.TinkerGraphProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TinkerGraphGraphSONTranslatorProvider extends TinkerGraphProvider {

    private static Set<String> SKIP_TESTS = new HashSet<>(Arrays.asList(
            "testProfileStrategyCallback",
            "testProfileStrategyCallbackSideEffect",
            "g_withSideEffectXa_setX_V_both_name_storeXaX_capXaX",
            "g_V_both_hasLabelXpersonX_order_byXage_decrX_name",
            "g_VX1X_out_injectXv2X_name",
            "shouldNeverPropagateANoBulkTraverser",
            "shouldNeverPropagateANullValuedTraverser",
            "shouldTraversalResetProperly",
            "shouldHidePartitionKeyForValues",
            //
            "g_VXlistXv1_v2_v3XX_name",
            "g_V_hasLabelXpersonX_asXpX_VXsoftwareX_addInEXuses_pX",
            "g_VXv1X_hasXage_gt_30X",
            "g_V_chooseXout_countX_optionX2L__nameX_optionX3L__valueMapX",
            "g_V_branchXlabelX_optionXperson__ageX_optionXsoftware__langX_optionXsoftware__nameX",
            //
            PageRankTest.Traversals.class.getCanonicalName(),
            ProgramTest.Traversals.class.getCanonicalName(),
            TraversalInterruptionTest.class.getCanonicalName(),
            TraversalInterruptionComputerTest.class.getCanonicalName(),
            EventStrategyProcessTest.class.getCanonicalName(),
            CoreTraversalTest.class.getCanonicalName(),
            PartitionStrategyProcessTest.class.getCanonicalName(),
            ElementIdStrategyProcessTest.class.getCanonicalName()));


    @Override
    public Map<String, Object> getBaseConfiguration(final String graphName, final Class<?> test, final String testMethodName,
                                                    final LoadGraphWith.GraphData loadGraphWith) {

        final Map<String, Object> config = super.getBaseConfiguration(graphName, test, testMethodName, loadGraphWith);
        config.put("skipTest", SKIP_TESTS.contains(testMethodName) || SKIP_TESTS.contains(test.getCanonicalName()));
        return config;
    }

    @Override
    public GraphTraversalSource traversal(final Graph graph) {
        if ((Boolean) graph.configuration().getProperty("skipTest"))
            return graph.traversal();
            //throw new VerificationException("This test current does not work with Gremlin-Python", EmptyTraversal.instance());
        else {
            final GraphTraversalSource g = graph.traversal();
            return g.withTranslator(new GraphSONTranslator<>(JavaTranslator.of(g, __.class)));
        }
    }
}