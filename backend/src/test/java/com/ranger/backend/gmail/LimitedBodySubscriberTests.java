package com.ranger.backend.gmail;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

class LimitedBodySubscriberTests {
    static final class Subscription implements Flow.Subscription {
        boolean cancelled=false; long requests=0;
        public void request(long n){requests+=n;}
        public void cancel(){cancelled=true;}
    }
    @Test void permitsBytesUpToTheLimit() {
        var subscriber=new LimitedBodySubscriber(4);var sub=new Subscription();subscriber.onSubscribe(sub);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2}),ByteBuffer.wrap(new byte[]{3,4})));
        subscriber.onComplete();
        assertArrayEquals(new byte[]{1,2,3,4},subscriber.getBody().toCompletableFuture().join());
        assertFalse(sub.cancelled);assertEquals(2,sub.requests);
    }
    @Test void cancelsBeforeBufferingAnOversizedResponse() {
        var subscriber=new LimitedBodySubscriber(4);var sub=new Subscription();subscriber.onSubscribe(sub);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2,3}),ByteBuffer.wrap(new byte[]{4,5})));
        assertTrue(sub.cancelled);
        assertThrows(CompletionException.class,()->subscriber.getBody().toCompletableFuture().join());
    }
}
