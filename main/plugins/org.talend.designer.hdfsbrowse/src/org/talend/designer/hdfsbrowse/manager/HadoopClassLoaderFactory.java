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
package org.talend.designer.hdfsbrowse.manager;

import java.util.Set;

import org.talend.core.classloader.ClassLoaderFactory;
import org.talend.core.hadoop.version.custom.ECustomVersionGroup;
import org.talend.core.hadoop.version.custom.ECustomVersionType;
import org.talend.designer.hdfsbrowse.model.HDFSConnectionBean;

/**
 * DOC ycbai class global comment. Detailled comment
 */
public class HadoopClassLoaderFactory {

    private static final String STANDALONE = "STANDALONE"; //$NON-NLS-1$ 

    private static final String EMBEDDED = "EMBEDDED"; //$NON-NLS-1$ 

    public static ClassLoader getClassLoader(HDFSConnectionBean connectionBean) {
        ClassLoader loader = null;
        if (connectionBean == null) {
            return loader;
        }

        if (connectionBean.isUseCustomVersion()) {
            return getCustomClassLoader(connectionBean);
        }

        String distribution = connectionBean.getDistribution();
        String version = connectionBean.getDfVersion();
        boolean enableKerberos = connectionBean.isEnableKerberos();
        if (distribution == null || version == null) {
            return loader;
        }

        return getClassLoader(distribution, version, enableKerberos, true);
    }

    public static ClassLoader getClassLoader(String distribution, String version, boolean enableKerberos,
            boolean showDownloadIfNotExist) {
        String index = "HDFS" + ":" + distribution + ":" + version; //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
        if (enableKerberos) {
            index += "?USE_KRB"; //$NON-NLS-1$
        }
        ClassLoader loader = ClassLoaderFactory.getClassLoader(index, showDownloadIfNotExist);
        if (loader == null) {
            loader = HadoopClassLoaderFactory.class.getClassLoader();
        }

        return loader;
    }

    public static ClassLoader getClassLoader(ECustomVersionType type, String distribution, String version,
            boolean enableKerberos, boolean showDownloadIfNotExist) {
        if (type == null) {
            return HadoopClassLoaderFactory.class.getClassLoader();
        }
        String baseIndex = type.getName() + ":" + distribution + ":" + version;
        String index = baseIndex;
        String hive_embeded = null;
        switch (type) {
        case HDFS:
            if (enableKerberos) {
                index += "?USE_KRB"; //$NON-NLS-1$
            }
            break;
        case HIVE:
            // take STANDALONE by default , if there is no STANDALONE for some version take EMBEDDED
            index += ":" + STANDALONE;
            hive_embeded = ":" + EMBEDDED;
            break;
        }

        ClassLoader loader = ClassLoaderFactory.getClassLoader(index, showDownloadIfNotExist);
        if (loader == null && hive_embeded != null) {
            index = baseIndex + hive_embeded;
            loader = ClassLoaderFactory.getClassLoader(index, showDownloadIfNotExist);
        }
        if (loader == null) {
            loader = HadoopClassLoaderFactory.class.getClassLoader();
        }

        return loader;
    }

    public static ClassLoader getCustomClassLoader(HDFSConnectionBean connectionBean) {
        String hcId = connectionBean.getRelativeHadoopClusterId();
        String index = "HadoopCustomVersion:" + hcId; //$NON-NLS-1$ 
        Object jars = connectionBean.getAdditionalProperties().get(ECustomVersionGroup.COMMON.getName());
        if (jars instanceof Set) {
            return ClassLoaderFactory.getCustomClassLoader(index, (Set<String>) jars);
        }

        return ClassLoaderFactory.getCustomClassLoader(index, String.valueOf(jars));
    }

}
