/**
 * ﻿Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */

package org.n52.movingcode.runtime;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.movingcode.runtime.processorconfig.ProcessorsDocument;

/**
 * Configuration Object for Code Processors. This object provides basic information about the
 * available processors, their capabilities, property attributes and provided runtime environments.
 * 
 * @author Matthias Mueller, TU Dresden
 * 
 */
public class ProcessorConfig implements Serializable {

	private static final long serialVersionUID = 3112343084611936675L;
	private static transient ProcessorConfig instance;
	private static transient ProcessorsDocument procConfigXMLBeans;
	private static final ProcessorsDocument blankConfig = ProcessorsDocument.Factory.newInstance();

	protected final PropertyChangeSupport propertyChangeSupport;
	public static final String PROCESSORCONFIG_UPDATE_EVENT_NAME = "ProcessorConfigUpdate";

	private static transient Logger logger = Logger.getLogger(ProcessorConfig.class);

	/**
	 * Private constructor to create a blank ProcessorConfig
	 */
	private ProcessorConfig() {
		procConfigXMLBeans = blankConfig;
		propertyChangeSupport = new PropertyChangeSupport(this);
	}

	/**
	 * TODO: Delete!
	 */
	public static final ProcessorConfig EMPTY_RUNTIME_CONFIG = new ProcessorConfig();

	/**
	 * Sets a new ProcessorConfig from {@link File} processorConfigXML.
	 * This will overwrite the old configuration.
	 * 
	 * @param processorConfigXML
	 * @return boolean
	 */
	public boolean setConfig(File processorConfigXML) {
		synchronized (procConfigXMLBeans) {
			try {
				procConfigXMLBeans = ProcessorsDocument.Factory.parse(processorConfigXML);
				return true;
			}
			catch (XmlException e) {
				logger.error("Reading new processor configuration failed! " + e.getMessage());
				return false;
			}
			catch (IOException e) {
				logger.error("Reading new processor configuration failed! " + e.getMessage());
				return false;
			}
		}
	}

	public boolean setConfig(InputStream processorConfigXMLStream) {
		synchronized (procConfigXMLBeans) {
			try {
				procConfigXMLBeans = ProcessorsDocument.Factory.parse(processorConfigXMLStream);
				return true;
			}
			catch (XmlException e) {
				logger.error("Reading new processor configuration failed! " + e.getMessage());
				return false;
			}
			catch (IOException e) {
				logger.error("Reading new processor configuration failed! " + e.getMessage());
				return false;
			}
		}

	}

	/**
	 * Adds {@link PropertyChangeListener} to the processorConfig.  
	 * 
	 * @param propertyName
	 * @param listener
	 */
	public void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * Removes a {@link PropertyChangeListener} from the processorConfig.
	 * 
	 * @param propertyName
	 * @param listener
	 */
	public void removePropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}

	/**
	 * For Testing purposes only
	 */
	public void notifyListeners() {
		propertyChangeSupport.firePropertyChange(PROCESSORCONFIG_UPDATE_EVENT_NAME, null, null);
	}

	/**
	 * Static method to access the ProcessorConfig.
	 * @return ProcessorConfig object
	 */
	public static synchronized ProcessorConfig getInstance() {

		if (instance == null) {
			instance = new ProcessorConfig();
		}

		return instance;
	}

	/**
	 * Get the currently used {@link ProcessorsDocument}
	 * 
	 * @return {@link ProcessorsDocument}
	 */
	public ProcessorsDocument getConfig() {
		return procConfigXMLBeans;
	}

	/**
	 * Write method for Object serialization.
	 * 
	 * @param oos {@link java.io.ObjectOutputStream}
	 * @throws IOException
	 */
	private synchronized void writeObject(java.io.ObjectOutputStream oos) throws IOException {
		oos.writeObject(procConfigXMLBeans.xmlText());
	}

	/**
	 * Read method for Object serialization.
	 * 
	 * @param ois {@link java.io.ObjectInputStream}
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private synchronized void readObject(java.io.ObjectInputStream ois) throws IOException, ClassNotFoundException {
		try {
			String processorConfigXMLBeansAsXml = (String) ois.readObject();
			XmlObject configXmlObject = XmlObject.Factory.parse(processorConfigXMLBeansAsXml);
			ProcessorsDocument configurationDocument = ProcessorsDocument.Factory.newInstance();
			configurationDocument.addNewProcessors().set(configXmlObject);
			this.setConfig(new ByteArrayInputStream(configurationDocument.xmlText().getBytes()));
		}
		catch (XmlException e) {
			logger.error(e.getMessage());
			throw new IOException(e.getMessage());
		}
	}

}