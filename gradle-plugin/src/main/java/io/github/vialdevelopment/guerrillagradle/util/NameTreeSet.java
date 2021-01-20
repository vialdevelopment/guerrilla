package io.github.vialdevelopment.guerrillagradle.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This is a tree data structure
 * for holding an inheritance tree of minecraft
 */
public class NameTreeSet implements Serializable {
    /** name of this tree node */
    public String name = "";
    /** inner tree nodes */
    public List<NameTreeSet> values = new ArrayList<>();
    /** parent tree node */
    public NameTreeSet superTree;
    /** constructor with name and parent */
    public NameTreeSet(String name, NameTreeSet superTree) {
        this.name = name;
        this.superTree = superTree;
    }

    /** recursively gets the tree node with this name */
    public NameTreeSet contains(String name) {
        if (this.name.equals(name)) {
            return this;
        }
        for (NameTreeSet value : values) {
            NameTreeSet found = value.contains(name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** adds a new tree node as a child */
    public NameTreeSet add(String name) {
        NameTreeSet ret = new NameTreeSet(name, this);
        values.add(ret);
        return ret;
    }

    /** adds the super and name to this tree node */
    public NameTreeSet add(String superName, String name) {
        NameTreeSet preExistingSuperNameTreeSet = contains(superName);
        NameTreeSet preExistingNameTreeSet = contains(name);
        if (preExistingNameTreeSet != null && preExistingSuperNameTreeSet != null) return preExistingNameTreeSet;
        if (preExistingSuperNameTreeSet != null) {
            return preExistingSuperNameTreeSet.add(name);
        }
        if (preExistingNameTreeSet != null) {
            preExistingNameTreeSet.superTree.values.remove(preExistingNameTreeSet);
            NameTreeSet newSuperNameTreeSet = add(superName);
            newSuperNameTreeSet.values.add(preExistingNameTreeSet);
            preExistingNameTreeSet.superTree = newSuperNameTreeSet;
            return preExistingNameTreeSet;
        }
        return add(superName).add(name);
    }

    /** recursively gets the full name of this tree node, ie ;superTreeName;thisName; */
    public List<String> getName(List<String> stringList) {
        if (this.name != "") {
            stringList.add(0, this.name);
        }
        if (superTree != null) {
            return superTree.getName(stringList);
        }
        return stringList;
    }

    /** consume a consumer on this tree node and all children and their children */
    public void forAll(Consumer<NameTreeSet> consumer) {
        consumer.accept(this);
        if (values.size() != 0) {
            for (NameTreeSet value : values) {
                value.forAll(consumer);
            }
        }
    }

}