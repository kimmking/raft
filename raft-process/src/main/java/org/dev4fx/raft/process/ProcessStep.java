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
package org.dev4fx.raft.process;

import java.util.Objects;

public interface ProcessStep {
    ProcessStep NO_OP = () -> false;

    boolean execute();

    default boolean finalise() {
        return !execute();
    }

    default ProcessStep then(final ProcessStep nextStep) {
        Objects.requireNonNull(nextStep);
        return () -> {
            final boolean workDone = execute();
            return workDone | nextStep.execute();
        };
    }

    default ProcessStep thenIfWorkDone(final ProcessStep nextStep) {
        Objects.requireNonNull(nextStep);
        return () -> {
            final boolean workDone = execute();
            return workDone && nextStep.execute();
        };
    }

    default ProcessStep thenIfWorkNotDone(final ProcessStep nextStep) {
        Objects.requireNonNull(nextStep);
        return () -> {
            final boolean workDone = execute();
            return workDone || nextStep.execute();
        };
    }

    static ProcessStep finalisable(final ProcessStep step) {
        return step::execute;
    }

    static ProcessStep nonFinalisable(final ProcessStep step) {
        return new ProcessStep() {
            @Override
            public boolean execute() {
                return step.execute();
            }

            @Override
            public boolean finalise() {
                step.execute();
                return true;
            }
        };
    }
}
