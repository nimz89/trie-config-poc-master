package com.expedia.www.config;

import java.util.LinkedHashMap;

public class ConfigEntry {

    public String key;
    public String value;
    public LinkedHashMap<String, String> runtimeAttributes = new LinkedHashMap<>();

}
