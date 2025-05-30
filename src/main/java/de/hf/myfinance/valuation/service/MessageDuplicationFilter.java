package de.hf.myfinance.valuation.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MessageDuplicationFilter {
    private final List<String> keys = new ArrayList<String>();

    public boolean isAlreadyQueued(String key) {
        if(keys.contains(key)) return true;
        else {
            keys.add(key);
            return false;
        }
    } 

    public void processKey(String key) {
        keys.remove(key);
    }
    
}
