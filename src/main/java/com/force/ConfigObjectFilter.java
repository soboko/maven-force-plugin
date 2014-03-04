package com.force;

import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

import com.force.sdk.codegen.filter.ObjectFilter;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.typesafe.config.Config;

public class ConfigObjectFilter implements ObjectFilter {
    private Config config;
    private Log log;

    public ConfigObjectFilter(Config config, final Log log) {
        this.config = config;
        this.log = log;
    }

    @Override
    public List<DescribeSObjectResult> filter(final List<DescribeSObjectResult> dsrs) {
        if (dsrs == null) return null;

        final Collection<String> objNamesLower = Collections2.transform(config.root().keySet(), new Function<String, String>() {
            @Override
            public String apply(final String s) {
                return s.toLowerCase().replaceAll("_", "");
            }
        });

        Iterable<DescribeSObjectResult> filtered = Iterables.filter(dsrs, new Predicate<DescribeSObjectResult>() {
            @Override
            public boolean apply(final DescribeSObjectResult dsr) {
                String nameLower = dsr.getName().toLowerCase();
                String sansC = nameLower.replaceAll("__c$", "").replaceAll("_", "");
                boolean result = objNamesLower.contains(nameLower) || objNamesLower.contains(sansC);
                if (result) {
                    log.info("Generating for " + dsr.getName());
                } else {
                    log.debug("Skipping for " + dsr.getName());
                }
                return result;
            }
        });

        return Lists.newArrayList(filtered);
    }
}