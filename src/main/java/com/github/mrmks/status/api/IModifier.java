package com.github.mrmks.status.api;

public interface IModifier {
    /**
     * Define the name of this modifier.
     * Should longer than 0 characters and shorter than 32 characters.
     */
    String getName();

    /**
     * Define the attribute dependencies.
     */
    SimpleDependency[] getDependencies();

    default SimpleDependency[] modifierDependencies() {
        return null;
    }

    /**
     * @param ids The ids array has the same order of {@link #getDependencies()}. minus means the optional dependency doesn't exist.
     * @param v the value of the handled value. now the v is an array with size smaller than 9;
     */
    void handle(short[] ids, int[] v, ModificationCache mt, int sessionId, int[] dataSrc, int[] dataTar);

    /*
     * For some functional requirement, you may need some data associated with the entity stored.
     * i.e. make the damage random in a range.
     *
     * You can use the store for this purpose. You can define an int array in maximum size of 16, and store you data.
     * If the size and the version both in 0, we will pass an empty int array in size of 0.
     */

    default byte storeSize() {
        return 0;
    }

    default int storeVersion() {
        return 0;
    }

    /*
     * if you are using store, we will try to update the data every time you change the storeVersion.
     * You should provide an updater for the auto-update purpose, but it's ok to return null if you want users update it manually.
     * You can also return false if your updater can't update to this version from previous stored version.
     */

    default Updater storeUpdater() {
        return null;
    }

    interface Updater {
        boolean accept(int perv, int now, int[] data, int[] worker);
    }
}
