/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
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

package hudson.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SerializableConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import static java.util.logging.Level.FINE;

import java.util.logging.Logger;

/**
 * {@link CollectionConverter} that ignores {@link CannotResolveClassException}.
 *
 * <p> This allows Hudson to load XML files that contain non-existent classes
 * (the expected scenario is that those classes belong to plugins that were
 * unloaded.)
 *
 * @author Kohsuke Kawaguchi
 */
public class RobustCollectionConverter extends CollectionConverter {

    private final SerializableConverter sc;

    public RobustCollectionConverter(XStream xs) {
        this(xs.getMapper(), xs.getReflectionProvider());
    }

    public RobustCollectionConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
        super(mapper);
        sc = new SerializableConverter(mapper, reflectionProvider);
    }

    @Override
    public boolean canConvert(Class type) {
        return super.canConvert(type) || type == CopyOnWriteArrayList.class || type == CopyOnWriteArraySet.class;
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        // CopyOnWriteArrayList used to serialize as custom serialization,
        // so read it in a compatible fashion.
        String s = reader.getAttribute("serialization");
        if (s != null && s.equals("custom")) {
            return sc.unmarshal(reader, context);
        } else {
            return super.unmarshal(reader, context);
        }
    }

    @Override
    protected void populateCollection(HierarchicalStreamReader reader, UnmarshallingContext context, Collection collection) {
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            try {
                Object item = readItem(reader, context, collection);
                collection.add(item);
            } catch (CannotResolveClassException e) {
                LOGGER.log(FINE, "Failed to resolve class " + e.getMessage());
                RobustReflectionConverter.addErrorInContext(context, e);
            } catch (LinkageError e) {
                LOGGER.log(FINE, "Failed to resolve class " + e.getMessage());
                RobustReflectionConverter.addErrorInContext(context, e);
            }
            reader.moveUp();
        }
    }
    private static final Logger LOGGER = Logger.getLogger(RobustCollectionConverter.class.getName());
}
