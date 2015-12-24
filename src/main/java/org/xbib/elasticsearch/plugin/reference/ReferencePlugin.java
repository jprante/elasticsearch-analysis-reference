package org.xbib.elasticsearch.plugin.reference;

import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.plugins.Plugin;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapper;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapperModule;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapperService;
import org.xbib.elasticsearch.index.mapper.reference.ReferenceMapperTypeParser;

import java.util.ArrayList;
import java.util.Collection;

public class ReferencePlugin extends Plugin {

    private final Settings settings;

    private final ReferenceMapperTypeParser refMapperTypeParser;

    @Inject
    public ReferencePlugin(Settings settings) {
        this.settings = settings;
        this.refMapperTypeParser = new ReferenceMapperTypeParser();
    }

    @Override
    public String name() {
        return "reference";
    }

    @Override
    public String description() {
        return "Reference plugin for Elasticsearch";
    }

    @Override
    public Collection<Module> nodeModules() {
        Collection<Module> modules = new ArrayList<>();
        if ("node".equals(settings.get("client.type"))) {
            if (settings.getAsBoolean("plugins.reference.enabled", true)) {
                modules.add(new ReferenceMapperModule(refMapperTypeParser));
            }
        }
        return modules;
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        Collection<Class<? extends LifecycleComponent>> services = new ArrayList<>();
        if ("node".equals(settings.get("client.type"))) {
            if (settings.getAsBoolean("plugins.reference.enabled", true)) {
                services.add(ReferenceMapperService.class);
            }
        }
        return services;
    }

    public void onModule(IndicesModule indicesModule) {
        if ("node".equals(settings.get("client.type"))) {
            if (settings.getAsBoolean("plugins.reference.enabled", true)) {
                indicesModule.registerMapper(ReferenceMapper.CONTENT_TYPE, refMapperTypeParser);
            }
        }
    }

}
