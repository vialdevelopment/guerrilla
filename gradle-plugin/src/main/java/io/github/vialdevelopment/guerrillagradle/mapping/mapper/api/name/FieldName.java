package io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name;

/**
 * Holder for field name
 */
public class FieldName implements Comparable {
    /** owner class name */
    public String ownerName;
    /** field name */
    public String fieldName;
    /** field description, can be null*/
    public String fieldDesc;

    public FieldName(String ownerName, String fieldName, String fieldDesc) {
        this.ownerName = ownerName.replace('.', '/');
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
    }

    @Override
    public String toString() {
        return "OwnerName: " + ownerName + ";FieldName: " + fieldName + ";FieldDesc: " + fieldDesc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return this.ownerName.equals(((FieldName) o).ownerName) &&
                this.fieldName.equals(((FieldName) o).fieldName) &&
                ((this.fieldDesc == null ? (((FieldName) o).fieldDesc == null) : ((FieldName) o).fieldDesc != null && this.fieldDesc.equals(((FieldName) o).fieldDesc)));
    }

    public FieldName clone() {
        return new FieldName(ownerName, fieldName, fieldDesc);
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) return 1;
        int compareOwner = ownerName.compareTo(((FieldName) o).ownerName);
        if (compareOwner != 0) return compareOwner;

        int compareFieldName = fieldName.compareTo(((FieldName) o).fieldName);
        if (compareFieldName != 0) return compareFieldName;

        if (fieldDesc == null && ((FieldName) o).fieldDesc == null) return 0;
        if (fieldDesc == null && ((FieldName) o).fieldDesc != null) return -1;
        if (fieldDesc != null && ((FieldName) o).fieldDesc == null) return 1;

        return fieldDesc.compareTo(((FieldName) o).fieldDesc);
    }

}
