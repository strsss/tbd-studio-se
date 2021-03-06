// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.hadoopcluster.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.talend.commons.ui.runtime.image.EImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.commons.ui.swt.formtools.Form;
import org.talend.commons.ui.swt.formtools.LabelledCombo;
import org.talend.core.hadoop.version.EHadoopDistributions;
import org.talend.core.hadoop.version.EHadoopVersion4Drivers;
import org.talend.core.hadoop.version.custom.HadoopCustomVersionDefineDialog;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.repository.hadoopcluster.i18n.Messages;
import org.talend.repository.hadoopcluster.ui.common.AbstractHadoopForm;
import org.talend.repository.hadoopcluster.ui.common.IHadoopClusterInfoForm;
import org.talend.repository.hadoopcluster.util.HCVersionUtil;
import org.talend.repository.model.hadoopcluster.HadoopClusterConnection;

/**
 * DOC ycbai class global comment. Detailled comment
 */
public class HadoopClusterForm extends AbstractHadoopForm<HadoopClusterConnection> {

    private static final int VISIBLE_DISTRIBUTION_COUNT = 5;

    private static final int VISIBLE_VERSION_COUNT = 6;

    private String[] existingNamesArray;

    private LabelledCombo distributionCombo;

    private LabelledCombo versionCombo;

    private Button customButton;

    private Button useYarnButton;

    private IHadoopClusterInfoForm hcInfoForm;

    private final boolean creation;

    public HadoopClusterForm(Composite parent, ConnectionItem connectionItem, String[] existingNames, boolean creation) {
        super(parent, SWT.NONE, existingNames);
        this.connectionItem = connectionItem;
        this.creation = creation;
        existingNamesArray = existingNames;
        setConnectionItem(connectionItem);
        setupForm();
        init();
        setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout layout = (GridLayout) getLayout();
        layout.marginHeight = 0;
        setLayout(layout);
    }

    public void init() {
        EHadoopDistributions distribution = EHadoopDistributions.getDistributionByName(getConnection().getDistribution(), false);
        if (distribution != null) {
            distributionCombo.setText(distribution.getDisplayName());
        } else {
            distributionCombo.select(0);
        }
        updateVersionPart();
        useYarnButton.setSelection(getConnection().isUseYarn());
        updateStatus(IStatus.OK, EMPTY_STRING);
    }

    @Override
    protected void adaptFormToReadOnly() {
        readOnly = isReadOnly();
        distributionCombo.setReadOnly(readOnly);
        versionCombo.setReadOnly(readOnly);
    }

    @Override
    protected void addFields() {
        addVersionFields();
    }

    private void addVersionFields() {
        Group distributionGroup = Form.createGroup(this, 4, Messages.getString("HadoopClusterForm.versionSettings")); //$NON-NLS-1$
        distributionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        distributionCombo = new LabelledCombo(distributionGroup,
                Messages.getString("HadoopClusterForm.distribution"), //$NON-NLS-1$
                Messages.getString("HadoopClusterForm.distribution.tooltip"), EHadoopDistributions.getAllDistributionDisplayNames() //$NON-NLS-1$
                        .toArray(new String[0]), 1, true);
        distributionCombo.setVisibleItemCount(VISIBLE_DISTRIBUTION_COUNT);
        versionCombo = new LabelledCombo(distributionGroup, Messages.getString("HadoopClusterForm.version"), //$NON-NLS-1$
                Messages.getString("HadoopClusterForm.version.tooltip"), new String[0], 1, true); //$NON-NLS-1$
        versionCombo.setVisibleItemCount(VISIBLE_VERSION_COUNT);
        customButton = new Button(distributionGroup, SWT.NULL);
        customButton.setImage(ImageProvider.getImage(EImage.THREE_DOTS_ICON));
        customButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
        useYarnButton = new Button(distributionGroup, SWT.CHECK);
        useYarnButton.setText(Messages.getString("HadoopClusterForm.useYarn")); //$NON-NLS-1$
        useYarnButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
    }

    private List<String> getDistributionVersions(EHadoopDistributions distribution) {
        List<String> result = new ArrayList<String>();
        List<EHadoopVersion4Drivers> v4dList = EHadoopVersion4Drivers.indexOfByDistribution(distribution);
        for (EHadoopVersion4Drivers v4d : v4dList) {
            result.add(v4d.getVersionDisplay());
        }
        return result;
    }

    @Override
    protected void addFieldsListeners() {
        distributionCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                String newDistributionDisplayName = distributionCombo.getText();
                EHadoopDistributions newDistribution = EHadoopDistributions
                        .getDistributionByDisplayName(newDistributionDisplayName);
                String newDistributionName = newDistribution.getName();
                String originalDistributionName = getConnection().getDistribution();
                getConnection().setDistribution(newDistribution.getName());
                getConnection().setUseCustomVersion(newDistribution == EHadoopDistributions.CUSTOM);
                boolean distrChanged = !StringUtils.equals(newDistributionName, originalDistributionName);
                if (distrChanged) {
                    getConnection().setDfVersion(null);
                }
                updateVersionPart();
                updateYarnContent();
                switchToInfoForm();
                checkFieldsValue();

            }
        });

        versionCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                String newVersionDisplayName = versionCombo.getText();
                if (StringUtils.isEmpty(newVersionDisplayName)) {
                    return;
                }
                EHadoopVersion4Drivers newVersion4Drivers = EHadoopVersion4Drivers.indexOfByVersionDisplay(newVersionDisplayName);
                getConnection().setDfVersion(newVersion4Drivers.getVersionValue());
                if (newVersion4Drivers.isSupportYARN() && !newVersion4Drivers.isSupportMR1()) {
                    getConnection().setUseYarn(true);
                } else {
                    getConnection().setUseYarn(false);
                }
                updateYarnContent();
                switchToInfoForm();
                checkFieldsValue();
            }
        });

        customButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                HadoopCustomVersionDefineDialog customVersionDialog = new HadoopCustomVersionDefineDialog(getShell(),
                        HCVersionUtil.getCustomVersionMap(getConnection()));
                if (customVersionDialog.open() == Window.OK) {
                    HCVersionUtil.injectCustomVersionMap(getConnection(), customVersionDialog.getLibMap());
                }
            }
        });

        useYarnButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                getConnection().setUseYarn(useYarnButton.getSelection());
                switchToInfoForm();
            }
        });

    }

    private void switchToInfoForm() {
        if (hcInfoForm != null) {
            hcInfoForm.dispose();
        }
        if (HCVersionUtil.isHDI(getConnection())) {
            hcInfoForm = new HDIInfoForm(this, connectionItem, existingNamesArray, creation);
        } else {
            hcInfoForm = new StandardHCInfoForm(this, connectionItem, existingNamesArray, creation);
        }
        hcInfoForm.setReadOnly(readOnly);
        hcInfoForm.setListener(listener);
        hcInfoForm.updateForm();
        hcInfoForm.checkFieldsValue();
        this.layout();
    }

    private void updateVersionPart() {
        EHadoopDistributions distribution = EHadoopDistributions.getDistributionByDisplayName(distributionCombo.getText());
        List<String> items = getDistributionVersions(distribution);
        String[] versions = new String[items.size()];
        items.toArray(versions);
        versionCombo.getCombo().setItems(versions);
        EHadoopVersion4Drivers version4Drivers = EHadoopVersion4Drivers.indexOfByVersion(getConnection().getDfVersion());
        if (version4Drivers != null) {
            versionCombo.setText(version4Drivers.getVersionDisplay());
        } else {
            versionCombo.getCombo().select(0);
        }
        if (distribution == EHadoopDistributions.CUSTOM) {
            versionCombo.setHideWidgets(true);
            hideControl(useYarnButton, false);
            hideControl(customButton, false);
        } else {
            versionCombo.setHideWidgets(false);
            hideControl(useYarnButton, true);
            hideControl(customButton, true);
        }
    }

    private void updateYarnContent() {
        useYarnButton.setSelection(getConnection().isUseYarn());
    }

    @Override
    public boolean checkFieldsValue() {
        if (distributionCombo.getSelectionIndex() == -1) {
            updateStatus(IStatus.ERROR, Messages.getString("HadoopClusterForm.check.distribution")); //$NON-NLS-1$
            return false;
        }

        if (!getConnection().isUseCustomVersion()) {
            if (versionCombo.getSelectionIndex() == -1) {
                updateStatus(IStatus.ERROR, Messages.getString("HadoopClusterForm.check.version")); //$NON-NLS-1$
                return false;
            }
        }

        if (hcInfoForm != null) {
            return hcInfoForm.checkFieldsValue();
        }

        updateStatus(IStatus.OK, null);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Control#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (isReadOnly() != readOnly) {
            adaptFormToReadOnly();
        }
        if (visible) {
            updateStatus(getStatusLevel(), getStatus());
        }
    }

}
