package com.kedevjimmy.pesatrack.security;

import org.junit.Test;

public class Pbkdf2Sha256Test {
    @Test public void knownAsciiUnicodeMultiBlockAndProductionIterationVectors() throws Exception { Pbkdf2Sha256Vectors.knownVectors(); }
    @Test public void equivalentToNativePbkdf2Sha256() throws Exception { Pbkdf2Sha256Vectors.nativeEquivalence(); }
    @Test public void callerPasswordAndSaltAreNotMutated() throws Exception { Pbkdf2Sha256Vectors.preservesCallerInputs(); }
    @Test public void invalidParametersAreRejected() throws Exception { Pbkdf2Sha256Vectors.rejectsInvalidParameters(); }
}
