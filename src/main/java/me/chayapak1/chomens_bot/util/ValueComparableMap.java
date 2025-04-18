package me.chayapak1.chomens_bot.util;


import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

// https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/utils/misc/ValueComparableMap.java
public class ValueComparableMap <K extends Comparable<K>, V> extends TreeMap<K, V> {
    private final Map<K, V> valueMap;

    public ValueComparableMap (final Ordering<? super V> partialValueOrdering) {
        this(partialValueOrdering, new HashMap<>());
    }

    private ValueComparableMap (final Ordering<? super V> partialValueOrdering, final HashMap<K, V> valueMap) {
        super(partialValueOrdering.onResultOf(valueMap::get).compound(Comparator.naturalOrder()));
        this.valueMap = valueMap;
    }

    @Override
    public V put (final K k, final V v) {
        if (valueMap.containsKey(k)) remove(k);
        valueMap.put(k, v);
        return super.put(k, v);
    }

    @Override
    public boolean containsKey (final Object key) {
        return valueMap.containsKey(key);
    }

    @Override
    public V getOrDefault (final Object key, final V defaultValue) {
        return containsKey(key) ? get(key) : defaultValue;
    }
}
