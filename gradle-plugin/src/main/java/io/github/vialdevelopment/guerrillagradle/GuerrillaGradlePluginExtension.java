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
    /** Type of mappings {@link io.github.vialdevelopment.guerrillagradle.mapping.manager.MappingManager} */
    public String mappingsType = "";
    /** Mappings */
    public String mappings = "";
    /** Regex list of classes to make public */
    public List<String> makePublic = new ArrayList<>();
    /** Should compute frames */
    public boolean computeFrames = true;
}
