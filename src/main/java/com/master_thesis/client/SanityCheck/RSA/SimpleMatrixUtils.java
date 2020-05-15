package com.master_thesis.client.SanityCheck.RSA;

import org.ejml.simple.SimpleMatrix;

public class SimpleMatrixUtils {


    public static SimpleMatrix getCofactorMatrix(SimpleMatrix a) {
        SimpleMatrix cofactor = a.copy();
        for (int i = 0; i < a.numRows(); i++) {
            for (int j = 0; j < a.numCols(); j++) {
                int sign = (i + j) % 2 == 0 ? 1 : -1;
                cofactor.set(i, j, createSubMatrix(a, i, j).determinant() * sign);
            }
        }
        return cofactor;
    }


    public static SimpleMatrix createSubMatrix(SimpleMatrix matrix, int excluding_row, int excluding_col) {
        SimpleMatrix mat = new SimpleMatrix(matrix.numRows() - 1, matrix.numCols() - 1);
        int r = -1;
        for (int i = 0; i < matrix.numRows(); i++) {
            if (i == excluding_row)
                continue;
            r++;
            int c = -1;
            for (int j = 0; j < matrix.numCols(); j++) {
                if (j == excluding_col)
                    continue;
                mat.set(r, ++c, matrix.get(i, j));
            }
        }
        return mat;
    }

}
