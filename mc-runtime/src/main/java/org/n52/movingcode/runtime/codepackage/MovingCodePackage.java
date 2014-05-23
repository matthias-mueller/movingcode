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

package org.n52.movingcode.runtime.codepackage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.joda.time.DateTime;

import de.tudresden.gis.geoprocessing.movingcode.schema.FunctionalDescriptionsListType;
import de.tudresden.gis.geoprocessing.movingcode.schema.PackageDescriptionDocument;
import static org.n52.movingcode.runtime.codepackage.Constants.*;

/**
 * This class provides methods for handling MovingCode Packages. This includes methods for validation,
 * copying, unzipping and for accessing the package's description.
 * 
 * MovingCode Packages a the basic entities for shipping code from platform to platform.
 * 
 * @author Matthias Mueller, TU Dresden
 * 
 */
public class MovingCodePackage {

	private static final Logger logger = Logger.getLogger(MovingCodePackage.class);

	// the physical instance of this package
	private final ICodePackage archive;

	// package description XML document
	private PackageDescriptionDocument packageDescription = null;

	// identifier of the provided functionality (e.g. WPS process identifier)
	private final String functionIdentifier;

	// Package time stamp, i.e. date of creation or last modification
	private final DateTime timeStamp;

	private final List<FunctionalType> supportedFuncTypes;

	/**
	 * Constructor for zipFiles. Creates a MovingCodePackage from a zipFile on disk.
	 * 
	 * @param {@link File} zipFile - a zip file with a valid package structure
	 */
	public MovingCodePackage(final File zipFile) {

		this.archive = new ZippedPackage(zipFile);
		this.packageDescription = this.archive.getDescription();

		if (this.packageDescription != null
				&& this.packageDescription.getPackageDescription().getContractedFunctionality().isSetWpsProcessDescription()) {
			this.functionIdentifier = this.packageDescription.getPackageDescription().getContractedFunctionality().getWpsProcessDescription().getIdentifier().getStringValue();
			this.supportedFuncTypes = getFunctionalTypes(this.packageDescription);
		}
		else {
			this.functionIdentifier = null;
			this.supportedFuncTypes = null;
		}
		this.timeStamp = getTimestamp(zipFile);
	}
	
	
	/**
	 * Constructor for geoprocessing feed entries. Creates a MovingCodePackage from an atom feed entry.
	 * 
	 * @param {@link GeoprocessingFeedEntry} atomFeedEntry - an entry from a geoprocessing feed
	 * 
	 */
	/**
	 * Constructor for geoprocessing feed entries. Creates a MovingCodePackage from a remote URL.
	 * Also requires the intended packageID an
	 * 
	 * @param zipPackageURL
	 * @param packageID
	 * @param packageStamp
	 */
	public MovingCodePackage(final URL zipPackageURL, final PackageID packageID, final DateTime packageTimeStamp) {

		PackageDescriptionDocument packageDescription = null;
		ZippedPackage archive = null;
		
		archive = new ZippedPackage(zipPackageURL);
		packageDescription = archive.getDescription();
		
		this.packageDescription = packageDescription;
		// TODO: Information from the feed might lag during updates
		// how can deal with that?
		if (packageDescription != null
				&& packageDescription.getPackageDescription().getContractedFunctionality().isSetWpsProcessDescription()) {
			this.functionIdentifier = packageDescription.getPackageDescription().getContractedFunctionality().getWpsProcessDescription().getIdentifier().getStringValue();
			this.supportedFuncTypes = getFunctionalTypes(packageDescription);
		}
		else {
			this.functionIdentifier = null;
			this.supportedFuncTypes = null;
		}
		
		this.timeStamp = packageTimeStamp;
		this.archive = archive;

	}

	/**
	 * Constructor for directories. Creates a MovingCodePackage from a workspace directory on disk.
	 * 
	 * @param {@link File} workspace - the directory where the code and possibly some related data is stored.
	 * @param {@link PackageDescriptionDocument} packageDescription - the XML document that contains the
	 *        description of the provided logic
	 * @param {@link DateTime} lastModified - the date of latest modification. This value is optional. If NULL,
	 *        the lastModified date is obtained from the workspace's content.
	 */
	public MovingCodePackage(final File workspace,
			final PackageDescriptionDocument packageDescription,
			final DateTime timestamp) {

		this.packageDescription = packageDescription;

		if (packageDescription != null
				&& packageDescription.getPackageDescription().getContractedFunctionality().isSetWpsProcessDescription()) {
			this.functionIdentifier = packageDescription.getPackageDescription().getContractedFunctionality().getWpsProcessDescription().getIdentifier().getStringValue();
			this.supportedFuncTypes = getFunctionalTypes(packageDescription);
		}
		else {
			this.functionIdentifier = null;
			this.supportedFuncTypes = null;
		}

		// set timestamp
		if (timestamp != null) {
			this.timeStamp = timestamp;
		}
		else {
			this.timeStamp = getLastModified(workspace);
		}
		
		this.archive = new PlainPackage(workspace, packageDescription);

	}

	/**
	 * Dump workspace to a given directory. Used to create copies from a template for execution or further
	 * manipulation.
	 * 
	 * @param {@link File} targetDirectory - directory to store the unzipped content
	 * @return {@link String} dumpWorkspacePath - absolute path of the dumped workspace
	 */
	public String dumpWorkspace(File targetDirectory) {
		String wsRoot = this.packageDescription.getPackageDescription().getWorkspace().getWorkspaceRoot();
		this.archive.dumpPackage(wsRoot, targetDirectory);
		if (wsRoot.startsWith("./")) {
			wsRoot = wsRoot.substring(2);
		}

		if (wsRoot.startsWith(".\\")) {
			wsRoot = wsRoot.substring(2);
		}
		return targetDirectory + File.separator + wsRoot;
	}

	/**
	 * Writes a copy of the {@link MovingCodePackage} to a given directory. This is going to be a zipFile
	 * 
	 * @param targetFile
	 *        - destination path and file
	 * @return boolean - true if successful, false if not
	 */
	public boolean dumpPackage(File targetFile) {
		return this.archive.dumpPackage(targetFile);
	}

	/**
	 * writes a copy of the package (zipfile) to a given directory TODO: implement for URL sources
	 * 
	 * @param targetFile
	 * @return boolean
	 */
	public boolean dumpDescription(File targetFile) {
		try {
			this.packageDescription.save(targetFile);
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	/**
	 * Returns the PackageDescription
	 * 
	 * @return {@link PackageDescriptionDocument}
	 */
	public PackageDescriptionDocument getDescription() {
		return this.packageDescription;
	}

	/**
	 * Does this object contain valid content?
	 * 
	 * @return boolean - true if content is valid, false if not
	 */
	public boolean isValid() {

		// a valid MovingCodePackage MUST have an identifier
		if (this.functionIdentifier == null) {
			return false;
		}

		// TODO: Identifiers of IO data must be unique!

		// check if there exists a package description
		// and return the validation result
		if (this.packageDescription != null) {
			if ( !this.packageDescription.isNil()) {
				//information on validation errors
				if (!this.packageDescription.validate()) {
					List<XmlError> errors = new ArrayList<XmlError>();
					this.packageDescription.validate(new XmlOptions().setErrorListener(errors));
					logger.warn("Package is not valid: "+errors);
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the unique *functional* identifier of this package. The identifier refers to the functional
	 * contract and not to a concrete implementation in this particular package.
	 * 
	 * @return String
	 */
	public final String getFunctionIdentifier() {
		return this.functionIdentifier;
	}

	/**
	 * Returns the timestamp of this package. The timestamp indicates the last update of the package content
	 * and can be used as a simple versioning machanism.
	 * 
	 * @return {@link DateTime} package timestamp
	 */
	public DateTime getTimestamp() {
		return this.timeStamp;
	}

	/**
	 * Helper method: returns the modification date of a given file.
	 * 
	 * @param file
	 *        - the file
	 * @return DateTime - date of last modification
	 */
	private static DateTime getTimestamp(File file) {
		return new DateTime(file.lastModified());
	}

	/**
	 * Static internal method to evaluate a {@link PackageDescriptionDocument} and return the type (i.e. the
	 * schema) of the functional description.
	 * 
	 * TODO: slight overhead (?) - currently only WPS 1.0 is supported in the schema.
	 * 
	 * @param description
	 *        {@link PackageDescriptionDocument}
	 * @return {@link List} of {@link FunctionalType} - the type of the functional description (i.e. WPS 1.0,
	 *         WSDL, ...).
	 */
	private static final List<FunctionalType> getFunctionalTypes(final PackageDescriptionDocument description) {
		ArrayList<FunctionalType> availableFunctionalDescriptions = new ArrayList<FunctionalType>();

		// retrieve functional description types
		FunctionalDescriptionsListType funcDescArray = description.getPackageDescription().getContractedFunctionality();
		if (funcDescArray.isSetWpsProcessDescription()) {
			availableFunctionalDescriptions.add(FunctionalType.WPS100);
		}
		return availableFunctionalDescriptions;
	}

	/**
	 * Static helper method to determine when a directory or its content has been modified last time.
	 * 
	 * @param directory
	 *        {@link File}
	 * @return last modification date {@link DateTime}
	 */
	private DateTime getLastModified(File directory) {
		List<File> files = new ArrayList<File>(FileUtils.listFiles(directory, null, true));
		Collections.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		return new DateTime(files.get(0).lastModified());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MovingCodePackage [archive=");
		builder.append(this.archive);
		builder.append(", packageDescription=");
		builder.append(this.packageDescription);
		builder.append(", functionalIdentifier=");
		builder.append(this.functionIdentifier);
		builder.append(", timeStamp=");
		builder.append(this.timeStamp);
		builder.append(", supportedFuncTypes=");
		builder.append(this.supportedFuncTypes);
		builder.append("]");
		return builder.toString();
	}

}
