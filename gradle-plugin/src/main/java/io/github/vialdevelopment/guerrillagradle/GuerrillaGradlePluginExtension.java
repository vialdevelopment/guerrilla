package io.github.vialdevelopment.guerrillagradle;

import java.util.ArrayList;
import java.util.List;

public class GuerrillaGradlePluginExtension {
    /** Package containing transformer classes */
    public String transformersPackage = "";
    /** Class registering transformers */
    public String transformerRegistrationClass = "";
    /** Should remap annotations */
    public boolean remap = true;
    /** MCP mappings version */
    public String mcpVersion = "";
    /** Regex list of classes to make public */
    public List<String> makePublic = new ArrayList<>();
    /** Mappings SRG File if you want to hard code it */
    public String mappingsSrgFile = "";

}
