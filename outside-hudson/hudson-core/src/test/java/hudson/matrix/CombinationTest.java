/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
*
*    Kohsuke Kawaguchi
 *     
 *
 *******************************************************************************/ 

package hudson.matrix;

import java.util.Map;
import java.util.HashMap;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Kohsuke Kawaguchi
 */
public class CombinationTest{
    AxisList axes = new AxisList(
            new Axis("a","X","x"),
            new Axis("b","Y","y"));

    @Test
    @SuppressWarnings({"RedundantStringConstructorCall"})
    public void testEval() {
       // Groovy is moved to a plugin. This should be moved to a test harness where groovy plugin
       // could be installed.
       
//        Map<String,String> r = new HashMap<String, String>();
//        r.put("a","X");
//        r.put("b",new String("Y")); // make sure this 'Y' is not the same object as literal "Y".
//        Combination c = new Combination(r);
//
//        r.put("a","x");
//        Combination d = new Combination(r);
//
//        assertTrue(eval(c, null));
//        assertTrue(eval(c,"    "));
//        assertTrue(eval(c,"true"));
//        assertTrue(eval(c,"a=='X'"));
//        assertTrue(eval(c,"b=='Y'"));
//        assertTrue(eval(c,"(a=='something').implies(b=='other')"));
//        assertTrue(eval(c,"index%2==0")^eval(d,"index%2==0"));
//        assertTrue(eval(c,"index%2==1")^eval(d,"index%2==1"));
    }

    private boolean eval(Combination c, String exp) {
        return c.evalScriptExpression(axes, exp);
    }
}
