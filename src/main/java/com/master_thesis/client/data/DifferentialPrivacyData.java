package com.master_thesis.client.data;

public class DifferentialPrivacyData extends HomomorphicHashData {

    public DifferentialPrivacyData(HomomorphicHashData input) {
        super(input);
        nonceData.construction = Construction.DP;
        serverData.forEach((k, v) -> v.construction = Construction.DP);
        verifierData.construction = Construction.DP;
    }
}
