package org.xbib.elasticsearch.module.reference;

import org.elasticsearch.common.inject.Binder;
import org.elasticsearch.common.inject.Module;

public class ReferenceModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(RegisterReferenceType.class).asEagerSingleton();
    }
}
