/*
 * Copyright (c) 2019 The University of Sheffield.
 *
 * This file is part of gateplugin-Python
 * (see https://github.com/GateNLP/gateplugin-Python).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package gate.plugin.python;

import gate.Resource;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import gate.creole.AbstractResource;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;
import gate.lib.interaction.process.pipes.Process4StringStream;
import gate.util.GateRuntimeException;

@CreoleResource(
        name = "PythonVersionLogger",
        comment = "Log the version of the Python plugin and the included gatenlp package",
        tool = true,
        isPrivate = true,
        autoinstances = {@AutoInstance(hidden = true)}
)
public class VersionLogger extends AbstractResource {
    protected boolean versionInfoShown = false;

    private static final long serialVersionUID = 1288492373838L;
    
    /**
     * Our logger instance.
     */
    public transient org.apache.log4j.Logger logger
            = org.apache.log4j.Logger.getLogger(this.getClass());

    public static String plugin_version = "unknown";
    public static String plugin_build = "unknown";
    

/**
     * Read the gatenlp version information.
     *
     * @return the version string or NONE if nothing found.
     */
    public static String readGateNlpVersion() {

        URL artifactURL = PythonPr.class.getResource("/creole.xml");
        List<String> lines = new ArrayList<>();
        File fromFile = null;
        Path pathInZip = null;
        try {
            artifactURL = new URL(artifactURL, ".");
        } catch (MalformedURLException ex) {
            throw new GateRuntimeException("Could not get jar URL");
        }
        if (artifactURL.toString().startsWith("file:/")) {
            try {
                String pythonProgramPathName = PythonPr.getPythonpathInZip();
                File theFile = new File(new File(pythonProgramPathName, "gatenlp"), "__init__.py");
                String source = theFile.getAbsolutePath();
                fromFile = new File(source);
                lines = java.nio.file.Files.readAllLines(fromFile.toPath(), Charset.forName("UTF-8"));
            } catch (IOException ex) {
                throw new GateRuntimeException("Error trying to read the resource "+fromFile, ex);
            }
        } else {
            URL fileURL = PythonPr.class.getResource("/resources/pythonpath/gatenlp/__init__.py");
            try (BufferedReader rdr = new BufferedReader(new InputStreamReader(fileURL.openStream())))
            {
                String line;
                while ((line = rdr.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException  ex) {
                throw new GateRuntimeException("Error trying to read the resource "+pathInZip, ex);
            }
        }
        // Find and extract the version number
        String version = "UNKNOWN";
        for(String line : lines) {
          if (line.startsWith("__version__")) {
            version = line.split("\"")[1];
            break;
          }
        }
        return version;
    }    
    
    @Override
    public Resource init() {
        if (!versionInfoShown) {
            // Show the version of this plugin
            try {
                Properties properties = new Properties();
                InputStream is = getClass().getClassLoader().getResourceAsStream("gateplugin-Python.git.properties");
                if (is != null) {
                    properties.load(is);
                    String buildVersion = properties.getProperty("gitInfo.build.version");
                    String isDirty = properties.getProperty("gitInfo.dirty");
                        logger.info("Plugin Python version: " + buildVersion
                                + " commit: "
                                + properties.getProperty("gitInfo.commit.id.abbrev")
                                + " dirty: " + isDirty
                        );
                    plugin_version = properties.getProperty("gitInfo.build.version");
                    plugin_build = properties.getProperty("gitInfo.commit.id.abbrev");
                } else {
                    logger.error("Could not obtain plugin Python version info");
                }
            } catch (IOException ex) {
                logger.error("Could not obtain plugin Python version info: " + ex.getMessage(), ex);
            }
            // Show the version of the interaction library
            try {
                Properties properties = new Properties();
                InputStream is = Process4StringStream.class.getClassLoader().getResourceAsStream("gatelib-interaction.git.properties");
                if (is != null) {
                    properties.load(is);
                    String buildVersion = properties.getProperty("gitInfo.build.version");
                    String isDirty = properties.getProperty("gitInfo.dirty");
                        logger.info("Lib interaction version: " + buildVersion
                                + " commit: " + properties.getProperty("gitInfo.commit.id.abbrev")
                                + " dirty: " + isDirty
                        );
                }
            } catch (IOException ex) {
                logger.error("Could not obtain lib interaction version info: " + ex.getMessage(), ex);
            }
            // Show the version and commit of the python-gatenlp submodule
            try {
                Properties properties = new Properties();
                InputStream is = getClass().getClassLoader().getResourceAsStream("python-gatenlp.git.properties");
                if (is != null) {
                    properties.load(is);
                    String buildVersion = VersionLogger.readGateNlpVersion();
                    String isDirty = properties.getProperty("gitInfo.dirty");
                        logger.info("Python gatenlp version: " + buildVersion
                                + " commit: " + properties.getProperty("gitInfo.commit.id.abbrev")
                                + " dirty: " + isDirty
                        );
                }
            } catch (IOException ex) {
                logger.error("Could not obtain lib interaction version info: " + ex.getMessage(), ex);
            }
            versionInfoShown = true;
        }

        return this;
    }
}
