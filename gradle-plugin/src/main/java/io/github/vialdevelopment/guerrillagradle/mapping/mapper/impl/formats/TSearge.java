package io.github.vialdevelopment.guerrillagradle.mapping.mapper.impl.formats;

import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.Mapper;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.ClassName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.FieldName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.MethodName;
import org.gradle.api.Project;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A TSearge mapper
 * @filetype tsrg
 */
public class TSearge extends Mapper {

    /**
     * Inits this mapper
     * @param project gradle project
     * @param mappings mappings file
     */
    @Override
    public void init(Project project, String mappings) {
        if (loadFromCache(project)) return;
        // read in the mappings
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(mappings);
        } catch (FileNotFoundException e) { e.printStackTrace(); return; }
        Scanner scanner = new Scanner(inputStream, "UTF-8");
        List<String> lines = new ArrayList<>();
        while (scanner.hasNextLine()) lines.add(scanner.nextLine());
        scanner.close();
        try {
            inputStream.close();
        } catch (IOException e) { e.printStackTrace(); return; }

        // class names first
        for (String line : lines) {
            if (!line.startsWith("\t")) {
                String[] lineSplitSpaces = line.split(" ");
                classNameMappings.put(new ClassName(lineSplitSpaces[0]), new ClassName(lineSplitSpaces[1]));
            }
        }

        ClassName currentClass = null;
        for (String line : lines) {
            String[] lineSplitSpaces = line.split(" ");
            if (line.startsWith("\t")) {
                if (lineSplitSpaces.length == 2) {
                    // field name
                    fieldNameMappings.put(
                            new FieldName(currentClass.className, lineSplitSpaces[0], null),
                            new FieldName(remapClassName(currentClass).className, lineSplitSpaces[1], null)
                            );
                } else if (lineSplitSpaces.length == 3) {

                    // method name
                    methodNameMappings.put(
                            new MethodName(currentClass.className, lineSplitSpaces[0].substring(2), lineSplitSpaces[1]),
                            new MethodName(remapClassName(currentClass).className, lineSplitSpaces[2].substring(2), remapMethodDesc(lineSplitSpaces[1]))
                    );
                }
            } else {
                currentClass = new ClassName(lineSplitSpaces[0]);
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
