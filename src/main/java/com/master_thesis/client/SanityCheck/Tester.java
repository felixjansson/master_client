package com.master_thesis.client.SanityCheck;

import com.master_thesis.client.SanityCheck.HomomorphicHash.HomomorphicHashServer;
import com.master_thesis.client.SanityCheck.HomomorphicHash.HomomorphicHashVerifier;
import com.master_thesis.client.SanityCheck.Linear.LinearClientData;
import com.master_thesis.client.SanityCheck.Linear.LinearProofData;
import com.master_thesis.client.SanityCheck.Linear.LinearSignatureServer;
import com.master_thesis.client.SanityCheck.Linear.LinearSignatureVerifier;
import com.master_thesis.client.data.DefaultPublicData;
import com.master_thesis.client.data.HomomorphicHashData;
import com.master_thesis.client.data.LinearSignatureData;
import com.master_thesis.client.data.RSAThresholdData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class Tester {

    private static final Logger log = LoggerFactory.getLogger(Tester.class);

    private LinearSignatureVerifier linearSignatureVerifier;
    private LinearSignatureServer linearSignatureServer;

    private HomomorphicHashServer homomorphicHashServer;
    private HomomorphicHashVerifier homomorphicHashVerifier;


    private LastClientTau rnComputations;

    private List<BigInteger> evals;
    private List<BigInteger> partialProofs;
    private List<BigInteger> homomorphicTaus;
    private BigInteger rn;
    private BigInteger finalEval;
    private LinearProofData linearFinalProof;
    private BigInteger hashFinalProof;
    private ReentrantLock lock;

    public Tester(LinearSignatureVerifier linearSignatureVerifier, LinearSignatureServer linearSignatureServer, HomomorphicHashServer homomorphicHashServer, HomomorphicHashVerifier homomorphicHashVerifier, LastClientTau rnComputations) {
        this.linearSignatureVerifier = linearSignatureVerifier;
        this.linearSignatureServer = linearSignatureServer;
        this.homomorphicHashServer = homomorphicHashServer;
        this.homomorphicHashVerifier = homomorphicHashVerifier;
        this.rnComputations = rnComputations;
        lock = new ReentrantLock();
        reset();
    }

    void reset() {
        evals = new LinkedList<>();
        partialProofs = new LinkedList<>();
        homomorphicTaus = new LinkedList<>();
        rn = null;
        finalEval = null;
        linearFinalProof = null;
        hashFinalProof = null;
    }

    public void clientPost(Object body, DefaultPublicData defaultPublicData) {
        computeLinear(body, defaultPublicData);
        computeHomomorphic(body, defaultPublicData);
        computeRSA(body, defaultPublicData);
    }

    private void computeRSA(Object body, DefaultPublicData defaultPublicData) {
        if (body instanceof RSAThresholdData.ServerData) {
            log.error("No RSA test implemented");
        } else if (body instanceof RSAThresholdData.VerifierData) {
            log.error("No RSA test implemented");
        } else if (body instanceof RSAThresholdData.NonceData) {
            log.error("No RSA test implemented");
        }
    }

    private void computeHomomorphic(Object body, DefaultPublicData defaultPublicData) {
        if (body instanceof HomomorphicHashData.ServerData) {
            lock.lock();
            try {
                HomomorphicHashData.ServerData tmp = (HomomorphicHashData.ServerData) body;
                evals.add(homomorphicHashServer.partialEval(List.of(tmp.getSecretShare())));
                partialProofs.add(homomorphicHashServer.partialProof(List.of(tmp.getSecretShare()), defaultPublicData.getFieldBase(), defaultPublicData.getGenerator()));
                if (evals.size() == defaultPublicData.getNumberOfServers()) {
                    finalEval = homomorphicHashVerifier.finalEval(evals.stream());
                    hashFinalProof = homomorphicHashVerifier.finalProof(partialProofs.stream());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        } else if (body instanceof HomomorphicHashData.VerifierData) {
            HomomorphicHashData.VerifierData tmp = (HomomorphicHashData.VerifierData) body;
            homomorphicTaus.add(tmp.getProofComponent());
        } else if (body instanceof HomomorphicHashData.NonceData) {
            HomomorphicHashData.NonceData tmp = (HomomorphicHashData.NonceData) body;
            homomorphicTaus.add(rnComputations.computeLastTau(tmp.getNonce(), defaultPublicData.getFieldBase(), defaultPublicData.getGenerator()));
        }
        if (finalEval != null && hashFinalProof != null && homomorphicTaus.size() == 2) {
            lock.lock();
            try {
                boolean valid = homomorphicHashVerifier.verify(finalEval, hashFinalProof, homomorphicTaus);
                log.info("Valid = {}, Result = {}, Proof = {}", valid, finalEval, hashFinalProof);
                reset();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    void computeLinear(Object body, DefaultPublicData defaultPublicData) {
        if (body instanceof LinearSignatureData.ServerData) {
            LinearSignatureData.ServerData tmp = (LinearSignatureData.ServerData) body;
            evals.add(linearSignatureServer.partialEval(List.of(tmp.getSecretShare())));
            if (evals.size() == defaultPublicData.getNumberOfServers()) {
                finalEval = linearSignatureVerifier.finalEval(evals.stream());
            }
        } else if (body instanceof LinearSignatureData.VerifierData) {
            LinearSignatureData.VerifierData tmp = (LinearSignatureData.VerifierData) body;
            LinearClientData tmp2 = new LinearClientData();
            tmp2.setFidPrime(tmp.getFidPrime());
            tmp2.setX(tmp.getX());
            tmp2.setsShare(tmp.getsShare());
            linearFinalProof = linearSignatureVerifier.finalProof(List.of(tmp2), defaultPublicData.getLinearSignatureData());
        } else if (body instanceof LinearSignatureData.NonceData) {
            LinearSignatureData.NonceData tmp = (LinearSignatureData.NonceData) body;
            rn = rnComputations.getRn(tmp.getNonce(), defaultPublicData.getLinearSignatureData().getSk());
        }
        if (finalEval != null && linearFinalProof != null && rn != null) {

            lock.lock();
            try {
                boolean valid = linearSignatureVerifier.verify(finalEval, linearFinalProof, defaultPublicData.getLinearSignatureData(), rn);
                log.info("Valid = {}, Result = {}, Proof = {}", valid, finalEval, linearFinalProof);
                reset();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

}
