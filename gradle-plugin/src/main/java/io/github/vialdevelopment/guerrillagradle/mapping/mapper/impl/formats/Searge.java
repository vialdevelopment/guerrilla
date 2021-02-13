package io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.formats;

import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.Mapper;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.ClassName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.FieldName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.MethodName;
import org.gradle.api.Project;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Searge mapper
 * @filetype srg
 */
public class Searge extends Mapper {

    /**
     * Initializes the searge mapper
     * @param mappings mappings file
     */
    @Override
    public void init(Project project, String mappings) {
        if (loadFromCache(project)) return;
        // read in the mappings
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(mappings);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        Scanner scanner = new Scanner(inputStream, "UTF-8");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] lineSplitSpaces = line.split(" ");
            switch(line.substring(0, 2)) {
                case "CL":
                    classNameMappings.put(
                            new ClassName(
                                    lineSplitSpaces[1]
                            ),
                            new ClassName(
                                    lineSplitSpaces[2]
                            )
                    );
                    break;
                case "FD":
                    fieldNameMappings.put(
                            new FieldName(
                                    lineSplitSpaces[1].substring(0, lineSplitSpaces[1].lastIndexOf("/")),
                                    lineSplitSpaces[1].substring(lineSplitSpaces[1].lastIndexOf("/")+1), null
                            ),
                            new FieldName(
                                    lineSplitSpaces[2].substring(0, lineSplitSpaces[2].lastIndexOf("/")),
                                    lineSplitSpaces[2].substring(lineSplitSpaces[2].lastIndexOf("/")+1), null
                            )
                    );
                    break;
                case "MD":
                    methodNameMappings.put(
                            new MethodName(
                                    lineSplitSpaces[1].substring(0, lineSplitSpaces[1].lastIndexOf("/")),
                                    lineSplitSpaces[1].substring(lineSplitSpaces[1].lastIndexOf("/")+1),
                                    lineSplitSpaces[2]
                            ),
                            new MethodName(
                                    lineSplitSpaces[3].substring(0, lineSplitSpaces[3].lastIndexOf("/")),
                                    lineSplitSpaces[3].substring(lineSplitSpaces[3].lastIndexOf("/")+1),
                                    lineSplitSpaces[4]
                            )
                    );
                    break;
            }
        }
        writeToCache(project);
    }

    @Override
    public FieldName remapFieldName(FieldName fieldName) {
        FieldName clone = fieldName.clone();
        clone.fieldDesc = null;
        FieldName temp = super.remapFieldName(clone);
        temp.fieldDesc = remapFieldDesc(fieldName.fieldDesc);
        return temp;
    }

}
