package org.xbib.elasticsearch.plugin.reference;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.xbib.elasticsearch.module.reference.ReferenceModule;

import java.util.Collection;

import static org.elasticsearch.common.collect.Lists.newArrayList;

public class ReferencePlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "analysis-reference-"
                + Build.getInstance().getVersion() + "-"
                + Build.getInstance().getShortHash();
    }

    @Override
    public String description() {
        return "Analysis reference plugin";
    }

    @Override
    public Collection<Class<? extends Module>> indexModules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        modules.add(ReferenceModule.class);
        return modules;
    }

}
