package org.xbib.elasticsearch.plugin.reference;

import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.plugins.MapperPlugin;
import org.elasticsearch.plugins.Plugin;
import org.xbib.elasticsearch.common.reference.ReferenceService;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapper;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapperModule;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapperTypeParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class ReferencePlugin extends Plugin implements MapperPlugin {

    private static final ReferenceMapperTypeParser referenceMapperTypeParser = new ReferenceMapperTypeParser();

    @Override
    public Map<String, Mapper.TypeParser> getMappers() {
        Map<String, Mapper.TypeParser> extra = new LinkedHashMap<>();
        extra.put(ReferenceMapper.MAPPER_TYPE, referenceMapperTypeParser);
        return extra;
    }

    @Override
    public Collection<Module> createGuiceModules() {
        Collection<Module> extra = new ArrayList<>();
        extra.add(new ReferenceMapperModule(referenceMapperTypeParser));
        return extra;
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> getGuiceServiceClasses() {
        Collection<Class<? extends LifecycleComponent>> extra = new ArrayList<>();
        extra.add(ReferenceService.class);
        return extra;
    }

}
