package org.xbib.elasticsearch.plugin.reference;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.xbib.elasticsearch.module.reference.ReferenceModule;

import java.util.ArrayList;
import java.util.Collection;

public class ReferencePlugin extends Plugin {

    private final Settings settings;

    @Inject
    public ReferencePlugin(Settings settings) {
        this.settings = settings;
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
    public Collection<Module> indexModules(Settings indexSettings) {
        Collection<Module> modules = new ArrayList<>();
        if (settings.getAsBoolean("plugins.reference.enabled", true)) {
            modules.add(new ReferenceModule());
        }
        return modules;
    }

}
