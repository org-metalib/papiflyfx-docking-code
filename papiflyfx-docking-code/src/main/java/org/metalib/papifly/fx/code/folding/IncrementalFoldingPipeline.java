package org.metalib.papifly.fx.code.folding;

import javafx.application.Platform;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeEvent;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.IncrementalLexerEngine;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class IncrementalFoldingPipeline implements AutoCloseable {

    public static final long DEFAULT_DEBOUNCE_MILLIS = 35;

    private final Document document;
    private final Supplier<TokenMap> tokenMapSupplier;
    private final Consumer<FoldMap> foldMapConsumer;
    private final Consumer<Runnable> fxDispatcher;
    private final Function<String, FoldProvider> providerResolver;
    private final ScheduledExecutorService worker;
    private final long debounceMillis;
    private final AtomicLong revision = new AtomicLong();
    private final Object lock = new Object();
    private final DocumentChangeListener documentChangeListener = this::onDocumentChanged;

    private volatile FoldMap foldMap = FoldMap.empty();
    private volatile String languageId = PlainTextLexer.LANGUAGE_ID;
    private volatile boolean disposed;
    private volatile Set<Integer> collapsedHeaderLines = Set.of();

    private PendingRequest pendingRequest;
    private ScheduledFuture<?> scheduledTask;

    public IncrementalFoldingPipeline(
        Document document,
        Supplier<TokenMap> tokenMapSupplier,
        Consumer<FoldMap> foldMapConsumer
    ) {
        this(
            document,
            tokenMapSupplier,
            foldMapConsumer,
            IncrementalFoldingPipeline::dispatchOnFxThread,
            languageId -> LanguageSupportRegistry.defaultRegistry().resolveFoldProvider(languageId),
            DEFAULT_DEBOUNCE_MILLIS
        );
    }

    IncrementalFoldingPipeline(
        Document document,
        Supplier<TokenMap> tokenMapSupplier,
        Consumer<FoldMap> foldMapConsumer,
        Consumer<Runnable> fxDispatcher,
        Function<String, FoldProvider> providerResolver,
        long debounceMillis
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.tokenMapSupplier = Objects.requireNonNull(tokenMapSupplier, "tokenMapSupplier");
        this.foldMapConsumer = Objects.requireNonNull(foldMapConsumer, "foldMapConsumer");
        this.fxDispatcher = Objects.requireNonNull(fxDispatcher, "fxDispatcher");
        this.providerResolver = Objects.requireNonNull(providerResolver, "providerResolver");
        this.debounceMillis = Math.max(0, debounceMillis);
        this.worker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "papiflyfx-folding-pipeline");
            thread.setDaemon(true);
            return thread;
        });
        this.document.addChangeListener(documentChangeListener);
        enqueue(document.getText(), 0, revision.get(), languageId, false, 0);
    }

    public FoldMap getFoldMap() {
        return foldMap;
    }

    public void setLanguageId(String languageId) {
        String normalized = LanguageSupportRegistry.defaultRegistry().normalizeLanguageId(languageId);
        this.languageId = normalized;
        long nextRevision = revision.incrementAndGet();
        enqueue(document.getText(), 0, nextRevision, normalized, true, debounceMillis);
    }

    public void setCollapsedHeaders(Set<Integer> collapsedHeaders) {
        Set<Integer> normalized = collapsedHeaders == null ? Set.of() : Set.copyOf(collapsedHeaders);
        this.collapsedHeaderLines = normalized;
        FoldMap updated = foldMap.withCollapsedHeaders(normalized);
        if (updated == foldMap) {
            return;
        }
        foldMap = updated;
        fxDispatcher.accept(() -> {
            if (!disposed) {
                foldMapConsumer.accept(updated);
            }
        });
    }

    @Override
    public void close() {
        dispose();
    }

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
        boolean forceFullRecompute,
        long delayMillis
    ) {
        synchronized (lock) {
            if (disposed) {
                return;
            }
            int mergedDirty = pendingRequest == null
                ? dirtyStartLine
                : Math.min(pendingRequest.dirtyStartLine(), dirtyStartLine);
            boolean mergedForce = forceFullRecompute
                || (pendingRequest != null && pendingRequest.forceFullRecompute());
            if (pendingRequest != null && targetRevision < pendingRequest.revision()) {
                return;
            }
            pendingRequest = new PendingRequest(
                textSnapshot,
                mergedDirty,
                targetRevision,
                targetLanguageId,
                mergedForce
            );
            scheduleLocked(delayMillis);
        }
    }

    private void enqueueLazy(
        int dirtyStartLine,
        long targetRevision,
        String targetLanguageId,
        boolean forceFullRecompute,
        long delayMillis
    ) {
        synchronized (lock) {
            if (disposed) {
                return;
            }
            int mergedDirty = pendingRequest == null
                ? dirtyStartLine
                : Math.min(pendingRequest.dirtyStartLine(), dirtyStartLine);
            boolean mergedForce = forceFullRecompute
                || (pendingRequest != null && pendingRequest.forceFullRecompute());
            if (pendingRequest != null && targetRevision < pendingRequest.revision()) {
                return;
            }
            pendingRequest = new PendingRequest(
                null,
                mergedDirty,
                targetRevision,
                targetLanguageId,
                mergedForce
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
        FoldMap baseline;
        synchronized (lock) {
            if (disposed || pendingRequest == null) {
                return;
            }
            request = pendingRequest;
            pendingRequest = null;
            baseline = request.forceFullRecompute() ? FoldMap.empty() : foldMap;
        }
        String textSnapshot = request.textSnapshot();
        if (textSnapshot == null) {
            textSnapshot = document.getText();
        }
        List<String> lines = IncrementalLexerEngine.splitLines(textSnapshot);
        FoldMap computed;
        try {
            FoldProvider provider = providerResolver.apply(request.languageId());
            computed = provider.recompute(
                lines,
                tokenMapSupplier.get(),
                baseline,
                request.dirtyStartLine(),
                Thread.currentThread()::isInterrupted
            );
        } catch (CancellationException cancellationException) {
            scheduleNextIfNeeded();
            return;
        } catch (Exception exception) {
            computed = FoldMap.empty();
        }
        computed = computed.withCollapsedHeaders(collapsedHeaderLines);
        if (request.revision() != revision.get() || disposed) {
            scheduleNextIfNeeded();
            return;
        }
        FoldMap foldMapToApply = computed;
        fxDispatcher.accept(() -> applyIfCurrent(request, foldMapToApply));
        scheduleNextIfNeeded();
    }

    private void applyIfCurrent(PendingRequest request, FoldMap computed) {
        if (disposed) {
            return;
        }
        if (request.revision() != revision.get()) {
            return;
        }
        foldMap = computed;
        foldMapConsumer.accept(computed);
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
        boolean forceFullRecompute
    ) {
    }
}

