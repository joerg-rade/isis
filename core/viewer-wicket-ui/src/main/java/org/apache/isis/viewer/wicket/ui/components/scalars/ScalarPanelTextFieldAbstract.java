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

package org.apache.isis.viewer.wicket.ui.components.scalars;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.mgr.AdapterManager;
import org.apache.isis.core.metamodel.facets.SingleIntValueFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.maxlen.MaxLengthFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.typicallen.TypicalLengthFacet;
import org.apache.isis.viewer.wicket.model.isis.WicketViewerSettings;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.ui.components.widgets.bootstrap.FormGroup;
import org.apache.isis.viewer.wicket.ui.panels.PanelAbstract;

/**
 * Adapter for {@link PanelAbstract panel}s that use a {@link ScalarModel} as
 * their backing model.
 *
 * <p>
 * Supports the concept of being {@link Rendering#COMPACT} (eg within a table) or
 * {@link Rendering#REGULAR regular} (eg within a form).
 * </p>
 *
 * <p>
 * This implementation is for panels that use a textfield/text area.
 * </p>
 */
public abstract class ScalarPanelTextFieldAbstract<T extends Serializable> extends ScalarPanelAbstract2 implements TextFieldValueModel.ScalarModelProvider {

    private static final long serialVersionUID = 1L;

    protected final Class<T> cls;

    protected static class ReplaceDisabledTagWithReadonlyTagBehaviour extends Behavior {
        @Override public void onComponentTag(final Component component, final ComponentTag tag) {
            super.onComponentTag(component, tag);
            if(component.isEnabled()) {
                return;
            }
            tag.remove("disabled");
            tag.put("readonly","readonly");
        }
    }

    private AbstractTextComponent<T> textField;


    public ScalarPanelTextFieldAbstract(final String id, final ScalarModel scalarModel, final Class<T> cls) {
        super(id, scalarModel);
        this.cls = cls;
    }

    // ///////////////////////////////////////////////////////////////////


    protected AbstractTextComponent<T> getTextField() {
        return textField;
    }

    /**
     * Optional hook for subclasses to override
     */
    protected AbstractTextComponent<T> createTextFieldForRegular(final String id) {
        return createTextField(id);
    }

    protected TextField<T> createTextField(final String id) {
        return new TextField<>(id, newTextFieldValueModel(), cls);
    }

    TextFieldValueModel<T> newTextFieldValueModel() {
        return new TextFieldValueModel<>(this);
    }

    // ///////////////////////////////////////////////////////////////////


    @Override
    protected MarkupContainer createComponentForRegular() {

        // even though only one of textField and scalarValueEditInlineContainer will ever be visible,
        // am instantiating both to avoid NPEs
        // elsewhere can use Component#isVisibilityAllowed or ScalarModel.getEditStyle() to check whichis visible.

        textField = createTextFieldForRegular(ID_SCALAR_VALUE);
        textField.setOutputMarkupId(true);

        addStandardSemantics();



        //
        // read-only/dialog edit
        //

        final MarkupContainer scalarIfRegularFormGroup = createScalarIfRegularFormGroup();

        final String describedAs = getModel().getDescribedAs();
        if(describedAs != null) {
            scalarIfRegularFormGroup.add(new AttributeModifier("title", Model.of(describedAs)));
        }

        return scalarIfRegularFormGroup;
    }

    protected Component getScalarValueComponent() {
        return textField;
    }

    private void addReplaceDisabledTagWithReadonlyTagBehaviourIfRequired(final Component component) {
        if(!getSettings().isReplaceDisabledTagWithReadonlyTag()) {
            return;
        }
        if (component == null) {
            return;
        }
        if (!component.getBehaviors(ReplaceDisabledTagWithReadonlyTagBehaviour.class).isEmpty()) {
            return;
        }
        component.add(new ReplaceDisabledTagWithReadonlyTagBehaviour());
    }

    protected MarkupContainer createScalarIfRegularFormGroup() {
        Fragment textFieldFragment = createTextFieldFragment("scalarValueContainer");
        final String name = getModel().getName();
        textField.setLabel(Model.of(name));

        final FormGroup formGroup = new FormGroup(ID_SCALAR_IF_REGULAR, this.textField);
        textFieldFragment.add(this.textField);
        formGroup.add(textFieldFragment);

        final String labelCaption = getRendering().getLabelCaption(textField);
        final Label scalarName = createScalarName(ID_SCALAR_NAME, labelCaption);

        formGroup.add(scalarName);

        return formGroup;
    }

    private Fragment createTextFieldFragment(String id) {
        return new Fragment(id, createTextFieldFragmentId(), this);
    }

    protected String createTextFieldFragmentId() {
        return "text";
    }

    /**
     * Overrides default to use a fragment, allowing the inner rendering to switch between a simple span
     * or a textarea
     */
    protected Component createInlinePromptComponent(
            final String id,
            final IModel<String> inlinePromptModel) {
        final Fragment fragment = new Fragment(id, "textInlinePrompt", this) {
            @Override protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("tabindex","-1");
            }
        };
        final Label label = new Label("scalarValue", inlinePromptModel);
        fragment.add(label);
        return fragment;
    }

    protected void addStandardSemantics() {
        textField.setRequired(getModel().isRequired());
        setTextFieldSizeAndMaxLengthIfSpecified();

        addValidatorForIsisValidation();
    }

    private void addValidatorForIsisValidation() {
        final ScalarModel scalarModel = getModel();

        textField.add(new IValidator<T>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void validate(final IValidatable<T> validatable) {
                final T proposedValue = validatable.getValue();
                final ObjectAdapter proposedAdapter = getPersistenceSession().adapterFor(proposedValue);
                final String reasonIfAny = scalarModel.validate(proposedAdapter);
                if (reasonIfAny != null) {
                    final ValidationError error = new ValidationError();
                    error.setMessage(reasonIfAny);
                    validatable.error(error);
                }
            }
        });
    }

    private void setTextFieldSizeAndMaxLengthIfSpecified() {

        final Integer maxLength = getValueOf(getModel(), MaxLengthFacet.class);
        Integer typicalLength = getValueOf(getModel(), TypicalLengthFacet.class);

        // doesn't make sense for typical length to be > maxLength
        if(typicalLength != null && maxLength != null && typicalLength > maxLength) {
            typicalLength = maxLength;
        }

        if (typicalLength != null) {
            textField.add(new AttributeModifier("size", Model.of("" + typicalLength)));
        }

        if(maxLength != null) {
            textField.add(new AttributeModifier("maxlength", Model.of("" + maxLength)));
        }
    }


    // //////////////////////////////////////


    /**
     * Mandatory hook method to build the component to render the model when in
     * {@link Rendering#COMPACT compact} format.
     * 
     * <p>
     * This default implementation uses a {@link Label}, however it may be overridden if required.
     */
    @Override
    protected Component createComponentForCompact() {
        Fragment compactFragment = getCompactFragment(CompactType.SPAN);
        final Label labelIfCompact = new Label(ID_SCALAR_IF_COMPACT, getModel().getObjectAsString());
        compactFragment.add(labelIfCompact);
        return labelIfCompact;
    }

    public enum CompactType {
        INPUT_CHECKBOX,
        SPAN
    }


    Fragment getCompactFragment(CompactType type) {
        Fragment compactFragment;
        switch (type) {
        case INPUT_CHECKBOX:
            compactFragment = new Fragment(ID_SCALAR_IF_COMPACT, "compactAsInputCheckbox", ScalarPanelTextFieldAbstract.this);
            break;
        case SPAN:
        default:
            compactFragment = new Fragment(ID_SCALAR_IF_COMPACT, "compactAsSpan", ScalarPanelTextFieldAbstract.this);
            break;
        }
        return compactFragment;
    }


    // //////////////////////////////////////

    @Override
    protected InlinePromptConfig getInlinePromptConfig() {
        return InlinePromptConfig.supportedAndHide(textField);
    }

    @Override
    protected IModel<String> obtainInlinePromptModel() {
        IModel<T> model = textField.getModel();
        // must be "live", for ajax updates.
        return (IModel<String>) model;
    }


    // //////////////////////////////////////


    @Override
    protected void onInitializeWhenViewMode() {
        super.onInitializeWhenViewMode();

        textField.setEnabled(false);
        addReplaceDisabledTagWithReadonlyTagBehaviourIfRequired(textField);

        setTitleAttribute("");
    }

    @Override
    protected void onInitializeWhenDisabled(final String disableReason) {
        super.onInitializeWhenDisabled(disableReason);

        textField.setEnabled(false);
        addReplaceDisabledTagWithReadonlyTagBehaviourIfRequired(textField);

        inlinePromptLink.setEnabled(false);

        setTitleAttribute(disableReason);
    }

    @Override
    protected void onInitializeWhenEnabled() {
        super.onInitializeWhenEnabled();
        textField.setEnabled(true);
        inlinePromptLink.setEnabled(true);
        setTitleAttribute("");
    }

    private void setTitleAttribute(final String titleAttribute) {
        AttributeModifier title = new AttributeModifier("title", Model.of(titleAttribute));
        textField.add(title);
        inlinePromptLink.add(title);
    }



    // //////////////////////////////////////

    private static Integer getValueOf(ScalarModel model, Class<? extends SingleIntValueFacet> facetType) {
        final SingleIntValueFacet facet = model.getFacet(facetType);
        return facet != null ? facet.value() : null;
    }

    @com.google.inject.Inject
    WicketViewerSettings settings;
    protected WicketViewerSettings getSettings() {
        return settings;
    }

    @Override
    public AdapterManager getAdapterManager() {
        return getPersistenceSession();
    }

}

