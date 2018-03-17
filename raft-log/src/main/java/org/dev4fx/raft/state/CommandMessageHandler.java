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
package org.dev4fx.raft.state;

import org.agrona.DirectBuffer;
import org.dev4fx.raft.sbe.CommandRequestDecoder;
import org.dev4fx.raft.sbe.MessageHeaderDecoder;

import java.util.Objects;

public class CommandMessageHandler implements MessageHandler {
    private final StateMachine stateMachine;
    private final CommandRequestDecoder commandRequestDecoder;
    private final MessageHeaderDecoder messageHeaderDecoder;

    public CommandMessageHandler(final StateMachine stateMachine) {
        this.stateMachine = Objects.requireNonNull(stateMachine);
        this.commandRequestDecoder = new CommandRequestDecoder();
        this.messageHeaderDecoder = new MessageHeaderDecoder();
    }

    @Override
    public void onMessage(final DirectBuffer source, final int offset, final int length) {
        messageHeaderDecoder.wrap(source, offset);
        final int templateId = messageHeaderDecoder.templateId();
        final int headerLenght = messageHeaderDecoder.encodedLength();
        switch (templateId) {
            case CommandRequestDecoder.TEMPLATE_ID :
                commandRequestDecoder.wrap(source,headerLenght + offset,
                        CommandRequestDecoder.BLOCK_LENGTH,
                        CommandRequestDecoder.SCHEMA_VERSION);
                stateMachine.onCommand(commandRequestDecoder.sourceId(), commandRequestDecoder.sequence(),
                        commandRequestDecoder.buffer(),
                        commandRequestDecoder.limit() + CommandRequestDecoder.payloadHeaderLength(),
                        commandRequestDecoder.payloadLength());
                break;
        }
    }
}
