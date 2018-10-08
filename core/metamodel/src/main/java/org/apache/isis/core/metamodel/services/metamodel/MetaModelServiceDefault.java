/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.metamodel.services.metamodel;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.datanucleus.enhancement.Persistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.applib.AppManifest;
import org.apache.isis.applib.AppManifest2;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.command.CommandDtoProcessor;
import org.apache.isis.applib.services.grid.GridService;
import org.apache.isis.applib.services.metamodel.DomainMember;
import org.apache.isis.applib.services.metamodel.MetaModelService6;
import org.apache.isis.applib.spec.Specification;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.actions.command.CommandFacet;
import org.apache.isis.core.metamodel.facets.object.objectspecid.ObjectSpecIdFacet;
import org.apache.isis.core.metamodel.services.appfeat.ApplicationFeatureId;
import org.apache.isis.core.metamodel.services.appfeat.ApplicationFeatureType;
import org.apache.isis.core.metamodel.services.appmanifest.AppManifestProvider;
import org.apache.isis.core.metamodel.spec.ObjectSpecId;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.Contributed;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.ObjectMember;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.core.metamodel.specloader.SpecificationLoader;
import org.apache.isis.objectstore.jdo.metamodel.facets.object.query.JdoNamedQuery;
import org.apache.isis.schema.metamodel.v1.FacetAttributeDto;
import org.apache.isis.schema.metamodel.v1.FacetDto;
import org.apache.isis.schema.metamodel.v1.FacetHolderDto;
import org.apache.isis.schema.metamodel.v1.MetamodelDto;
import org.apache.isis.schema.metamodel.v1.ObjectSpecDto;

@DomainService(
        nature = NatureOfService.DOMAIN,
        menuOrder = "" + Integer.MAX_VALUE
)
public class MetaModelServiceDefault implements MetaModelService6 {

    @SuppressWarnings("unused")
    private final static Logger LOG = LoggerFactory.getLogger(MetaModelServiceDefault.class);


    @Programmatic
    public Class<?> fromObjectType(final String objectType) {
        if(objectType == null) {
            return null;
        }
        final ObjectSpecId objectSpecId = new ObjectSpecId(objectType);
        final ObjectSpecification objectSpecification = specificationLookup.lookupBySpecId(objectSpecId);
        return objectSpecification != null? objectSpecification.getCorrespondingClass(): null;
    }

    @Override
    public String toObjectType(final Class<?> domainType) {
        if(domainType == null) {
            return null;
        }
        final ObjectSpecification objectSpecification = specificationLookup.loadSpecification(domainType);
        final ObjectSpecIdFacet objectSpecIdFacet = objectSpecification.getFacet(ObjectSpecIdFacet.class);
        final ObjectSpecId objectSpecId = objectSpecIdFacet.value();
        return objectSpecId.asString();
    }

    @Override
    public void rebuild(final Class<?> domainType) {
        specificationLookup.invalidateCache(domainType);
        gridService.remove(domainType);
        final ObjectSpecification objectSpecification = specificationLookup.loadSpecification(domainType);
        // ensure the spec is fully rebuilt
        objectSpecification.getObjectActions(Contributed.INCLUDED);
        objectSpecification.getAssociations(Contributed.INCLUDED);
        specificationLookup.postProcess(objectSpecification);
    }

    // //////////////////////////////////////



    @Programmatic
    public List<DomainMember> export() {

        final Collection<ObjectSpecification> specifications = specificationLookup.allSpecifications();

        final List<DomainMember> rows = Lists.newArrayList();
        for (final ObjectSpecification spec : specifications) {
            if (exclude(spec)) {
                continue;
            }
            final List<ObjectAssociation> properties = spec.getAssociations(Contributed.EXCLUDED, ObjectAssociation.Filters.PROPERTIES);
            for (final ObjectAssociation property : properties) {
                final OneToOneAssociation otoa = (OneToOneAssociation) property;
                if (exclude(otoa)) {
                    continue;
                }
                rows.add(new DomainMemberDefault(spec, otoa));
            }
            final List<ObjectAssociation> associations = spec.getAssociations(Contributed.EXCLUDED, ObjectAssociation.Filters.COLLECTIONS);
            for (final ObjectAssociation collection : associations) {
                final OneToManyAssociation otma = (OneToManyAssociation) collection;
                if (exclude(otma)) {
                    continue;
                }
                rows.add(new DomainMemberDefault(spec, otma));
            }
            final List<ObjectAction> actions = spec.getObjectActions(Contributed.INCLUDED);
            for (final ObjectAction action : actions) {
                if (exclude(action)) {
                    continue;
                }
                rows.add(new DomainMemberDefault(spec, action));
            }
        }

        Collections.sort(rows);

        return rows;
    }



    protected boolean exclude(final OneToOneAssociation property) {
        return false;
    }

    protected boolean exclude(final OneToManyAssociation collection) {
        return false;
    }

    protected boolean exclude(final ObjectAction action) {
        return false;
    }

    protected boolean exclude(final ObjectSpecification spec) {
        return isBuiltIn(spec) || spec.isAbstract() || spec.isMixin();
    }

    protected boolean isBuiltIn(final ObjectSpecification spec) {
        final String className = spec.getFullIdentifier();
        return className.startsWith("java") || className.startsWith("org.joda");
    }



    // //////////////////////////////////////

    @Override
    public Sort sortOf(final Class<?> domainType) {
        return sortOf(domainType, Mode.STRICT);
    }


    @Override
    public Sort sortOf(
            final Class<?> domainType, final Mode mode) {
        if(domainType == null) {
            return null;
        }
        final ObjectSpecification objectSpec = specificationLookup.loadSpecification(domainType);
        if(objectSpec.isService()) {
            return Sort.DOMAIN_SERVICE;
        }
        if(objectSpec.isViewModel()) {
            return Sort.VIEW_MODEL;
        }
        if(objectSpec.isValue()) {
            return Sort.VALUE;
        }
        if(objectSpec.isMixin()) {
            return Sort.VALUE;
        }
        if(objectSpec.isParentedOrFreeCollection()) {
            return Sort.COLLECTION;
        }
        final Class<?> correspondingClass = objectSpec.getCorrespondingClass();
        if(Persistable.class.isAssignableFrom(correspondingClass)) {
            return Sort.JDO_ENTITY;
        }
        if(mode == Mode.RELAXED) {
            return Sort.UNKNOWN;
        }
        throw new IllegalArgumentException(String.format(
                "Unable to determine what sort of domain object is '%s'", objectSpec.getFullIdentifier()));
    }

    @Override
    public Sort sortOf(final Bookmark bookmark) {
        return sortOf(bookmark, Mode.STRICT);
    }

    @Override
    public Sort sortOf(final Bookmark bookmark, final Mode mode) {
        if(bookmark == null) {
            return null;
        }

        final Class<?> domainType;
        switch (mode) {
            case RELAXED:
                try {
                    domainType = this.fromObjectType(bookmark.getObjectType());
                } catch (Exception e) {
                    return Sort.UNKNOWN;
                }
                break;

            case STRICT:
                // fall through to...
            default:
                domainType = this.fromObjectType(bookmark.getObjectType());
                break;
        }
        return sortOf(domainType, mode);
    }

    @Override
    public CommandDtoProcessor commandDtoProcessorFor(final String memberIdentifier) {
        final ApplicationFeatureId featureId = ApplicationFeatureId
                .newFeature(ApplicationFeatureType.MEMBER, memberIdentifier);

        final ObjectSpecId objectSpecId = featureId.getObjectSpecId();
        if(objectSpecId == null) {
            return null;
        }

        final ObjectSpecification spec = specificationLookup.lookupBySpecId(objectSpecId);
        if(spec == null) {
            return null;
        }
        final ObjectMember objectMemberIfAny = spec.getMember(featureId.getMemberName());
        if (objectMemberIfAny == null) {
            return null;
        }
        final CommandFacet commandFacet = objectMemberIfAny.getFacet(CommandFacet.class);
        if(commandFacet == null) {
            return null;
        }
        return commandFacet.getProcessor();
    }

    @Override
    public AppManifest2 getAppManifest2() {
        AppManifest appManifest = getAppManifest();
        return appManifest instanceof AppManifest2 ? (AppManifest2) appManifest : null;
    }

    @Override
    public AppManifest getAppManifest() {
        return appManifestProvider.getAppManifest();
    }

    @javax.inject.Inject
    SpecificationLoader specificationLookup;

    @javax.inject.Inject
    GridService gridService;

    @javax.inject.Inject
    AppManifestProvider appManifestProvider;

    @Override
    public MetamodelDto exportMetaModel(final Flags flags) {
        MetamodelDto metamodelDto = new MetamodelDto();

        for (final ObjectSpecification specification : specificationLookup.allSpecifications()) {
            ObjectSpecDto specDto = asDto(specification, flags);
            metamodelDto.getObjectSpecification().add(specDto);
        }

        sortSpecs(metamodelDto.getObjectSpecification());

        return metamodelDto;
    }

    private ObjectSpecDto asDto(
            final ObjectSpecification specification,
            final Flags flags) {

        final ObjectSpecDto specDto = new ObjectSpecDto();
        specDto.setId(specification.getFullIdentifier());
        specDto.setFacets(new FacetHolderDto.Facets());

        addFacets(specification, specDto.getFacets(), flags);

        return specDto;
    }

    private void addFacets(
            final FacetHolder facetHolder,
            final FacetHolderDto.Facets facets,
            final Flags flags) {
        final Class<? extends Facet>[] facetTypes = facetHolder.getFacetTypes();
        for (final Class<? extends Facet> facetType : facetTypes) {
            final Facet facet = facetHolder.getFacet(facetType);
            if(!facet.isNoop() || !flags.isIgnoreNoop()) {
                facets.getFacet().add(asDto(facet));
            }
        }
        sortFacets(facets.getFacet());
    }

    private FacetDto asDto(
            final Facet facet) {
        final FacetDto facetDto = new FacetDto();
        facetDto.setId(facet.facetType().getCanonicalName());
        facetDto.setFqcn(facet.getClass().getCanonicalName());

        addFacetAttributes(facet, facetDto);

        return facetDto;
    }

    private void addFacetAttributes(
            final Facet facet,
            final FacetDto facetDto) {

        Map<String, Object> attributeMap = Maps.newTreeMap();
        facet.appendAttributesTo(attributeMap);

        for (final String key : attributeMap.keySet()) {
            Object attributeObj = attributeMap.get(key);
            if(attributeObj == null) {
                continue;
            }

            String str = asStr(attributeObj);
            addAttribute(facetDto,key, str);
        }

        sortFacetAttributes(facetDto.getAttr());
    }

    private String asStr(final Object attributeObj) {
        String str;
        if(attributeObj instanceof Method) {
            str = asStr((Method) attributeObj);
        } else if(attributeObj instanceof Enum) {
            str = asStr((Enum) attributeObj);
        } else if(attributeObj instanceof Class) {
            str = asStr((Class) attributeObj);
        } else if(attributeObj instanceof Specification) {
            str = asStr((Specification) attributeObj);
        } else if(attributeObj instanceof Facet) {
            str = asStr((Facet) attributeObj);
        } else if(attributeObj instanceof JdoNamedQuery) {
            str = asStr((JdoNamedQuery) attributeObj);
        } else if(attributeObj instanceof Pattern) {
            str = asStr((Pattern) attributeObj);
        } else if(attributeObj instanceof CommandDtoProcessor) {
            str = asStr((CommandDtoProcessor) attributeObj);
        } else if(attributeObj instanceof ObjectSpecification) {
            str = asStr((ObjectSpecification) attributeObj);
        } else if(attributeObj instanceof ObjectMember) {
            str = asStr((ObjectMember) attributeObj);
        } else if(attributeObj instanceof List) {
            str = asStr((List<?>) attributeObj);
        } else if(attributeObj instanceof Object[]) {
            str = asStr((Object[]) attributeObj);
        } else  {
            str = "" + attributeObj;
        }
        return str;
    }

    private String asStr(final Specification attributeObj) {
        return attributeObj.getClass().getName();
    }

    private String asStr(final ObjectSpecification attributeObj) {
        return attributeObj.getFullIdentifier();
    }

    private String asStr(final JdoNamedQuery attributeObj) {
        return attributeObj.getName();
    }

    private String asStr(final CommandDtoProcessor attributeObj) {
        return attributeObj.getClass().getName();
    }

    private String asStr(final Pattern attributeObj) {
        return attributeObj.pattern();
    }

    private String asStr(final Facet attributeObj) {
        return attributeObj.getClass().getName();
    }

    private String asStr(final ObjectMember attributeObj) {
        return attributeObj.getId();
    }

    private String asStr(final Class attributeObj) {
        return attributeObj.getCanonicalName();
    }

    private String asStr(final Enum attributeObj) {
        return attributeObj.name();
    }

    private String asStr(final Method attributeObj) {
        return attributeObj.toGenericString();
    }

    private String asStr(final Object[] list) {
        if(list.length == 0) {
            return null; // skip
        }
        List<String> strings = Lists.newArrayList();
        for (final Object o : list) {
            String s = asStr(o);
            strings.add(s);
        }
        return Joiner.on(",").join(strings);
    }

    private String asStr(final List<?> list) {
        if(list.isEmpty()) {
            return null; // skip
        }
        List<String> strings = Lists.newArrayList();
        for (final Object o : list) {
            String s = asStr(o);
            strings.add(s);
        }
        return Joiner.on(",").join(strings);
    }

    private void addAttribute(
            final FacetDto facetDto, final String key, final String str) {
        if(str == null) {
            return;
        }
        FacetAttributeDto attributeDto = new FacetAttributeDto();
        attributeDto.setName(key);
        attributeDto.setValue(str);
        facetDto.getAttr().add(attributeDto);
    }

    private void sortFacetAttributes(final List<FacetAttributeDto> attributes) {
        Collections.sort(attributes, new Comparator<FacetAttributeDto>() {
            @Override
            public int compare(final FacetAttributeDto o1, final FacetAttributeDto o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    private static void sortSpecs(final List<ObjectSpecDto> specifications) {
        Collections.sort(specifications, new Comparator<ObjectSpecDto>() {
            @Override
            public int compare(final ObjectSpecDto o1, final ObjectSpecDto o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }

    private void sortFacets(final List<FacetDto> facets) {
        Collections.sort(facets, new Comparator<FacetDto>() {
            @Override public int compare(final FacetDto o1, final FacetDto o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }


}
