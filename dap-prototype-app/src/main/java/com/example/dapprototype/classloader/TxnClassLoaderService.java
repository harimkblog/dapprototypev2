package com.example.dapprototype.classloader;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing the TxnClassLoader and loading transaction model classes dynamically.
 */
@Service
public class TxnClassLoaderService {
    
    private static final Logger logger = LoggerFactory.getLogger(TxnClassLoaderService.class);
    
    @Value("${txn.classloader.paths:}")
    private String classloaderPaths;
    
    private TxnClassLoader txnClassLoader;
    
    @PostConstruct
    public void initialize() {
        try {
            URL[] urls = buildClassLoaderUrls();
            // Use the current thread's context class loader as parent
            // This ensures txn-models classes can access dependencies like MapStruct
            ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
            txnClassLoader = new TxnClassLoader(urls, parentClassLoader);
            logger.info("TxnClassLoader initialized successfully with {} URLs", urls.length);
            logger.info("Parent ClassLoader: {}", parentClassLoader.getClass().getName());
            for (URL url : urls) {
                logger.debug("TxnClassLoader URL: {}", url);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize TxnClassLoader", e);
            throw new RuntimeException("Failed to initialize TxnClassLoader", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (txnClassLoader != null) {
            try {
                txnClassLoader.close();
                logger.info("TxnClassLoader closed successfully");
            } catch (Exception e) {
                logger.error("Error closing TxnClassLoader", e);
            }
        }
    }
    
    /**
     * Loads a class using the TxnClassLoader.
     * 
     * @param className the fully qualified class name
     * @return the loaded Class object
     * @throws ClassNotFoundException if the class cannot be found
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if (txnClassLoader == null) {
            throw new IllegalStateException("TxnClassLoader is not initialized");
        }
        return txnClassLoader.loadClass(className);
    }
    
    /**
     * Creates a new instance of a class loaded by TxnClassLoader.
     * 
     * @param className the fully qualified class name
     * @return a new instance of the class
     * @throws Exception if the class cannot be loaded or instantiated
     */
    public Object createInstance(String className) throws Exception {
        Class<?> clazz = loadClass(className);
        return clazz.getDeclaredConstructor().newInstance();
    }
    
    /**
     * Gets the TxnClassLoader instance.
     * 
     * @return the TxnClassLoader
     */
    public TxnClassLoader getTxnClassLoader() {
        return txnClassLoader;
    }
    
    /**
     * Builds the URLs for the class loader from configured paths or default locations.
     * 
     * @return array of URLs
     * @throws MalformedURLException if URL construction fails
     */
    private URL[] buildClassLoaderUrls() throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        
        // If specific paths are configured, use them
        if (classloaderPaths != null && !classloaderPaths.isEmpty()) {
            String[] paths = classloaderPaths.split(",");
            for (String path : paths) {
                File file = new File(path.trim());
                if (file.exists()) {
                    urls.add(file.toURI().toURL());
                    logger.info("Added configured path to TxnClassLoader: {}", file.getAbsolutePath());
                } else {
                    logger.warn("Configured path does not exist: {}", path);
                }
            }
        }
        
        // Add default txn-models module location (compiled classes)
        String txnModelsPath = findTxnModelsPath();
        if (txnModelsPath != null) {
            File txnModelsFile = new File(txnModelsPath);
            if (txnModelsFile.exists()) {
                urls.add(txnModelsFile.toURI().toURL());
                logger.info("Added txn-models path to TxnClassLoader: {}", txnModelsFile.getAbsolutePath());
            }
        }
        
        // If no URLs were found, try to add current classpath as fallback
        if (urls.isEmpty()) {
            logger.warn("No valid URLs found for TxnClassLoader, using current class location as fallback");
            URL currentLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            urls.add(currentLocation);
        }
        
        return urls.toArray(new URL[0]);
    }
    
    /**
     * Finds the path to the txn-models module's compiled classes.
     * 
     * @return the path to txn-models/target/classes or null if not found
     */
    private String findTxnModelsPath() {
        // Try different potential locations
        String[] potentialPaths = {
            "../dap-prototype-txn-models/target/classes",
            "../../dap-prototype-txn-models/target/classes",
            "/workspaces/dapprototypev2/dap-prototype-txn-models/target/classes"
        };
        
        for (String path : potentialPaths) {
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                return file.getAbsolutePath();
            }
        }
        
        return null;
    }
}
