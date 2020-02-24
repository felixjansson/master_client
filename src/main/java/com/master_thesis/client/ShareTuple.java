package com.master_thesis.client;

import java.util.List;

public class ShareTuple {
    
    final List<Integer> shares;
    final Integer proofComponent;

    public ShareTuple(List<Integer> shares, Integer proofComponent) {
        this.shares = shares;
        this.proofComponent = proofComponent;
    }
}
