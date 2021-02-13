package io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name;

import java.io.Serializable;

/**
 * Holder for method name
 */
public class MethodName implements Comparable, Serializable {
    /** owner class name */
    public String ownerName;
    /** method name */
    public String methodName;
    /** method desc */
    public String methodDesc;

    public MethodName(String ownerName, String methodName, String methodDesc) {
        this.ownerName = ownerName.replace('.', '/');
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    @Override
    public String toString() {
        return "OwnerName: " + ownerName + ";MethodName: " + methodName + ";MethodDesc: " + methodDesc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return this.ownerName.equals(((MethodName) o).ownerName) &&
                this.methodName.equals(((MethodName) o).methodName) &&
                this.methodDesc.equals(((MethodName) o).methodDesc);
    }

    public MethodName clone() {
        return new MethodName(ownerName, methodName, methodDesc);
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) return 1;
        if (!(o instanceof MethodName)) return 0;

        int compareOwnerName = ownerName.compareTo(((MethodName) o).ownerName);
        if (compareOwnerName != 0) return compareOwnerName;

        int compareMethodName = methodName.compareTo(((MethodName) o).methodName);
        if (compareMethodName != 0) return compareMethodName;

        return methodDesc.compareTo(((MethodName) o).methodDesc);
    }
}
