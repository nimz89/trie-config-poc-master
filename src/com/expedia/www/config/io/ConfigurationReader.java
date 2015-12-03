package com.expedia.www.config.io;

import com.expedia.www.config.ConfigEntry;

import java.util.List;

public interface ConfigurationReader {
    List<String> getRuntimeAttributePrecedence();
    ConfigEntry getNextEntry();
}
