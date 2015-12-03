package com.expedia.www.config;

public class RuntimeAttribute {
    public RuntimeAttribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public RuntimeAttribute(String name) {
        this.name = name;
    }

    public String name, value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuntimeAttribute attribute = (RuntimeAttribute) o;

        if (name != null ? !name.equals(attribute.name) : attribute.name != null) return false;
        if (value != null ? !value.equals(attribute.value) : attribute.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
