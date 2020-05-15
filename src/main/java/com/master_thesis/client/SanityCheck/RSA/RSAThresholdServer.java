package com.master_thesis.client.SanityCheck.RSA;

import ch.qos.logback.classic.Logger;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.master_thesis.client.data.RSAThresholdData.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RSAThresholdServer {
    private static final Logger log = (Logger) LoggerFactory.getLogger(RSAThresholdServer.class);


    /**
     * This is the partial Eval function from the Threshold Signature based construction.
     * @param shares a list of all secret shares that is given to this server.
     * @return The sum of all shares (y_j)
     */
    public BigInteger partialEval(List<BigInteger> shares) {
        return shares.stream().reduce(BigInteger.ZERO, BigInteger::add);
    }


    /**
     * This is the Threshold Signature partial proof function.
     * It calls the subroutine PartialProof_i one time per client.
     * @param proofInformation a list of information sent from each client.
     * @return A set of proofs, one for each client.
     */
    public Map<Integer, RSAOutgoingData.ProofData> partialProof(List<ServerData> proofInformation) {
        // For every item in the list of client data we compute the partial proof subroutine.
        // This is done by calling this::partialProofSubroutine
        return proofInformation.stream()
                .collect(Collectors.toMap(ServerData::getId, this::partialProofSubroutine));
    }


    /**
     * This is the subroutine from partial proof in the Threshold Signature construction.
     * @param rsaProofInfo The information send by a single client.
     * @return A set of variables that is used in later stages of the computations. The returned values are
     *  - a vector with proofs (the sigma vector).
     *  - the N value for the computation
     *  - the determinant of the matrix A_iS.
     *  - the client proof, Tau.
     */
    RSAOutgoingData.ProofData partialProofSubroutine(ServerData rsaProofInfo) {
        SimpleMatrix matrixOfClient = (SimpleMatrix) getObjectFromByteArray(rsaProofInfo.getMatrixOfClient());

        // Create A_iS, which is t by t matrix.
        int t = matrixOfClient.numCols();
        SimpleMatrix squareMatrixOfClient = matrixOfClient.rows(0, t); // TODO: 2020-03-11 Handle when server < t

        // We compute the adjugate matrix C_iS by first finding the cofactor and then transposing it.
        SimpleMatrix adjugateMatrix = SimpleMatrixUtils.getCofactorMatrix(squareMatrixOfClient).transpose();

        // Find t rows from skShares
        SimpleMatrix skShares = ((SimpleMatrix) getObjectFromByteArray(rsaProofInfo.getSkShare())).rows(0, t);

        // Create variables that will be returned as part of the result of the partial proof.
        BigInteger[] result = new BigInteger[t];
        BigInteger clientProof = rsaProofInfo.getProofComponent();
        BigInteger rsaN = rsaProofInfo.getRsaN();

        // Compute every cell in the partial proof (sigma) vector.
        for (int i = 0; i < result.length; i++) {
            long v = Math.round(2 * Math.round(adjugateMatrix.get(0, i)) * skShares.get(i));
            BigInteger exponent = BigInteger.valueOf(v);
            try {
                result[i] = clientProof.modPow(exponent, rsaN);
            } catch (ArithmeticException e) {
                log.info("{}", rsaProofInfo);
                throw e;
            }
        }

        // Store the results in a data class and return it.
        return new RSAOutgoingData.ProofData(rsaN, result, squareMatrixOfClient.determinant(), clientProof);
    }

    private Object getObjectFromByteArray(byte[] arr) {
        ByteArrayInputStream bios = new ByteArrayInputStream(arr);
        try {
            ObjectInputStream ios = new ObjectInputStream(bios);
            return ios.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
