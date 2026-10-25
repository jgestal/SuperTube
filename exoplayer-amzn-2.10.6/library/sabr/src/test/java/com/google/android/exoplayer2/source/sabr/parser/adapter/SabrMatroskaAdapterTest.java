package com.google.android.exoplayer2.source.sabr.parser.adapter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

public class SabrMatroskaAdapterTest {
    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    public void networkFailure_isNotCanceled() {
        IOException quic = new IOException("Exception in CronetUrlRequest: net::ERR_QUIC_PROTOCOL_ERROR");
        assertFalse(SabrMatroskaAdapter.isCanceled(new IOException(quic)));
    }

    @Test
    public void readTimeout_isNotCanceled() {
        assertFalse(SabrMatroskaAdapter.isCanceled(new IOException(new SocketTimeoutException("timeout"))));
    }

    @Test
    public void interruptedLoad_isCanceled() {
        assertTrue(SabrMatroskaAdapter.isCanceled(new IOException(new InterruptedIOException())));
        assertTrue(SabrMatroskaAdapter.isCanceled(new IOException(new InterruptedException())));
    }

    @Test
    public void interruptedThread_isCanceled() {
        Thread.currentThread().interrupt();
        assertTrue(SabrMatroskaAdapter.isCanceled(new IOException("any")));
    }
}
