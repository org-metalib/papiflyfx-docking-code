package org.metalib.papifly.fx.code.lexer;

import javafx.application.Platform;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeEvent;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;

import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Debounced asynchronous incremental lexer pipeline.
 */
public class IncrementalLexerPipeline implements AutoCloseable {

    static final long DEFAULT_DEBOUNCE_MILLIS = 35;
    private static final System.Logger LOGGER = System.getLogger(IncrementalLexerPipeline.class.getName());
    private static final PlainTextLexer PLAIN_TEXT_FALLBACK_LEXER = new PlainTextLexer();

    private final Document document;
    private final Consumer<TokenMap> tokenMapConsumer;
    private final Consumer<Runnable> fxDispatcher;
    private final Function<String, Lexer> lexerResolver;
    private final ScheduledExecutorService worker;
    private final long debounceMillis;
    private final AtomicLong revision = new AtomicLong();
    private final Object lock = new Object();
    private final DocumentChangeListener documentChangeListener = this::onDocumentChanged;

    private volatile TokenMap tokenMap = TokenMap.empty();
    private volatile String languageId = PlainTextLexer.LANGUAGE_ID;
    private volatile boolean disposed;

    private PendingRequest pendingRequest;
    private ScheduledFuture<?> scheduledTask;

    /**
     * Creates a pipeline with default FX dispatcher and debounce.
     *
     * @param document source document model
     * @param tokenMapConsumer consumer notified with updated token maps
     */
    public IncrementalLexerPipeline(Document document, Consumer<TokenMap> tokenMapConsumer) {
        this(
            document,
            tokenMapConsumer,
            IncrementalLexerPipeline::dispatchOnFxThread,
            DEFAULT_DEBOUNCE_MILLIS,
            languageId -> LanguageSupportRegistry.defaultRegistry().resolveLexer(languageId)
        );
    }

    IncrementalLexerPipeline(
        Document document,
        Consumer<TokenMap> tokenMapConsumer,
        Consumer<Runnable> fxDispatcher,
        long debounceMillis
    ) {
        this(
            document,
            tokenMapConsumer,
            fxDispatcher,
            debounceMillis,
            languageId -> LanguageSupportRegistry.defaultRegistry().resolveLexer(languageId)
        );
    }

    IncrementalLexerPipeline(
        Document document,
        Consumer<TokenMap> tokenMapConsumer,
        Consumer<Runnable> fxDispatcher,
        long debounceMillis,
        Function<String, Lexer> lexerResolver
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.tokenMapConsumer = Objects.requireNonNull(tokenMapConsumer, "tokenMapConsumer");
        this.fxDispatcher = Objects.requireNonNull(fxDispatcher, "fxDispatcher");
        this.lexerResolver = Objects.requireNonNull(lexerResolver, "lexerResolver");
        this.debounceMillis = Math.max(0, debounceMillis);
        this.worker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "papiflyfx-lexer-pipeline");
            thread.setDaemon(true);
            return thread;
        });

        document.addChangeListener(documentChangeListener);
        enqueue(document.getText(), 0, revision.get(), languageId, false, 0);
    }

    /**
     * Returns current token map snapshot.
     *
     * @return latest token map snapshot
     */
    public TokenMap getTokenMap() {
        return tokenMap;
    }

    /**
     * Sets active language id and requests full re-lex.
     *
     * @param languageId requested language id
     */
    public void setLanguageId(String languageId) {
        String normalizedLanguageId = LanguageSupportRegistry.defaultRegistry().normalizeLanguageId(languageId);
        this.languageId = normalizedLanguageId;
        long nextRevision = revision.incrementAndGet();
        enqueue(document.getText(), 0, nextRevision, normalizedLanguageId, true, debounceMillis);
    }

    /**
     * Stops worker tasks and detaches document listeners.
     */
    public void dispose() {
        synchronized (lock) {
            if (disposed) {
                return;
            }
            disposed = true;
            pendingRequest = null;
            if (scheduledTask != null) {
                scheduledTask.cancel(true);
                scheduledTask = null;
            }
        }
        document.removeChangeListener(documentChangeListener);
        worker.shutdownNow();
    }

    @Override
    public void close() {
        dispose();
    }

    private void onDocumentChanged(DocumentChangeEvent event) {
        long nextRevision = revision.incrementAndGet();
        int dirtyStartLine = document.getLineForOffset(Math.min(event.offset(), document.length()));
        enqueueLazy(dirtyStartLine, nextRevision, languageId, false, debounceMillis);
    }

    private void enqueue(
        String textSnapshot,
        int dirtyStartLine,
        long targetRevision,
        String targetLanguageId,
        boolean forceFullRelex,
        long delayMillis
    ) {
        synchronized (lock) {
            if (disposed) {
                return;
            }
            int mergedDirtyStart = pendingRequest == null
                ? dirtyStartLine
                : Math.min(pendingRequest.dirtyStartLine(), dirtyStartLine);
            boolean mergedForceFullRelex = forceFullRelex
                || (pendingRequest != null && pendingRequest.forceFullRelex());

            if (pendingRequest != null && targetRevision < pendingRequest.revision()) {
                return;
            }

            pendingRequest = new PendingRequest(
                textSnapshot,
                mergedDirtyStart,
                targetRevision,
                targetLanguageId,
                mergedForceFullRelex
            );
            scheduleLocked(delayMillis);
        }
    }

    /**
     * Enqueues a lazy pending request that defers the document text snapshot
     * until the debounce fires, avoiding O(n) getText() on every keystroke.
     */
    private void enqueueLazy(
        int dirtyStartLine,
        long targetRevision,
        String targetLanguageId,
        boolean forceFullRelex,
        long delayMillis
    ) {
        synchronized (lock) {
            if (disposed) {
                return;
            }
            int mergedDirtyStart = pendingRequest == null
                ? dirtyStartLine
                : Math.min(pendingRequest.dirtyStartLine(), dirtyStartLine);
            boolean mergedForceFullRelex = forceFullRelex
                || (pendingRequest != null && pendingRequest.forceFullRelex());

            if (pendingRequest != null && targetRevision < pendingRequest.revision()) {
                return;
            }

            // Use null textSnapshot to signal lazy snapshot
            pendingRequest = new PendingRequest(
                null,
                mergedDirtyStart,
                targetRevision,
                targetLanguageId,
                mergedForceFullRelex
            );
            scheduleLocked(delayMillis);
        }
    }

    private void scheduleLocked(long delayMillis) {
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }
        scheduledTask = worker.schedule(this::processPending, Math.max(0, delayMillis), TimeUnit.MILLISECONDS);
    }

    private void processPending() {
        PendingRequest request;
        TokenMap baseline;
        synchronized (lock) {
            if (disposed || pendingRequest == null) {
                return;
            }
            request = pendingRequest;
            pendingRequest = null;
            baseline = request.forceFullRelex() ? TokenMap.empty() : tokenMap;
        }

        // Clear any stale interrupt flag left by cancel(true) targeting a prior
        // scheduled task.  Without this, the interrupt can leak into a newer
        // request that was picked up by the same single-thread executor, causing
        // a spurious CancellationException that silently drops the request.
        Thread.interrupted();

        // Resolve lazy text snapshot (deferred from change events)
        String textSnapshot = request.textSnapshot();
        if (textSnapshot == null) {
            textSnapshot = document.getText();
        }
        List<String> lines = IncrementalLexerEngine.splitLines(textSnapshot);

        TokenMap computed;
        try {
            Lexer lexer = lexerResolver.apply(request.languageId());
            computed = IncrementalLexerEngine.relex(
                baseline,
                lines,
                request.dirtyStartLine(),
                lexer
            );
        } catch (CancellationException cancellationException) {
            scheduleNextIfNeeded();
            return;
        } catch (Exception exception) {
            LOGGER.log(
                System.Logger.Level.WARNING,
                "Lexer failure for languageId=" + request.languageId()
                    + ", revision=" + request.revision()
                    + ", dirtyStartLine=" + request.dirtyStartLine()
                    + "; applying plain-text fallback",
                exception
            );
            try {
                computed = IncrementalLexerEngine.relex(
                    TokenMap.empty(),
                    lines,
                    0,
                    PLAIN_TEXT_FALLBACK_LEXER
                );
            } catch (Exception fallbackException) {
                LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Plain-text lexer fallback failed for revision=" + request.revision()
                        + ", keeping previous tokens",
                    fallbackException
                );
                scheduleNextIfNeeded();
                return;
            }
        }

        if (request.revision() != revision.get() || disposed) {
            scheduleNextIfNeeded();
            return;
        }

        TokenMap tokenMapToApply = computed;
        fxDispatcher.accept(() -> applyIfCurrent(request, tokenMapToApply));
        scheduleNextIfNeeded();
    }

    private void applyIfCurrent(PendingRequest request, TokenMap computed) {
        if (disposed) {
            return;
        }
        if (request.revision() != revision.get()) {
            return;
        }
        tokenMap = computed;
        tokenMapConsumer.accept(computed);
    }

    private void scheduleNextIfNeeded() {
        synchronized (lock) {
            if (disposed || pendingRequest == null) {
                return;
            }
            scheduleLocked(0);
        }
    }

    private static void dispatchOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
            return;
        }
        Platform.runLater(runnable);
    }

    private record PendingRequest(
        String textSnapshot,
        int dirtyStartLine,
        long revision,
        String languageId,
        boolean forceFullRelex
    ) {
    }
}
