package com.example.dapprototype.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Custom URLClassLoader for loading transaction model classes dynamically.
 * The system class loader is the parent of this class loader.
 */
public class TxnClassLoader extends URLClassLoader {
    
    /**
     * Creates a new TxnClassLoader with the specified URLs.
     * Uses the system class loader as the parent.
     * 
     * @param urls the URLs from which to load classes and resources
     */
    public TxnClassLoader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader());
    }
    
    /**
     * Creates a new TxnClassLoader with the specified URLs and parent class loader.
     * 
     * @param urls the URLs from which to load classes and resources
     * @param parent the parent class loader
     */
    public TxnClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    @Override
    public String toString() {
        return "TxnClassLoader[parent=" + getParent() + "]";
    }
}
