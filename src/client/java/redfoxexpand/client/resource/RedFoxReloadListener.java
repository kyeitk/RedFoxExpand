package redfoxexpand.client.resource;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Two-stage reload: prepare an immutable generation, then atomically install it. */
public final class RedFoxReloadListener implements PreparableReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("redfoxexpand/resource");
    private final SnapshotLoader loader = new SnapshotLoader();

    @Override
    public CompletableFuture<Void> reload(SharedState state, Executor prepareExecutor,
                                          PreparationBarrier barrier, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> loader.load(state.resourceManager()), prepareExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(snapshot -> {
                    SnapshotService.install(snapshot);
                    ResourceSnapshot.LoadReport report = snapshot.report();
                    LOGGER.info("Installed RedFoxExpand generation {}: {} active definitions, {} textures, {} warnings, {} errors",
                            snapshot.generation(), report.activeDefinitions(), report.validatedTextures(),
                            report.warnings(), report.errors());
                    for (String message : report.messages()) {
                        if (message.startsWith("ERROR")) LOGGER.error(message);
                        else LOGGER.warn(message);
                    }
                }, applyExecutor)
                .exceptionally(error -> {
                    LOGGER.error("RedFoxExpand reload failed before commit; retaining generation {}",
                            SnapshotService.current().generation(), error);
                    return null;
                });
    }
}
