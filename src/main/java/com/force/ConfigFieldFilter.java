package com.force;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;

import com.force.sdk.codegen.filter.FieldFilter;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.typesafe.config.Config;

public class ConfigFieldFilter implements FieldFilter {
    private Config config;
    private Set<String> globalIncludeFields = Sets.newTreeSet();
    private Set<String> globalExcludeFields = Sets.newTreeSet();
    private Log log;

    public ConfigFieldFilter(Config config, final Log log) {
        this.log = log;
        this.config = config;

        // add wildcards
        if (config.hasPath("_")) {
            log.info("Has wildcard fields:");
            for (String s : config.getStringList("_")) {
                s = s.toLowerCase();
                if (s.startsWith("~")) {
                    s = s.replaceAll("^~", "");
                    globalExcludeFields.add(s);
                } else {
                    globalIncludeFields.add(s);
                }
            }
            if (log.isInfoEnabled()) {
                if (!globalIncludeFields.isEmpty())
                    log.info("  Including fields " + StringUtils.join(globalIncludeFields, ", "));
                if (!globalExcludeFields.isEmpty())
                    log.info("  Excluding fields " + StringUtils.join(globalExcludeFields, ", "));
            }
        } else {
            log.info("No wildcard fields.");
        }
    }

    @Override
    public List<Field> filter(final DescribeSObjectResult dsr) {
        if (dsr == null) return null;

        List<String> strList;
        if (config.hasPath(dsr.getName()))
            strList = config.getStringList(dsr.getName());
        else
            strList = config.getStringList(dsr.getName().replaceAll("__c$", "").replaceAll("_", ""));

        if (strList.isEmpty()) {
            log.warn("For object " + dsr.getName() + " - no fields matched, skipping!");
            return Collections.emptyList();
        }

        log.info("For object " + dsr.getName() + ":");

        Set<String> includeFields = Sets.newTreeSet();
        Set<String> excludeFields = Sets.newTreeSet();

        final AtomicBoolean includeAll = new AtomicBoolean(false);

        for (String s : strList) {
            s = s.toLowerCase();
            if (s.equals("_")) {
                includeAll.set(true);
            } else if (s.startsWith("~")) {
                s = s.replaceAll("^~", "");
                excludeFields.add(s);
            } else {
                includeFields.add(s);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("  Include everything not explicitly excluded: " + includeAll);
            if (!includeFields.isEmpty()) log.info("  Including fields " + StringUtils.join(includeFields, ", "));
            if (!excludeFields.isEmpty()) log.info("  Excluding fields " + StringUtils.join(excludeFields, ", "));
        }

        final Set<String> completeIncludes = Sets.union(includeFields, globalIncludeFields);
        final Set<String> completeExcludes = Sets.union(excludeFields, globalExcludeFields);

        Field f = new Field();

        Iterable<Field> filteredFields = Iterables.filter(Lists.newArrayList(dsr.getFields()), new Predicate<Field>() {
            @Override
            public boolean apply(final Field field) {
                String fieldNameLower = field.getName().toLowerCase();
                String sansId = fieldNameLower.replaceAll("id$", "");

                if (field.getType() == FieldType.combobox) {
                    field.setType(FieldType.picklist);
                }

                if (includeAll.get()) {
                    return !completeExcludes.contains(fieldNameLower) && !completeExcludes.contains(sansId);
                } else {
                    return completeIncludes.contains(fieldNameLower) || completeIncludes.contains(sansId);
                }
            }
        });

        return Lists.newArrayList(filteredFields);
    }

}