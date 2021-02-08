package io.github.vialdevelopment.guerrillagradle;

import java.util.ArrayList;
import java.util.List;

public class GuerrillaGradlePluginExtension {
    /** Package containing transformer classes */
    public String transformers = "";
    /** Class registering transformers */
    public String transformer = "";
    /** Should remap annotations */
    public boolean remap = true;
    /** MCP mappings version */
    public String mcpVersion = "";
    /** Regex list of classes to make public */
    public List<String> makePublic = new ArrayList<>();

}
