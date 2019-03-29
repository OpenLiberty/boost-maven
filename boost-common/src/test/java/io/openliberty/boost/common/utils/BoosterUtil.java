package io.openliberty.boost.common.utils;

import java.util.HashMap;
import java.util.Map;

import io.openliberty.boost.common.BoostException;
import io.openliberty.boost.common.boosters.liberty.AbstractBoosterLibertyConfig;

public class BoosterUtil {

    public static Map<String, String> createDependenciesWithBoosterAndVersion(Class<?> booster, String version)
            throws BoostException {
        Map<String, String> map = new HashMap<String, String>();
        map.put(AbstractBoosterLibertyConfig.getCoordindates(booster), version);
        return map;
    }

}