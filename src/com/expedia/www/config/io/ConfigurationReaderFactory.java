package com.expedia.www.config.io;

public interface ConfigurationReaderFactory {
    public ConfigurationReader getInstance(String configNamespace);
}
