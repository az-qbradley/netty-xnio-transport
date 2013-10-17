package org.jboss.netty.xnio;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.AbstractEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;
import org.xnio.XnioExecutor;
import org.xnio.XnioIoThread;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public final class XnioEventLoop extends AbstractEventExecutor implements EventLoop {
    private final XnioIoThread executor;
    public XnioEventLoop(XnioIoThread executor) {
        this.executor = executor;
    }
    @Override
    public void shutdown() {
        // Not supported
    }

    @Override
    public EventLoopGroup parent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return thread == executor;
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return channel.newFailedFuture(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return promise.setFailure(new UnsupportedOperationException());
    }

    @Override
    public boolean isShuttingDown() {
        return executor.getWorker().isShutdown();
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return newFailedFuture(new UnsupportedOperationException());
    }

    @Override
    public Future<?> terminationFuture() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isShutdown() {
        return executor.getWorker().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.getWorker().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.getWorker().awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledFutureWrapper wrapper = new ScheduledFutureWrapper(command, delay, unit);
        wrapper.key = executor.executeAfter(wrapper, delay, unit);
        return wrapper;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(callable, delay, unit);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public XnioEventLoop next() {
        return this;
    }

    private static final class ScheduledFutureWrapper<V> extends DefaultPromise<V> implements ScheduledFuture<V>, Runnable {
        private XnioExecutor.Key key;
        private final Runnable task;
        private final long delay;
        private final TimeUnit unit;
        ScheduledFutureWrapper(Runnable task, long delay, TimeUnit unit) {
            this.task = task;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delay, this.unit);
        }

        @Override
        public int compareTo(Delayed o) {
            long d = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
            if (d < 0) {
                return -1;
            } else if (d > 0) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public void run() {
            try {
                task.run();
                trySuccess(null);
            } catch (Throwable t) {
                tryFailure(t);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (setUncancellable() && key.remove()) {
                return true;
            }
            return false;
        }
    }

}
