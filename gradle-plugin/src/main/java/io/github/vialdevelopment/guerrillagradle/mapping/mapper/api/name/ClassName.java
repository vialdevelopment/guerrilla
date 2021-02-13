package io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name;

import java.io.Serializable;

/**
 * Holder for class name
 */
public class ClassName implements Comparable, Serializable {
    /** class name */
    public String className;

    public ClassName(String className) {
        this.className = className.replace('.', '/');
    }

    @Override
    public String toString() {
        return "ClassName: " + className;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return this.className.equals(((ClassName)o).className);
    }

    public ClassName clone() {
        return new ClassName(className);
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) return 1;
        return className.compareTo(((ClassName)o).className);
    }

}
