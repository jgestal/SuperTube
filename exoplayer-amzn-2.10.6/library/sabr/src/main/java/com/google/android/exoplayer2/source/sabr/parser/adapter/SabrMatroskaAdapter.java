package com.google.android.exoplayer2.source.sabr.parser.adapter;

import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.mkv.MatroskaExtractor;
import com.google.android.exoplayer2.source.sabr.parser.SabrStream;
import com.google.android.exoplayer2.source.sabr.parser.exceptions.SabrReloadPlayerError;
import com.google.android.exoplayer2.source.sabr.parser.exceptions.SabrBackoffError;
import com.google.android.exoplayer2.source.sabr.parser.misc.SabrExtractorInput;
import com.google.android.exoplayer2.upstream.HttpDataSource.HttpDataSourceException;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

public class SabrMatroskaAdapter extends MatroskaExtractor {
    private static final String TAG = SabrMatroskaAdapter.class.getSimpleName();
    private final SabrExtractorInput extractorInput;

    public SabrMatroskaAdapter(SabrStream sabrStream) {
        this.extractorInput = new SabrExtractorInput(sabrStream);
    }

    public SabrMatroskaAdapter(int flags, SabrStream sabrStream) {
        super(flags);
        this.extractorInput = new SabrExtractorInput(sabrStream);
    }

    @Override
    public int read(ExtractorInput input, PositionHolder seekPosition)
            throws IOException, InterruptedException {
        int result = RESULT_END_OF_INPUT;

        try {
            extractorInput.init(input);
            result = super.read(extractorInput, seekPosition);
        } catch (SabrReloadPlayerError | SabrBackoffError e) {
            Log.e(TAG, "Propagating SABR control signal: %s", e.getMessage());
            throw e;
        } catch (HttpDataSourceException e) {
            if (!isCanceled(e)) {
                // FIX: a real network failure (e.g. net::ERR_QUIC_PROTOCOL_ERROR) swallowed here ended the chunk early
                // and the playback stalled until the video was reloaded. Propagate it: the chunk source fails over
                // to another media network, the load is retried and, if it keeps failing, the error fixer switches
                // the network engine.
                Log.e(TAG, "Propagating network error: %s", e.getMessage());
                throw e;
            }
            Log.e(TAG, "User doing seek? %s: %s", e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "User doing seek? %s: %s", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
        } finally {
            if (result != RESULT_CONTINUE) {
                Log.e(TAG, "MatroskaAdapter: disposing, result=%s", result);
                extractorInput.dispose();
            }
        }

        return result;
    }

    /** The load was canceled (seek, track change, release): not an error. Package-private for tests. */
    static boolean isCanceled(Throwable error) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }

        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            // A read timeout is a SocketTimeoutException (an InterruptedIOException too) but a real stall
            if (cause instanceof SocketTimeoutException) {
                return false;
            }
            if (cause instanceof InterruptedIOException || cause instanceof InterruptedException) {
                return true;
            }
        }

        return false;
    }
}
