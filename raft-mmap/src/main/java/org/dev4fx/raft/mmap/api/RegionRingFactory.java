/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hover-raft (tools4j), Anton Anufriev, Marco Terzer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.dev4fx.raft.mmap.api;

import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Region ring factory.
 */
@FunctionalInterface
public interface RegionRingFactory {
    /**
     * Creates array of regions based on the length of the array and size of a region.
     * @param ringSize
     * @param regionSize
     * @param fileChannelSupplier
     * @param fileSizeEnsurer
     * @param mapMode
     * @return array of Regions
     */
    Region[] create(int ringSize,
                    int regionSize,
                    Supplier<FileChannel> fileChannelSupplier,
                    FileSizeEnsurer fileSizeEnsurer,
                    FileChannel.MapMode mapMode);

    /**
     * Should be invoked once all regions are created.
     * For async regions, it will register async processors to be polled
     * in a separate thread.
     */
    default void onComplete() {}

    static <T extends AsyncRegion> RegionRingFactory forAsync(final RegionFactory<T> regionFactory,
                                                              final Consumer<Processor> processorConsumer,
                                                              final Runnable onComplete) {
        Objects.requireNonNull(regionFactory);
        Objects.requireNonNull(processorConsumer);
        Objects.requireNonNull(onComplete);

        return new RegionRingFactory() {
            @Override
            public Region[] create(final int ringSize, final int regionSize, final Supplier<FileChannel> fileChannelSupplier, final FileSizeEnsurer fileSizeEnsurer, final FileChannel.MapMode mapMode) {
                final AsyncRegion[] regions = new AsyncRegion[ringSize];

                for (int i = 0; i < ringSize; i++) {
                    regions[i] = regionFactory.create(regionSize, fileChannelSupplier, fileSizeEnsurer, mapMode);
                }

                processorConsumer.accept(() -> {
                    boolean processed = false;
                    for (final Processor region : regions) {
                        processed |= region.process();
                    }
                    return processed;
                });
                return regions;
            }

            @Override
            public void onComplete() {
                onComplete.run();
            }
        };
    }

    static <T extends Region> RegionRingFactory forSync(final RegionFactory<T> regionFactory) {
        Objects.requireNonNull(regionFactory);

        return (ringSize, regionSize, fileChannelSupplier, fileSizeEnsurer, mapMode) -> {
            final Region[] regions = new Region[ringSize];

            for (int i = 0; i < ringSize; i++) {
                regions[i] = regionFactory.create(regionSize, fileChannelSupplier, fileSizeEnsurer, mapMode);
            }
            return regions;
        };
    }

}
